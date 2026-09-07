package com.fourDirection.allDirection.data

import com.google.firebase.firestore.PropertyName

data class ChatMessage(
    @get:PropertyName("text")
    @set:PropertyName("text")
    var text: String = "",

    @get:PropertyName("isUser")
    @set:PropertyName("isUser")
    var isUser: Boolean = false,

    @get:PropertyName("timestamp")
    @set:PropertyName("timestamp")
    var timestamp: Long = System.currentTimeMillis()
)
