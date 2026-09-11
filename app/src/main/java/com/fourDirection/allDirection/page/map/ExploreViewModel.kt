package com.fourDirection.allDirection.page.map

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fourDirection.allDirection.data.PlaceDetail
import com.fourDirection.allDirection.data.PlacesRepository
import com.mapbox.common.MapboxOptions
import com.mapbox.geojson.Point
import com.mapbox.search.*
import com.mapbox.search.result.SearchResult
import com.mapbox.search.result.SearchSuggestion
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

    private val _isRoutingMode = MutableStateFlow(false)
    val isRoutingMode = _isRoutingMode.asStateFlow()

    private val _waypoints = MutableStateFlow<List<Waypoint>>(
        listOf(Waypoint(name = "Start", isPlaceholder = true), Waypoint(name = "End", isPlaceholder = true))
    )
    val waypoints = _waypoints.asStateFlow()

    private val _currentRoute = MutableStateFlow<RouteInfo?>(null)
    val currentRoute = _currentRoute.asStateFlow()

    private val _isCalculatingRoute = MutableStateFlow(false)
    val isCalculatingRoute = _isCalculatingRoute.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    private var searchEngine: SearchEngine? = null
    private var directionsRepository: DirectionsRepository? = null

    fun init(accessToken: String, repository: PlacesRepository) {
        this.placesRepository = repository
        if (searchEngine == null) {
            try {
                MapboxOptions.accessToken = accessToken
                searchEngine = SearchEngine.createSearchEngineWithBuiltInDataProviders(
                    ApiType.SEARCH_BOX,
                    SearchEngineSettings()
                )
                directionsRepository = DirectionsRepository(accessToken)
            } catch (e: Exception) {
                Log.e("ExploreViewModel", "Failed to init SearchEngine", e)
            }
        }
    }

    private val searchCallback = object : SearchSuggestionsCallback {
        override fun onSuggestions(suggestions: List<SearchSuggestion>, responseInfo: ResponseInfo) {
            Log.d("ExploreViewModel", "Received ${suggestions.size} suggestions")
            _suggestions.value = suggestions
            _isSearching.value = false
        }

        override fun onError(e: Exception) {
            Log.e("ExploreViewModel", "Search error", e)
            _suggestions.value = emptyList()
            _isSearching.value = false
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

    fun clearSuggestions() {
        _suggestions.value = emptyList()
        _isSearching.value = false
    }

    // --- Routing Logic ---

    fun toggleRoutingMode() {
        _isRoutingMode.value = !_isRoutingMode.value
        if (!_isRoutingMode.value) {
            clearRoute()
        }
    }

    fun addWaypoint() {
        val current = _waypoints.value.toMutableList()
        if (current.size >= 15) return // Limit to 15 waypoints
        val insertionIndex = current.size - 1
        current.add(insertionIndex, Waypoint(name = "Waypoint ${current.size - 1}", isPlaceholder = true))
        _waypoints.value = current
    }

    fun removeWaypoint(index: Int) {
        val current = _waypoints.value.toMutableList()
        if (current.size <= 2) return
        current.removeAt(index)
        _waypoints.value = current
    }

    fun onWaypointQueryChanged(index: Int, query: String, proximity: Point? = null) {
        Log.d("ExploreViewModel", "onWaypointQueryChanged: index=$index, query=$query")
        val current = _waypoints.value.toMutableList()
        if (index < current.size) {
            current[index] = current[index].copy(name = query, isPlaceholder = query.isEmpty())
            _waypoints.value = current
        }

        if (query.isNotBlank()) {
            _isSearching.value = true
            // Don't clear suggestions immediately to avoid flickering
            val options = SearchOptions(
                proximity = proximity,
                types = listOf(QueryType.POI, QueryType.ADDRESS, QueryType.PLACE),
                requestDebounce = 300
            )
            try {
                searchEngine?.search(query, options, searchCallback)
            } catch (e: Exception) {
                Log.e("ExploreViewModel", "Search engine error", e)
                _isSearching.value = false
            }
        } else {
            _suggestions.value = emptyList()
            _isSearching.value = false
        }
    }

    fun updateWaypoint(index: Int, suggestion: SearchSuggestion) {
        searchEngine?.select(suggestion, object : SearchSelectionCallback {
            override fun onResult(suggestion: SearchSuggestion, result: SearchResult, responseInfo: ResponseInfo) {
                result.coordinate?.let { point ->
                    val current = _waypoints.value.toMutableList()
                    if (index < current.size) {
                        current[index] = Waypoint(name = suggestion.name, point = point, isPlaceholder = false)
                        _waypoints.value = current
                    }
                }
                _suggestions.value = emptyList()
            }
            override fun onResults(suggestion: SearchSuggestion, results: List<SearchResult>, responseInfo: ResponseInfo) {
                if (results.isNotEmpty()) {
                    results.first().coordinate?.let { point ->
                        val current = _waypoints.value.toMutableList()
                        if (index < current.size) {
                            current[index] = Waypoint(name = suggestion.name, point = point, isPlaceholder = false)
                            _waypoints.value = current
                        }
                    }
                }
                _suggestions.value = emptyList()
            }
            override fun onError(e: Exception) {}
            override fun onSuggestions(suggestions: List<SearchSuggestion>, responseInfo: ResponseInfo) {}
        })
    }

    fun calculateRoute() {
        val points = _waypoints.value.mapNotNull { it.point }
        if (points.size < 2) return

        viewModelScope.launch {
            _isCalculatingRoute.value = true
            val route = directionsRepository?.getRoute(points)
            _currentRoute.value = route
            _isCalculatingRoute.value = false
        }
    }

    fun clearRoute() {
        _currentRoute.value = null
        _waypoints.value = listOf(
            Waypoint(name = "Start", isPlaceholder = true),
            Waypoint(name = "End", isPlaceholder = true)
        )
    }
}
