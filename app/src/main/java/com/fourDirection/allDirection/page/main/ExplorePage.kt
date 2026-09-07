package com.fourDirection.allDirection.page.main

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mapbox.common.MapboxOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.search.*
import com.mapbox.search.result.SearchResult
import com.mapbox.search.result.SearchSuggestion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ExploreViewModel : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _suggestions = MutableStateFlow<List<SearchSuggestion>>(emptyList())
    val suggestions = _suggestions.asStateFlow()

    private var searchEngine: SearchEngine? = null

    fun initSearchEngine(accessToken: String) {
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

    fun onQueryChanged(query: String, proximity: Point? = null) {
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
            searchEngine?.search(query, options, searchCallback)
        } else {
            _suggestions.value = emptyList()
        }
    }

    fun selectSuggestion(suggestion: SearchSuggestion, onResult: (Point) -> Unit) {
        searchEngine?.select(suggestion, object : SearchSelectionCallback {
            override fun onResult(
                suggestion: SearchSuggestion,
                result: SearchResult,
                responseInfo: ResponseInfo
            ) {
                result.coordinate?.let { onResult(it) }
                _suggestions.value = emptyList()
                _searchQuery.value = suggestion.name
            }

            override fun onResults(
                suggestion: SearchSuggestion,
                results: List<SearchResult>,
                responseInfo: ResponseInfo
            ) {
                if (results.isNotEmpty()) {
                    results.first().coordinate?.let { onResult(it) }
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
}

@Composable
fun ExplorePage(
    modifier: Modifier = Modifier,
    exploreViewModel: ExploreViewModel = viewModel()
) {
    val accessToken = "pk.eyJ1IjoiamFuZGRpIiwiYSI6ImNtdG9qYmx1ejB1cTEyd29majMxYzRvenMifQ.MHg_MphmkzDyLjIYLdLnmQ"
    var isMapReady by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        MapboxOptions.accessToken = accessToken
        exploreViewModel.initSearchEngine(accessToken)
        isMapReady = true
    }

    val searchQuery by exploreViewModel.searchQuery.collectAsState()
    val suggestions by exploreViewModel.suggestions.collectAsState()

    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(101.6869, 3.1390)) // Kuala Lumpur
            zoom(12.0)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
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
                        modifier = Modifier.weight(1f),
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
                                    exploreViewModel.selectSuggestion(suggestions.first()) { point ->
                                        mapViewportState.flyTo(
                                            CameraOptions.Builder().center(point).zoom(15.0).build()
                                        )
                                    }
                                }
                            }
                        )
                    )
                }
            }

            if (suggestions.isNotEmpty()) {
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
                                        exploreViewModel.selectSuggestion(suggestion) { point ->
                                            mapViewportState.flyTo(
                                                CameraOptions.Builder().center(point).zoom(15.0).build()
                                            )
                                        }
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
