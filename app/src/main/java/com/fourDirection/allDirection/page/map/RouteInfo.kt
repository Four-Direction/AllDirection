package com.fourDirection.allDirection.page.map

import com.mapbox.geojson.Point

data class Waypoint(
    val name: String = "",
    val point: Point? = null,
    val isPlaceholder: Boolean = false
)

data class RouteInfo(
    val points: List<Point>,
    val distanceKm: Double,
    val durationMin: Double,
    val bounds: List<Point> // Used for fitting the map
)
