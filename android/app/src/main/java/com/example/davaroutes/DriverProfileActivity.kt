package com.example.davaroutes

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

class DriverProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val fullName = intent.getStringExtra("full_name") ?: ""
        val email = intent.getStringExtra("email") ?: ""
        val userId = intent.getStringExtra("user_id") ?: ""
        val vehicles = vehiclesFromJson(intent.getStringExtra(EXTRA_VEHICLES_JSON))
        val selectedVehicleId = intent.getStringExtra(EXTRA_SELECTED_VEHICLE_ID)
        val selectedVehicle = vehicles.firstOrNull { it.id == selectedVehicleId }
            ?: vehicles.firstOrNull()

        setContent {
            DavaRoutesTheme {
                DriverProfileContent(
                    fullName = fullName,
                    email = email,
                    userId = userId,
                    vehicles = vehicles,
                    selectedVehicle = selectedVehicle,
                    activity = this@DriverProfileActivity
                )
            }
        }
    }

    @Composable
    private fun DriverProfileContent(
        fullName: String,
        email: String,
        userId: String,
        vehicles: List<VehicleResponse>,
        selectedVehicle: VehicleResponse?,
        activity: DriverProfileActivity
    ) {
        val setupItems = listOf(
            fullName.isNotBlank(),
            email.isNotBlank(),
            vehicles.isNotEmpty(),
            selectedVehicle != null
        )
        val completionPercent = setupItems.count { it } * 100 / setupItems.size

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkNavy)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ScreenHeader(title = "Driver Profile", onBack = { activity.finish() })

            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(NavyCard, CircleShape)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                Text("DP", color = Orange, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            }

            InfoCard {
                InfoRow("Full name", fullName.ifBlank { "Not set" })
                Divider(color = Color(0xFF35566B))
                InfoRow("Email", email.ifBlank { "Not set" })
                Divider(color = Color(0xFF35566B))
                InfoRow("User ID", userId.ifBlank { "Not available" })
            }

            SectionTitle("Preference Profiles")
            InfoCard {
                ProfileRow("Balanced Daily", "Default balance between time, comfort, and charging.")
                Divider(color = Color(0xFF35566B))
                ProfileRow("Family EV", "Prioritizes safe stops, food, hotels, and slower pace.")
                Divider(color = Color(0xFF35566B))
                ProfileRow("Business Fast", "Optimized for short stops and fast charging.")
            }

            SectionTitle("Management")
            InfoCard {
                StatusRow(
                    label = "Profile completion",
                    value = "$completionPercent%",
                    description = buildCompletionDescription(
                        fullName = fullName,
                        email = email,
                        hasVehicles = vehicles.isNotEmpty(),
                        hasSelectedVehicle = selectedVehicle != null
                    )
                )
                Divider(color = Color(0xFF35566B))
                StatusRow(
                    label = "Route preferences",
                    value = selectedVehicle.routePreferenceLabel(),
                    description = selectedVehicle.routePreferenceDescription()
                )
                Divider(color = Color(0xFF35566B))
                StatusRow(
                    label = "Preferred partners",
                    value = selectedVehicle.partnerPreferenceLabel(),
                    description = selectedVehicle.partnerPreferenceDescription()
                )
            }

            Button(
                onClick = { activity.logout() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB23A48))
            ) {
                Text("Log out", color = SoftWhite, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    private fun logout() {
        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

        GoogleSignIn.getClient(this, googleSignInOptions).signOut()

        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}

@Composable
fun ScreenHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Button(
            onClick = onBack,
            modifier = Modifier
                .width(40.dp)
                .height(40.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NavyCard),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text("<", fontSize = 20.sp, color = SoftWhite)
        }
        Text(title, color = Orange, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Spacer(modifier = Modifier.width(40.dp))
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, color = Orange, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
}

@Composable
fun InfoCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCard)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = MutedText, style = MaterialTheme.typography.labelSmall)
        Text(value, color = SoftWhite, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ProfileRow(name: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(name, color = SoftWhite, fontWeight = FontWeight.SemiBold)
        Text(description, color = MutedText, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun ComingSoonRow(label: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MutedText)
        Text("Coming soon", color = Orange, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun StatusRow(label: String, value: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = MutedText)
            Text(value, color = Orange, fontWeight = FontWeight.SemiBold)
        }
        Text(description, color = SoftWhite, style = MaterialTheme.typography.bodySmall)
    }
}

private fun buildCompletionDescription(
    fullName: String,
    email: String,
    hasVehicles: Boolean,
    hasSelectedVehicle: Boolean
): String {
    val missingItems = listOfNotNull(
        "name".takeIf { fullName.isBlank() },
        "email".takeIf { email.isBlank() },
        "vehicle".takeIf { !hasVehicles },
        "main vehicle".takeIf { !hasSelectedVehicle }
    )

    return if (missingItems.isEmpty()) {
        "Your account has the core setup needed for personalized routes."
    } else {
        "Complete ${missingItems.joinToString(", ")} to improve route personalization."
    }
}

private fun VehicleResponse?.routePreferenceLabel(): String {
    return when (this?.powertrain?.uppercase()) {
        "EV" -> "Charging-aware"
        "HYBRID" -> "Balanced"
        "ICE" -> "Fuel-aware"
        else -> "Basic"
    }
}

private fun VehicleResponse?.routePreferenceDescription(): String {
    return when (this?.powertrain?.uppercase()) {
        "EV" -> "Routes can prioritize charging stops, connector compatibility, and range confidence."
        "HYBRID" -> "Routes can balance fuel stops, charging opportunities, and efficient detours."
        "ICE" -> "Routes can prioritize fuel stops, service reminders, and lower-detour options."
        else -> "Add a vehicle to unlock route preferences based on powertrain and consumption."
    }
}

private fun VehicleResponse?.partnerPreferenceLabel(): String {
    return when (this?.powertrain?.uppercase()) {
        "EV" -> "Charging networks"
        "HYBRID" -> "Mixed mobility"
        "ICE" -> "Fuel and service"
        else -> "Not set"
    }
}

private fun VehicleResponse?.partnerPreferenceDescription(): String {
    return when (this?.powertrain?.uppercase()) {
        "EV" -> "Relevant partners include fast chargers, destination chargers, food, and hotels."
        "HYBRID" -> "Relevant partners include fuel chains, charging locations, restaurants, and service centers."
        "ICE" -> "Relevant partners include fuel chains, restaurants, parking, and service centers."
        else -> "Add a vehicle so DavaRoutes can suggest relevant partner categories."
    }
}
