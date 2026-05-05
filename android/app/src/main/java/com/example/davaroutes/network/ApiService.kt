package com.example.davaroutes.network

import com.example.davaroutes.data.LoginRequest
import com.example.davaroutes.data.LoginResponse
import com.example.davaroutes.data.RegisterRequest
import com.example.davaroutes.data.RegisterResponse
import com.example.davaroutes.data.BootstrapResponse
import com.example.davaroutes.data.RoutePreviewRequest
import com.example.davaroutes.data.TripRequest
import com.example.davaroutes.data.TripResponse
import com.example.davaroutes.data.VehicleCreateRequest
import com.example.davaroutes.data.VehicleResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Header
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @POST("auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<RegisterResponse>

    @GET("me/bootstrap")
    suspend fun bootstrap(
        @Header("Authorization") authorization: String
    ): Response<BootstrapResponse>

    @POST("trips")
    suspend fun createTrip(
        @Body trip: TripRequest
    ): Response<Unit>

    @POST("vehicles")
    suspend fun createVehicle(
        @Header("Authorization") authorization: String,
        @Body vehicle: VehicleCreateRequest
    ): Response<VehicleResponse>

    @POST("routes/preview")
    suspend fun previewRoute(
        @Body trip: RoutePreviewRequest
    ): Response<TripResponse>

    @DELETE("vehicles/{vehicleId}")
    suspend fun deleteVehicle(
        @Path("vehicleId") vehicleId: String,
        @Header("Authorization") authorization: String
    ): Response<Unit>
}
