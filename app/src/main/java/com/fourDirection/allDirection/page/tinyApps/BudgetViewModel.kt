package com.fourDirection.allDirection.page.tinyApps

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fourDirection.allDirection.api.CurrencyApiService
import com.fourDirection.allDirection.data.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class BudgetViewModel : ViewModel() {
    private val userRepository = UserRepository()
    private val currencyApiService = CurrencyApiService()
    private val auth = FirebaseAuth.getInstance()

    private val _budgetPlans = mutableStateListOf<BudgetPlan>()
    val budgetPlans: List<BudgetPlan> get() = _budgetPlans

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _exchangeRates = MutableStateFlow<Map<String, Double>>(emptyMap())
    val exchangeRates = _exchangeRates.asStateFlow()

    init {
        loadBudgetPlans()
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
        
        // Optimistic UI update
        val index = _budgetPlans.indexOfFirst { it.id == plan.id }
        if (index != -1) {
            _budgetPlans[index] = plan
        } else {
            _budgetPlans.add(plan)
        }
        _budgetPlans.sortBy { it.startDate }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                userRepository.saveBudgetPlan(uid, plan)
                // Refresh to ensure sync
                val loadedPlans = userRepository.getBudgetPlans(uid)
                _budgetPlans.clear()
                _budgetPlans.addAll(loadedPlans.sortedBy { it.startDate })
            } catch (e: Exception) {
                Log.e("BudgetViewModel", "Error saving budget plan", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    @Suppress("unused")
    fun deleteBudgetPlan(planId: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                userRepository.deleteBudgetPlan(uid, planId)
                _budgetPlans.removeIf { it.id == planId }
            } catch (e: Exception) {
                Log.e("BudgetViewModel", "Error deleting budget plan", e)
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

    fun calculateDaysRemaining(startDate: LocalDate): Long {
        return ChronoUnit.DAYS.between(LocalDate.now(), startDate)
    }

    @Suppress("unused")
    fun getExchangedAmount(amount: Double, rate: Double): Double {
        return amount * rate
    }

    @Suppress("unused")
    fun calculateDailyBudget(totalBudget: Double, startDate: LocalDate, endDate: LocalDate): Double {
        val days = (ChronoUnit.DAYS.between(startDate, endDate) + 1).coerceAtLeast(1)
        return totalBudget / days
    }
}
