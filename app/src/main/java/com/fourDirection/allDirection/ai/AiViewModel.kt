package com.fourDirection.allDirection.ai

import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fourDirection.allDirection.data.ChatMessage
import com.fourDirection.allDirection.data.ChatRepository
import kotlinx.coroutines.launch

class AiViewModel : ViewModel() {
    private val aiService = AiService()
    private val chatRepository = ChatRepository()
    
    val messages = mutableStateListOf<ChatMessage>()
    
    var isLoading by mutableStateOf(false)
        private set

    init {
        loadChatHistory()
    }

    fun loadChatHistory() {
        viewModelScope.launch {
            Log.d("AiViewModel", "Loading chat history...")
            val history = chatRepository.getChatHistory()
            messages.clear()
            // Always start with the welcome message if the history is empty
            if (history.isEmpty()) {
                Log.d("AiViewModel", "History empty, adding welcome message")
                messages.add(ChatMessage("Hello! I'm your travel assistant. How can I help you today?", false))
            } else {
                Log.d("AiViewModel", "Adding ${history.size} messages to UI")
                messages.addAll(history)
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        
        val userMessage = ChatMessage(text, true)
        messages.add(userMessage)
        isLoading = true
        
        viewModelScope.launch {
            Log.d("AiViewModel", "Sending message and saving to Firestore: $text")
            // Save user message
            chatRepository.saveMessage(userMessage)
            
            val response = aiService.sendMessage(text)
            val aiMessage = ChatMessage(response, false)
            messages.add(aiMessage)
            
            // Save AI response
            chatRepository.saveMessage(aiMessage)
            
            isLoading = false
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            android.util.Log.d("AiViewModel", "Clearing chat history...")
            chatRepository.clearHistory()
            messages.clear()
            messages.add(ChatMessage("Hello! I'm your travel assistant. How can I help you today?", false))
        }
    }
}
