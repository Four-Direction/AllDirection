package com.fourDirection.allDirection.page.tinyApps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fourDirection.allDirection.data.BudgetPlan
import java.time.format.DateTimeFormatter

@Composable
fun BudgetPlannerListPage(
    viewModel: BudgetViewModel,
    onBack: () -> Unit,
    onAddClick: () -> Unit,
    onPlanClick: (BudgetPlan) -> Unit,
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
                text = "Budget Planner",
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

        Box(modifier = Modifier.weight(1f)) {
            if (plans.isEmpty() && !isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White.copy(alpha = 0.2f), modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No planned trips yet", color = Color.White.copy(alpha = 0.4f), fontSize = 16.sp)
                    Text("Tap 'Add' to start planning", color = Color.White.copy(alpha = 0.3f), fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(plans, key = { it.id }) { plan ->
                        BudgetPlanItem(
                            plan = plan,
                            daysLeft = viewModel.calculateDaysRemaining(plan.startDate)
                        ) { onPlanClick(plan) }
                    }
                }
            }

            // Add Button at the bottom
            Button(
                onClick = onAddClick,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = lightBlue.copy(alpha = 0.2f)),
                border = BorderStroke(1.dp, lightBlue.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = lightBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add New Trip", color = lightBlue, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun BudgetPlanItem(
    plan: BudgetPlan,
    daysLeft: Long,
    onClick: () -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    val lightBlue = Color(0xFF81D4FA)

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
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
                Spacer(modifier = Modifier.height(6.dp))
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

            // Decorative Countdown Badge
            Surface(
                color = when {
                    daysLeft > 7 -> lightBlue.copy(alpha = 0.15f)
                    (daysLeft in 0..7) -> Color.Yellow.copy(alpha = 0.15f)
                    else -> Color.Gray.copy(alpha = 0.15f)
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.wrapContentSize()
            ) {
                Text(
                    text = when {
                        daysLeft > 0 -> "$daysLeft days left"
                        daysLeft == 0L -> "Today!"
                        else -> "Past"
                    },
                    color = when {
                        daysLeft > 7 -> lightBlue
                        daysLeft in 0..7 -> Color.Yellow
                        else -> Color.Gray
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}
