package com.fourDirection.allDirection.data

import java.time.LocalDate
import java.util.UUID

enum class GroupBudgetType {
    INDIVIDUAL, EQUAL, RATIO
}

data class IndividualBudget(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val amount: Double,
    val isAmountInExchangeCurrency: Boolean = false
)

data class ExpenseItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val amount: Double,
    val category: String,
    val individualId: String? = null
)

data class BudgetPlan(
    val id: String = UUID.randomUUID().toString(),
    val tripId: String? = null,
    val tripName: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val baseCurrency: String,
    val exchangeCurrency: String,
    val isLocalTrip: Boolean = false,
    val isGroup: Boolean = false,
    val groupType: GroupBudgetType = GroupBudgetType.INDIVIDUAL,
    val individualBudgets: List<IndividualBudget> = emptyList(),
    val expenses: List<ExpenseItem> = emptyList(),
    val customCategories: List<String> = emptyList()
)
