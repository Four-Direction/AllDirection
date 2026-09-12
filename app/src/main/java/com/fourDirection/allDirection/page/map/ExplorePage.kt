package com.fourDirection.allDirection.page.map

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fourDirection.allDirection.data.PlacesRepository
import com.fourDirection.allDirection.page.main.LocationDetailSheet
import com.fourDirection.allDirection.ui.theme.GlowBlue
import com.mapbox.common.MapboxOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.CircleAnnotation
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotation
import com.mapbox.maps.extension.compose.annotation.generated.PolylineAnnotation
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@Composable
fun ExplorePage(
    modifier: Modifier = Modifier,
    exploreViewModel: ExploreViewModel = viewModel(),
    hazeState: HazeState,
    shouldFocusSearch: Boolean = false,
    initialSearchQuery: String? = null,
    initialTripRoute: List<String>? = null,
    onSearchFocused: () -> Unit = {},
    onRoutingModeChange: (Boolean) -> Unit = {},
    onRouteHandled: () -> Unit = {}
) {
    val accessToken = "pk.eyJ1IjoiamFuZGRpIiwiYSI6ImNtdG9qYmx1ejB1cTEyd29majMxYzRvenMifQ.MHg_MphmkzDyLjIYLdLnmQ"
    var isMapReady by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val placesRepository = remember { PlacesRepository(context) }
    var isSearchFocused by remember { mutableStateOf(false) }

    val selectedPlaceDetail by exploreViewModel.selectedPlaceDetail.collectAsState()
    val isFetchingDetails by exploreViewModel.isFetchingDetails.collectAsState()
    val navigateMapTo by exploreViewModel.navigateMapTo.collectAsState()

    val isRoutingMode by exploreViewModel.isRoutingMode.collectAsState()
    val waypoints by exploreViewModel.waypoints.collectAsState()
    val currentRoute by exploreViewModel.currentRoute.collectAsState()
    val isCalculatingRoute by exploreViewModel.isCalculatingRoute.collectAsState()
    val isSearching by exploreViewModel.isSearching.collectAsState()

    LaunchedEffect(isRoutingMode) {
        onRoutingModeChange(isRoutingMode)
    }

    LaunchedEffect(Unit) {
        MapboxOptions.accessToken = accessToken
        exploreViewModel.init(accessToken, placesRepository)
        isMapReady = true
        
        // Handle initial search if provided
        initialSearchQuery?.let { query ->
            exploreViewModel.onQueryChanged(query, autoSelect = true)
        }

        // Handle initial trip route from calendar
        initialTripRoute?.let { addresses ->
            exploreViewModel.setTripRoute(addresses)
            onRouteHandled()
        }
    }

    LaunchedEffect(shouldFocusSearch) {
        if (shouldFocusSearch) {
            focusRequester.requestFocus()
            onSearchFocused()
        }
    }

    val searchQuery by exploreViewModel.searchQuery.collectAsState()
    val suggestions by exploreViewModel.suggestions.collectAsState()

    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(101.6869, 3.1390)) // Kuala Lumpur
            zoom(12.0)
        }
    }

    LaunchedEffect(navigateMapTo) {
        navigateMapTo?.let { point ->
            mapViewportState.flyTo(
                CameraOptions.Builder().center(point).zoom(15.0).build()
            )
            exploreViewModel.onNavigateHandled()
        }
    }

    LaunchedEffect(currentRoute) {
        currentRoute?.let { route ->
            val bounds = route.bounds
            if (bounds.size >= 2) {
                // Simplified zoom to bounds
                val center = Point.fromLngLat(
                    (bounds[0].longitude() + bounds[1].longitude()) / 2.0,
                    (bounds[0].latitude() + bounds[1].latitude()) / 2.0
                )
                mapViewportState.flyTo(
                    CameraOptions.Builder().center(center).zoom(11.0).build()
                )
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .hazeSource(state = hazeState)
    ) {
        // Mapbox Map
        if (isMapReady) {
            MapboxMap(
                modifier = Modifier.fillMaxSize(),
                mapViewportState = mapViewportState
            ) {
                if (isRoutingMode) {
                    currentRoute?.let { route ->
                        // Glow layer
                        PolylineAnnotation(points = route.points) {
                            lineColor = Color(0xFF00D1FF).copy(alpha = 0.3f)
                            lineWidth = 12.0
                        }
                        // Core layer
                        PolylineAnnotation(points = route.points) {
                            lineColor = Color(0xFF00D1FF)
                            lineWidth = 5.0
                        }
                    }
                    waypoints.forEachIndexed { index, waypoint ->
                        waypoint.point?.let { point ->
                            val color = when {
                                index == 0 -> Color(0xFF00D1FF) // Start
                                index == waypoints.size - 1 -> Color.Red // End
                                else -> Color.White // Intermediate
                            }

                            // Outer Glow for Point
                            CircleAnnotation(point = point) {
                                circleColor = color.copy(alpha = 0.3f)
                                circleRadius = 12.0
                            }

                            // Core Point
                            CircleAnnotation(point = point) {
                                circleColor = color
                                circleRadius = 6.0
                                circleStrokeColor = Color.Black
                                circleStrokeWidth = 1.5
                            }
                        }
                    }
                }
            }
        } else {
            // Show a placeholder or loader while Mapbox is being initialized
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }

        // Invisible Layer to catch taps and dismiss search focus
        if (isSearchFocused) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            focusManager.clearFocus()
                        })
                    }
            )
        }

        // Search UI Overlay
        if (!isRoutingMode) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(28.dp)),
                    color = Color.Black.copy(alpha = 0.8f),
                    tonalElevation = 8.dp,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.White.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        TextField(
                            value = searchQuery,
                            onValueChange = { exploreViewModel.onQueryChanged(it, mapViewportState.cameraState?.center) },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester)
                                .onFocusChanged { isSearchFocused = it.isFocused },
                            placeholder = {
                                Text(
                                    "Search places...",
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
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = { 
                                    if (suggestions.isNotEmpty()) {
                                        exploreViewModel.selectSuggestion(suggestions.first())
                                        focusManager.clearFocus()
                                    } else {
                                        focusManager.clearFocus()
                                    }
                                }
                            )
                        )
                    }
                }

                if (isSearchFocused && suggestions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .clip(RoundedCornerShape(16.dp)),
                        color = Color.Black.copy(alpha = 0.9f),
                        tonalElevation = 8.dp
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(suggestions) { suggestion ->
                                ListItem(
                                    headlineContent = { 
                                        Text(suggestion.name, color = Color.White, fontSize = 14.sp) 
                                    },
                                    supportingContent = { 
                                        Text(suggestion.descriptionText ?: "", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp) 
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            exploreViewModel.selectSuggestion(suggestion)
                                            focusManager.clearFocus()
                                        },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                RoutingPanel(
                    waypoints = waypoints,
                    suggestions = suggestions,
                    routeInfo = currentRoute,
                    isCalculating = isCalculatingRoute,
                    isSearching = isSearching,
                    onQueryChanged = { index, query -> exploreViewModel.onWaypointQueryChanged(index, query, mapViewportState.cameraState?.center) },
                    onClearSuggestions = { exploreViewModel.clearSuggestions() },
                    onWaypointUpdate = { index, suggestion -> exploreViewModel.updateWaypoint(index, suggestion) },
                    onAddWaypoint = { exploreViewModel.addWaypoint() },
                    onRemoveWaypoint = { index -> exploreViewModel.removeWaypoint(index) },
                    onCalculateRoute = { exploreViewModel.calculateRoute() },
                    onClose = { exploreViewModel.toggleRoutingMode() }
                )
            }
        }

        // Floating Action Buttons
        if (!isRoutingMode) {
            FloatingActionButton(
                onClick = { exploreViewModel.toggleRoutingMode() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .padding(bottom = 80.dp), // Avoid bottom dock
                containerColor = GlowBlue,
                contentColor = Color.Black,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Directions, contentDescription = "Plan Route")
            }
        }

        if (isFetchingDetails) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = GlowBlue)
            }
        }

        selectedPlaceDetail?.let { detail ->
            LocationDetailSheet(
                placeDetail = detail,
                placesRepository = placesRepository,
                onDismiss = { exploreViewModel.dismissDetails() },
                onAddToTrip = { /* Handle add to trip plan */ }
            )
        }
    }
}
