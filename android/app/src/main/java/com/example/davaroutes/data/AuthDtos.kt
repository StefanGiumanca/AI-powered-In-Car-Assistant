package com.example.davaroutes.data

data class LoginRequest(
    val email: String,
    val password: String
)

data class GoogleLoginRequest(
    val id_token: String
)

data class LoginResponse(
    val access_token: String,
    val token_type: String,
    val user: AuthUser
)

data class AuthUser(
    val id: String,
    val email: String,
    val full_name: String
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val full_name: String
)

data class RegisterResponse(
    val id: String,
    val email: String,
    val full_name: String
)

data class BootstrapResponse(
    val user: AuthUser,
    val vehicles: List<VehicleResponse>,
    val driver_profiles: List<DriverProfileResponse>?,
    val latest_vehicle_state: Any?
)
