package com.fourDirection.allDirection.page.tinyApps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fourDirection.allDirection.data.BudgetPlan
import com.fourDirection.allDirection.ui.theme.GlowBlue
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun BudgetTrackerListPage(
    viewModel: BudgetViewModel,
    onBack: () -> Unit,
    onPlanClick: (BudgetPlan) -> Unit
) {
    val plans = viewModel.budgetPlans
    val isLoading by viewModel.isLoading.collectAsState()
    val lightBlue = Color(0xFF81D4FA)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                text = "Spending Tracker",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = lightBlue, strokeWidth = 2.dp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Select an active trip to log spending", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.weight(1f)) {
            if (plans.isEmpty() && !isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No budgets found", color = Color.White.copy(alpha = 0.4f), fontSize = 16.sp)
                    Text("Create a plan first", color = Color.White.copy(alpha = 0.3f), fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    // Show live trips first
                    items(plans.sortedByDescending { 
                        val now = LocalDate.now()
                        !now.isBefore(it.startDate) && !now.isAfter(it.endDate)
                    }, key = { it.id }) { plan ->
                        val now = LocalDate.now()
                        val isOngoing = !now.isBefore(plan.startDate) && !now.isAfter(plan.endDate)
                        TrackerPlanItem(
                            plan = plan,
                            isOngoing = isOngoing,
                            onClick = { onPlanClick(plan) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TrackerPlanItem(
    plan: BudgetPlan,
    isOngoing: Boolean,
    onClick: () -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = if (isOngoing) 0.12f else 0.05f),
        border = BorderStroke(1.dp, if (isOngoing) GlowBlue.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = plan.tripName,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${plan.startDate.format(dateFormatter)} - ${plan.endDate.format(dateFormatter)}",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp
                    )
                }
            }

            if (isOngoing) {
                Surface(
                    color = GlowBlue.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "LIVE",
                        color = GlowBlue,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
