package com.example.davaroutes.data

data class RecommendationQueryRequest(
    val user_id: String,
    val vehicle_id: String? = null,
    val driver_profile_id: String? = null,
    val query: String,
    val latitude: Double,
    val longitude: Double,
    val trip_id: String? = null
)

data class RecommendationQueryResponse(
    val message: String,
    val candidates: List<RecommendationCandidateResponse>
)

data class RecommendationCandidateResponse(
    val google_place_id: String,
    val name: String,
    val address: String?,
    val latitude: Double,
    val longitude: Double,
    val rating: Double?,
    val score: Double,
    val reason: String,
    val partner_name: String?,
    val offer_title: String?,
    val loyalty_points: Int?,
    val matches_requested_brand: Boolean
)
