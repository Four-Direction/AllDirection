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
    val locations: List<String> = emptyList(),
    val events: List<String> = emptyList(),
    val hotel: String? = null,
    val startLocation: String? = null,
    val endLocation: String? = null
)
