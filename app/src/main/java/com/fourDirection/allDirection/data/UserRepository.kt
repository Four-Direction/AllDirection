package com.fourDirection.allDirection.data

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun saveUserToFirestore(user: FirebaseUser, customName: String? = null) {
        val userData = hashMapOf(
            "uid" to user.uid,
            "name" to (customName ?: user.displayName ?: ""),
            "email" to (user.email ?: ""),
            "photoUrl" to (user.photoUrl?.toString() ?: ""),
            "createdAt" to com.google.firebase.Timestamp.now()
        )

        db.collection("users").document(user.uid).set(userData).await()
    }
}
