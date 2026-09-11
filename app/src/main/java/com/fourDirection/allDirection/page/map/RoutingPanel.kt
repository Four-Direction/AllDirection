package com.fourDirection.allDirection.page.map

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.fourDirection.allDirection.ui.theme.GlowBlue
import com.mapbox.search.result.SearchSuggestion

@Composable
fun RoutingPanel(
    waypoints: List<Waypoint>,
    suggestions: List<SearchSuggestion>,
    routeInfo: RouteInfo?,
    isCalculating: Boolean,
    isSearching: Boolean,
    onQueryChanged: (Int, String) -> Unit,
    onClearSuggestions: () -> Unit,
    onWaypointUpdate: (Int, SearchSuggestion) -> Unit,
    onAddWaypoint: () -> Unit,
    onRemoveWaypoint: (Int) -> Unit,
    onCalculateRoute: () -> Unit,
    onClose: () -> Unit
) {
    var activeWaypointIndex by remember { mutableStateOf<Int?>(null) }
    var isMinimized by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .animateContentSize(),
        shape = RoundedCornerShape(24.dp),
        color = Color.Black.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Plan Your Route", 
                    color = Color.White, 
                    fontSize = 18.sp, 
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
                
                IconButton(
                    onClick = { isMinimized = !isMinimized }, 
                    modifier = Modifier.size(48.dp).align(Alignment.Center)
                ) {
                    Icon(
                        imageVector = if (isMinimized) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                        contentDescription = if (isMinimized) "Expand" else "Minimize",
                        tint = GlowBlue,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                IconButton(onClick = onClose, modifier = Modifier.align(Alignment.CenterEnd)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            AnimatedVisibility(visible = !isMinimized) {
                val density = LocalDensity.current
                Column {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Start Waypoint (Outside Scrollable)
                    if (waypoints.isNotEmpty()) {
                        WaypointWithPopup(
                            index = 0,
                            waypoint = waypoints[0],
                            activeWaypointIndex = activeWaypointIndex,
                            suggestions = suggestions,
                            isSearching = isSearching,
                            density = density,
                            placeholder = "Start Location",
                            onValueChange = { query ->
                                activeWaypointIndex = 0
                                onQueryChanged(0, query)
                            },
                            onActivate = {
                                onClearSuggestions()
                                activeWaypointIndex = 0
                            },
                            onWaypointUpdate = { idx, suggestion ->
                                onWaypointUpdate(idx, suggestion)
                                activeWaypointIndex = null
                            },
                            onRemove = null
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Intermediate Waypoints Section (Scrollable Window)
                    if (waypoints.size > 2) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 180.dp) // Adjusted height
                                .clip(RoundedCornerShape(16.dp)),
                            color = Color.White.copy(alpha = 0.03f),
                            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.05f))
                        ) {
                            val scrollState = rememberScrollState()
                            Column(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .verticalScroll(scrollState),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (index in 1 until waypoints.size - 1) {
                                    key(index) {
                                        WaypointWithPopup(
                                            index = index,
                                            waypoint = waypoints[index],
                                            activeWaypointIndex = activeWaypointIndex,
                                            suggestions = suggestions,
                                            isSearching = isSearching,
                                            density = density,
                                            placeholder = "Stop $index",
                                            onValueChange = { query ->
                                                activeWaypointIndex = index
                                                onQueryChanged(index, query)
                                            },
                                            onActivate = {
                                                onClearSuggestions()
                                                activeWaypointIndex = index
                                            },
                                            onWaypointUpdate = { idx, suggestion ->
                                                onWaypointUpdate(idx, suggestion)
                                                activeWaypointIndex = null
                                            },
                                            onRemove = { onRemoveWaypoint(index) }
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // End Waypoint (Outside Scrollable)
                    if (waypoints.size > 1) {
                        val endIndex = waypoints.size - 1
                        WaypointWithPopup(
                            index = endIndex,
                            waypoint = waypoints[endIndex],
                            activeWaypointIndex = activeWaypointIndex,
                            suggestions = suggestions,
                            isSearching = isSearching,
                            density = density,
                            placeholder = "End Destination",
                            onValueChange = { query ->
                                activeWaypointIndex = endIndex
                                onQueryChanged(endIndex, query)
                            },
                            onActivate = {
                                onClearSuggestions()
                                activeWaypointIndex = endIndex
                            },
                            onWaypointUpdate = { idx, suggestion ->
                                onWaypointUpdate(idx, suggestion)
                                activeWaypointIndex = null
                            },
                            onRemove = null
                        )
                    }

                    if (waypoints.size < 15) {
                        TextButton(
                            onClick = onAddWaypoint,
                            modifier = Modifier.align(Alignment.Start)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = GlowBlue, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Stop", color = GlowBlue, fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onCalculateRoute,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GlowBlue),
                        enabled = waypoints.count { !it.isPlaceholder } >= 2 && !isCalculating
                    ) {
                        if (isCalculating) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black)
                        } else {
                            Text("Get Directions", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }

                    routeInfo?.let { info ->
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            RouteDetailItem(label = "Distance", value = "%.1f km".format(info.distanceKm))
                            RouteDetailItem(label = "Duration", value = formatDuration(info.durationMin))
                        }
                    }
                }
            }
            
            if (isMinimized && routeInfo != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Text("%.1f km".format(routeInfo.distanceKm), color = GlowBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(formatDuration(routeInfo.durationMin), color = GlowBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun WaypointWithPopup(
    index: Int,
    waypoint: Waypoint,
    activeWaypointIndex: Int?,
    suggestions: List<SearchSuggestion>,
    isSearching: Boolean,
    density: Density,
    placeholder: String,
    onValueChange: (String) -> Unit,
    onActivate: () -> Unit,
    onWaypointUpdate: (Int, SearchSuggestion) -> Unit,
    onRemove: (() -> Unit)?
) {
    var itemWidth by remember { mutableStateOf(0.dp) }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
                itemWidth = with(density) { coords.size.width.toDp() }
            }
    ) {
        WaypointInputItem(
            index = index,
            waypoint = waypoint,
            isActive = activeWaypointIndex == index,
            placeholder = placeholder,
            onValueChange = onValueChange,
            onActivate = onActivate,
            onRemove = onRemove
        )

        if (activeWaypointIndex == index && (suggestions.isNotEmpty() || isSearching)) {
            Popup(
                alignment = Alignment.BottomStart,
                offset = IntOffset(0, 0), // BottomStart should already be below the anchor
                properties = PopupProperties(
                    focusable = false,
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true
                )
            ) {
                Card(
                    modifier = Modifier
                        .width(itemWidth)
                        .padding(top = 4.dp) // Manual gap
                        .heightIn(max = 250.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSearching) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = GlowBlue, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Search, contentDescription = null, tint = GlowBlue, modifier = Modifier.size(14.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (isSearching) "Searching..." else "Suggestions",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            itemsIndexed(suggestions) { _, suggestion ->
                                ListItem(
                                    headlineContent = { Text(suggestion.name, color = Color.White, fontSize = 13.sp) },
                                    supportingContent = { Text(suggestion.descriptionText ?: "", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp) },
                                    modifier = Modifier.clickable {
                                        Log.d("RoutingPanel", "Selected index=$index suggestion=${suggestion.name}")
                                        onWaypointUpdate(index, suggestion)
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

@Composable
fun WaypointInputItem(
    index: Int,
    waypoint: Waypoint,
    isActive: Boolean,
    placeholder: String,
    onValueChange: (String) -> Unit,
    onActivate: () -> Unit,
    onRemove: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isActive) GlowBlue.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.05f))
            .clickable { onActivate() }
            .padding(horizontal = 12.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when {
                index == 0 -> Icons.Default.TripOrigin
                onRemove == null -> Icons.Default.Place
                else -> Icons.Default.FiberManualRecord
            },
            contentDescription = null,
            tint = if (index == 0) GlowBlue else if (onRemove == null) Color.Red else Color.White,
            modifier = Modifier.size(18.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        TextField(
            value = if (waypoint.isPlaceholder) "" else waypoint.name,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { 
                    if (it.isFocused) {
                        onActivate() 
                    }
                },
            placeholder = {
                Text(
                    text = placeholder,
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 14.sp
                )
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
        )

        if (onRemove != null) {
            IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Remove", tint = Color.White.copy(alpha = 0.4f))
            }
        }
    }
}

@Composable
fun RouteDetailItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
        Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

fun formatDuration(minutes: Double): String {
    val h = (minutes / 60).toInt()
    val m = (minutes % 60).toInt()
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
