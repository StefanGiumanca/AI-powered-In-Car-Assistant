package com.example.davaroutes

import android.content.Intent
import android.os.Bundle
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

        setContent {
            DavaRoutesTheme {
                HomeDashboard(
                    accessToken = accessToken,
                    tokenType = tokenType,
                    userId = userId,
                    email = email,
                    fullName = fullName,
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
        activity: UserDashboard
    ) {
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
                VehicleStatusCard()

                Spacer(modifier = Modifier.height(8.dp))

                // Plan Trip Button (Primary Action)
                PlanTripButton(
                    accessToken = accessToken,
                    tokenType = tokenType,
                    userId = userId,
                    email = email,
                    fullName = fullName,
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
                            activity.startActivity(intent)
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
    fun VehicleStatusCard() {
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
                        text = "OK",
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
                        text = "Tesla Model 3",
                        style = MaterialTheme.typography.bodyLarge,
                        color = SoftWhite,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Battery and Range Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Battery Status
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                Color(0xFF0B2133),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Battery",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedText
                        )
                        Text(
                            text = "72%",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Orange,
                            fontWeight = FontWeight.Bold
                        )
                        LinearProgressIndicator(
                            progress = { 0.72f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp),
                            color = Orange,
                            trackColor = Color(0xFF35566B),
                            strokeCap = StrokeCap.Round
                        )
                    }

                    // Estimated Range
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                Color(0xFF0B2133),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Range",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedText
                        )
                        Text(
                            text = "310 km",
                            style = MaterialTheme.typography.headlineSmall,
                            color = SoftWhite,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Estimated",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedText
                        )
                    }
                }

                // Service Status
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color(0xFF0B2133),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Service Status",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedText
                    )
                    Text(
                        text = "Good",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.SemiBold
                    )
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
                activity.startActivity(intent)
            }
        ) {
            Text(
                text = "Plan Trip",
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
