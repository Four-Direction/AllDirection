package com.fourDirection.allDirection.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class ChatRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val userId: String?
        get() {
            val uid = auth.currentUser?.uid
            Log.d("ChatRepository", "Current User ID: $uid")
            return uid
        }

    suspend fun saveMessage(message: ChatMessage) {
        val uid = userId
        if (uid == null) {
            Log.w("ChatRepository", "Cannot save message: User not logged in")
            return
        }
        try {
            Log.d("ChatRepository", "Saving message to Firestore for user $uid: ${message.text}")
            db.collection("users").document(uid)
                .collection("chats")
                .add(message)
                .await()
            Log.d("ChatRepository", "Message saved successfully")
        } catch (e: Exception) {
            android.util.Log.e("ChatRepository", "Error saving message", e)
        }
    }

    suspend fun getChatHistory(): List<ChatMessage> {
        val uid = userId
        if (uid == null) {
            Log.w("ChatRepository", "Cannot get history: User not logged in")
            return emptyList()
        }
        return try {
            Log.d("ChatRepository", "Fetching chat history for user $uid")
            val snapshot = db.collection("users").document(uid)
                .collection("chats")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .await()
            
            val history = snapshot.toObjects(ChatMessage::class.java)
            Log.d("ChatRepository", "Fetched ${history.size} messages")
            history
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error getting chat history", e)
            emptyList()
        }
    }

    suspend fun clearHistory() {
        val uid = userId ?: return
        try {
            val collection = db.collection("users").document(uid).collection("chats")
            val snapshot = collection.get().await()
            
            db.runBatch { batch ->
                for (document in snapshot.documents) {
                    batch.delete(document.reference)
                }
            }.await()
        } catch (e: Exception) {
            android.util.Log.e("ChatRepository", "Error clearing history", e)
        }
    }
}
