package com.fourDirection.allDirection.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import android.util.Log

@Serializable
data class OpenExchangeResponse(
    val result: String,
    val base_code: String,
    val rates: Map<String, Double>
)

class CurrencyApiService {
    private val httpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
    }

    suspend fun fetchLatestRates(baseCurrency: String = "USD"): Map<String, Double> {
        return try {
            val url = "https://open.er-api.com/v6/latest/$baseCurrency"
            val response: OpenExchangeResponse = httpClient.get(url).body()
            if (response.result == "success") {
                response.rates
            } else {
                getFallbackRates()
            }
        } catch (e: Exception) {
            Log.e("CurrencyApi", "Failed to fetch rates", e)
            getFallbackRates()
        }
    }

    private fun getFallbackRates(): Map<String, Double> {
        return mapOf(
            "EUR" to 0.94,
            "GBP" to 0.81,
            "JPY" to 154.5,
            "AUD" to 1.55,
            "CAD" to 1.38,
            "CHF" to 0.91,
            "CNY" to 7.24,
            "HKD" to 7.83,
            "INR" to 83.5,
            "USD" to 1.0
        )
    }
}
