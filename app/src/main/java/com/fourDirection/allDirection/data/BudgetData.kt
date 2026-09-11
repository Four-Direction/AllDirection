package com.fourDirection.allDirection.data

import java.time.LocalDate
import java.util.UUID

data class BudgetPlan(
    val id: String = UUID.randomUUID().toString(),
    val tripId: String? = null,
    val tripName: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val baseCurrency: String = "USD",
    val exchangeCurrency: String = "USD",
    val isLocalTrip: Boolean = false,
    val isGroup: Boolean = false,
    val groupType: GroupBudgetType = GroupBudgetType.INDIVIDUAL,
    val individualBudgets: List<IndividualBudget> = emptyList(),
    val expenses: List<ExpenseItem> = emptyList(),
    val customCategories: List<String> = emptyList()
)

enum class GroupBudgetType {
    INDIVIDUAL,
    SAME_BUDGET,
    DIFFERENT_BUDGET
}

data class IndividualBudget(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val amount: Double = 0.0,
    val isAmountInExchangeCurrency: Boolean = false
)

data class ExpenseItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val amount: Double,
    val category: String,
    val individualId: String? = null
)

object DefaultCategories {
    val list = listOf(
        "Food & Drinks",
        "Entertainment",
        "Emergency",
        "Tickets",
        "Accommodation",
        "Transport",
        "Shopping"
    )
}
