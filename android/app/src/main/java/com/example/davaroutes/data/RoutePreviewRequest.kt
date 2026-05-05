package com.example.davaroutes.data

data class RouteLocationDto(
    val label: String,
    val lat: Double,
    val lng: Double
)

data class RoutePreviewRequest(
    val origin: RouteLocationDto,
    val destination: RouteLocationDto,
    val current_range: String?,
    val route_preferences: String?
)
