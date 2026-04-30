package com.example.davaroutes.network

import com.example.davaroutes.data.LoginRequest
import com.example.davaroutes.data.LoginResponse
import com.example.davaroutes.data.RegisterRequest
import com.example.davaroutes.data.RegisterResponse
import com.example.davaroutes.data.TripRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @POST("auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<RegisterResponse>

    @POST("trips")
    suspend fun createTrip(
        @Body trip: TripRequest
    ): Response<Unit>
}