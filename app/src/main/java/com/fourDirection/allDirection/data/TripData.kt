package com.fourDirection.allDirection.data

import java.time.LocalDate
import java.util.UUID

data class Trip(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val dayPlans: Map<LocalDate, DayPlan> = emptyMap()
)

data class DayPlan(
    val date: LocalDate,
    val description: String = "",
    val location: String = "",
    val events: List<String> = emptyList()
)
