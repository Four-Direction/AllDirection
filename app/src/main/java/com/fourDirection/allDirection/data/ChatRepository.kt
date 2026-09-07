package com.fourDirection.allDirection.data

import com.fourDirection.allDirection.page.main.ChatMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class ChatRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val userId: String?
        get() = auth.currentUser?.uid

    suspend fun saveMessage(message: ChatMessage) {
        val uid = userId ?: return
        try {
            db.collection("users").document(uid)
                .collection("chats")
                .add(message)
                .await()
        } catch (e: Exception) {
            android.util.Log.e("ChatRepository", "Error saving message", e)
        }
    }

    suspend fun getChatHistory(): List<ChatMessage> {
        val uid = userId ?: return emptyList()
        return try {
            val snapshot = db.collection("users").document(uid)
                .collection("chats")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .await()
            
            snapshot.toObjects(ChatMessage::class.java)
        } catch (e: Exception) {
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
