package com.fourDirection.allDirection.page.main

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.fourDirection.allDirection.page.main.GroupChatPage
import com.fourDirection.allDirection.page.map.ExplorePage
import com.fourDirection.allDirection.page.tinyApps.CurrencyConverterPage
import com.fourDirection.allDirection.page.tinyApps.EmergencyInfoPage
import com.fourDirection.allDirection.page.tinyApps.TipCalculatorPage
import com.fourDirection.allDirection.page.tinyApps.BudgetTrackerPage
import com.fourDirection.allDirection.ui.theme.GlowBlue
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild

sealed class NavItem(val route: String, val icon: ImageVector, val label: String) {
    data object Home : NavItem("home", Icons.Default.Home, "Home")
    data object Planner : NavItem("planner", Icons.Default.Explore, "Explore")
    data object Booking : NavItem("AI", Icons.Default.AutoAwesome, "AI")
    data object Social : NavItem("social", Icons.Default.Group, "Social")
    data object Profile : NavItem("profile", Icons.Default.Person, "Profile")
}

@Composable
fun MainContainer(
    userName: String,
    userEmail: String,
    userPhotoUrl: String,
    totalDistance: Double,
    period: String,
    onSignOut: () -> Unit,
    onUserUpdate: (String, String) -> Unit
) {
    var selectedItem by remember { mutableStateOf(0) }
    var isCurrencyConverterVisible by remember { mutableStateOf(false) }
    var isTipCalculatorVisible by remember { mutableStateOf(false) }
    var isEmergencyInfoVisible by remember { mutableStateOf(false) }
    var isBudgetTrackerVisible by remember { mutableStateOf(false) }
    var isConnectionsVisible by remember { mutableStateOf(false) }
    var isAccountVisible by remember { mutableStateOf(false) }
    var isSettingsVisible by remember { mutableStateOf(false) }
    var selectedGroup by remember { mutableStateOf<Map<String, Any>?>(null) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE) }
    var distanceUnit by remember { mutableStateOf(sharedPrefs.getString("distance_unit", "km") ?: "km") }

    var shouldFocusExploreSearch by remember { mutableStateOf(false) }
    var isRoutingActive by remember { mutableStateOf(false) }
    var plannerSubTab by remember { mutableStateOf(0) } // 0 for Map, 1 for Calendar
    var isPlannerCreationMode by remember { mutableStateOf(false) }
    var pendingLocationSearch by remember { mutableStateOf<String?>(null) }
    
    val items = listOf(
        NavItem.Home,
        NavItem.Planner,
        NavItem.Booking,
        NavItem.Social,
        NavItem.Profile
    )
    
    // Initialize HazeState for backdrop blur
    val hazeState = remember { HazeState() }

    // Handle system back button
    BackHandler(enabled = selectedItem != 0 || isCurrencyConverterVisible || isTipCalculatorVisible || isEmergencyInfoVisible || isBudgetTrackerVisible || isConnectionsVisible || isAccountVisible || isSettingsVisible || selectedGroup != null || isPlannerCreationMode) {
        if (isCurrencyConverterVisible) {
            isCurrencyConverterVisible = false
        } else if (isTipCalculatorVisible) {
            isTipCalculatorVisible = false
        } else if (isEmergencyInfoVisible) {
            isEmergencyInfoVisible = false
        } else if (isBudgetTrackerVisible) {
            isBudgetTrackerVisible = false
        } else if (isConnectionsVisible) {
            isConnectionsVisible = false
        } else if (isAccountVisible) {
            isAccountVisible = false
        } else if (isSettingsVisible) {
            isSettingsVisible = false
        } else if (selectedGroup != null) {
            selectedGroup = null
        } else if (isPlannerCreationMode) {
            isPlannerCreationMode = false
        } else {
            selectedItem = 0
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (!isCurrencyConverterVisible && !isTipCalculatorVisible && !isEmergencyInfoVisible && !isBudgetTrackerVisible && !isConnectionsVisible && !isAccountVisible && !isSettingsVisible && selectedGroup == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CircleShape),
                        shape = CircleShape,
                        color = Color.DarkGray.copy(alpha = 0.8f),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.Black.copy(alpha = 0.8f)),
                        tonalElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            items.forEachIndexed { index, item ->
                                val isSelected = selectedItem == index
                                val color = if (isSelected) GlowBlue else Color.White.copy(alpha = 0.6f)

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(CircleShape)
                                        .clickable { selectedItem = index },
                                    contentAlignment = androidx.compose.ui.Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.label,
                                            tint = color,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        if (isSelected) {
                                            Text(
                                                text = item.label,
                                                color = color,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .haze(state = hazeState)
        ) {
            when (items[selectedItem]) {
                NavItem.Home -> HomePage(
                    userName = userName,
                    totalDistance = totalDistance,
                    period = period,
                    distanceUnit = distanceUnit,
                    hazeState = hazeState,
                    onCurrencyClick = { isCurrencyConverterVisible = true },
                    onTipClick = { isTipCalculatorVisible = true },
                    onEmergencyClick = { isEmergencyInfoVisible = true },
                    onBudgetClick = { isBudgetTrackerVisible = true },
                    onConnectionsClick = { isConnectionsVisible = true },
                    onCalendarClick = { 
                        selectedItem = items.indexOf(NavItem.Planner)
                        plannerSubTab = 1
                    },
                    onSearchClick = { 
                        selectedItem = items.indexOf(NavItem.Planner)
                        plannerSubTab = 0
                        shouldFocusExploreSearch = true
                    }
                )
                NavItem.Planner -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (plannerSubTab == 0) {
                            ExplorePage(
                                hazeState = hazeState,
                                shouldFocusSearch = shouldFocusExploreSearch,
                                initialSearchQuery = pendingLocationSearch,
                                onSearchFocused = { 
                                    shouldFocusExploreSearch = false
                                    pendingLocationSearch = null
                                },
                                onRoutingModeChange = { isRoutingActive = it }
                            )
                        } else {
                            CalendarPage(
                                onLocationClick = { location ->
                                    pendingLocationSearch = location
                                    plannerSubTab = 0
                                },
                                isCreationMode = isPlannerCreationMode,
                                onCreationModeChange = { isPlannerCreationMode = it }
                            )
                        }
                        
                        // Top Switcher Docker
                        if (!isRoutingActive) {
                            var mapTabSize by remember { mutableStateOf(IntSize.Zero) }
                            var calendarTabSize by remember { mutableStateOf(IntSize.Zero) }
                            val density = LocalDensity.current
                            
                            val springSpec = spring<Float>(
                                dampingRatio = 0.8f,
                                stiffness = 400f
                            )
                            
                            val indicatorOffset by animateFloatAsState(
                                targetValue = if (plannerSubTab == 0) 0f else with(density) { mapTabSize.width.toDp().toPx() },
                                animationSpec = springSpec,
                                label = "indicatorOffset"
                            )

                            val switcherTopPadding by animateDpAsState(
                                targetValue = if (plannerSubTab == 1 && isPlannerCreationMode) 0.dp else 130.dp,
                                label = "switcherTopPadding"
                            )

                            val switcherAlpha by animateFloatAsState(
                                targetValue = if (plannerSubTab == 1 && isPlannerCreationMode) 0f else 1f,
                                label = "switcherAlpha"
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .zIndex(1f),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .graphicsLayer { alpha = switcherAlpha }
                                        .padding(top = switcherTopPadding)
                                        .clip(CircleShape),
                                    color = Color.Black.copy(alpha = 0.6f),
                                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f))
                                ) {
                                    Box(modifier = Modifier.padding(4.dp)) {
                                        // Background Indicator - only rendered when sizes are known to avoid jitter
                                        if (mapTabSize.width > 0 && calendarTabSize.width > 0) {
                                            val currentWidth = if (plannerSubTab == 0) mapTabSize.width else calendarTabSize.width
                                            val indicatorWidth by animateFloatAsState(
                                                targetValue = with(density) { currentWidth.toDp().toPx() },
                                                animationSpec = springSpec,
                                                label = "indicatorWidth"
                                            )

                                            Box(
                                                modifier = Modifier
                                                    .offset(x = with(density) { indicatorOffset.toDp() })
                                                    .size(
                                                        width = with(density) { indicatorWidth.toDp() },
                                                        height = 36.dp
                                                    )
                                                    .background(GlowBlue, CircleShape)
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            SubTabItem(
                                                label = "Map",
                                                isSelected = plannerSubTab == 0,
                                                onClick = { plannerSubTab = 0 },
                                                modifier = Modifier.onGloballyPositioned { mapTabSize = it.size }
                                            )
                                            SubTabItem(
                                                label = "Calendar",
                                                isSelected = plannerSubTab == 1,
                                                onClick = { plannerSubTab = 1 },
                                                modifier = Modifier.onGloballyPositioned { calendarTabSize = it.size }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                NavItem.Booking -> AiPage(hazeState = hazeState)
                NavItem.Social -> SocialPage(onGroupClick = { group -> selectedGroup = group })
                NavItem.Profile -> ProfilePage(
                    userName = userName,
                    userEmail = userEmail,
                    userPhotoUrl = userPhotoUrl,
                    onSignOut = onSignOut,
                    onAccountClick = { isAccountVisible = true },
                    onConnectionsClick = { isConnectionsVisible = true },
                    onSettingsClick = { isSettingsVisible = true }
                )
            }

            if (isCurrencyConverterVisible) {
                CurrencyConverterPage(onDismiss = { isCurrencyConverterVisible = false })
            }
            
            if (isTipCalculatorVisible) {
                TipCalculatorPage(onDismiss = { isTipCalculatorVisible = false })
            }
            
            if (isEmergencyInfoVisible) {
                EmergencyInfoPage(onDismiss = { isEmergencyInfoVisible = false })
            }

            if (isBudgetTrackerVisible) {
                BudgetTrackerPage(onDismiss = { isBudgetTrackerVisible = false })
            }

            if (isConnectionsVisible) {
                ConnectionsPage(onDismiss = { isConnectionsVisible = false })
            }

            if (isAccountVisible) {
                AccountPage(
                    userName = userName,
                    photoUrl = userPhotoUrl,
                    onDismiss = { isAccountVisible = false },
                    onUpdateSuccess = onUserUpdate
                )
            }

            if (isSettingsVisible) {
                SettingsPage(
                    onDismiss = { isSettingsVisible = false },
                    onUnitChanged = { distanceUnit = it }
                )
            }

            if (selectedGroup != null) {
                GroupChatPage(
                    groupId = selectedGroup!!["id"] as String,
                    groupName = selectedGroup!!["groupName"] as String,
                    onDismiss = { selectedGroup = null }
                )
            }

            // Acknowledge innerPadding to satisfy Scaffold lint without clipping the content
            Spacer(Modifier.padding(innerPadding).align(Alignment.BottomCenter))
        }
    }
}

@Composable
fun SubTabItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.Black else Color.White.copy(alpha = 0.7f),
        animationSpec = tween(durationMillis = 300),
        label = "textColor"
    )

    Surface(
        onClick = onClick,
        color = Color.Transparent,
        shape = CircleShape,
        modifier = modifier.height(36.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = textColor,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
fun PlaceholderPage(title: String) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = title, color = Color.White, style = MaterialTheme.typography.headlineLarge)
        }
    }
}
