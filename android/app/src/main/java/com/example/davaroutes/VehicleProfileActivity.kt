package com.example.davaroutes

import android.os.Bundle
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
import androidx.compose.material3.Divider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.davaroutes.ui.theme.DarkNavy
import com.example.davaroutes.ui.theme.DavaRoutesTheme
import com.example.davaroutes.ui.theme.MutedText
import com.example.davaroutes.ui.theme.NavyCard
import com.example.davaroutes.ui.theme.Orange
import com.example.davaroutes.ui.theme.SoftWhite

class VehicleProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DavaRoutesTheme {
                VehicleProfileContent(activity = this@VehicleProfileActivity)
            }
        }
    }

    @Composable
    private fun VehicleProfileContent(activity: VehicleProfileActivity) {
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

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = NavyCard)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    InfoRow("Selected vehicle", "Tesla Model 3")
                    Divider(color = Color(0xFF35566B))
                    InfoRow("Battery capacity", "75 kWh")
                    Divider(color = Color(0xFF35566B))
                    InfoRow("Estimated range", "310 km")
                    LinearProgressIndicator(
                        progress = { 0.72f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = Orange,
                        trackColor = Color(0xFF35566B),
                        strokeCap = StrokeCap.Round
                    )
                }
            }

            SectionTitle("Available Vehicles")
            InfoCard {
                ProfileRow("Hyundai Kona Electric", "EV option seeded for range and charging flows.")
                Divider(color = Color(0xFF35566B))
                ProfileRow("Toyota Corolla Hybrid", "Hybrid option for fuel and efficiency comparisons.")
                Divider(color = Color(0xFF35566B))
                ProfileRow("Dacia Logan", "Combustion profile for classic fuel routing.")
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
}
