package com.fourDirection.allDirection.page.main

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fourDirection.allDirection.data.DayPlan
import com.fourDirection.allDirection.data.Trip
import com.fourDirection.allDirection.ui.theme.GlowBlue
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarPage(
    viewModel: CalendarViewModel = viewModel()
) {
    val context = LocalContext.current
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    val today = remember { LocalDate.now() }
    var showDatePicker by remember { mutableStateOf(false) }

    // Trip Planning State
    var rangeStart by remember { mutableStateOf<LocalDate?>(null) }
    var rangeEnd by remember { mutableStateOf<LocalDate?>(null) }
    val trips = viewModel.trips
    var selectedTrip by remember { mutableStateOf<Trip?>(null) }
    var isCreationMode by remember { mutableStateOf(false) }
    val isLoading by viewModel.isLoading.collectAsState()

    // Handle error events
    LaunchedEffect(Unit) {
        viewModel.errorEvents.collectLatest { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
        }
    }

    // Synchronize selectedTrip with ViewModel list updates
    LaunchedEffect(trips) {
        selectedTrip?.let { current ->
            selectedTrip = trips.find { it.id == current.id }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // --- HEADER ---
                AnimatedVisibility(
                    visible = selectedTrip == null,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Previous Month",
                                tint = Color.White
                            )
                        }

                        Text(
                            text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showDatePicker = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Next Month",
                                tint = Color.White
                            )
                        }
                    }
                }

                if (selectedTrip == null) {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // --- WEEKDAY LABELS ---
                val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                Row(modifier = Modifier.fillMaxWidth()) {
                    daysOfWeek.forEach { day ->
                        Text(
                            text = day,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // --- CALENDAR GRID ---
                val firstDayOfMonth = currentMonth.atDay(1)
                val dayOfWeekOffset = firstDayOfMonth.dayOfWeek.value % 7 // Sunday = 0, Monday = 1...
                val daysInMonth = currentMonth.lengthOfMonth()
                
                val totalSlots = dayOfWeekOffset + daysInMonth

                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    contentPadding = PaddingValues(bottom = 200.dp) // Leave extra space for overlay and dock
                ) {
                    items(totalSlots) { index ->
                        if (index >= dayOfWeekOffset) {
                            val dayOfMonth = index - dayOfWeekOffset + 1
                            val date = currentMonth.atDay(dayOfMonth)
                            
                            val isToday = date == today
                            val isSelected = date == rangeStart || date == rangeEnd
                            val isInRange = rangeStart != null && rangeEnd != null && 
                                            !date.isBefore(rangeStart) && !date.isAfter(rangeEnd)
                            
                            val tripForDate = trips.find { !date.isBefore(it.startDate) && !date.isAfter(it.endDate) }

                            CalendarCell(
                                day = dayOfMonth.toString(),
                                isToday = isToday,
                                isSelected = isSelected,
                                isInRange = isInRange,
                                hasTrip = tripForDate != null,
                                onClick = {
                                    if (isCreationMode) {
                                        if (rangeStart == null || (rangeStart != null && rangeEnd != null)) {
                                            rangeStart = date
                                            rangeEnd = null
                                        } else if (date.isBefore(rangeStart)) {
                                            rangeStart = date
                                        } else {
                                            rangeEnd = date
                                        }
                                    } else if (tripForDate != null) {
                                        selectedTrip = tripForDate
                                    }
                                }
                            )
                        } else {
                            Box(modifier = Modifier.aspectRatio(1f))
                        }
                    }
                }
            }

            // --- PLAN TRIP FAB ---
            if (!isCreationMode && selectedTrip == null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 24.dp, bottom = 120.dp)
                ) {
                    ExtendedFloatingActionButton(
                        onClick = { isCreationMode = true },
                        containerColor = GlowBlue,
                        contentColor = Color.Black,
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text("Plan Trip", fontWeight = FontWeight.Bold) },
                        shape = CircleShape
                    )
                }
            }

            // --- TRIP CREATION OVERLAY ---
            AnimatedVisibility(
                visible = isCreationMode,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp), // Lift above navigation dock
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF1A1A1A),
                    tonalElevation = 8.dp,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "New Trip Details",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            DateDisplayBox(label = "Start", date = rangeStart, modifier = Modifier.weight(1f))
                            DateDisplayBox(label = "End", date = rangeEnd, modifier = Modifier.weight(1f))
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        var tripName by remember { mutableStateOf("") }
                        
                        OutlinedTextField(
                            value = tripName,
                            onValueChange = { tripName = it },
                            placeholder = { Text("Trip Name (e.g. Paris Summer)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = GlowBlue,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            ),
                            singleLine = true
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = {
                                if (tripName.isNotBlank() && rangeStart != null && rangeEnd != null) {
                                    val newTrip = Trip(
                                        name = tripName,
                                        startDate = rangeStart!!,
                                        endDate = rangeEnd!!
                                    )
                                    viewModel.saveTrip(newTrip)
                                    selectedTrip = newTrip
                                    isCreationMode = false
                                    rangeStart = null
                                    rangeEnd = null
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GlowBlue,
                                disabledContainerColor = Color.White.copy(alpha = 0.12f),
                                disabledContentColor = Color.White.copy(alpha = 0.38f)
                            ),
                            enabled = tripName.isNotBlank() && rangeStart != null && rangeEnd != null && !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black)
                            } else {
                                Text(
                                    text = "Create Trip",
                                    fontWeight = FontWeight.Bold,
                                    color = if (tripName.isNotBlank() && rangeStart != null && rangeEnd != null) Color.Black else Color.Unspecified
                                )
                            }
                        }
                    }
                }
            }

            // --- TRIP VIEW / EDITOR ---
            AnimatedVisibility(
                visible = selectedTrip != null,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                if (selectedTrip != null) {
                    TripDetailView(
                        trip = selectedTrip!!,
                        onDismiss = { selectedTrip = null },
                        onUpdateTrip = { updatedTrip ->
                            viewModel.saveTrip(updatedTrip)
                        },
                        onDeleteTrip = {
                            viewModel.deleteTrip(selectedTrip!!.id)
                            selectedTrip = null
                        }
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        MonthYearPicker(
            currentMonth = currentMonth,
            onMonthSelected = {
                currentMonth = it
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

@Composable
fun MonthYearPicker(
    currentMonth: YearMonth,
    onMonthSelected: (YearMonth) -> Unit,
    onDismiss: () -> Unit
) {
    val years = remember { 
        val now = LocalDate.now().year
        (now - 5..now + 25).toList()
    }
    
    var selectedYear by remember { mutableIntStateOf(currentMonth.year) }
    val listState = rememberLazyListState()

    // Scroll to center the selected year on initial open
    LaunchedEffect(Unit) {
        val index = years.indexOf(selectedYear)
        if (index >= 0) {
            // Roughly center the year in the LazyRow
            listState.scrollToItem(index)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1A1A1A),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Jump to Date",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Year Selector
                LazyRow(
                    state = listState,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(years) { year ->
                        val isSelected = year == selectedYear
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) GlowBlue else Color.White.copy(alpha = 0.05f))
                                .clickable { selectedYear = year }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = year.toString(),
                                color = if (isSelected) Color.Black else Color.White,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Month Selector
                val months = Month.entries.toTypedArray()
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.height(240.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(months) { month ->
                        val isSelected = month == currentMonth.month && selectedYear == currentMonth.year
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) GlowBlue else Color.White.copy(alpha = 0.05f))
                                .clickable { 
                                    onMonthSelected(YearMonth.of(selectedYear, month))
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                                color = if (isSelected) Color.Black else Color.White,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(contentColor = GlowBlue)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
fun DateDisplayBox(label: String, date: LocalDate?, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = label, color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = 0.05f),
            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Text(
                text = date?.toString() ?: "Pick a date",
                color = if (date != null) Color.White else Color.White.copy(alpha = 0.3f),
                modifier = Modifier.padding(8.dp),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun CalendarCell(
    day: String,
    isToday: Boolean,
    isSelected: Boolean = false,
    isInRange: Boolean = false,
    hasTrip: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        // Range Background
        if (isInRange) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f)
                    .background(
                        color = GlowBlue.copy(alpha = 0.15f),
                        shape = if (isSelected) RoundedCornerShape(50) else RoundedCornerShape(0)
                    )
            )
        }

        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .padding(4.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isSelected -> GlowBlue
                        isToday -> GlowBlue.copy(alpha = 0.2f)
                        else -> Color.Transparent
                    }
                )
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = day,
                    color = when {
                        isSelected -> Color.Black
                        isToday -> GlowBlue
                        else -> Color.White
                    },
                    fontSize = 16.sp,
                    fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
                )
                if (hasTrip && !isSelected) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(GlowBlue)
                    )
                }
            }
        }
    }
}

