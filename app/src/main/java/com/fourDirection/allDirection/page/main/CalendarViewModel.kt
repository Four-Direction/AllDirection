package com.fourDirection.allDirection.page.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fourDirection.allDirection.data.DayPlan
import com.fourDirection.allDirection.data.Trip
import com.fourDirection.allDirection.data.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.mapbox.search.*
import com.mapbox.search.result.SearchResult
import com.mapbox.search.result.SearchSuggestion
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class CalendarViewModel : ViewModel() {
    private val userRepository = UserRepository()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _trips = MutableStateFlow<List<Trip>>(emptyList())
    val trips = _trips.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorEvents = MutableSharedFlow<String>()
    val errorEvents = _errorEvents.asSharedFlow()

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    // --- Search Logic ---
    private var searchEngine: SearchEngine? = null
    private val _suggestions = MutableStateFlow<List<SearchSuggestion>>(emptyList())
    val suggestions = _suggestions.asStateFlow()

    private var tripsListener: ListenerRegistration? = null

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val uid = firebaseAuth.currentUser?.uid
        if (uid != null) {
            startTripsListener(uid)
        } else {
            stopTripsListener()
            _trips.value = emptyList()
        }
    }

    init {
        auth.addAuthStateListener(authStateListener)
    }

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

    private fun startTripsListener(uid: String) {
        tripsListener?.remove()
        tripsListener = db.collection("users").document(uid)
            .collection("trips")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("CalendarViewModel", "Error listening to trips", e)
                    return@addSnapshotListener
                }

                val loadedTrips = snapshot?.documents?.mapNotNull { doc ->
                    val id = doc.getString("id") ?: return@mapNotNull null
                    val name = doc.getString("name") ?: ""
                    val startStr = doc.getString("startDate") ?: return@mapNotNull null
                    val endStr = doc.getString("endDate") ?: return@mapNotNull null
                    
                    val startDate = LocalDate.parse(startStr, dateFormatter)
                    val endDate = LocalDate.parse(endStr, dateFormatter)
                    
                    @Suppress("UNCHECKED_CAST")
                    val dayPlansRaw = doc.get("dayPlans") as? Map<String, Map<String, Any>> ?: emptyMap()
                    
                    val dayPlans = dayPlansRaw.mapValues { (dateStr, data) ->
                        val eventsList = data["events"] as? List<*>
                        val locationsList = data["locations"] as? List<*> ?: data["location"]?.let { listOf(it) } ?: emptyList<Any>()
                        DayPlan(
                            date = LocalDate.parse(dateStr, dateFormatter),
                            description = data["description"] as? String ?: "",
                            locations = locationsList.filterIsInstance<String>(),
                            events = eventsList?.filterIsInstance<String>() ?: emptyList(),
                            hotel = data["hotel"] as? String,
                            startLocation = data["startLocation"] as? String,
                            endLocation = data["endLocation"] as? String
                        )
                    }.mapKeys { LocalDate.parse(it.key, dateFormatter) }

                    Trip(id, name, startDate, endDate, dayPlans)
                } ?: emptyList()

                _trips.value = loadedTrips
            }
    }

    private fun stopTripsListener() {
        tripsListener?.remove()
        tripsListener = null
    }

    override fun onCleared() {
        auth.removeAuthStateListener(authStateListener)
        stopTripsListener()
        super.onCleared()
    }

    fun saveTrip(trip: Trip) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                _isLoading.value = true
                userRepository.saveTrip(uid, trip)
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
            try {
                userRepository.deleteTrip(uid, tripId)
            } catch (e: Exception) {
                _errorEvents.emit("Failed to delete trip: ${e.message}")
            }
        }
    }
}
