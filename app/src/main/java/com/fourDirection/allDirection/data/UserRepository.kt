package com.fourDirection.allDirection.data

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

class UserRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun saveUserToFirestore(user: FirebaseUser, customName: String? = null) {
        try {
            val userData = hashMapOf(
                "uid" to user.uid,
                "name" to (customName ?: user.displayName ?: ""),
                "name_lowercase" to (customName ?: user.displayName ?: "").lowercase(),
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
                        "locations" to plan.locations,
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
                    val locationsList = data["locations"] as? List<*> ?: data["location"]?.let { listOf(it) } ?: emptyList<Any>()
                    DayPlan(
                        date = LocalDate.parse(dateStr, dateFormatter),
                        description = data["description"] as? String ?: "",
                        locations = locationsList.filterIsInstance<String>(),
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

    // --- Connections Management ---

    suspend fun searchUsersByName(query: String): List<Map<String, Any>> {
        val lowercaseQuery = query.lowercase()
        return try {
            // Search using name_lowercase for case-insensitive matches
            val lowercaseSnapshot = db.collection("users")
                .whereGreaterThanOrEqualTo("name_lowercase", lowercaseQuery)
                .whereLessThanOrEqualTo("name_lowercase", lowercaseQuery + "\uf8ff")
                .limit(10)
                .get().await()
            
            val results = lowercaseSnapshot.documents.mapNotNull { it.data }.toMutableList()

            // If we didn't find enough results, try searching by original name field (case-sensitive)
            // for users who haven't logged in since the name_lowercase change.
            if (results.size < 5) {
                val legacySnapshot = db.collection("users")
                    .whereGreaterThanOrEqualTo("name", query)
                    .whereLessThanOrEqualTo("name", query + "\uf8ff")
                    .limit(10)
                    .get().await()
                
                legacySnapshot.documents.forEach { doc ->
                    val data = doc.data
                    if (data != null && results.none { it["uid"] == data["uid"] }) {
                        results.add(data)
                    }
                }
            }
            
            results.take(10)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error searching users", e)
            emptyList()
        }
    }

    suspend fun addConnection(uid: String, connectionUid: String, connectionName: String) {
        try {
            val connectionData = hashMapOf(
                "uid" to connectionUid,
                "name" to connectionName,
                "addedAt" to Timestamp.now()
            )
            
            // Add to current user's connections
            db.collection("users").document(uid)
                .collection("friends").document(connectionUid)
                .set(connectionData).await()
                
            // Mutual connection
            val currentUserName = getUserName(uid) ?: "User"
            val meData = hashMapOf(
                "uid" to uid,
                "name" to currentUserName,
                "addedAt" to Timestamp.now()
            )
            db.collection("users").document(connectionUid)
                .collection("friends").document(uid)
                .set(meData).await()
                
        } catch (e: Exception) {
            Log.e("UserRepository", "Error adding connection", e)
            throw e
        }
    }

    suspend fun getConnections(uid: String): List<Map<String, Any>> {
        return try {
            val snapshot = db.collection("users").document(uid)
                .collection("friends")
                .orderBy("name")
                .get().await()
            
            snapshot.documents.mapNotNull { it.data }
        } catch (e: Exception) {
            Log.e("UserRepository", "Error getting connections", e)
            emptyList()
        }
    }

    // --- Groups Management ---

    suspend fun createGroup(uid: String, groupName: String, members: List<Map<String, String>>) {
        try {
            val groupId = UUID.randomUUID().toString()
            val groupData = hashMapOf(
                "id" to groupId,
                "name" to groupName,
                "createdBy" to uid,
                "createdAt" to Timestamp.now(),
                "memberUids" to (members.map { it["uid"] ?: "" } + uid).distinct()
            )

            // 1. Create the group in a global groups collection
            db.collection("groups").document(groupId).set(groupData).await()

            // 2. Add group reference to all members
            val batch = db.batch()
            val allMemberUids = (members.map { it["uid"] ?: "" } + uid).distinct()
            
            allMemberUids.forEach { memberUid ->
                val memberGroupRef = db.collection("users").document(memberUid)
                    .collection("myGroups").document(groupId)
                batch.set(memberGroupRef, mapOf(
                    "groupId" to groupId,
                    "groupName" to groupName,
                    "joinedAt" to Timestamp.now()
                ))
            }
            batch.commit().await()
        } catch (e: Exception) {
            Log.e("UserRepository", "Error creating group", e)
            throw e
        }
    }

    suspend fun getMyGroups(uid: String): List<Map<String, Any>> {
        return try {
            val snapshot = db.collection("users").document(uid)
                .collection("myGroups")
                .orderBy("groupName")
                .get().await()
            
            snapshot.documents.mapNotNull { it.data }
        } catch (e: Exception) {
            Log.e("UserRepository", "Error getting groups", e)
            emptyList()
        }
    }
}
