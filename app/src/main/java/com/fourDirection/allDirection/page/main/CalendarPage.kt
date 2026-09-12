package com.fourDirection.allDirection.page.main

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.TripOrigin
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fourDirection.allDirection.data.DayPlan
import com.fourDirection.allDirection.data.Trip
import com.fourDirection.allDirection.ui.theme.GlowBlue
import com.google.firebase.auth.FirebaseAuth
import com.mapbox.search.result.SearchSuggestion
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

@Composable
fun CalendarPage(
    viewModel: CalendarViewModel = viewModel(),
    onLocationClick: (String) -> Unit = {},
    onSeeRoute: (List<String>) -> Unit = {}
) {
    val context = LocalContext.current
    val accessToken = "pk.eyJ1IjoiamFuZGRpIiwiYSI6ImNtdG9qYmx1ejB1cTEyd29majMxYzRvenMifQ.MHg_MphmkzDyLjIYLdLnmQ"
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    val today = remember { LocalDate.now() }
    var showDatePicker by remember { mutableStateOf(false) }

    // Trip Planning State
    var rangeStart by remember { mutableStateOf<LocalDate?>(null) }
    var rangeEnd by remember { mutableStateOf<LocalDate?>(null) }
    val trips by viewModel.trips.collectAsState()
    var selectedTripId by remember { mutableStateOf<String?>(null) }
    val selectedTrip = remember(trips, selectedTripId) {
        trips.find { it.id == selectedTripId }
    }
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
                                        selectedTripId = tripForDate.id
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
                        var isCollaborative by remember { mutableStateOf(false) }
                        
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

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { isCollaborative = !isCollaborative }
                        ) {
                            Checkbox(
                                checked = isCollaborative,
                                onCheckedChange = { isCollaborative = it },
                                colors = CheckboxDefaults.colors(checkedColor = GlowBlue)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Collaborative", color = Color.White, fontSize = 14.sp)
                                Text("Allow others to edit this plan", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                            }
                        }
                        
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
                                            endDate = rangeEnd!!,
                                            isCollaborative = isCollaborative
                                        )
                                        viewModel.saveTrip(newTrip)
                                        selectedTripId = newTrip.id
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
            val currentUid = remember<String?> { FirebaseAuth.getInstance().currentUser?.uid }
            AnimatedVisibility(
                visible = selectedTrip != null,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                if (selectedTrip != null) {
                    TripDetailView(
                        trip = selectedTrip,
                        currentUid = currentUid,
                        onToggleCollaborative = { isCollab ->
                            viewModel.saveTrip(selectedTrip.copy(isCollaborative = isCollab))
                        },
                        onDismiss = { selectedTripId = null },
                        onEditDay = { editingDayPlan = it },
                        onLocationClick = onLocationClick,
                        onSeeRoute = { dayPlan ->
                            // Find previous day's hotel
                            val dayList = selectedTrip.dayPlans.keys.sorted()
                            val currentDayIndex = dayList.indexOf(dayPlan.date)
                            val prevHotel = if (currentDayIndex > 0) {
                                selectedTrip.dayPlans[dayList[currentDayIndex - 1]]?.hotel
                            } else null

                            val routeAddresses = mutableListOf<String>()
                            
                            // Start Point
                            val isFirstDay = dayPlan.date == selectedTrip.startDate
                            val isLastDay = dayPlan.date == selectedTrip.endDate
                            
                            val start = if (isFirstDay) dayPlan.startLocation else prevHotel
                            if (start != null) routeAddresses.add(start)
                            
                            // Stops
                            routeAddresses.addAll(dayPlan.locations)
                            
                            // End Point
                            val end = if (isLastDay) dayPlan.endLocation else dayPlan.hotel
                            if (end != null) routeAddresses.add(end)
                            
                            if (routeAddresses.size >= 2) {
                                onSeeRoute(routeAddresses)
                            }
                        },
                        onDeleteTrip = {
                            viewModel.deleteTrip(selectedTrip.id)
                            selectedTripId = null
                        },
                        onLeaveTrip = {
                            viewModel.leaveTrip(selectedTrip.id)
                            selectedTripId = null
                        }
                    )
                }
            }

            // --- DAY PLAN EDIT DIALOG ---
            if (editingDayPlan != null && selectedTrip != null) {
                val canEdit = (currentUid != null && selectedTrip.ownerUid == currentUid) || selectedTrip.isCollaborative
                DayPlanEditDialog(
                    dayPlan = editingDayPlan!!,
                    trip = selectedTrip,
                    viewModel = viewModel,
                    canEdit = canEdit,
                    onDismiss = { editingDayPlan = null },
                    onLocationClick = onLocationClick,
                    onSeeRoute = onSeeRoute,
                    onSave = { updatedPlan ->
                        val newPlans = selectedTrip.dayPlans.toMutableMap()
                        newPlans[updatedPlan.date] = updatedPlan
                        val updatedTrip = selectedTrip.copy(dayPlans = newPlans)
                        viewModel.saveTrip(updatedTrip)
                        editingDayPlan = updatedPlan // Update local state to reflect changes
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
    currentUid: String?,
    onToggleCollaborative: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onEditDay: (DayPlan) -> Unit,
    onLocationClick: (String) -> Unit = {},
    onSeeRoute: (DayPlan) -> Unit = {},
    onDeleteTrip: () -> Unit,
    onLeaveTrip: () -> Unit
) {
    val isOwner = currentUid != null && trip.ownerUid == currentUid
    val canEdit = isOwner || trip.isCollaborative

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.55f), 
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
                Column(modifier = Modifier.weight(1f)) {
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
                    
                    if (isOwner) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onToggleCollaborative(!trip.isCollaborative) }
                        ) {
                            Switch(
                                checked = trip.isCollaborative,
                                onCheckedChange = onToggleCollaborative,
                                modifier = Modifier.scale(0.6f),
                                colors = SwitchDefaults.colors(checkedThumbColor = GlowBlue)
                            )
                            Text(
                                text = "Collaborative",
                                color = if (trip.isCollaborative) GlowBlue else Color.White.copy(alpha = 0.4f),
                                fontSize = 10.sp
                            )
                        }
                    } else if (trip.isCollaborative) {
                        Text(
                            text = "Collaborative Plan",
                            color = GlowBlue,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isOwner) {
                        IconButton(onClick = onDeleteTrip) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                        }
                    } else {
                        IconButton(onClick = onLeaveTrip) {
                            Icon(Icons.Default.Delete, contentDescription = "Leave Plan", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                        }
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
                        canEdit = canEdit,
                        onEditClick = { onEditDay(dayPlan) },
                        onSeeRouteClick = { onSeeRoute(dayPlan) }
                    )
                }
            }
        }
    }
}

