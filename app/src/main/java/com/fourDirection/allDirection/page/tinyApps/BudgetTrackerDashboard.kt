package com.fourDirection.allDirection.page.tinyApps

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fourDirection.allDirection.data.*
import com.fourDirection.allDirection.ui.theme.GlowBlue
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.Calendar
import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.clip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetTrackerDashboard(
    viewModel: BudgetViewModel,
    plan: BudgetPlan,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lightBlue = Color(0xFF81D4FA)
    var selectedIndividualId by remember(plan.id) { 
        mutableStateOf(plan.individualBudgets.firstOrNull()?.id ?: "main_user") 
    }
    var showAddSpending by remember { mutableStateOf(false) }
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }
    
    val transactions = viewModel.transactions
    val selectedPerson = plan.individualBudgets.find { it.id == selectedIndividualId } ?: plan.individualBudgets.firstOrNull()
    
    val allCategories = remember(viewModel.customCategories) {
        DefaultCategories.list + viewModel.customCategories
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(plan.baseCurrency) {
        viewModel.fetchRates(plan.baseCurrency)
    }
    val rates by viewModel.exchangeRates.collectAsState()
    val currentRate = rates[plan.exchangeCurrency] ?: 1.0

    val todayAllowance = viewModel.calculateAllowanceForDay(plan, selectedIndividualId)
    val todaySpent = transactions
        .filter { 
            val isPersonMatch = (it.individualId ?: "main_user") == selectedIndividualId
            isPersonMatch && it.date.isEqual(LocalDate.now()) 
        }
        .sumOf { it.amount }
    val remainingToday = todayAllowance - todaySpent

    val totalTripBudget = if (plan.isGroup && plan.groupType == GroupBudgetType.DIFFERENT_BUDGET) {
        selectedPerson?.amount ?: 0.0
    } else {
        plan.individualBudgets.sumOf { it.amount }
    }
    
    val totalSpentSoFar = transactions
        .filter { (it.individualId ?: "main_user") == selectedIndividualId }
        .sumOf { it.amount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Column {
                Text(plan.tripName, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Spending Dashboard", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (plan.isGroup && plan.groupType == GroupBudgetType.DIFFERENT_BUDGET) {
            LazyColumn(modifier = Modifier.heightIn(max = 50.dp)) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        plan.individualBudgets.forEach { person ->
                            FilterChip(
                                selected = selectedIndividualId == person.id,
                                onClick = { selectedIndividualId = person.id ?: "main_user" },
                                label = { Text(person.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = lightBlue.copy(alpha = 0.2f),
                                    selectedLabelColor = lightBlue,
                                    labelColor = Color.White.copy(alpha = 0.6f)
                                )
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Box(
            modifier = Modifier.fillMaxWidth().height(220.dp),
            contentAlignment = Alignment.Center
        ) {
            SpendingPulse(
                target = todayAllowance,
                spent = todaySpent,
                modifier = Modifier.size(190.dp)
            )
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = String.format(Locale.US, "%.0f", remainingToday),
                    color = if (remainingToday >= 0) Color.White else Color(0xFFFF5252),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = if (remainingToday >= 0) "Safe to spend" else "Overspent today",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = Color.White.copy(alpha = 0.05f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatItem(label = "Today's Spent", value = String.format(Locale.US, "%.0f", todaySpent), color = lightBlue)
                    Box(modifier = Modifier.width(1.dp).height(30.dp).background(Color.White.copy(alpha = 0.1f)))
                    StatItem(label = "Trip Spent", value = String.format(Locale.US, "%.0f", totalSpentSoFar), color = Color.White)
                    Box(modifier = Modifier.width(1.dp).height(30.dp).background(Color.White.copy(alpha = 0.1f)))
                    StatItem(label = "Total Budget", value = String.format(Locale.US, "%.0f", totalTripBudget), color = Color.White.copy(alpha = 0.7f))
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                val tripProgress = if (totalTripBudget > 0) (totalSpentSoFar / totalTripBudget).toFloat().coerceIn(0f, 1f) else 0f
                LinearProgressIndicator(
                    progress = { tripProgress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = if (tripProgress > 0.9f) Color(0xFFFF5252) else lightBlue,
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Recent Spending", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Hold an item to remove it", color = lightBlue.copy(alpha = 0.5f), fontSize = 10.sp)
            }
            IconButton(
                onClick = { showAddSpending = true },
                modifier = Modifier.background(lightBlue, CircleShape).size(32.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Spending", tint = Color.Black, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val filteredTransactions = transactions.filter { (it.individualId ?: "main_user") == selectedIndividualId }
            if (filteredTransactions.isEmpty()) {
                item {
                    Text("No spending logged.", color = Color.White.copy(alpha = 0.3f), fontSize = 14.sp, modifier = Modifier.padding(top = 20.dp))
                }
            } else {
                items(filteredTransactions, key = { it.id }) { tx ->
                    TransactionItem(tx = tx, onLongClick = { transactionToDelete = tx })
                }
            }
        }
    }

    if (showAddSpending) {
        AddSpendingDialog(
            plan = plan,
            categories = allCategories,
            individualId = selectedIndividualId,
            exchangeRate = currentRate,
            onDismiss = { showAddSpending = false },
            onSave = { tx ->
                viewModel.addTransaction(tx)
                showAddSpending = false
            }
        )
    }

    transactionToDelete?.let { tx ->
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            containerColor = Color(0xFF1A1A1A),
            title = { Text("Remove Spending?", color = Color.White) },
            text = { Text("This will delete '${tx.name}' from your history.", color = Color.White.copy(alpha = 0.7f)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeTransaction(plan.id, tx.id)
                    transactionToDelete = null
                }) { Text("Delete", color = Color(0xFFFF5252)) }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) { Text("Cancel", color = Color.White) }
            }
        )
    }
}

@Composable
fun SpendingPulse(target: Double, spent: Double, modifier: Modifier = Modifier) {
    val progress = if (target > 0) (spent / target).toFloat().coerceIn(0f, 1.2f) else 0f
    val ringColor = if (progress > 1f) Color(0xFFFF5252) else GlowBlue
    Canvas(modifier = modifier) {
        drawCircle(color = Color.White.copy(alpha = 0.05f), style = Stroke(width = 16.dp.toPx()))
        drawArc(color = ringColor, startAngle = -90f, sweepAngle = (progress.coerceAtMost(1f)) * 360f, useCenter = false, style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round))
        if (progress > 1f) {
            drawArc(color = Color.White.copy(alpha = 0.3f), startAngle = -90f, sweepAngle = (progress - 1f) * 360f, useCenter = false, style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round))
        }
    }
}

@Composable
fun StatItem(modifier: Modifier = Modifier, label: String, value: String, color: Color = Color.White) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
        Text(value, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionItem(tx: Transaction, onLongClick: () -> Unit) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd") }
    val icon = when(tx.category) {
        "Food & Drinks" -> Icons.Default.Restaurant
        "Transport" -> Icons.Default.DirectionsCar
        "Accommodation" -> Icons.Default.Hotel
        "Shopping" -> Icons.Default.LocalMall
        "Entertainment" -> Icons.Default.ConfirmationNumber
        "Tickets" -> Icons.Default.AirplaneTicket
        "Emergency" -> Icons.Default.HealthAndSafety
        else -> Icons.Default.ReceiptLong
    }

    Surface(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = {}, onLongClick = onLongClick),
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.05f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).background(GlowBlue.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = GlowBlue, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(tx.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(tx.category, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                        if (!tx.date.isEqual(LocalDate.now())) {
                            Text(" • ${tx.date.format(dateFormatter)}", color = GlowBlue.copy(alpha = 0.6f), fontSize = 11.sp)
                        }
                    }
                }
            }
            Text("-${String.format(Locale.US, "%.2f", tx.amount)}", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSpendingDialog(
    plan: BudgetPlan,
    categories: List<String>,
    individualId: String?,
    exchangeRate: Double,
    onDismiss: () -> Unit,
    onSave: (Transaction) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull() ?: "Other") }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var useExchangeCurrency by remember { mutableStateOf(false) }
    
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1A),
        title = { Text("Log Spending", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("What did you buy?") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White), singleLine = true)
                
                Column {
                    OutlinedTextField(
                        value = amountText, 
                        onValueChange = { amountText = it }, 
                        label = { Text("Amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        singleLine = true,
                        trailingIcon = {
                            if (!plan.isLocalTrip) {
                                TextButton(onClick = { useExchangeCurrency = !useExchangeCurrency }) {
                                    Text(if (useExchangeCurrency) plan.exchangeCurrency else plan.baseCurrency, color = GlowBlue)
                                }
                            }
                        }
                    )
                    if (useExchangeCurrency && amountText.isNotEmpty()) {
                        val converted = (amountText.replace(",", ".").toDoubleOrNull() ?: 0.0) / exchangeRate
                        Text("≈ " + String.format(Locale.US, "%.2f", converted) + " ${plan.baseCurrency}", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
                    }
                }
                
                Text("Date", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                OutlinedButton(onClick = {
                    val initialDate = if (!selectedDate.isBefore(plan.startDate) && !selectedDate.isAfter(plan.endDate)) selectedDate else LocalDate.now()
                    DatePickerDialog(context, { _, year, month, day -> selectedDate = LocalDate.of(year, month + 1, day) }, initialDate.year, initialDate.monthValue - 1, initialDate.dayOfMonth).show()
                }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (selectedDate.isEqual(LocalDate.now())) "Today" else selectedDate.toString(), color = Color.White)
                }

                if (selectedDate.isAfter(LocalDate.now())) {
                    Text("Note: This will count towards " + selectedDate.toString() + "'s budget.", color = GlowBlue.copy(alpha = 0.8f), fontSize = 11.sp)
                }

                Text("Category", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                var expanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(selectedCategory, color = Color.White) }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.heightIn(max = 200.dp)) {
                        categories.forEach { cat -> DropdownMenuItem(text = { Text(cat) }, onClick = { selectedCategory = cat; expanded = false }) }
                    }
                }
            }
        },
        confirmButton = {
            val amtValue = amountText.replace(",", ".").toDoubleOrNull() ?: 0.0
            val finalAmount = if (useExchangeCurrency) amtValue / exchangeRate else amtValue
            val isValid = name.isNotBlank() && amtValue > 0
            Button(onClick = { if (isValid) onSave(Transaction(planId = plan.id, name = name, amount = finalAmount, category = selectedCategory, date = selectedDate, individualId = individualId ?: "main_user")) }, enabled = isValid, colors = ButtonDefaults.buttonColors(containerColor = GlowBlue, disabledContainerColor = Color.Gray)) { Text("Save", color = if (isValid) Color.Black else Color.White) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Color.White.copy(alpha = 0.6f)) } }
    )
}
