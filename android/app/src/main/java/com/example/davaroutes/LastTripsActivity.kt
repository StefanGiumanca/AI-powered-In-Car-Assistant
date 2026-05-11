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
import androidx.compose.runtime.remember
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
import org.json.JSONArray
import org.json.JSONObject

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
        val navigationHistory = remember { activity.loadNavigationHistory() }
        val recentPlaces = remember { activity.loadRecentPlaces() }

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

            SectionTitle("Navigation History")
            if (navigationHistory.isEmpty()) {
                InfoCard {
                    Text(
                        text = "No generated routes yet",
                        color = MutedText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                navigationHistory.forEach { route ->
                    TripPreviewCard(route)
                }
            }

            SectionTitle("Recent Searches")
            if (recentPlaces.isEmpty()) {
                InfoCard {
                    Text(
                        text = "No recent searches yet",
                        color = MutedText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                InfoCard {
                    recentPlaces.forEachIndexed { index, place ->
                        RecentSearchRow(place)
                        if (index < recentPlaces.lastIndex) {
                            Divider(color = Color(0xFF35566B))
                        }
                    }
                }
            }

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
    private fun TripPreviewCard(route: NavigationHistoryUi) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = NavyCard)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(route.destinationName, color = SoftWhite, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = route.destinationAddress.ifBlank { "Saved route" },
                            color = MutedText,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text("Saved", color = Orange, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                }
                Divider(color = Color(0xFF35566B))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Metric("Distance", "%.2f km".format(route.distanceKm))
                    Metric("Duration", "%.0f min".format(route.durationMinutes))
                    Metric("Stops", route.stops.size.toString())
                }

                if (route.stops.isNotEmpty()) {
                    Divider(color = Color(0xFF35566B))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        route.stops.forEachIndexed { index, stop ->
                            Text(
                                text = "${index + 1}. $stop",
                                color = MutedText,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
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

    @Composable
    private fun RecentSearchRow(place: RecentPlaceUi) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = place.name,
                color = SoftWhite,
                fontWeight = FontWeight.SemiBold
            )

            if (place.address.isNotBlank()) {
                Text(
                    text = place.address,
                    color = MutedText,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Text(
                text = "%.5f, %.5f".format(place.lat, place.lng),
                color = Orange,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }

    private fun loadRecentPlaces(): List<RecentPlaceUi> {
        val preferences = getSharedPreferences(
            "davaroutes_search_history",
            MODE_PRIVATE
        )
        val rawHistory = preferences.getString("places", "[]").orEmpty()
        val history = mutableListOf<RecentPlaceUi>()

        try {
            val jsonArray = JSONArray(rawHistory)
            for (index in 0 until jsonArray.length()) {
                val item = jsonArray.optJSONObject(index) ?: continue
                history.add(
                    RecentPlaceUi(
                        name = item.optString("name"),
                        address = item.optString("address"),
                        lat = item.optDouble("lat"),
                        lng = item.optDouble("lng")
                    )
                )
            }
        } catch (_: Exception) {
            return emptyList()
        }

        return history
            .filter { it.name.isNotBlank() && it.lat != 0.0 && it.lng != 0.0 }
            .take(6)
    }

    private fun loadNavigationHistory(): List<NavigationHistoryUi> {
        val preferences = getSharedPreferences(
            "davaroutes_search_history",
            MODE_PRIVATE
        )
        val rawHistory = preferences.getString("routes", "[]").orEmpty()
        val history = mutableListOf<NavigationHistoryUi>()

        try {
            val jsonArray = JSONArray(rawHistory)
            for (index in 0 until jsonArray.length()) {
                val item = jsonArray.optJSONObject(index) ?: continue
                history.add(
                    NavigationHistoryUi(
                        destinationName = item.optString("destination_name"),
                        destinationAddress = item.optString("destination_address"),
                        distanceKm = item.optDouble("distance_km"),
                        durationMinutes = item.optDouble("duration_minutes"),
                        stops = item.optJSONArray("stops").toStopNames()
                    )
                )
            }
        } catch (_: Exception) {
            return emptyList()
        }

        return history
            .filter { it.destinationName.isNotBlank() && it.distanceKm > 0.0 }
            .take(10)
    }

    private fun JSONArray?.toStopNames(): List<String> {
        if (this == null) return emptyList()

        val stops = mutableListOf<String>()
        for (index in 0 until length()) {
            val item = opt(index)
            when (item) {
                is JSONObject -> {
                    val name = item.optString("name")
                    if (name.isNotBlank()) {
                        stops.add(name)
                    }
                }
                is String -> {
                    if (item.isNotBlank()) {
                        stops.add(item)
                    }
                }
            }
        }
        return stops
    }
}

private data class NavigationHistoryUi(
    val destinationName: String,
    val destinationAddress: String,
    val distanceKm: Double,
    val durationMinutes: Double,
    val stops: List<String>
)

private data class RecentPlaceUi(
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double
)
