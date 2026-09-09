package com.fourDirection.allDirection.page.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fourDirection.allDirection.page.tinyApps.CurrencyConverterPage
import com.fourDirection.allDirection.page.tinyApps.EmergencyInfoPage
import com.fourDirection.allDirection.page.tinyApps.TipCalculatorPage
import com.fourDirection.allDirection.ui.theme.GlowBlue
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild

sealed class NavItem(val route: String, val icon: ImageVector, val label: String) {
    data object Home : NavItem("home", Icons.Default.Home, "Home")
    data object Explore : NavItem("explore", Icons.Default.Explore, "Explore")
    data object Calendar : NavItem("calendar", Icons.Default.CalendarToday, "Calendar")
    data object Booking : NavItem("AI", Icons.Default.AutoAwesome, "AI")
    data object Saved : NavItem("saved", Icons.Default.Bookmark, "Saved")
    data object Profile : NavItem("profile", Icons.Default.Person, "Profile")
}

@Composable
fun MainContainer(
    userName: String,
    userEmail: String,
    totalDistance: Double,
    period: String,
    onSignOut: () -> Unit
) {
    var selectedItem by remember { mutableStateOf(0) }
    var isCurrencyConverterVisible by remember { mutableStateOf(false) }
    var isTipCalculatorVisible by remember { mutableStateOf(false) }
    var isEmergencyInfoVisible by remember { mutableStateOf(false) }
    var shouldFocusExploreSearch by remember { mutableStateOf(false) }
    
    
    val items = listOf(
        NavItem.Home,
        NavItem.Explore,
        NavItem.Calendar,
        NavItem.Booking,
        NavItem.Saved,
        NavItem.Profile
    )
    
    // Initialize HazeState for backdrop blur
    val hazeState = remember { HazeState() }

    // Handle system back button
    BackHandler(enabled = selectedItem != 0 || isCurrencyConverterVisible || isTipCalculatorVisible || isEmergencyInfoVisible) {
        if (isCurrencyConverterVisible) {
            isCurrencyConverterVisible = false
        } else if (isTipCalculatorVisible) {
            isTipCalculatorVisible = false
        } else if (isEmergencyInfoVisible) {
            isEmergencyInfoVisible = false
        } else {
            selectedItem = 0
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (!isCurrencyConverterVisible && !isTipCalculatorVisible && !isEmergencyInfoVisible) {
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
                    onCalendarClick = { selectedItem = items.indexOf(NavItem.Calendar) },
                    onSearchClick = { 
                        selectedItem = items.indexOf(NavItem.Explore)
                        shouldFocusExploreSearch = true
                    }
                )
                NavItem.Explore -> ExplorePage(
                    hazeState = hazeState,
                    shouldFocusSearch = shouldFocusExploreSearch,
                    onSearchFocused = { shouldFocusExploreSearch = false }
                )
                NavItem.Calendar -> CalendarPage()
                NavItem.Booking -> AiPage(hazeState = hazeState)
                NavItem.Saved -> PlaceholderPage("Saved")
                NavItem.Profile -> ProfilePage(
                    userName = userName,
                    userEmail = userEmail,
                    onSignOut = onSignOut
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

            // Acknowledge innerPadding to satisfy Scaffold lint without clipping the content
            Spacer(Modifier.padding(innerPadding).align(androidx.compose.ui.Alignment.BottomCenter))
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
