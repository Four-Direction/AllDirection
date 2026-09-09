package com.fourDirection.allDirection.page.main

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fourDirection.allDirection.data.Trip
import com.fourDirection.allDirection.data.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.mapbox.search.*
import com.mapbox.search.result.SearchResult
import com.mapbox.search.result.SearchSuggestion
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CalendarViewModel : ViewModel() {
    private val userRepository = UserRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _trips = mutableStateListOf<Trip>()
    val trips: List<Trip> get() = _trips

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorEvents = MutableSharedFlow<String>()
    val errorEvents = _errorEvents.asSharedFlow()

    // --- Search Logic ---
    private var searchEngine: SearchEngine? = null
    private val _suggestions = MutableStateFlow<List<SearchSuggestion>>(emptyList())
    val suggestions = _suggestions.asStateFlow()

    fun initSearchEngine(accessToken: String) {
        if (searchEngine == null) {
            try {
                searchEngine = SearchEngine.createSearchEngineWithBuiltInDataProviders(
                    ApiType.SEARCH_BOX,
                    SearchEngineSettings()
                )
            } catch (e: Exception) {
                Log.e("CalendarViewModel", "Failed to init SearchEngine", e)
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

    fun onSearchQueryChanged(query: String) {
        if (query.isNotBlank()) {
            searchEngine?.search(query, SearchOptions(requestDebounce = 300), searchCallback)
        } else {
            _suggestions.value = emptyList()
        }
    }

    fun selectSuggestion(suggestion: SearchSuggestion, onResult: (String) -> Unit) {
        searchEngine?.select(suggestion, object : SearchSelectionCallback {
            override fun onResult(suggestion: SearchSuggestion, result: SearchResult, responseInfo: ResponseInfo) {
                onResult(suggestion.name)
                _suggestions.value = emptyList()
            }

            override fun onResults(suggestion: SearchSuggestion, results: List<SearchResult>, responseInfo: ResponseInfo) {
                if (results.isNotEmpty()) {
                    onResult(suggestion.name)
                }
                _suggestions.value = emptyList()
            }

            override fun onError(e: Exception) {
                _suggestions.value = emptyList()
            }

            override fun onSuggestions(suggestions: List<SearchSuggestion>, responseInfo: ResponseInfo) {}
        })
    }

    init {
        loadTrips()
    }

    fun loadTrips() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val loadedTrips = userRepository.getTrips(uid)
            _trips.clear()
            _trips.addAll(loadedTrips)
            _isLoading.value = false
        }
    }

    fun saveTrip(trip: Trip) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                _isLoading.value = true
                userRepository.saveTrip(uid, trip)
                // Refresh local list
                val index = _trips.indexOfFirst { it.id == trip.id }
                if (index != -1) {
                    _trips[index] = trip
                } else {
                    _trips.add(trip)
                }
            } catch (e: Exception) {
                _errorEvents.emit("Failed to save trip: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteTrip(tripId: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            userRepository.deleteTrip(uid, tripId)
            _trips.removeIf { it.id == tripId }
        }
    }
}
