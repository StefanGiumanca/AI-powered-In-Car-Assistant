package com.example.davaroutes.data

data class TripRequest (
    val user_id: String,
    val vehicle_id: String,
    val driver_profile_id: String?,

    val origin_label: String,
    val origin_lat: Double,
    val origin_lng: Double,

    val destination_label: String,
    val destination_lat: Double,
    val destination_lng: Double,
    val stops: List<RouteLocationDto> = emptyList(),

    val departure_time: String,
    val requested_mode: String,

    val current_range: String,
    val route_preferences: String
)
