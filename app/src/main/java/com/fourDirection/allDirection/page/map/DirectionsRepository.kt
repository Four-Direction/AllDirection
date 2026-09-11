package com.fourDirection.allDirection.page.map

import android.util.Log
import com.mapbox.geojson.Point
import com.mapbox.geojson.utils.PolylineUtils
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*

class DirectionsRepository(private val accessToken: String) {
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
        install(Logging) {
            level = LogLevel.INFO
        }
    }

    suspend fun getRoute(waypoints: List<Point>): RouteInfo? {
        if (waypoints.size < 2) return null

        val coordsString = waypoints.joinToString(";") { "${it.longitude()},${it.latitude()}" }
        val url = "https://api.mapbox.com/directions/v5/mapbox/driving/$coordsString"

        return try {
            val responseString = client.get(url) {
                parameter("access_token", accessToken)
                parameter("geometries", "polyline6")
                parameter("overview", "full")
            }.body<String>()

            val response = Json.parseToJsonElement(responseString).jsonObject
            val routes = response["routes"]?.jsonArray
            if (routes.isNullOrEmpty()) return null

            val route = routes[0].jsonObject
            val geometry = route["geometry"]?.jsonPrimitive?.content ?: ""
            val distance = route["distance"]?.jsonPrimitive?.doubleOrNull ?: 0.0
            val duration = route["duration"]?.jsonPrimitive?.doubleOrNull ?: 0.0

            val points = PolylineUtils.decode(geometry, 6)
            
            val minLon = points.minOfOrNull { it.longitude() } ?: 0.0
            val maxLon = points.maxOfOrNull { it.longitude() } ?: 0.0
            val minLat = points.minOfOrNull { it.latitude() } ?: 0.0
            val maxLat = points.maxOfOrNull { it.latitude() } ?: 0.0

            RouteInfo(
                points = points,
                distanceKm = distance / 1000.0,
                durationMin = duration / 60.0,
                bounds = listOf(
                    Point.fromLngLat(minLon, minLat),
                    Point.fromLngLat(maxLon, maxLat)
                )
            )
        } catch (e: Exception) {
            Log.e("DirectionsRepository", "Error fetching route", e)
            null
        }
    }
}
