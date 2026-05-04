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

class LastTripsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DavaRoutesTheme {
                LastTripsContent(activity = this@LastTripsActivity)
            }
        }
    }

    @Composable
    private fun LastTripsContent(activity: LastTripsActivity) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkNavy)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ScreenHeader(title = "Trip History", onBack = { activity.finish() })

            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(NavyCard, RoundedCornerShape(20.dp))
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                Text("TR", color = Orange, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            }

            TripPreviewCard("Ploiesti to Brasov", "EV route with charging recommendations", "Coming soon")
            TripPreviewCard("Daily commute", "Saved route summary and efficiency metrics", "Coming soon")
            TripPreviewCard("Service-aware trip", "Maintenance-aware routing will appear here", "Coming soon")

            SectionTitle("Post-trip Analytics")
            InfoCard {
                ComingSoonRow("Cost and charging summary")
                Divider(color = Color(0xFF35566B))
                ComingSoonRow("Time saved")
                Divider(color = Color(0xFF35566B))
                ComingSoonRow("Efficiency score")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    @Composable
    private fun TripPreviewCard(title: String, subtitle: String, status: String) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = NavyCard)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(title, color = SoftWhite, fontWeight = FontWeight.SemiBold)
                        Text(subtitle, color = MutedText, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(status, color = Orange, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                }
                Divider(color = Color(0xFF35566B))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Metric("Distance", "-- km")
                    Metric("Duration", "--")
                    Metric("Stops", "--")
                }
            }
        }
    }

    @Composable
    private fun Metric(label: String, value: String) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, color = MutedText, style = MaterialTheme.typography.labelSmall)
            Text(value, color = SoftWhite, fontWeight = FontWeight.SemiBold)
        }
    }
}
