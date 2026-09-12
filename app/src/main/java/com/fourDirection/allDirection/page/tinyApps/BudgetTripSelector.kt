package com.fourDirection.allDirection.page.tinyApps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fourDirection.allDirection.data.Trip
import com.fourDirection.allDirection.data.UserRepository
import com.fourDirection.allDirection.page.main.CalendarViewModel
import com.google.firebase.auth.FirebaseAuth
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import android.app.DatePickerDialog
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import java.util.Calendar

@Composable
fun BudgetTripSelector(
    onBack: () -> Unit,
    plannedTripIds: Set<String>,
    onTripSelected: (Trip) -> Unit,
    onManualTripAdd: (String, LocalDate, LocalDate) -> Unit
) {

    val calendarViewModel: CalendarViewModel = viewModel()
    val allTrips by calendarViewModel.trips.collectAsState()
    val isTripsLoading by calendarViewModel.isLoading.collectAsState()
    var showManualDialog by remember { mutableStateOf(false) }

    val trips = remember(allTrips, plannedTripIds) {
        allTrips.filter { 
            it.endDate.isAfter(LocalDate.now().minusDays(1)) &&
            !plannedTripIds.contains(it.id)
        }.sortedBy { it.startDate }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    text = "Select a Trip",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isTripsLoading && trips.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(trips, key = { it.id }) { trip ->
                        TripSelectionItem(trip = trip, onClick = { onTripSelected(trip) })
                    }
                    
                    item {
                        Button(
                            onClick = { showManualDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Custom Trip", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    if (showManualDialog) {
        ManualTripDialog(
            onDismiss = { showManualDialog = false },
            onConfirm = { name, start, end ->
                showManualDialog = false
                val newTrip = Trip(name = name, startDate = start, endDate = end)
                calendarViewModel.saveTrip(newTrip) // Store to Calendar
                onTripSelected(newTrip) // Navigate to editor
                onManualTripAdd(name, start, end) // Keep the callback for consistency
            }
        )
    }
}

@Composable
fun TripSelectionItem(trip: Trip, onClick: () -> Unit) {
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(trip.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                "${trip.startDate.format(dateFormatter)} - ${trip.endDate.format(dateFormatter)}",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualTripDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, LocalDate, LocalDate) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var endDate by remember { mutableStateOf(LocalDate.now().plusDays(7)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1A),
        title = { Text("Custom Trip", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Trip Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DatePickerField(
                        label = "Start Date",
                        date = startDate,
                        onDateSelected = { startDate = it },
                        modifier = Modifier.weight(1f)
                    )
                    DatePickerField(
                        label = "End Date",
                        date = endDate,
                        onDateSelected = { endDate = it },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name, startDate, endDate) }) {
                Text("Confirm", color = Color(0xFF81D4FA))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.6f))
            }
        }
    )
}

@Composable
fun DatePickerField(
    label: String,
    date: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    calendar.set(date.year, date.monthValue - 1, date.dayOfMonth)

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            onDateSelected(LocalDate.of(year, month + 1, dayOfMonth))
        },
        date.year,
        date.monthValue - 1,
        date.dayOfMonth
    )

    Column(modifier = modifier) {
        Text(text = label, color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
        Surface(
            onClick = { datePickerDialog.show() },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            shape = RoundedCornerShape(8.dp),
            color = Color.White.copy(alpha = 0.05f),
            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f))
        ) {
            Text(
                text = date.toString(),
                color = Color.White,
                modifier = Modifier.padding(12.dp),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
