package com.fourDirection.allDirection.data

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

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
}
