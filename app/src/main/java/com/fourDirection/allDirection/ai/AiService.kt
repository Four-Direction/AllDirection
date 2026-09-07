package com.fourDirection.allDirection.ai

import android.util.Log
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class AiRequest(val prompt: String)

@Serializable
data class AiResponse(val reply: String)

class AiService {
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            })
        }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    Log.d("AiService", message)
                }
            }
            level = LogLevel.ALL
        }
    }

    suspend fun sendMessage(message: String): String {
        return try {
            Log.d("AiService", "Sending message: $message")
            val response = client.post("https://ai-render-backend-nigz.onrender.com/api/chat") {
                contentType(ContentType.Application.Json)
                setBody(AiRequest(prompt = message))
            }
            
            if (response.status.isSuccess()) {
                val aiResponse: AiResponse = response.body()
                Log.d("AiService", "Received response: ${aiResponse.reply}")
                aiResponse.reply
            } else {
                Log.e("AiService", "Server error: ${response.status}")
                val errorBody = response.bodyAsText()
                "Error: ${response.status}. $errorBody"
            }
        } catch (e: Exception) {
            Log.e("AiService", "Exception: ", e)
            "Error: ${e.message ?: "Connection failed."}"
        }
    }
}
