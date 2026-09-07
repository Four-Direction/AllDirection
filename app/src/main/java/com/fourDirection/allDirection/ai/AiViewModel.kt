package com.fourDirection.allDirection.ai

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fourDirection.allDirection.data.ChatRepository
import com.fourDirection.allDirection.page.main.ChatMessage
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

    private fun loadChatHistory() {
        viewModelScope.launch {
            val history = chatRepository.getChatHistory()
            if (history.isEmpty()) {
                messages.add(ChatMessage("Hello! I'm your travel assistant. How can I help you today?", false))
            } else {
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
            chatRepository.clearHistory()
            messages.clear()
            messages.add(ChatMessage("Hello! I'm your travel assistant. How can I help you today?", false))
        }
    }
}
