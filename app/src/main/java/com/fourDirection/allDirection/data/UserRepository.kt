package com.fourDirection.allDirection.data

import android.util.Log
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class UserRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun saveUserToFirestore(user: FirebaseUser, customName: String? = null) {
        try {
            val userData = hashMapOf(
                "uid" to user.uid,
                "name" to (customName ?: user.displayName ?: ""),
                "email" to (user.email ?: ""),
                "photoUrl" to (user.photoUrl?.toString() ?: ""),
                "createdAt" to com.google.firebase.Timestamp.now(),
                "totalDistance" to 0.0,
                "lastLogin" to com.google.firebase.Timestamp.now()
            )

            db.collection("users").document(user.uid).set(userData).await()
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Error saving user to Firestore", e)
            throw e
        }
    }

    suspend fun getUserName(uid: String): String? {
        return try {
            val document = db.collection("users").document(uid).get().await()
            document.getString("name")
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getUserTravelData(uid: String): Map<String, Any>? {
        return try {
            val document = db.collection("users").document(uid).get().await()
            document.data
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateLastLogin(uid: String) {
        try {
            db.collection("users").document(uid).update("lastLogin", com.google.firebase.Timestamp.now()).await()
        } catch (e: Exception) {
            // Log error
        }
    }

    // --- Trip Management ---

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    suspend fun saveTrip(uid: String, trip: Trip) {
        try {
            val dayPlansData = trip.dayPlans.mapKeys { it.key.format(dateFormatter) }
                .mapValues { (_, plan) ->
                    mapOf(
                        "date" to plan.date.format(dateFormatter),
                        "description" to plan.description,
                        "location" to plan.location,
                        "events" to plan.events
                    )
                }

            val tripData = hashMapOf(
                "id" to trip.id,
                "name" to trip.name,
                "startDate" to trip.startDate.format(dateFormatter),
                "endDate" to trip.endDate.format(dateFormatter),
                "dayPlans" to dayPlansData
            )

            db.collection("users").document(uid)
                .collection("trips").document(trip.id)
                .set(tripData).await()
        } catch (e: Exception) {
            Log.e("UserRepository", "Error saving trip", e)
            throw e
        }
    }

    suspend fun getTrips(uid: String): List<Trip> {
        return try {
            val snapshot = db.collection("users").document(uid)
                .collection("trips").get().await()
            
            snapshot.documents.mapNotNull { doc ->
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
                    DayPlan(
                        date = LocalDate.parse(dateStr, dateFormatter),
                        description = data["description"] as? String ?: "",
                        location = data["location"] as? String ?: "",
                        events = eventsList?.filterIsInstance<String>() ?: emptyList()
                    )
                }.mapKeys { LocalDate.parse(it.key, dateFormatter) }

                Trip(id, name, startDate, endDate, dayPlans)
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Error getting trips", e)
            emptyList()
        }
    }

    suspend fun deleteTrip(uid: String, tripId: String) {
        try {
            db.collection("users").document(uid)
                .collection("trips").document(tripId)
                .delete().await()
        } catch (e: Exception) {
            Log.e("UserRepository", "Error deleting trip", e)
            throw e
        }
    }
}