@Composable
fun TripDetailView(
    trip: Trip,
    onDismiss: () -> Unit,
    onUpdateTrip: (Trip) -> Unit,
    onDeleteTrip: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.5f), // Lowered from 0.6f to show more calendar
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = Color(0xFF121212),
        tonalElevation = 8.dp,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = trip.name,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${trip.startDate} - ${trip.endDate}",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 10.sp
                    )
                }
                Row {
                    IconButton(onClick = onDeleteTrip) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // Day List (Grid)
            val days = remember(trip) {
                var current = trip.startDate
                val list = mutableListOf<LocalDate>()
                while (!current.isAfter(trip.endDate)) {
                    list.add(current)
                    current = current.plusDays(1)
                }
                list
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(days) { date ->
                    val dayPlan = trip.dayPlans[date] ?: DayPlan(date = date)
                    DayPlanCard(
                        dayPlan = dayPlan,
                        onUpdate = { updatedPlan ->
                            val newPlans = trip.dayPlans.toMutableMap()
                            newPlans[date] = updatedPlan
                            onUpdateTrip(trip.copy(dayPlans = newPlans))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DayPlanCard(
    dayPlan: DayPlan,
    onUpdate: (DayPlan) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${dayPlan.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())}, ${dayPlan.date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${dayPlan.date.dayOfMonth}",
                    color = GlowBlue,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { isEditing = !isEditing }, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = if (isEditing) GlowBlue else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (isEditing) {
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = dayPlan.location,
                    onValueChange = { onUpdate(dayPlan.copy(location = it)) },
                    label = { Text("Location") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = GlowBlue
                    ),
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = dayPlan.description,
                    onValueChange = { onUpdate(dayPlan.copy(description = it)) },
                    label = { Text("Daily Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = GlowBlue
                    )
                )
            } else {
                if (dayPlan.location.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = dayPlan.location, color = Color.White, fontSize = 14.sp)
                    }
                }
                
                if (dayPlan.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = dayPlan.description, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                }
                
                if (dayPlan.location.isBlank() && dayPlan.description.isBlank()) {
                    Text(text = "No plans yet. Tap edit to add.", color = Color.White.copy(alpha = 0.3f), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}
