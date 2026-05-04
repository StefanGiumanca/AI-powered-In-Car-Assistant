package com.example.davaroutes

import android.os.Bundle
import android.app.Activity.RESULT_OK
import android.content.Intent
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
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.davaroutes.data.EXTRA_SELECTED_VEHICLE_ID
import com.example.davaroutes.data.EXTRA_VEHICLES_JSON
import com.example.davaroutes.data.VehicleResponse
import com.example.davaroutes.data.vehiclesFromJson

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
            if (vehicles.isEmpty()) {
                EmptyVehicleCard(
                    accessToken = accessToken,
                    tokenType = tokenType,
                    activity = activity
                )
            } else {
                vehicles.forEach { vehicle ->
                    VehicleCard(
                        vehicle = vehicle,
                        isSelected = vehicle.id == selectedVehicleId,
                        onSelect = {
                            val resultIntent = Intent()
                            resultIntent.putExtra(EXTRA_SELECTED_VEHICLE_ID, vehicle.id)
                            activity.setResult(RESULT_OK, resultIntent)
                            activity.finish()
                        }
                    )
                }
            }

            SectionTitle("Service Monitor")
            InfoCard {
                ComingSoonRow("Service interval alerts")
                Divider(color = Color(0xFF35566B))
                ComingSoonRow("Odometer tracking")
                Divider(color = Color(0xFF35566B))
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
        onSelect: () -> Unit
    ) {
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

                Divider(color = Color(0xFF35566B))
                VehicleDetailRow("VIN", vehicle.vin ?: "Not set")
                VehicleDetailRow("Connector", vehicle.connector_type ?: "Not set")
                VehicleDetailRow("Battery", vehicle.battery_capacity_kwh?.let { "${it.cleanNumber()} kWh" } ?: "Not set")
                VehicleDetailRow("Fuel tank", vehicle.fuel_tank_liters?.let { "${it.cleanNumber()} L" } ?: "Not set")
                VehicleDetailRow("Consumption", vehicle.consumptionLabel())
                VehicleDetailRow("Service", vehicle.serviceLabel())

                Button(
                    modifier = Modifier.fillMaxWidth(),
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
            }
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
