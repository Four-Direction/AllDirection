package com.fourDirection.allDirection.page.main

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fourDirection.allDirection.data.Trip
import com.fourDirection.allDirection.data.UserRepository
import com.google.firebase.auth.FirebaseAuth
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
