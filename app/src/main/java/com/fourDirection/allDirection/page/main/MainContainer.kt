package com.fourDirection.allDirection.page.main

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fourDirection.allDirection.page.main.GroupChatPage
import com.fourDirection.allDirection.page.map.ExplorePage
import com.fourDirection.allDirection.page.tinyApps.CurrencyConverterPage
import com.fourDirection.allDirection.page.tinyApps.EmergencyInfoPage
import com.fourDirection.allDirection.page.tinyApps.TipCalculatorPage
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
    var isConnectionsVisible by remember { mutableStateOf(false) }
    var isAccountVisible by remember { mutableStateOf(false) }
    var selectedGroup by remember { mutableStateOf<Map<String, Any>?>(null) }
    var shouldFocusExploreSearch by remember { mutableStateOf(false) }
    var isRoutingActive by remember { mutableStateOf(false) }
    var plannerSubTab by remember { mutableStateOf(0) } // 0 for Map, 1 for Calendar
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
    BackHandler(enabled = selectedItem != 0 || isCurrencyConverterVisible || isTipCalculatorVisible || isEmergencyInfoVisible || isConnectionsVisible || isAccountVisible || selectedGroup != null) {
        if (isCurrencyConverterVisible) {
            isCurrencyConverterVisible = false
        } else if (isTipCalculatorVisible) {
            isTipCalculatorVisible = false
        } else if (isEmergencyInfoVisible) {
            isEmergencyInfoVisible = false
        } else if (isConnectionsVisible) {
            isConnectionsVisible = false
        } else if (isAccountVisible) {
            isAccountVisible = false
        } else if (selectedGroup != null) {
            selectedGroup = null
        } else {
            selectedItem = 0
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (!isCurrencyConverterVisible && !isTipCalculatorVisible && !isEmergencyInfoVisible && !isConnectionsVisible && !isAccountVisible && selectedGroup == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    contentAlignment = androidx.compose.ui.Alignment.Center
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
                    hazeState = hazeState,
                    onCurrencyClick = { isCurrencyConverterVisible = true },
                    onTipClick = { isTipCalculatorVisible = true },
                    onEmergencyClick = { isEmergencyInfoVisible = true },
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
                                }
                            )
                        }
                        
                        // Top Switcher Docker
                        if (!isRoutingActive) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 80.dp)
                                    .clip(CircleShape),
                                color = Color.Black.copy(alpha = 0.6f),
                                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SubTabItem(
                                        label = "Map",
                                        isSelected = plannerSubTab == 0,
                                        onClick = { plannerSubTab = 0 }
                                    )
                                    SubTabItem(
                                        label = "Calendar",
                                        isSelected = plannerSubTab == 1,
                                        onClick = { plannerSubTab = 1 }
                                    )
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
                    onConnectionsClick = { isConnectionsVisible = true }
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
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) GlowBlue else Color.Transparent,
        shape = CircleShape,
        modifier = Modifier.height(36.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.7f),
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
        Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text(text = title, color = Color.White, style = MaterialTheme.typography.headlineLarge)
        }
    }
}
