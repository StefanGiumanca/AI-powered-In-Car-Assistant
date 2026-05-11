package com.example.davaroutes

import android.content.Intent
import android.os.Bundle
import android.app.Activity.RESULT_OK
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.davaroutes.data.EXTRA_CREATED_VEHICLE_JSON
import com.example.davaroutes.data.EXTRA_DELETED_VEHICLE_ID
import com.example.davaroutes.data.EXTRA_SELECTED_VEHICLE_ID
import com.example.davaroutes.data.EXTRA_VEHICLES_JSON
import com.example.davaroutes.data.PREF_MAIN_VEHICLE_ID
import com.example.davaroutes.data.VEHICLE_PREFS
import com.example.davaroutes.data.VehicleResponse
import com.example.davaroutes.data.vehicleFromJson
import com.example.davaroutes.data.vehiclesFromJson
import com.example.davaroutes.data.vehiclesToJson
import com.example.davaroutes.ui.theme.DavaRoutesTheme
import com.example.davaroutes.ui.theme.DarkNavy
import com.example.davaroutes.ui.theme.NavyCard
import com.example.davaroutes.ui.theme.Orange
import com.example.davaroutes.ui.theme.SoftWhite
import com.example.davaroutes.ui.theme.MutedText

class UserDashboard : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val accessToken = intent.getStringExtra("access_token") ?: ""
        val tokenType = intent.getStringExtra("token_type") ?: "bearer"
        val userId = intent.getStringExtra("user_id") ?: ""
        val email = intent.getStringExtra("email") ?: ""
        val fullName = intent.getStringExtra("full_name") ?: ""
        val vehiclesJson = intent.getStringExtra(EXTRA_VEHICLES_JSON) ?: "[]"

        setContent {
            DavaRoutesTheme {
                HomeDashboard(
                    accessToken = accessToken,
                    tokenType = tokenType,
                    userId = userId,
                    email = email,
                    fullName = fullName,
                    vehiclesJson = vehiclesJson,
                    activity = this@UserDashboard
                )
            }
        }
    }

    @Composable
    fun HomeDashboard(
        accessToken: String,
        tokenType: String,
        userId: String,
        email: String,
        fullName: String,
        vehiclesJson: String,
        activity: UserDashboard
    ) {
        val preferences = activity.getSharedPreferences(VEHICLE_PREFS, MODE_PRIVATE)
        var vehicles by remember {
            mutableStateOf(vehiclesFromJson(vehiclesJson))
        }
        var selectedVehicleId by remember {
            mutableStateOf(preferences.getString(PREF_MAIN_VEHICLE_ID, null))
        }
        val selectedVehicle = vehicles.firstOrNull { it.id == selectedVehicleId }
            ?: vehicles.firstOrNull()

        val addVehicleLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val createdVehicle = vehicleFromJson(
                    result.data?.getStringExtra(EXTRA_CREATED_VEHICLE_JSON)
                )

                if (createdVehicle != null) {
                    vehicles = vehicles
                        .filterNot { it.id == createdVehicle.id }
                        .plus(createdVehicle)

                    if (selectedVehicleId == null) {
                        selectedVehicleId = createdVehicle.id
                        preferences.edit().putString(PREF_MAIN_VEHICLE_ID, createdVehicle.id).apply()
                    }
                }
            }
        }

        val vehicleProfileLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val deletedId = result.data?.getStringExtra(EXTRA_DELETED_VEHICLE_ID)
                val selectedId = result.data?.getStringExtra(EXTRA_SELECTED_VEHICLE_ID)

                if (!deletedId.isNullOrBlank()) {
                    vehicles = vehicles.filterNot { it.id == deletedId }
                    if (selectedVehicleId == deletedId) {
                        selectedVehicleId = vehicles.firstOrNull()?.id
                        preferences.edit().putString(PREF_MAIN_VEHICLE_ID, selectedVehicleId).apply()
                    }
                }

                if (!selectedId.isNullOrBlank()) {
                    selectedVehicleId = selectedId
                    preferences.edit().putString(PREF_MAIN_VEHICLE_ID, selectedId).apply()
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkNavy)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with App Title and Greeting
                DashboardHeader(fullName = fullName, email = email)

                Spacer(modifier = Modifier.height(8.dp))

                // Vehicle Status Card
                VehicleStatusCard(
                    selectedVehicle = selectedVehicle,
                    vehicleCount = vehicles.size,
                    onAddCarClick = {
                        val intent = Intent(activity, AddVehicleActivity::class.java)
                        intent.putExtra("access_token", accessToken)
                        intent.putExtra("token_type", tokenType)
                        addVehicleLauncher.launch(intent)
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Plan Trip Button (Primary Action)
                PlanTripButton(
                    accessToken = accessToken,
                    tokenType = tokenType,
                    userId = userId,
                    email = email,
                    fullName = fullName,
                    vehiclesJson = vehiclesToJson(vehicles),
                    activity = activity
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Secondary Actions Section
                Text(
                    text = "Quick Access",
                    style = MaterialTheme.typography.titleMedium,
                    color = Orange,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ActionCard(
                        title = "Driver Profile",
                        icon = R.drawable.ic_id_card,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val intent = Intent(activity, DriverProfileActivity::class.java)
                            intent.putExtra("full_name", fullName)
                            intent.putExtra("email", email)
                            intent.putExtra("user_id", userId)
                            activity.startActivity(intent)
                        }
                    )
                    ActionCard(
                        title = "Vehicle Profile",
                        icon = R.drawable.ic_car,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val intent = Intent(activity, VehicleProfileActivity::class.java)
                            intent.putExtra("access_token", accessToken)
                            intent.putExtra("token_type", tokenType)
                            intent.putExtra(EXTRA_VEHICLES_JSON, vehiclesToJson(vehicles))
                            intent.putExtra(EXTRA_SELECTED_VEHICLE_ID, selectedVehicleId)
                            vehicleProfileLauncher.launch(intent)
                        }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ActionCard(
                        title = "Recommendations",
                        icon = R.drawable.ic_lightning,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val intent = Intent(activity, RecommendationsActivity::class.java)
                            activity.startActivity(intent)
                        }
                    )
                     ActionCard(
                         title = "Last trips",
                         icon = R.drawable.ic_clock,
                         modifier = Modifier.weight(1f),
                         onClick = {
                             val intent = Intent(activity, LastTripsActivity::class.java)
                             activity.startActivity(intent)
                         }
                     )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    @Composable
    fun DashboardHeader(fullName: String, email: String) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "DavaRoutes",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Orange,
                fontSize = 32.sp
            )

            val greetingName = fullName.takeIf { it.isNotEmpty() }
                ?: email.takeIf { it.isNotEmpty() }
                ?: "Driver"

            Text(
                text = "Welcome back, $greetingName",
                style = MaterialTheme.typography.bodyLarge,
                color = SoftWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "Ready to plan your next trip?",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedText
            )
        }
    }

    @Composable
    fun VehicleStatusCard(
        selectedVehicle: VehicleResponse?,
        vehicleCount: Int,
        onAddCarClick: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = NavyCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Vehicle Status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SoftWhite
                    )
                    Text(
                        text = if (selectedVehicle == null) "No car" else "Main car",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Vehicle Name
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Selected Vehicle",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedText
                    )
                    Text(
                        text = selectedVehicle?.model ?: "No vehicle added yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = SoftWhite,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = selectedVehicle?.dashboardSummary()
                        ?: "Add a car to unlock range estimates, service reminders, and personalized charging recommendations.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedText
                )

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Orange,
                        contentColor = Color.White
                    ),
                    onClick = onAddCarClick
                ) {
                    Text(if (vehicleCount == 0) "Add Car" else "Add Another Car", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    @Composable
    fun PlanTripButton(
        accessToken: String,
        tokenType: String,
        userId: String,
        email: String,
        fullName: String,
        vehiclesJson: String,
        activity: UserDashboard
    ) {
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Orange,
                contentColor = Color.White
            ),
            onClick = {
                val intent = Intent(activity, MainActivity::class.java)
                intent.putExtra("access_token", accessToken)
                intent.putExtra("token_type", tokenType)
                intent.putExtra("user_id", userId)
                intent.putExtra("email", email)
                intent.putExtra("full_name", fullName)
                intent.putExtra(EXTRA_VEHICLES_JSON, vehiclesJson)
                activity.startActivity(intent)
            }
        ) {
            Text(
                text = "Go to Map",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }

    @Composable
    fun ActionCard(
        title: String,
        icon: Any,
        modifier: Modifier = Modifier,
        onClick: () -> Unit
    ) {
        Card(
            modifier = modifier
                .fillMaxHeight(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = NavyCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            onClick = onClick
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (icon) {
                    is Int -> {
                        Icon(
                            painter = painterResource(id = icon),
                            contentDescription = title,
                            tint = Orange,
                            modifier = Modifier
                                .size(32.dp)
                                .padding(bottom = 8.dp)
                        )
                    }
                    is String -> {
                        Text(
                            text = icon,
                            fontSize = 32.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = SoftWhite,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

private fun VehicleResponse.dashboardSummary(): String {
    val yearText = year?.toString()
    val connectorText = connector_type?.takeIf { it.isNotBlank() }
    val capacityText = battery_capacity_kwh?.let { "${it.cleanNumber()} kWh" }
        ?: fuel_tank_liters?.let { "${it.cleanNumber()} L tank" }
    val serviceText = service_interval_km?.let { "service every $it km" }
        ?: service_interval_months?.let { "service every $it months" }

    return listOfNotNull(
        yearText,
        powertrain,
        connectorText,
        capacityText,
        serviceText
    ).joinToString(" - ")
}

private fun Double.cleanNumber(): String {
    return if (this % 1.0 == 0.0) {
        toInt().toString()
    } else {
        toString()
    }
}
