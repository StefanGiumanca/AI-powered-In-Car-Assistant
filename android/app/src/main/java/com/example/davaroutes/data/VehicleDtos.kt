package com.example.davaroutes.data

data class VehicleCreateRequest(
    val vin: String?,
    val model: String,
    val year: Int?,
    val powertrain: String,
    val connector_type: String?,
    val battery_capacity_kwh: Double?,
    val fuel_tank_liters: Double?,
    val consumption_kwh_per_100km: Double?,
    val consumption_l_per_100km: Double?,
    val service_interval_km: Int?,
    val service_interval_months: Int?
)

data class VehicleResponse(
    val id: String,
    val vin: String?,
    val model: String,
    val year: Int?,
    val powertrain: String,
    val connector_type: String?,
    val battery_capacity_kwh: Double?,
    val fuel_tank_liters: Double?,
    val consumption_kwh_per_100km: Double?,
    val consumption_l_per_100km: Double?,
    val service_interval_km: Int?,
    val service_interval_months: Int?
)
