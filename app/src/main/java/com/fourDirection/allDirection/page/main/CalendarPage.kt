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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fourDirection.allDirection.data.DayPlan
import com.fourDirection.allDirection.data.Trip
import com.fourDirection.allDirection.ui.theme.GlowBlue
import com.mapbox.search.result.SearchSuggestion
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarPage(
    viewModel: CalendarViewModel = viewModel(),
    onLocationClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val accessToken = "pk.eyJ1IjoiamFuZGRpIiwiYSI6ImNtdG9qYmx1ejB1cTEyd29majMxYzRvenMifQ.MHg_MphmkzDyLjIYLdLnmQ"
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
    var editingDayPlan by remember { mutableStateOf<DayPlan?>(null) }

    // Handle error events
    LaunchedEffect(Unit) {
        viewModel.initSearchEngine(accessToken)
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
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { 
                                    isCreationMode = false
                                    rangeStart = null
                                    rangeEnd = null
                                },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = MaterialTheme.shapes.medium,
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Text("Cancel")
                            }

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
                                modifier = Modifier.weight(1.5f).height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GlowBlue,
                                    disabledContainerColor = Color.White.copy(alpha = 0.12f),
                                    disabledContentColor = Color.White.copy(alpha = 0.38f)
                                ),
                                enabled = tripName.isNotBlank() && rangeStart != null && rangeEnd != null && !isLoading
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black)
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
                        onEditDay = { editingDayPlan = it },
                        onLocationClick = onLocationClick,
                        onDeleteTrip = {
                            viewModel.deleteTrip(selectedTrip!!.id)
                            selectedTrip = null
                        }
                    )
                }
            }

            // --- DAY PLAN EDIT DIALOG ---
            if (editingDayPlan != null && selectedTrip != null) {
                DayPlanEditDialog(
                    dayPlan = editingDayPlan!!,
                    viewModel = viewModel,
                    onDismiss = { editingDayPlan = null },
                    onLocationClick = onLocationClick,
                    onSave = { updatedPlan ->
                        val newPlans = selectedTrip!!.dayPlans.toMutableMap()
                        newPlans[updatedPlan.date] = updatedPlan
                        val updatedTrip = selectedTrip!!.copy(dayPlans = newPlans)
                        viewModel.saveTrip(updatedTrip)
                        editingDayPlan = null
                    }
                )
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
    onEditDay: (DayPlan) -> Unit,
    onLocationClick: (String) -> Unit = {},
    onDeleteTrip: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.45f), // Slightly lower to show more calendar
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
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(days) { date ->
                    val dayPlan = trip.dayPlans[date] ?: DayPlan(date = date)
                    DayPlanCard(
                        dayPlan = dayPlan,
                        onEditClick = { onEditDay(dayPlan) }
                    )
                }
            }
        }
    }
}

@Composable
fun DayPlanCard(
    dayPlan: DayPlan,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onEditClick() }
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = "${dayPlan.date.dayOfMonth} ${dayPlan.date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())}",
                    color = GlowBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                if (dayPlan.locations.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        dayPlan.locations.take(2).forEach { loc ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(10.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = loc, 
                                    color = Color.White, 
                                    fontSize = 10.sp, 
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (dayPlan.locations.size > 2) {
                            Text("+${dayPlan.locations.size - 2} more", color = GlowBlue, fontSize = 8.sp)
                        }
                    }
                }
                
                if (dayPlan.description.isNotBlank()) {
                    Text(
                        text = dayPlan.description, 
                        color = Color.White.copy(alpha = 0.7f), 
                        fontSize = 9.sp, 
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun DayPlanEditDialog(
    dayPlan: DayPlan,
    viewModel: CalendarViewModel,
    onDismiss: () -> Unit,
    onLocationClick: (String) -> Unit = {},
    onSave: (DayPlan) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var locations by remember { mutableStateOf(dayPlan.locations) }
    var newLocationQuery by remember { mutableStateOf("") }
    var description by remember { mutableStateOf(dayPlan.description) }
    val suggestions by viewModel.suggestions.collectAsState()
    var showSuggestions by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1A1A1A),
            tonalElevation = 8.dp,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${dayPlan.date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())}, ${dayPlan.date.dayOfMonth} ${dayPlan.date.month.getDisplayName(TextStyle.FULL, Locale.getDefault())}",
                        color = GlowBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Column(modifier = Modifier.weight(1f)) {
                    if (!isEditing) {
                        // VIEW MODE
                        if (locations.isEmpty() && description.isBlank()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No plans for today.", color = Color.White.copy(alpha = 0.3f))
                            }
                        } else {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                if (locations.isNotEmpty()) {
                                    Text("Locations", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    locations.forEach { loc ->
                                        Surface(
                                            onClick = { onLocationClick(loc) },
                                            color = Color.White.copy(alpha = 0.05f),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = GlowBlue, modifier = Modifier.size(20.dp))
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(text = loc, color = Color.White, fontSize = 15.sp)
                                            }
                                        }
                                    }
                                }

                                if (description.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Text("Daily Notes", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = description,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        lineHeight = 22.sp
                                    )
                                }
                            }
                        }
                    } else {
                        // EDIT MODE
                        // Current Locations List
                        if (locations.isNotEmpty()) {
                            Text("Locations", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            locations.forEach { loc ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = GlowBlue, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = loc, color = Color.White, fontSize = 14.sp)
                                    }
                                    IconButton(onClick = { locations = locations.filter { it != loc } }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Add New Location
                        Box {
                            OutlinedTextField(
                                value = newLocationQuery,
                                onValueChange = { 
                                    newLocationQuery = it
                                    viewModel.onSearchQueryChanged(it)
                                    showSuggestions = it.isNotBlank()
                                },
                                label = { Text("Add Location") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = GlowBlue
                                ),
                                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) }
                            )

                            if (showSuggestions && suggestions.isNotEmpty()) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 200.dp)
                                        .padding(top = 64.dp), // Adjust to appear below textfield
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF222222),
                                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f)),
                                    tonalElevation = 8.dp
                                ) {
                                    LazyColumn {
                                        items(suggestions) { suggestion ->
                                            ListItem(
                                                headlineContent = { Text(suggestion.name, color = Color.White, fontSize = 14.sp) },
                                                supportingContent = { Text(suggestion.descriptionText ?: "", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp) },
                                                modifier = Modifier.clickable {
                                                    viewModel.selectSuggestion(suggestion) { selectedName ->
                                                        if (!locations.contains(selectedName)) {
                                                            locations = locations + selectedName
                                                        }
                                                        newLocationQuery = ""
                                                        showSuggestions = false
                                                    }
                                                },
                                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Daily Notes") },
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = GlowBlue
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { 
                        if (isEditing) {
                            onSave(dayPlan.copy(locations = locations, description = description)) 
                        } else {
                            isEditing = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GlowBlue),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = if (isEditing) "Save Details" else "Change Details",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
