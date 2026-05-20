package com.example.davaroutes

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.davaroutes.data.RecommendationQueryRequest
import com.example.davaroutes.data.RouteLocationDto
import com.example.davaroutes.data.TripRequest
import com.example.davaroutes.network.RetrofitClient
import com.example.davaroutes.ui.theme.DavaRoutesTheme
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class CreateTripActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val userId = intent.getStringExtra("user_id") ?: ""
        val originLat = intent.getDoubleExtra("origin_lat", 0.0)
        val originLng = intent.getDoubleExtra("origin_lng", 0.0)

        val destinationName = intent.getStringExtra("destination_name") ?: "Destination"
        val destinationLat = intent.getDoubleExtra("destination_lat", 0.0)
        val destinationLng = intent.getDoubleExtra("destination_lng", 0.0)

        val currentRange = intent.getStringExtra("current_range") ?: ""
        val routePreferences = intent.getStringExtra("route_preferences") ?: ""
        val vehicleId = intent.getStringExtra("vehicle_id") ?: ""
        val driverProfileId = intent.getStringExtra("driver_profile_id") ?: ""

        setContent {
            DavaRoutesTheme {
                CreateTripScreen(
                    userId = userId,
                    originLat = originLat,
                    originLng = originLng,
                    destinationName = destinationName,
                    destinationLat = destinationLat,
                    destinationLng = destinationLng,
                    initialCurrentRange = currentRange,
                    initialRoutePreferences = routePreferences,
                    vehicleId = vehicleId,
                    driverProfileId = driverProfileId
                )
            }
        }
    }

    @Composable
    fun CreateTripScreen(
        userId: String,
        originLat: Double,
        originLng: Double,
        destinationName: String,
        destinationLat: Double,
        destinationLng: Double,
        initialCurrentRange: String,
        initialRoutePreferences: String,
        vehicleId: String,
        driverProfileId: String
    ) {
        var currentRange by remember { mutableStateOf(initialCurrentRange) }
        var routePreferences by remember { mutableStateOf(initialRoutePreferences) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Configurează traseul",
                style = MaterialTheme.typography.headlineMedium
            )

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Plecare: locația ta curentă")
                    Text("Destinație: $destinationName")
                }
            }

            OutlinedTextField(
                value = currentRange,
                onValueChange = { currentRange = it },
                label = { Text("Range actual") },
                placeholder = { Text("Ex: 120 km") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = routePreferences,
                onValueChange = { routePreferences = it },
                label = { Text("Preferințe pe traseu") },
                placeholder = {
                    Text("Ex: benzinării, restaurante, stații de încărcare")
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (userId.isBlank()) {
                        Toast.makeText(
                            this@CreateTripActivity,
                            "User ID lipsă",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@Button
                    }

                    if (
                        originLat == 0.0 ||
                        originLng == 0.0 ||
                        destinationLat == 0.0 ||
                        destinationLng == 0.0
                    ) {
                        Toast.makeText(
                            this@CreateTripActivity,
                            "Locația sau destinația lipsesc",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@Button
                    }

                    if (currentRange.isBlank()) {
                        Toast.makeText(
                            this@CreateTripActivity,
                            "Introdu range-ul actual",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@Button
                    }

                    lifecycleScope.launch {
                        try {
                            val recommendedStop = findAiRecommendedStop(
                                userId = userId,
                                vehicleId = vehicleId,
                                driverProfileId = driverProfileId,
                                query = routePreferences,
                                latitude = originLat,
                                longitude = originLng
                            )

                            val trip = TripRequest(
                                user_id = userId,
                                vehicle_id = vehicleId,
                                driver_profile_id = driverProfileId.ifBlank { null },

                                origin_label = "Current location",
                                origin_lat = originLat,
                                origin_lng = originLng,

                                destination_label = destinationName,
                                destination_lat = destinationLat,
                                destination_lng = destinationLng,
                                stops = listOfNotNull(recommendedStop),

                                departure_time = LocalDateTime.now().toString(),
                                requested_mode = "driver",

                                current_range = currentRange,
                                route_preferences = routePreferences
                            )

                            val response = RetrofitClient.api.createTrip(trip)

                            if (response.isSuccessful) {
                                Toast.makeText(
                                    this@CreateTripActivity,
                                    "Ruta a fost trimisă către server",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    this@CreateTripActivity,
                                    "Eroare server: ${response.code()}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(
                                this@CreateTripActivity,
                                "Eroare: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            ) {
                Text("Găsește ruta")
            }
        }
    }

    private suspend fun findAiRecommendedStop(
        userId: String,
        vehicleId: String,
        driverProfileId: String,
        query: String,
        latitude: Double,
        longitude: Double
    ): RouteLocationDto? {
        if (
            query.isBlank() ||
            userId.isBlank()
        ) {
            return null
        }

        val response = RetrofitClient.api.recommendFromDriverQuery(
            RecommendationQueryRequest(
                user_id = userId,
                vehicle_id = vehicleId.ifBlank { null },
                driver_profile_id = driverProfileId.ifBlank { null },
                query = query,
                latitude = latitude,
                longitude = longitude
            )
        )

        return response.body()
            ?.candidates
            ?.firstOrNull()
            ?.let { candidate ->
                RouteLocationDto(
                    label = candidate.name,
                    lat = candidate.latitude,
                    lng = candidate.longitude
                )
            }
    }
}
