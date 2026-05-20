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

class RecommendationsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DavaRoutesTheme {
                RecommendationsContent(activity = this@RecommendationsActivity)
            }
        }
    }

    @Composable
    private fun RecommendationsContent(activity: RecommendationsActivity) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkNavy)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ScreenHeader(title = "Recommendations", onBack = { activity.finish() })

            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(NavyCard, RoundedCornerShape(20.dp))
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                Text("AI", color = Orange, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            }

            Text(
                text = "Personalized stops and partner offers will appear here after trip planning.",
                color = MutedText,
                style = MaterialTheme.typography.bodyMedium
            )

            RecommendationCard(
                title = "Ionity Ploiesti FastCharge",
                subtitle = "Fast charging stop with partner discount support.",
                badge = "Charging"
            )
            RecommendationCard(
                title = "Kaufland Charge Ploiesti",
                subtitle = "Charging plus shopping stop for longer routes.",
                badge = "Partner"
            )
            RecommendationCard(
                title = "McDonald's Ploiesti DN1",
                subtitle = "Food stop candidate for family and daily profiles.",
                badge = "Offer"
            )

            SectionTitle("Next")
            InfoCard {
                ComingSoonRow("Live ranking from recommendation API")
                Divider(color = Color(0xFF35566B))
                ComingSoonRow("Why this option explanation")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    @Composable
    private fun RecommendationCard(title: String, subtitle: String, badge: String) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = NavyCard)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(title, color = SoftWhite, fontWeight = FontWeight.SemiBold)
                    Text(subtitle, color = MutedText, style = MaterialTheme.typography.bodySmall)
                }
                Text(badge, color = Orange, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
