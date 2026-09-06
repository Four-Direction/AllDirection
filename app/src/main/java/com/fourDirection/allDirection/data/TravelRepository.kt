package com.fourDirection.allDirection.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class TrendingCity(
    val name: String,
    val country: String,
    val imageUrl: String,
    val description: String = ""
)

class TravelRepository(context: Context) {
    
    /**
     * Fetches trending cities with live image URLs from a placeholder travel API.
     * In a production app, you would use Retrofit to call a real travel service 
     * like Amadeus or TripAdvisor, and fetch photos using the Google Places API.
     */
    suspend fun getTrendingCities(): List<TrendingCity> = withContext(Dispatchers.IO) {
        // Simulating a live API call that returns city data and image URLs
        listOf(
            TrendingCity(
                name = "Paris",
                country = "France",
                imageUrl = "https://images.unsplash.com/photo-1502602898657-3e91760cbb34?q=80&w=800",
                description = "The City of Light"
            ),
            TrendingCity(
                name = "Tokyo",
                country = "Japan",
                imageUrl = "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?q=80&w=800",
                description = "A neon-lit metropolis"
            ),
            TrendingCity(
                name = "New York",
                country = "USA",
                imageUrl = "https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?q=80&w=800",
                description = "The Big Apple"
            ),
            TrendingCity(
                name = "Rome",
                country = "Italy",
                imageUrl = "https://images.unsplash.com/photo-1552832230-c0197dd311b5?q=80&w=800",
                description = "The Eternal City"
            ),
            TrendingCity(
                name = "London",
                country = "UK",
                imageUrl = "https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?q=80&w=800",
                description = "Historic and modern"
            ),
            TrendingCity(
                name = "Bangkok",
                country = "Thailand",
                imageUrl = "https://images.unsplash.com/photo-1508009603885-50cf7c579365?q=80&w=800",
                description = "Vibrant street life"
            )
        )
    }
}
