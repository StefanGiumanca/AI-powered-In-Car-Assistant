package com.example.davaroutes.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

const val EXTRA_VEHICLES_JSON = "vehicles_json"
const val EXTRA_SELECTED_VEHICLE_ID = "selected_vehicle_id"
const val EXTRA_CREATED_VEHICLE_JSON = "created_vehicle_json"
const val EXTRA_UPDATED_VEHICLE_JSON = "updated_vehicle_json"
const val EXTRA_DELETED_VEHICLE_ID = "deleted_vehicle_id"

const val VEHICLE_PREFS = "vehicle_preferences"
const val PREF_MAIN_VEHICLE_ID = "main_vehicle_id"

private val gson = Gson()
private val vehicleListType = object : TypeToken<List<VehicleResponse>>() {}.type

fun vehiclesToJson(vehicles: List<VehicleResponse>): String = gson.toJson(vehicles)

fun vehiclesFromJson(json: String?): List<VehicleResponse> {
    if (json.isNullOrBlank()) {
        return emptyList()
    }

    return runCatching {
        gson.fromJson<List<VehicleResponse>>(json, vehicleListType)
    }.getOrDefault(emptyList()).orEmpty()
}

fun vehicleToJson(vehicle: VehicleResponse): String = gson.toJson(vehicle)

fun vehicleFromJson(json: String?): VehicleResponse? {
    if (json.isNullOrBlank()) {
        return null
    }

    return runCatching {
        gson.fromJson(json, VehicleResponse::class.java)
    }.getOrNull()
}
