package com.example.davaroutes.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

const val EXTRA_DRIVER_PROFILES_JSON = "driver_profiles_json"

data class DriverProfileResponse(
    val id: String,
    val profile_name: String?,
    val profile_type: String,
    val preferred_amenities: List<String>,
    val preferred_brands: List<String>,
    val avoid_tolls: Boolean,
    val avoid_highways: Boolean,
    val max_detour_minutes: Int,
    val break_frequency_minutes: Int
)

private val driverProfileGson = Gson()
private val driverProfileListType = object : TypeToken<List<DriverProfileResponse>>() {}.type

fun driverProfilesToJson(driverProfiles: List<DriverProfileResponse>): String =
    driverProfileGson.toJson(driverProfiles)

fun driverProfilesFromJson(json: String?): List<DriverProfileResponse> {
    if (json.isNullOrBlank()) {
        return emptyList()
    }

    return runCatching {
        driverProfileGson.fromJson<List<DriverProfileResponse>>(json, driverProfileListType)
    }.getOrDefault(emptyList()).orEmpty()
}
