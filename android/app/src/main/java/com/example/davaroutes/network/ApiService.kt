package com.example.davaroutes.network

import com.example.davaroutes.data.TripRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("trips")
    suspend fun createTrip(
        @Body trip: TripRequest
    ): Response<Unit>
}