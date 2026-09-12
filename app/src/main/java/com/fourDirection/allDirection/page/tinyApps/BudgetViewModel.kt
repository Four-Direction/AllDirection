package com.fourDirection.allDirection.page.tinyApps

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fourDirection.allDirection.api.CurrencyApiService
import com.fourDirection.allDirection.data.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class BudgetViewModel : ViewModel() {
    private val userRepository = UserRepository()
    private val currencyApiService = CurrencyApiService()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _budgetPlans = mutableStateListOf<BudgetPlan>()
    val budgetPlans: List<BudgetPlan> get() = _budgetPlans

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _exchangeRates = MutableStateFlow<Map<String, Double>>(emptyMap())
    val exchangeRates = _exchangeRates.asStateFlow()

    private val _transactions = mutableStateListOf<Transaction>()
    val transactions: List<Transaction> get() = _transactions

    private val _uiEvents = MutableSharedFlow<String>()
    val uiEvents = _uiEvents.asSharedFlow()

    private var transactionsListener: ListenerRegistration? = null

    init {
        loadBudgetPlans()
        loadCustomCategories()
    }

    private val _customCategories = mutableStateListOf<String>()
    val customCategories: List<String> get() = _customCategories

    private fun loadCustomCategories() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val cats = userRepository.getCustomCategories(uid)
            _customCategories.clear()
            _customCategories.addAll(cats)
        }
    }

    fun addCustomCategory(category: String) {
        val uid = auth.currentUser?.uid ?: return
        if (category.isBlank() || _customCategories.contains(category) || DefaultCategories.list.contains(category)) return
        _customCategories.add(category)
        viewModelScope.launch {
            try {
                userRepository.saveCustomCategory(uid, category)
            } catch (e: Exception) {
                Log.e("BudgetViewModel", "Error saving category", e)
            }
        }
    }

    fun loadBudgetPlans() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val loadedPlans = userRepository.getBudgetPlans(uid)
                _budgetPlans.clear()
                _budgetPlans.addAll(loadedPlans.sortedBy { it.startDate })
            } catch (e: Exception) {
                Log.e("BudgetViewModel", "Error loading plans", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveBudgetPlan(plan: BudgetPlan) {
        val uid = auth.currentUser?.uid ?: return
        val index = _budgetPlans.indexOfFirst { it.id == plan.id }
        if (index != -1) _budgetPlans[index] = plan else _budgetPlans.add(plan)
        _budgetPlans.sortBy { it.startDate }
        viewModelScope.launch {
            try {
                _isLoading.value = true
                userRepository.saveBudgetPlan(uid, plan)
                loadBudgetPlans()
            } catch (e: Exception) {
                Log.e("BudgetViewModel", "Error saving budget plan", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchRates(baseCurrency: String) {
        viewModelScope.launch {
            try {
                val rates = currencyApiService.fetchLatestRates(baseCurrency)
                _exchangeRates.value = rates
            } catch (e: Exception) {
                Log.e("BudgetViewModel", "Error fetching rates", e)
            }
        }
    }

    fun startTransactionsListener(planId: String) {
        val uid = auth.currentUser?.uid ?: return
        transactionsListener?.remove()
        _transactions.clear()
        transactionsListener = db.collection("users").document(uid)
            .collection("budgetPlans").document(planId)
            .collection("transactions")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    viewModelScope.launch { _uiEvents.emit("Sync Error: ${e.localizedMessage}") }
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val loaded = snapshot.documents.mapNotNull { doc ->
                        try {
                            Transaction(
                                id = doc.getString("id") ?: doc.id,
                                planId = doc.getString("planId") ?: planId,
                                name = doc.getString("name") ?: "Unnamed",
                                amount = (doc.get("amount") as? Number)?.toDouble() ?: 0.0,
                                category = doc.getString("category") ?: "Other",
                                date = LocalDate.parse(doc.getString("date") ?: LocalDate.now().toString()),
                                individualId = doc.getString("individualId")
                            )
                        } catch (_: Exception) { null }
                    }
                    _transactions.clear()
                    _transactions.addAll(loaded.sortedWith(compareByDescending<Transaction> { it.date }.thenByDescending { it.id }))
                }
            }
    }

    fun stopTransactionsListener() {
        transactionsListener?.remove()
        transactionsListener = null
    }

    fun addTransaction(transaction: Transaction) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                userRepository.saveTransaction(uid, transaction)
                _uiEvents.emit("Spending Saved!")
            } catch (e: Exception) {
                val msg = if (e.localizedMessage?.contains("permission") == true) 
                    "System sync error. Please try again." else "Could not save spending."
                _uiEvents.emit(msg)
                Log.e("BudgetViewModel", "Save error", e)
            }
        }
    }

    fun removeTransaction(planId: String, transactionId: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                userRepository.deleteTransaction(uid, planId, transactionId)
                _uiEvents.emit("Entry Removed")
            } catch (e: Exception) {
                _uiEvents.emit("Delete failed")
            }
        }
    }

    fun calculateAllowanceForDay(
        plan: BudgetPlan, 
        individualId: String?, 
        targetDate: LocalDate = LocalDate.now()
    ): Double {
        val tripDays = (ChronoUnit.DAYS.between(plan.startDate, plan.endDate) + 1).coerceAtLeast(1)
        val targetId = if (individualId == null || individualId == "null" || individualId.isEmpty()) "main_user" else individualId

        val individualBudget = if (plan.isGroup && plan.groupType == GroupBudgetType.DIFFERENT_BUDGET) {
            plan.individualBudgets.find { it.id == targetId }?.amount ?: 0.0
        } else {
            plan.individualBudgets.firstOrNull()?.amount ?: 0.0
        }

        val dailyTarget = individualBudget / tripDays
        val previousSpent = transactions
            .filter { 
                val isPersonMatch = (it.individualId ?: "main_user") == targetId
                isPersonMatch && it.date.isBefore(targetDate) && it.date.isAfter(plan.startDate.minusDays(1)) 
            }
            .sumOf { it.amount }
            
        val daysPassed = ChronoUnit.DAYS.between(plan.startDate, targetDate).coerceAtLeast(0)
        val carryOver = (dailyTarget * daysPassed) - previousSpent
        return dailyTarget + carryOver
    }

    fun calculateDaysRemaining(startDate: LocalDate): Long {
        return ChronoUnit.DAYS.between(LocalDate.now(), startDate)
    }

    override fun onCleared() {
        stopTransactionsListener()
        super.onCleared()
    }
}
