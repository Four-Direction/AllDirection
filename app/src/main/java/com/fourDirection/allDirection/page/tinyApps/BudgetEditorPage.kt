package com.fourDirection.allDirection.page.tinyApps

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fourDirection.allDirection.data.*
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetEditorPage(
    viewModel: BudgetViewModel,
    initialPlan: BudgetPlan?,
    newTrip: Trip?,
    onBack: () -> Unit,
    onPlanChanged: (BudgetPlan) -> Unit,
    onDone: (BudgetPlan) -> Unit
) {
    val lightBlue = Color(0xFF81D4FA)
    
    // Use keys for remember to ensure state is reset when editing a different plan
    val planKey = remember(initialPlan?.id, newTrip?.id) { initialPlan?.id ?: newTrip?.id ?: "new" }

    // Plan Basic Info
    val planId = remember(planKey) { initialPlan?.id ?: UUID.randomUUID().toString() }
    val tripId = remember(planKey) { initialPlan?.tripId ?: newTrip?.id }
    val tripName = remember(planKey) { initialPlan?.tripName ?: newTrip?.name ?: "New Trip" }
    val startDate = remember(planKey) { initialPlan?.startDate ?: newTrip?.startDate ?: LocalDate.now() }
    val endDate = remember(planKey) { initialPlan?.endDate ?: newTrip?.endDate ?: LocalDate.now().plusDays(1) }
    
    // Section 1: Currency
    var baseCurrency by remember(planKey) { mutableStateOf(initialPlan?.baseCurrency ?: "USD") }
    var exchangeCurrency by remember(planKey) { mutableStateOf(initialPlan?.exchangeCurrency ?: "EUR") }
    var isLocalTrip by remember(planKey) { mutableStateOf(initialPlan?.isLocalTrip ?: false) }
    
    // Section 2: Budget Type & Individuals
    var isGroup by remember(planKey) { mutableStateOf(initialPlan?.isGroup ?: false) }
    var groupType by remember(planKey) { mutableStateOf(initialPlan?.groupType ?: GroupBudgetType.INDIVIDUAL) }
    
    val individualBudgets = remember(planKey) { 
        mutableStateListOf<IndividualBudget>().apply {
            if (initialPlan != null && (initialPlan.individualBudgets.isNotEmpty())) {
                addAll(initialPlan.individualBudgets)
            } else {
                add(IndividualBudget(id = "main_user", name = "You"))
            }
        }
    }
    
    // Section 3: Expenses
    val expenses = remember(planKey) { 
        mutableStateListOf<ExpenseItem>().apply {
            initialPlan?.let { addAll(it.expenses) }
        }
    }
    
    // Custom Categories from ViewModel (Globally synced)
    val globalCustomCategories = viewModel.customCategories

    // Notify parent of changes to allow safety-saving
    fun triggerPlanChanged() {
        onPlanChanged(BudgetPlan(
            id = planId,
            tripId = tripId,
            tripName = tripName,
            startDate = startDate,
            endDate = endDate,
            baseCurrency = baseCurrency,
            exchangeCurrency = exchangeCurrency,
            isLocalTrip = isLocalTrip,
            isGroup = isGroup,
            groupType = groupType,
            individualBudgets = individualBudgets.toList(),
            expenses = expenses.toList(),
            customCategories = globalCustomCategories.toList() // Sync global ones
        ))
    }

    // Chart Display State
    var showDailyBudget by remember { mutableStateOf(value = false) }
    var showInExchangeCurrency by remember { mutableStateOf(value = false) }
    var showAddExpensePopup by remember { mutableStateOf(value = false) }

    val scrollState = rememberScrollState()

    // Fetch rates when currency changes
    LaunchedEffect(baseCurrency) {
        viewModel.fetchRates(baseCurrency)
    }
    val rates by viewModel.exchangeRates.collectAsState()
    val rate = rates[exchangeCurrency] ?: 1.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp)
            .verticalScroll(scrollState)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text("Budget: $tripName", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- SECTION 1: CURRENCY ---
        Text("Currency", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White.copy(alpha = 0.05f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    CurrencySelector(label = "Base", currency = baseCurrency, onCurrencySelected = { baseCurrency = it; triggerPlanChanged() }, modifier = Modifier.weight(1f))
                    if (!isLocalTrip) {
                        CurrencySelector(label = "Exchange", currency = exchangeCurrency, onCurrencySelected = { exchangeCurrency = it; triggerPlanChanged() }, modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                BudgetButtonSmall(label = "Local Trip", isSelected = isLocalTrip, onClick = { isLocalTrip = !isLocalTrip; triggerPlanChanged() }, modifier = Modifier.fillMaxWidth())
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- SECTION 2: BUDGET TYPE ---
        Text("Who's traveling?", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            BudgetButtonSmall(label = "Individual", isSelected = !isGroup, onClick = { isGroup = false; groupType = GroupBudgetType.INDIVIDUAL; triggerPlanChanged() }, modifier = Modifier.weight(1f))
            BudgetButtonSmall(label = "Group", isSelected = isGroup, onClick = { isGroup = true; if(groupType == GroupBudgetType.INDIVIDUAL) groupType = GroupBudgetType.SAME_BUDGET; triggerPlanChanged() }, modifier = Modifier.weight(1f))
        }

        if (isGroup) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BudgetButtonSmall(label = "Same Budget", isSelected = groupType == GroupBudgetType.SAME_BUDGET, onClick = { groupType = GroupBudgetType.SAME_BUDGET; triggerPlanChanged() }, modifier = Modifier.weight(1f))
                BudgetButtonSmall(label = "Different Budget", isSelected = groupType == GroupBudgetType.DIFFERENT_BUDGET, onClick = { groupType = GroupBudgetType.DIFFERENT_BUDGET; triggerPlanChanged() }, modifier = Modifier.weight(1f))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            if (individualBudgets.size < 2 && isGroup) {
                individualBudgets.clear()
                individualBudgets.add(IndividualBudget(id = "main_user", name = "You"))
                individualBudgets.add(IndividualBudget(name = "Person 2"))
                triggerPlanChanged()
            }
            
            if (groupType == GroupBudgetType.SAME_BUDGET) {
               BudgetInputItem(
                   individual = individualBudgets[0], 
                   rate = rate, 
                   currency = exchangeCurrency, 
                   isLocal = isLocalTrip, 
                   onUpdate = { updated -> 
                       individualBudgets[0] = updated
                       // Apply to all in same budget mode
                       for (i in 1 until individualBudgets.size) {
                           individualBudgets[i] = individualBudgets[i].copy(amount = updated.amount)
                       }
                       triggerPlanChanged()
                   }, 
                   onDelete = null
               )
               Spacer(modifier = Modifier.height(8.dp))
               Text("This budget applies to all ${individualBudgets.size} people.", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
            } else {
                individualBudgets.forEachIndexed { index, individual ->
                    BudgetInputItem(
                        individual = individual, 
                        rate = rate, 
                        currency = exchangeCurrency, 
                        isLocal = isLocalTrip, 
                        onUpdate = { individualBudgets[index] = it; triggerPlanChanged() }, 
                        onDelete = if (individualBudgets.size > 2) ({ individualBudgets.removeAt(index); triggerPlanChanged() }) else null
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Button(onClick = { individualBudgets.add(IndividualBudget(name = "Person ${individualBudgets.size + 1}")); triggerPlanChanged() }, colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent), contentPadding = PaddingValues(0.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = lightBlue, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Person", color = lightBlue, fontSize = 14.sp)
                }
            }
        } else {
            Spacer(modifier = Modifier.height(16.dp))
            BudgetInputItem(
                individual = individualBudgets[0], 
                rate = rate, 
                currency = exchangeCurrency, 
                isLocal = isLocalTrip, 
                onUpdate = { individualBudgets[0] = it; triggerPlanChanged() }, 
                onDelete = null
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- SECTION 3: VISUALIZATION ---
        Text("Expense Breakdown", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
        Spacer(modifier = Modifier.height(12.dp))
        
        if (isGroup && groupType == GroupBudgetType.DIFFERENT_BUDGET) {
            // Multiple Pie Charts
            individualBudgets.forEach { individual ->
                var isExpanded by remember { mutableStateOf(false) }
                val personExpenses = expenses.filter { it.individualId == individual.id }
                
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).animateContentSize(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.05f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(individual.name, color = lightBlue, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { isExpanded = !isExpanded }, modifier = Modifier.size(24.dp)) {
                                Icon(
                                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                        }
                        
                        if (isExpanded) {
                            Spacer(modifier = Modifier.height(16.dp))
                            ExpenseVisualizationRow(
                                expenses = personExpenses,
                                baseCurrency = baseCurrency,
                                exchangeCurrency = exchangeCurrency,
                                rate = rate,
                                showDailyBudget = showDailyBudget,
                                showInExchangeCurrency = showInExchangeCurrency,
                                startDate = startDate,
                                endDate = endDate,
                                onToggleDaily = { showDailyBudget = it },
                                onToggleCurrency = { showInExchangeCurrency = !showInExchangeCurrency }
                            )
                        }
                    }
                }
            }
        } else {
            // Single Pie Chart
            ExpenseVisualizationRow(
                expenses = expenses,
                baseCurrency = baseCurrency,
                exchangeCurrency = exchangeCurrency,
                rate = rate,
                showDailyBudget = showDailyBudget,
                showInExchangeCurrency = showInExchangeCurrency,
                startDate = startDate,
                endDate = endDate,
                onToggleDaily = { showDailyBudget = it },
                onToggleCurrency = { showInExchangeCurrency = !showInExchangeCurrency }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { showAddExpensePopup = true },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = lightBlue.copy(alpha = 0.2f)),
            border = BorderStroke(1.dp, lightBlue.copy(alpha = 0.5f))
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = lightBlue)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Item", color = lightBlue, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = { 
                if (tripName.isBlank()) return@Button
                
                onDone(BudgetPlan(
                    id = planId,
                    tripId = tripId,
                    tripName = tripName,
                    startDate = startDate,
                    endDate = endDate,
                    baseCurrency = baseCurrency,
                    exchangeCurrency = exchangeCurrency,
                    isLocalTrip = isLocalTrip,
                    isGroup = isGroup,
                    groupType = groupType,
                    individualBudgets = individualBudgets.toList(),
                    expenses = expenses.toList(),
                    customCategories = globalCustomCategories.toList()
                ))
            },
            modifier = Modifier.align(Alignment.CenterHorizontally).width(160.dp).height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = lightBlue,
                disabledContainerColor = Color.Gray
            ),
            enabled = tripName.isNotBlank()
        ) {
            Text("Done", color = Color.Black, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(120.dp))
    }

    if (showAddExpensePopup) {
        AddExpenseDialog(
            categories = DefaultCategories.list + globalCustomCategories,
            individuals = if (isGroup && groupType == GroupBudgetType.DIFFERENT_BUDGET) individualBudgets else emptyList(),
            onDismiss = { showAddExpensePopup = false },
            onAddExpense = { expenses.add(it); triggerPlanChanged() },
            onAddCategory = { viewModel.addCustomCategory(it); triggerPlanChanged() }
        )
    }
}

@Composable
fun ExpenseVisualizationRow(
    expenses: List<ExpenseItem>,
    baseCurrency: String,
    exchangeCurrency: String,
    rate: Double,
    showDailyBudget: Boolean,
    showInExchangeCurrency: Boolean,
    startDate: LocalDate,
    endDate: LocalDate,
    onToggleDaily: (Boolean) -> Unit,
    onToggleCurrency: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        PieChart(
            expenses = expenses,
            modifier = Modifier.size(160.dp),
            showExchanged = showInExchangeCurrency,
            rate = rate,
            showDailyBudget = showDailyBudget,
            startDate = startDate,
            endDate = endDate
        )
        Spacer(modifier = Modifier.width(24.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BudgetButtonCircle(label = "Trip", isSelected = !showDailyBudget, onClick = { onToggleDaily(false) })
            BudgetButtonCircle(label = "Day", isSelected = showDailyBudget, onClick = { onToggleDaily(true) })
            BudgetButtonCircle(
                label = if(showInExchangeCurrency) exchangeCurrency else baseCurrency, 
                isSelected = showInExchangeCurrency, 
                onClick = onToggleCurrency
            )
        }
    }
}

@Composable
fun BudgetInputItem(
    individual: IndividualBudget,
    rate: Double,
    currency: String,
    isLocal: Boolean,
    onUpdate: (IndividualBudget) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (individual.name != "You") {
                    Text(individual.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                if (onDelete != null) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Remove", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                    }
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = if(individual.amount == 0.0) "" else individual.amount.toString(),
                    onValueChange = { onUpdate(individual.copy(amount = it.toDoubleOrNull() ?: 0.0)) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Enter Budget", color = Color.White.copy(alpha = 0.3f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                if (!isLocal) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "≈ ${String.format(Locale.US, "%.2f", individual.amount * rate)} $currency",
                        color = Color(0xFF81D4FA),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun PieChart(
    expenses: List<ExpenseItem>,
    modifier: Modifier = Modifier,
    showExchanged: Boolean,
    rate: Double,
    showDailyBudget: Boolean = false,
    startDate: LocalDate = LocalDate.now(),
    endDate: LocalDate = LocalDate.now().plusDays(1)
) {
    val categoryTotals = expenses.groupBy { it.category }
        .mapValues { (_, items) -> 
            val totalInBase = items.sumOf { it.amount }
            if (showExchanged) totalInBase * rate else totalInBase
        }
    
    val total = categoryTotals.values.sum()
    val colors = listOf(
        Color(0xFFFF5252), Color(0xFFFFEB3B), Color(0xFF4CAF50), Color(0xFF2196F3),
        Color(0xFFFF9800), Color(0xFF9C27B0), Color(0xFF00BCD4)
    )

    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (total == 0.0) {
                drawCircle(color = Color.White.copy(alpha = 0.1f), style = Stroke(width = 20.dp.toPx()))
            } else {
                var startAngle = -90f
                categoryTotals.entries.forEachIndexed { index, entry ->
                    val sweepAngle = (entry.value / total).toFloat() * 360f
                    drawArc(
                        color = colors[index % colors.size],
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round)
                    )
                    startAngle += sweepAngle
                }
            }
        }
        
        val days = (ChronoUnit.DAYS.between(startDate, endDate) + 1).coerceAtLeast(1)
        val displayAmount = if (showDailyBudget) total / days else total
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = String.format(Locale.US, "%.0f", displayAmount),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            if (showDailyBudget) {
                Text("per day", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseDialog(
    categories: List<String>,
    individuals: List<IndividualBudget>,
    onDismiss: () -> Unit,
    onAddExpense: (ExpenseItem) -> Unit,
    onAddCategory: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull() ?: "Other") }
    var selectedIndividualId by remember { mutableStateOf(individuals.firstOrNull()?.id) }
    var showNewCategoryInput by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        title = { Text("Add Expense", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("What is it?") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount (Base)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                
                if (individuals.isNotEmpty()) {
                    Text("Who is spending?", color = Color.White.copy(alpha = 0.6f))
                    var indExpanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(onClick = { indExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(individuals.find { it.id == selectedIndividualId }?.name ?: "Select Person", color = Color.White)
                        }
                        DropdownMenu(expanded = indExpanded, onDismissRequest = { indExpanded = false }) {
                            individuals.forEach {
                                DropdownMenuItem(text = { Text(it.name) }, onClick = { selectedIndividualId = it.id; indExpanded = false })
                            }
                        }
                    }
                }

                Text("Category", color = Color.White.copy(alpha = 0.6f))
                Box {
                    var catExpanded by remember { mutableStateOf(false) }
                    OutlinedButton(onClick = { catExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(selectedCategory, color = Color.White)
                    }
                    DropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }, modifier = Modifier.heightIn(max = 250.dp)) {
                        categories.forEach {
                            DropdownMenuItem(text = { Text(it) }, onClick = { selectedCategory = it; catExpanded = false })
                        }
                        DropdownMenuItem(text = { Text("+ New Category") }, onClick = { showNewCategoryInput = true; catExpanded = false })
                    }
                }
                
                if (showNewCategoryInput) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextField(value = newCategoryName, onValueChange = { newCategoryName = it }, label = { Text("New Category") }, modifier = Modifier.weight(1f))
                        IconButton(onClick = { 
                            if(newCategoryName.isNotBlank()) {
                                onAddCategory(newCategoryName)
                                selectedCategory = newCategoryName
                                showNewCategoryInput = false
                            }
                        }) { Icon(Icons.Default.Check, contentDescription = null, tint = Color.Green) }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull()
                    if (name.isNotBlank() && amt != null) {
                        onAddExpense(ExpenseItem(name = name, amount = amt, category = selectedCategory, individualId = selectedIndividualId))
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF81D4FA))
            ) { Text("Add", color = Color.Black) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Color.White.copy(alpha = 0.6f)) } }
    )
}

@Composable
fun CurrencySelector(
    label: String,
    currency: String,
    onCurrencySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }
    val touristCurrencies = listOf(
        "USD", "EUR", "GBP", "JPY", "AUD", "CAD", "CHF", "CNY", "HKD", "INR",
        "NZD", "BRL", "ZAR", "TRY", "KRW", "SGD", "MXN", "MYR", "IDR", "PHP",
        "THB", "ILS", "AED", "SAR", "VND", "TWD", "NOK", "SEK", "DKK", "PLN"
    )

    Surface(
        onClick = { showPicker = true },
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(label, color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(getFlagEmoji(currency), modifier = Modifier.padding(end = 4.dp))
                Text(currency, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        
        DropdownMenu(
            expanded = showPicker, 
            onDismissRequest = { showPicker = false },
            modifier = Modifier.background(Color(0xFF1A1A1A)).heightIn(max = 400.dp)
        ) {
            touristCurrencies.sorted().forEach { code ->
                DropdownMenuItem(
                    text = { 
                        Row {
                            Text(getFlagEmoji(code))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(code, color = Color.White)
                        }
                    }, 
                    onClick = { 
                        onCurrencySelected(code)
                        showPicker = false 
                    }
                )
            }
        }
    }
}

@Composable
fun BudgetButtonSmall(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lightBlue = Color(0xFF81D4FA)
    Surface(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) lightBlue.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, if (isSelected) lightBlue.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f))
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = label,
                color = if (isSelected) lightBlue else Color.White,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun BudgetButtonCircle(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val lightBlue = Color(0xFF81D4FA)
    Surface(
        onClick = onClick,
        modifier = Modifier.size(48.dp),
        shape = RoundedCornerShape(24.dp),
        color = if (isSelected) lightBlue.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, if (isSelected) lightBlue.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f))
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = label,
                color = if (isSelected) lightBlue else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
    }
}


