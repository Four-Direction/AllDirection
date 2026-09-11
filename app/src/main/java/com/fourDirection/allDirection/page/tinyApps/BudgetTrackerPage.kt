package com.fourDirection.allDirection.page.tinyApps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fourDirection.allDirection.data.BudgetPlan
import com.fourDirection.allDirection.data.Trip

enum class BudgetPlannerView {
    SELECTION,
    LIST,
    TRIP_SELECTOR,
    EDITOR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetTrackerPage(onDismiss: () -> Unit) {
    var currentView by remember { mutableStateOf(BudgetPlannerView.SELECTION) }
    var editingPlanState by remember { mutableStateOf<BudgetPlan?>(null) }
    var selectedTripForNewPlan by remember { mutableStateOf<Trip?>(null) }
    var showQuitConfirmation by remember { mutableStateOf(value = false) }

    val viewModel: BudgetViewModel = viewModel()

    // Handle internal back navigation
    BackHandler(enabled = currentView != BudgetPlannerView.SELECTION) {
        if (currentView == BudgetPlannerView.EDITOR) {
            showQuitConfirmation = true
        } else {
            when (currentView) {
                BudgetPlannerView.LIST -> currentView = BudgetPlannerView.SELECTION
                BudgetPlannerView.TRIP_SELECTOR -> currentView = BudgetPlannerView.LIST
                else -> {}
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .safeDrawingPadding(),
    ) {
        when (currentView) {
            BudgetPlannerView.SELECTION -> {
                BudgetSelectionView(
                    onDismiss = onDismiss,
                    onPlannerClick = { currentView = BudgetPlannerView.LIST },
                    onTrackerClick = { /* Handle tracker click */ }
                )
            }
            BudgetPlannerView.LIST -> {
                BudgetPlannerListPage(
                    viewModel = viewModel,
                    onBack = { currentView = BudgetPlannerView.SELECTION },
                    onAddClick = { currentView = BudgetPlannerView.TRIP_SELECTOR },
                    onPlanClick = { plan ->
                        editingPlanState = plan
                        currentView = BudgetPlannerView.EDITOR
                    }
                )
            }
            BudgetPlannerView.TRIP_SELECTOR -> {
                BudgetTripSelector(
                    onBack = { currentView = BudgetPlannerView.LIST },
                    plannedTripIds = viewModel.budgetPlans.asSequence().mapNotNull { it.tripId }.toSet(),
                    onTripSelected = { trip ->
                        selectedTripForNewPlan = trip
                        editingPlanState = null
                        currentView = BudgetPlannerView.EDITOR
                    },
                    onManualTripAdd = { _, _, _ -> }
                )
            }
            BudgetPlannerView.EDITOR -> {
                BudgetEditorPage(
                    viewModel = viewModel,
                    initialPlan = editingPlanState,
                    newTrip = selectedTripForNewPlan,
                    onBack = { showQuitConfirmation = true },
                    onPlanChanged = { editingPlanState = it },
                    onDone = { plan ->
                        viewModel.saveBudgetPlan(plan)
                        editingPlanState = null
                        currentView = BudgetPlannerView.LIST
                    }
                )
            }
        }

        if (showQuitConfirmation) {
            AlertDialog(
                onDismissRequest = { showQuitConfirmation = false },
                containerColor = Color(0xFF1A1A1A),
                title = { Text("Unsaved Changes", color = Color.White) },
                text = { Text("Do you want to save your progress before quitting?", color = Color.White.copy(alpha = 0.7f)) },
                confirmButton = {
                    TextButton(onClick = {
                        editingPlanState?.let { viewModel.saveBudgetPlan(it) }
                        showQuitConfirmation = false
                        editingPlanState = null
                        currentView = BudgetPlannerView.LIST
                    }) { Text("Save", color = Color(0xFF81D4FA)) }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showQuitConfirmation = false
                        editingPlanState = null
                        currentView = BudgetPlannerView.LIST
                    }) { Text("Discard", color = Color.White.copy(alpha = 0.6f)) }
                }
            )
        }
    }
}

@Composable
fun BudgetSelectionView(
    onDismiss: () -> Unit,
    onPlannerClick: () -> Unit,
    onTrackerClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                text = "Budget",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(0.4f))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                BudgetButton(
                    icon = Icons.Default.CalendarToday,
                    label = "Planner",
                    modifier = Modifier.weight(1f),
                    onClick = onPlannerClick
                )
                BudgetButton(
                    icon = Icons.Default.Assessment,
                    label = "Tracker",
                    modifier = Modifier.weight(1f),
                    onClick = onTrackerClick
                )
            }
            
            Spacer(modifier = Modifier.weight(0.6f))
        }
    }
}

@Composable
fun BudgetButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val lightBlue = Color(0xFF81D4FA)
    
    Surface(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        shape = MaterialTheme.shapes.large,
        color = lightBlue.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, lightBlue.copy(alpha = 0.3f))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = lightBlue,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                color = lightBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
