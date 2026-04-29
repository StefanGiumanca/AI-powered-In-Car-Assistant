package com.example.davaroutes

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.davaroutes.data.TripRequest
import com.example.davaroutes.network.RetrofitClient
import com.example.davaroutes.ui.theme.DavaRoutesTheme
import kotlinx.coroutines.launch

class CreateTripActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DavaRoutesTheme {
                CreateTripScreen()
            }
        }
    }

    @Composable
    fun CreateTripScreen() {
        var userId by remember { mutableStateOf("") }
        var vehicleId by remember { mutableStateOf("") }
        var driverProfileId by remember { mutableStateOf("") }

        var originLabel by remember { mutableStateOf("") }
        var originLat by remember { mutableStateOf("") }
        var originLng by remember { mutableStateOf("") }

        var destinationLabel by remember { mutableStateOf("") }
        var destinationLat by remember { mutableStateOf("") }
        var destinationLng by remember { mutableStateOf("") }

        var departureTime by remember { mutableStateOf("") }
        var requestedMode by remember { mutableStateOf("") }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Create trip", style = MaterialTheme.typography.headlineMedium)

            OutlinedTextField(userId, { userId = it }, label = { Text("User ID") })
            OutlinedTextField(vehicleId, { vehicleId = it }, label = { Text("Vehicle ID") })
            OutlinedTextField(driverProfileId, { driverProfileId = it }, label = { Text("Driver Profile ID optional") })

            OutlinedTextField(originLabel, { originLabel = it }, label = { Text("Origin label") })
            OutlinedTextField(originLat, { originLat = it }, label = { Text("Origin latitude") })
            OutlinedTextField(originLng, { originLng = it }, label = { Text("Origin longitude") })

            OutlinedTextField(destinationLabel, { destinationLabel = it }, label = { Text("Destination label") })
            OutlinedTextField(destinationLat, { destinationLat = it }, label = { Text("Destination latitude") })
            OutlinedTextField(destinationLng, { destinationLng = it }, label = { Text("Destination longitude") })

            OutlinedTextField(
                departureTime,
                { departureTime = it },
                label = { Text("Departure time") },
                placeholder = { Text("2026-04-29T10:00:00") }
            )

            OutlinedTextField(
                requestedMode,
                { requestedMode = it },
                label = { Text("Requested mode") },
                placeholder = { Text("driver / passenger") }
            )

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val trip = TripRequest(
                        user_id = userId,
                        vehicle_id = vehicleId,
                        driver_profile_id = driverProfileId.ifBlank { null },

                        origin_label = originLabel,
                        origin_lat = originLat.toDoubleOrNull() ?: 0.0,
                        origin_lng = originLng.toDoubleOrNull() ?: 0.0,

                        destination_label = destinationLabel,
                        destination_lat = destinationLat.toDoubleOrNull() ?: 0.0,
                        destination_lng = destinationLng.toDoubleOrNull() ?: 0.0,

                        departure_time = departureTime,
                        requested_mode = requestedMode
                    )

                    lifecycleScope.launch {
                        try {
                            val response = RetrofitClient.api.createTrip(trip)

                            if (response.isSuccessful) {
                                Toast.makeText(
                                    this@CreateTripActivity,
                                    "Trip creat cu succes",
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
                Text("Send trip")
            }
        }
    }
}