@Composable
fun DayPlanCard(
    dayPlan: DayPlan,
    canEdit: Boolean,
    onEditClick: () -> Unit,
    onSeeRouteClick: () -> Unit
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
                
                if (dayPlan.startLocation != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TripOrigin, contentDescription = null, tint = GlowBlue, modifier = Modifier.size(10.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(text = "Start: ${dayPlan.startLocation}", color = Color.White, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                if (dayPlan.locations.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        dayPlan.locations.take(1).forEach { loc ->
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
                    }
                }

                if (dayPlan.endLocation != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Place, contentDescription = null, tint = Color.Red, modifier = Modifier.size(10.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(text = "End: ${dayPlan.endLocation}", color = Color.White, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                
                if (dayPlan.hotel != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = GlowBlue, modifier = Modifier.size(10.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "Hotel: ${dayPlan.hotel}",
                            color = Color.White,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
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
    trip: Trip,
    viewModel: CalendarViewModel,
    canEdit: Boolean,
    onDismiss: () -> Unit,
    onLocationClick: (String) -> Unit = {},
    onSeeRoute: (List<String>) -> Unit = {},
    onSave: (DayPlan) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var locations by remember { mutableStateOf(dayPlan.locations) }
    var hotel by remember { mutableStateOf(dayPlan.hotel) }
    var startLocation by remember { mutableStateOf(dayPlan.startLocation) }
    var endLocation by remember { mutableStateOf(dayPlan.endLocation) }
    var description by remember { mutableStateOf(dayPlan.description) }
    
    val suggestions by viewModel.suggestions.collectAsState()
    var activeSearchField by remember { mutableStateOf<String?>(null) } // "locations", "hotel", "start", "end"

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1A1A1A),
            tonalElevation = 8.dp,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                // Header
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

                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    if (!isEditing) {
                        // VIEW MODE
                        if (locations.isEmpty() && description.isBlank() && hotel == null && startLocation == null && endLocation == null) {
                            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                Text("No plans for today.", color = Color.White.copy(alpha = 0.3f))
                            }
                        } else {
                            if (startLocation != null) {
                                ViewLocationItem("Starting From", startLocation!!, onLocationClick)
                            }
                            
                            if (locations.isNotEmpty()) {
                                Text("Locations", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
                                locations.forEach { loc ->
                                    ViewLocationItem(null, loc, onLocationClick)
                                }
                            }
                            
                            if (hotel != null) {
                                ViewLocationItem("Hotel", hotel!!, onLocationClick)
                            }
                            
                            if (endLocation != null) {
                                ViewLocationItem("Final Destination", endLocation!!, onLocationClick)
                            }

                            if (description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(24.dp))
                                Text("Daily Notes", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = description, color = Color.White, fontSize = 15.sp, lineHeight = 22.sp)
                            }
                        }
                    } else {
                        // EDIT MODE
                        val isFirstDay = dayPlan.date == trip.startDate
                        val isLastDay = dayPlan.date == trip.endDate
                        
                        // Start Location (First Day Only)
                        if (isFirstDay) {
                            EditAddressField(
                                label = "Start Location",
                                value = startLocation,
                                isActive = activeSearchField == "start",
                                onActivate = { activeSearchField = "start" },
                                onValueChange = { viewModel.onSearchQueryChanged(it) },
                                onRemove = { startLocation = null },
                                suggestions = suggestions,
                                onSuggestionSelect = { startLocation = it; activeSearchField = null }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Locations List
                        Text("Locations", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        var draggedIndex by remember { mutableStateOf<Int?>(null) }
                        var targetIndex by remember { mutableStateOf<Int?>(null) }
                        var dragYOffset by remember { mutableStateOf(0f) }
                        val slotPositions = remember { mutableStateMapOf<Int, Float>() }
                        val slotHeights = remember { mutableStateMapOf<Int, Int>() }
                        var editRootCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { editRootCoordinates = it }
                        ) {
                            locations.forEachIndexed { index, loc ->
                                val isBeingDragged = draggedIndex == index
                                
                                val targetSlot = when {
                                    draggedIndex == null || targetIndex == null -> index
                                    index == draggedIndex -> targetIndex!!
                                    draggedIndex!! < targetIndex!! && index > draggedIndex!! && index <= targetIndex!! -> index - 1
                                    draggedIndex!! > targetIndex!! && index < draggedIndex!! && index >= targetIndex!! -> index + 1
                                    else -> index
                                }

                                val itemOffset = if (targetSlot != index) {
                                    val currentPos = slotPositions[index] ?: 0f
                                    val targetPos = slotPositions[targetSlot] ?: 0f
                                    targetPos - currentPos
                                } else {
                                    0f
                                }

                                val animatedYOffset by animateFloatAsState(targetValue = itemOffset, label = "reorder")

                                key(loc + index) {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp)
                                            .onGloballyPositioned { coords ->
                                                editRootCoordinates?.let { root ->
                                                    slotPositions[index] = root.localPositionOf(coords, Offset.Zero).y
                                                    slotHeights[index] = coords.size.height
                                                }
                                            }
                                            .graphicsLayer {
                                                translationY = if (isBeingDragged) dragYOffset else animatedYOffset
                                                alpha = if (isBeingDragged) 0.8f else 1f
                                                scaleX = if (isBeingDragged) 1.02f else 1f
                                                scaleY = if (isBeingDragged) 1.02f else 1f
                                            }
                                            .zIndex(if (isBeingDragged) 10f else 0f),
                                        color = if (isBeingDragged) Color.White.copy(alpha = 0.1f) else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp),
                                        border = if (isBeingDragged) BorderStroke(1.dp, GlowBlue.copy(alpha = 0.5f)) else null
                                    ) {
                                        EditLocationListItem(
                                            text = loc,
                                            onRemove = { locations = locations.filterIndexed { i, _ -> i != index } },
                                            onDragStart = {
                                                draggedIndex = index
                                                targetIndex = index
                                                dragYOffset = 0f
                                            },
                                            onDrag = { dragAmount ->
                                                dragYOffset += dragAmount
                                                
                                                val currentY = (slotPositions[index] ?: 0f) + dragYOffset + (slotHeights[index] ?: 0) / 2f
                                                
                                                var bestTarget = targetIndex
                                                var minDistance = Float.MAX_VALUE
                                                
                                                slotPositions.forEach { (i, pos) ->
                                                    val height = slotHeights[i] ?: 0
                                                    val center = pos + height / 2f
                                                    val distance = abs(center - currentY)
                                                    if (distance < minDistance) {
                                                        minDistance = distance
                                                        bestTarget = i
                                                    }
                                                }
                                                targetIndex = bestTarget
                                            },
                                            onDragEnd = {
                                                if (draggedIndex != null && targetIndex != null && draggedIndex != targetIndex) {
                                                    val list = locations.toMutableList()
                                                    val item = list.removeAt(draggedIndex!!)
                                                    list.add(targetIndex!!, item)
                                                    locations = list
                                                }
                                                draggedIndex = null
                                                targetIndex = null
                                                dragYOffset = 0f
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        
                        EditAddressField(
                            label = "Add Location",
                            value = null,
                            isActive = activeSearchField == "locations",
                            onActivate = { activeSearchField = "locations" },
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            onRemove = {},
                            suggestions = suggestions,
                            onSuggestionSelect = { if (!locations.contains(it)) locations = locations + it; activeSearchField = null }
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        // Hotel (Every Day)
                        EditAddressField(
                            label = "Hotel",
                            value = hotel,
                            isActive = activeSearchField == "hotel",
                            onActivate = { activeSearchField = "hotel" },
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            onRemove = { hotel = null },
                            suggestions = suggestions,
                            onSuggestionSelect = { hotel = it; activeSearchField = null }
                        )

                        // End Location (Last Day Only)
                        if (isLastDay) {
                            Spacer(modifier = Modifier.height(16.dp))
                            EditAddressField(
                                label = "End Destination",
                                value = endLocation,
                                isActive = activeSearchField == "end",
                                onActivate = { activeSearchField = "end" },
                                onValueChange = { viewModel.onSearchQueryChanged(it) },
                                onRemove = { endLocation = null },
                                suggestions = suggestions,
                                onSuggestionSelect = { endLocation = it; activeSearchField = null }
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Daily Notes") },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = GlowBlue
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // See Trip Route Button
                if (!isEditing && (locations.isNotEmpty() || hotel != null || startLocation != null || endLocation != null)) {
                    Button(
                        onClick = {
                            val routeAddresses = mutableListOf<String>()
                            
                            // Start Point: Prev day hotel or current startLocation
                            val isFirstDay = dayPlan.date == trip.startDate
                            val isLastDay = dayPlan.date == trip.endDate
                            
                            val start = if (isFirstDay) startLocation else {
                                trip.dayPlans[dayPlan.date.minusDays(1)]?.hotel
                            }
                            if (start != null) routeAddresses.add(start)
                            
                            // Stops: current day locations
                            routeAddresses.addAll(locations)
                            
                            // End Point: current day endLocation or current day hotel
                            val end = if (isLastDay) endLocation else hotel
                            if (end != null) routeAddresses.add(end)
                            
                            if (routeAddresses.size >= 2) {
                                onSeeRoute(routeAddresses)
                            } else {
                                Log.d("CalendarPage", "Not enough locations for a route: $routeAddresses")
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp).padding(bottom = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GlowBlue.copy(alpha = 0.2f)),
                        border = BorderStroke(1.dp, GlowBlue),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Default.Directions, contentDescription = null, tint = GlowBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("See Trip Route", color = GlowBlue, fontWeight = FontWeight.Bold)
                    }
                }

                if (canEdit) {
                    Button(
                        onClick = { 
                            if (isEditing) {
                                onSave(dayPlan.copy(
                                    locations = locations, 
                                    description = description,
                                    hotel = hotel,
                                    startLocation = startLocation,
                                    endLocation = endLocation
                                )) 
                                isEditing = false
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
}

@Composable
fun ViewLocationItem(label: String?, text: String, onLocationClick: (String) -> Unit) {
    Surface(
        onClick = { onLocationClick(text) },
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
            Column {
                if (label != null) {
                    Text(text = label, color = GlowBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Text(text = text, color = Color.White, fontSize = 15.sp)
            }
        }
    }
}

@Composable
fun EditLocationListItem(
    text: String, 
    onRemove: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit
) {
    Surface(
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = GlowBlue, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = text, color = Color.White, fontSize = 14.sp)
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                }
                
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Reorder",
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier
                        .size(40.dp)
                        .padding(8.dp)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { onDragStart() },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    onDrag(dragAmount.y)
                                },
                                onDragEnd = { onDragEnd() },
                                onDragCancel = { onDragEnd() }
                            )
                        }
                )
            }
        }
    }
}

@Composable
fun EditAddressField(
    label: String,
    value: String?,
    isActive: Boolean,
    onActivate: () -> Unit,
    onValueChange: (String) -> Unit,
    onRemove: () -> Unit,
    suggestions: List<SearchSuggestion>,
    onSuggestionSelect: (String) -> Unit
) {
    if (value != null) {
        Surface(
            color = Color.White.copy(alpha = 0.1f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            border = BorderStroke(1.dp, GlowBlue.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = GlowBlue, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = label, color = GlowBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(text = value, color = Color.White, fontSize = 14.sp)
                    }
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                }
            }
        }
    } else {
        var query by remember { mutableStateOf("") }
        Box {
            OutlinedTextField(
                value = query,
                onValueChange = { 
                    query = it
                    onValueChange(it)
                },
                label = { Text(label) },
                modifier = Modifier.fillMaxWidth().onFocusChanged { if (it.isFocused) onActivate() },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = GlowBlue
                ),
                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) }
            )

            if (isActive && query.isNotBlank() && suggestions.isNotEmpty()) {
                Popup(
                    alignment = Alignment.BottomStart,
                    offset = IntOffset(0, 10),
                    properties = PopupProperties(dismissOnClickOutside = true)
                ) {
                    Surface(
                        modifier = Modifier.width(300.dp).heightIn(max = 200.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF222222),
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f)),
                        tonalElevation = 8.dp
                    ) {
                        LazyColumn {
                            items(suggestions) { suggestion ->
                                ListItem(
                                    headlineContent = { Text(suggestion.name, color = Color.White, fontSize = 14.sp) },
                                    supportingContent = { Text(suggestion.descriptionText ?: "", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp) },
                                    modifier = Modifier.clickable {
                                        onSuggestionSelect(suggestion.name)
                                        query = ""
                                    },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}