package com.example.davaroutes.data

data class LoginRequest(
    val email: String,
    val password: String
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
    val full_name: String,

    val profile_name: String?,
    val profile_type: String?,

    val vehicle_model: String?,
    val vehicle_year: Int?,
    val powertrain: String?
)

data class RegisterResponse(
    val access_token: String,
    val token_type: String,
    val user: AuthUser
)