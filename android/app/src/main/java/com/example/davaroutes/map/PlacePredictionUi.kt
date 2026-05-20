package com.example.davaroutes.map

data class PlacePredictionUi(
    val placeId: String,
    val primaryText: String,
    val secondaryText: String
)

data class RouteStopUi(
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double
)
