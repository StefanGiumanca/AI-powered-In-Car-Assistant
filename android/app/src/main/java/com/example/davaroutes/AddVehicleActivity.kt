package com.example.davaroutes

import android.app.Activity.RESULT_OK
import android.os.Bundle
import android.content.Intent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.davaroutes.data.VehicleCreateRequest
import com.example.davaroutes.data.EXTRA_CREATED_VEHICLE_JSON
import com.example.davaroutes.data.vehicleToJson
import com.example.davaroutes.network.RetrofitClient
import com.example.davaroutes.ui.components.modernTextFieldColors
import com.example.davaroutes.ui.theme.DarkNavy
import com.example.davaroutes.ui.theme.DavaRoutesTheme
import com.example.davaroutes.ui.theme.MutedText
import com.example.davaroutes.ui.theme.NavyCard
import com.example.davaroutes.ui.theme.Orange
import com.example.davaroutes.ui.theme.SoftWhite
import kotlinx.coroutines.launch

class AddVehicleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val accessToken = intent.getStringExtra("access_token") ?: ""

        setContent {
            DavaRoutesTheme {
                AddVehicleScreen(
                    accessToken = accessToken,
                    activity = this@AddVehicleActivity
                )
            }
        }
    }

    @Composable
    private fun AddVehicleScreen(
        accessToken: String,
        activity: AddVehicleActivity
    ) {
        var vin by remember { mutableStateOf("") }
        var model by remember { mutableStateOf("") }
        var year by remember { mutableStateOf("") }
        var powertrain by remember { mutableStateOf("EV") }
        var connectorType by remember { mutableStateOf("") }
        var batteryCapacity by remember { mutableStateOf("") }
        var fuelTank by remember { mutableStateOf("") }
        var consumptionKwh by remember { mutableStateOf("") }
        var consumptionLiters by remember { mutableStateOf("") }
        var serviceKm by remember { mutableStateOf("") }
        var serviceMonths by remember { mutableStateOf("") }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkNavy)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ScreenHeader(title = "Add Car", onBack = { activity.finish() })

            Text(
                text = "Create a vehicle profile used by route planning, service reminders, and charging recommendations.",
                color = MutedText,
                style = MaterialTheme.typography.bodyMedium
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = NavyCard)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    VehicleField(model, { model = it }, "Model", "Tesla Model 3")
                    VehicleField(vin, { vin = it }, "VIN optional", "WVWZZZ...")
                    VehicleField(year, { year = it }, "Year optional", "2024")
                    VehicleField(powertrain, { powertrain = it.uppercase() }, "Powertrain", "EV / ICE / HYBRID")
                    VehicleField(connectorType, { connectorType = it }, "Connector type optional", "CCS2")
                    VehicleField(batteryCapacity, { batteryCapacity = it }, "Battery capacity kWh optional", "75")
                    VehicleField(fuelTank, { fuelTank = it }, "Fuel tank liters optional", "50")
                    VehicleField(consumptionKwh, { consumptionKwh = it }, "Consumption kWh/100km optional", "16.5")
                    VehicleField(consumptionLiters, { consumptionLiters = it }, "Consumption L/100km optional", "6.2")
                    VehicleField(serviceKm, { serviceKm = it }, "Service interval km optional", "15000")
                    VehicleField(serviceMonths, { serviceMonths = it }, "Service interval months optional", "12")
                }
            }

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Orange,
                    contentColor = Color.White
                ),
                onClick = {
                    val normalizedPowertrain = powertrain.trim().uppercase()

                    if (model.isBlank()) {
                        Toast.makeText(activity, "Model is required", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    if (normalizedPowertrain !in setOf("EV", "ICE", "HYBRID")) {
                        Toast.makeText(activity, "Powertrain must be EV, ICE, or HYBRID", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val request = VehicleCreateRequest(
                        vin = vin.blankToNull(),
                        model = model.trim(),
                        year = year.toIntOrNull(),
                        powertrain = normalizedPowertrain,
                        connector_type = connectorType.blankToNull(),
                        battery_capacity_kwh = if (normalizedPowertrain == "ICE") null else batteryCapacity.toDoubleOrNull(),
                        fuel_tank_liters = if (normalizedPowertrain == "EV") null else fuelTank.toDoubleOrNull(),
                        consumption_kwh_per_100km = if (normalizedPowertrain == "ICE") null else consumptionKwh.toDoubleOrNull(),
                        consumption_l_per_100km = if (normalizedPowertrain == "EV") null else consumptionLiters.toDoubleOrNull(),
                        service_interval_km = serviceKm.toIntOrNull(),
                        service_interval_months = serviceMonths.toIntOrNull()
                    )

                    lifecycleScope.launch {
                        try {
                            val response = RetrofitClient.api.createVehicle(
                                authorization = "Bearer $accessToken",
                                vehicle = request
                            )

                            if (response.isSuccessful) {
                                response.body()?.let { createdVehicle ->
                                    val resultIntent = Intent()
                                    resultIntent.putExtra(
                                        EXTRA_CREATED_VEHICLE_JSON,
                                        vehicleToJson(createdVehicle)
                                    )
                                    activity.setResult(RESULT_OK, resultIntent)
                                }
                                Toast.makeText(activity, "Vehicle added", Toast.LENGTH_SHORT).show()
                                activity.finish()
                            } else {
                                Toast.makeText(activity, "Server error: ${response.code()}", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(activity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            ) {
                Text("Save Car", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    @Composable
    private fun VehicleField(
        value: String,
        onValueChange: (String) -> Unit,
        label: String,
        placeholder: String
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = modernTextFieldColors()
        )
    }
}

private fun String.blankToNull(): String? = trim().takeIf { it.isNotEmpty() }
