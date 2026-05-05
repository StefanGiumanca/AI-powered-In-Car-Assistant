package com.example.davaroutes

import android.os.Bundle
import android.content.Intent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.davaroutes.ui.theme.DarkNavy
import com.example.davaroutes.ui.theme.DavaRoutesTheme
import com.example.davaroutes.ui.theme.MutedText
import com.example.davaroutes.ui.theme.NavyCard
import com.example.davaroutes.ui.theme.Orange
import com.example.davaroutes.ui.theme.SoftWhite
import com.example.davaroutes.data.EXTRA_DELETED_VEHICLE_ID
import com.example.davaroutes.data.EXTRA_SELECTED_VEHICLE_ID
import com.example.davaroutes.data.EXTRA_VEHICLES_JSON
import com.example.davaroutes.data.VehicleResponse
import com.example.davaroutes.data.vehiclesFromJson
import com.example.davaroutes.network.RetrofitClient
import kotlinx.coroutines.launch

class VehicleProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val accessToken = intent.getStringExtra("access_token") ?: ""
        val tokenType = intent.getStringExtra("token_type") ?: "bearer"
        val vehicles = vehiclesFromJson(intent.getStringExtra(EXTRA_VEHICLES_JSON))
        val selectedVehicleId = intent.getStringExtra(EXTRA_SELECTED_VEHICLE_ID)

        setContent {
            DavaRoutesTheme {
                VehicleProfileContent(
                    accessToken = accessToken,
                    tokenType = tokenType,
                    vehicles = vehicles,
                    selectedVehicleId = selectedVehicleId,
                    activity = this@VehicleProfileActivity
                )
            }
        }
    }

    @Composable
    private fun VehicleProfileContent(
        accessToken: String,
        tokenType: String,
        vehicles: List<VehicleResponse>,
        selectedVehicleId: String?,
        activity: VehicleProfileActivity
    ) {
        val vehicleList = remember { mutableStateOf(vehicles) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkNavy)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ScreenHeader(title = "Vehicle Profile", onBack = { activity.finish() })

            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(NavyCard, RoundedCornerShape(20.dp))
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                Text("EV", color = Orange, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            }

            SectionTitle("Available Vehicles")
            if (vehicleList.value.isEmpty()) {
                EmptyVehicleCard(
                    accessToken = accessToken,
                    tokenType = tokenType,
                    activity = activity
                )
            } else {
                vehicleList.value.forEach { vehicle ->
                    VehicleCard(
                        vehicle = vehicle,
                        isSelected = vehicle.id == selectedVehicleId,
                        accessToken = accessToken,
                        tokenType = tokenType,
                        activity = activity,
                        onSelect = {
                            val resultIntent = Intent()
                            resultIntent.putExtra(EXTRA_SELECTED_VEHICLE_ID, vehicle.id)
                            activity.setResult(RESULT_OK, resultIntent)
                            activity.finish()
                        },
                        onDelete = {
                            vehicleList.value = vehicleList.value.filter { it.id != vehicle.id }
                        }
                    )
                }
            }

            SectionTitle("Service Monitor")
            InfoCard {
                ComingSoonRow("Service interval alerts")
                HorizontalDivider(color = Color(0xFF35566B))
                ComingSoonRow("Odometer tracking")
                HorizontalDivider(color = Color(0xFF35566B))
                ComingSoonRow("Battery health history")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    @Composable
    private fun EmptyVehicleCard(
        accessToken: String,
        tokenType: String,
        activity: VehicleProfileActivity
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = NavyCard)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("No vehicles found", color = Orange, fontWeight = FontWeight.Bold)
                Text(
                    text = "Vehicles returned by /me/bootstrap will appear here.",
                    color = MutedText
                )
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Orange,
                        contentColor = Color.White
                    ),
                    onClick = {
                        val intent = Intent(activity, AddVehicleActivity::class.java)
                        intent.putExtra("access_token", accessToken)
                        intent.putExtra("token_type", tokenType)
                        activity.startActivity(intent)
                    }
                ) {
                    Text("Add Car", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    @Composable
    private fun VehicleCard(
        vehicle: VehicleResponse,
        isSelected: Boolean,
        accessToken: String,
        tokenType: String,
        activity: VehicleProfileActivity,
        onSelect: () -> Unit,
        onDelete: () -> Unit
    ) {
        val scope = rememberCoroutineScope()
        val showDeleteConfirm = remember { mutableStateOf(false) }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = NavyCard)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(vehicle.model, color = SoftWhite, fontWeight = FontWeight.Bold)
                        Text(vehicle.headerDetails(), color = MutedText)
                    }
                    Text(
                        text = if (isSelected) "Main" else "Available",
                        color = Orange,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                HorizontalDivider(color = Color(0xFF35566B))
                VehicleDetailRow("VIN", vehicle.vin ?: "Not set")
                VehicleDetailRow("Connector", vehicle.connector_type ?: "Not set")
                VehicleDetailRow("Battery", vehicle.battery_capacity_kwh?.let { "${it.cleanNumber()} kWh" } ?: "Not set")
                VehicleDetailRow("Fuel tank", vehicle.fuel_tank_liters?.let { "${it.cleanNumber()} L" } ?: "Not set")
                VehicleDetailRow("Consumption", vehicle.consumptionLabel())
                VehicleDetailRow("Service", vehicle.serviceLabel())

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        enabled = !isSelected,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Orange,
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFF35566B),
                            disabledContentColor = SoftWhite
                        ),
                        onClick = onSelect
                    ) {
                        Text(if (isSelected) "Selected as Main Car" else "Use as Main Car")
                    }

                    Button(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE53935),
                            contentColor = Color.White
                        ),
                        onClick = {
                            showDeleteConfirm.value = true
                        }
                    ) {
                        Text("Delete")
                    }
                }
            }
        }
        
        if (showDeleteConfirm.value) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm.value = false },
                title = { Text("Delete Vehicle", color = SoftWhite) },
                text = { Text("Are you sure you want to delete ${vehicle.model}?", color = MutedText) },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    val response = RetrofitClient.api.deleteVehicle(
                                        vehicleId = vehicle.id,
                                        authorization = "$tokenType $accessToken"
                                    )
                                    if (response.isSuccessful) {
                                        val resultIntent = Intent().apply {
                                            putExtra(EXTRA_DELETED_VEHICLE_ID, vehicle.id)
                                        }
                                        activity.setResult(RESULT_OK, resultIntent)
                                        Toast.makeText(activity, "Vehicle deleted successfully", Toast.LENGTH_SHORT).show()
                                        onDelete()
                                        showDeleteConfirm.value = false
                                    } else {
                                        Toast.makeText(activity, "Failed to delete vehicle", Toast.LENGTH_SHORT).show()
                                        showDeleteConfirm.value = false
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(activity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    showDeleteConfirm.value = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                    ) {
                        Text("Delete", color = Color.White)
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { showDeleteConfirm.value = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF35566B))
                    ) {
                        Text("Cancel", color = SoftWhite)
                    }
                },
                containerColor = NavyCard
            )
        }
    }

    @Composable
    private fun VehicleDetailRow(label: String, value: String) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = MutedText)
            Text(value, color = SoftWhite, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun VehicleResponse.headerDetails(): String {
    return listOfNotNull(year?.toString(), powertrain).joinToString(" - ")
}

private fun VehicleResponse.consumptionLabel(): String {
    return consumption_kwh_per_100km?.let { "${it.cleanNumber()} kWh/100km" }
        ?: consumption_l_per_100km?.let { "${it.cleanNumber()} L/100km" }
        ?: "Not set"
}

private fun VehicleResponse.serviceLabel(): String {
    return service_interval_km?.let { "$it km" }
        ?: service_interval_months?.let { "$it months" }
        ?: "Not set"
}

private fun Double.cleanNumber(): String {
    return if (this % 1.0 == 0.0) {
        toInt().toString()
    } else {
        toString()
    }
}
