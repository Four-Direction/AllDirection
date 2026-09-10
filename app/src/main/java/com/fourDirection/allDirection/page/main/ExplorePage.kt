package com.fourDirection.allDirection.page.main

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fourDirection.allDirection.data.PlaceDetail
import com.fourDirection.allDirection.data.PlacesRepository
import com.fourDirection.allDirection.ui.theme.GlowBlue
import com.mapbox.common.MapboxOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.search.*
import com.mapbox.search.result.SearchResult
import com.mapbox.search.result.SearchSuggestion
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ExploreViewModel : ViewModel() {
    private var placesRepository: PlacesRepository? = null

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _suggestions = MutableStateFlow<List<SearchSuggestion>>(emptyList())
    val suggestions = _suggestions.asStateFlow()

    private val _selectedPlaceDetail = MutableStateFlow<PlaceDetail?>(null)
    val selectedPlaceDetail = _selectedPlaceDetail.asStateFlow()

    private val _isFetchingDetails = MutableStateFlow(false)
    val isFetchingDetails = _isFetchingDetails.asStateFlow()

    private val _navigateMapTo = MutableStateFlow<Point?>(null)
    val navigateMapTo = _navigateMapTo.asStateFlow()

    private var searchEngine: SearchEngine? = null

    fun init(accessToken: String, repository: PlacesRepository) {
        this.placesRepository = repository
        if (searchEngine == null) {
            try {
                MapboxOptions.accessToken = accessToken
                searchEngine = SearchEngine.createSearchEngineWithBuiltInDataProviders(
                    ApiType.SEARCH_BOX,
                    SearchEngineSettings()
                )
            } catch (e: Exception) {
                Log.e("ExploreViewModel", "Failed to init SearchEngine", e)
            }
        }
    }

    private val searchCallback = object : SearchSuggestionsCallback {
        override fun onSuggestions(suggestions: List<SearchSuggestion>, responseInfo: ResponseInfo) {
            _suggestions.value = suggestions
        }

        override fun onError(e: Exception) {
            _suggestions.value = emptyList()
        }
    }

    fun onQueryChanged(query: String, proximity: Point? = null, autoSelect: Boolean = false) {
        _searchQuery.value = query
        if (query.isNotBlank()) {
            val options = SearchOptions(
                proximity = proximity,
                types = listOf(
                    QueryType.POI,
                    QueryType.ADDRESS,
                    QueryType.PLACE,
                    QueryType.NEIGHBORHOOD,
                    QueryType.LOCALITY
                ),
                requestDebounce = 500
            )
            
            if (autoSelect) {
                searchEngine?.search(query, options, object : SearchSuggestionsCallback {
                    override fun onSuggestions(suggestions: List<SearchSuggestion>, responseInfo: ResponseInfo) {
                        if (suggestions.isNotEmpty()) {
                            selectSuggestion(suggestions.first()) { /* Point handled inside selectSuggestion */ }
                        }
                    }
                    override fun onError(e: Exception) {}
                })
            } else {
                searchEngine?.search(query, options, searchCallback)
            }
        } else {
            _suggestions.value = emptyList()
        }
    }

    fun selectSuggestion(suggestion: SearchSuggestion, onResult: (Point) -> Unit = {}) {
        searchEngine?.select(suggestion, object : SearchSelectionCallback {
            override fun onResult(
                suggestion: SearchSuggestion,
                result: SearchResult,
                responseInfo: ResponseInfo
            ) {
                result.coordinate?.let { 
                    onResult(it)
                    _navigateMapTo.value = it
                }
                _suggestions.value = emptyList()
                _searchQuery.value = suggestion.name
                
                // Fetch Google Places details
                fetchPlaceDetails(result)
            }

            override fun onResults(
                suggestion: SearchSuggestion,
                results: List<SearchResult>,
                responseInfo: ResponseInfo
            ) {
                if (results.isNotEmpty()) {
                    val result = results.first()
                    result.coordinate?.let { 
                        onResult(it)
                        _navigateMapTo.value = it
                    }
                    fetchPlaceDetails(result)
                }
                _suggestions.value = emptyList()
                _searchQuery.value = suggestion.name
            }

            override fun onError(e: Exception) {
            }

            override fun onSuggestions(
                suggestions: List<SearchSuggestion>,
                responseInfo: ResponseInfo
            ) {
            }
        })
    }

    fun onNavigateHandled() {
        _navigateMapTo.value = null
    }

    private fun fetchPlaceDetails(result: SearchResult) {
        viewModelScope.launch {
            _isFetchingDetails.value = true
            // Check if there's a Google Place ID in externalIDs
            var googleId = result.externalIDs["google_place_id"]
            
            // Fallback: search for Google Place ID if missing
            if (googleId == null) {
                googleId = placesRepository?.searchForPlaceId(
                    name = result.name,
                    latitude = result.coordinate.latitude(),
                    longitude = result.coordinate.longitude()
                )
            }

            if (googleId != null) {
                _selectedPlaceDetail.value = placesRepository?.fetchPlaceDetails(googleId)
            } else {
                _selectedPlaceDetail.value = PlaceDetail(
                    id = result.id,
                    name = result.name,
                    address = result.fullAddress
                )
            }
            _isFetchingDetails.value = false
        }
    }

    fun dismissDetails() {
        _selectedPlaceDetail.value = null
    }
}

@Composable
fun ExplorePage(
    modifier: Modifier = Modifier,
    exploreViewModel: ExploreViewModel = viewModel(),
    hazeState: HazeState,
    shouldFocusSearch: Boolean = false,
    initialSearchQuery: String? = null,
    onSearchFocused: () -> Unit = {}
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

    LaunchedEffect(Unit) {
        MapboxOptions.accessToken = accessToken
        exploreViewModel.init(accessToken, placesRepository)
        isMapReady = true
        
        // Handle initial search if provided
        initialSearchQuery?.let { query ->
            exploreViewModel.onQueryChanged(query, autoSelect = true)
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
            )
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
