package com.example.davaroutes.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.davaroutes.ui.theme.MutedText
import com.example.davaroutes.ui.theme.NavyCard
import com.example.davaroutes.ui.theme.Orange
import com.example.davaroutes.ui.theme.SoftWhite

@Composable
fun SearchDestinationCard(
    destinationName: String,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = NavyCard.copy(alpha = 0.96f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF35566B),
                    contentColor = SoftWhite
                ),
                onClick = onSearchClick
            ) {
                Text(
                    text = destinationName.ifBlank { "Search destination" },
                    fontWeight = FontWeight.SemiBold
                )
            }

        }
    }
}

@Composable
fun RouteSummaryCard(
    destinationName: String,
    distanceKm: Double?,
    durationMinutes: Double?,
    isLoadingRoute: Boolean,
    onCancelClick: () -> Unit,
    onGoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCard)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Selected destination",
                color = Orange,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = destinationName,
                color = SoftWhite,
                fontWeight = FontWeight.SemiBold
            )

            distanceKm?.let {
                RouteInfoRow("Distance", "%.2f km".format(it))
            }

            durationMinutes?.let {
                RouteInfoRow("Duration", "%.0f minutes".format(it))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Orange
                    ),
                    onClick = onCancelClick
                ) {
                    Text("Cancel")
                }

                Button(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !isLoadingRoute,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Orange,
                        contentColor = Color.White
                    ),
                    onClick = onGoClick
                ) {
                    Text(
                        text = if (isLoadingRoute) "Loading..." else "Go there",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun RouteInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MutedText)
        Text(value, color = SoftWhite, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun RoutePreviewActionsCard(
    destinationName: String,
    distanceKm: Double?,
    durationMinutes: Double?,
    currentRange: String,
    routePreferences: String,
    onChangeDetailsClick: () -> Unit,
    onStartTripClick: () -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCard.copy(alpha = 0.96f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Route preview",
                color = Orange,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = destinationName,
                color = SoftWhite,
                fontWeight = FontWeight.SemiBold
            )

            distanceKm?.let {
                RouteInfoRow("Distance", "%.2f km".format(it))
            }

            durationMinutes?.let {
                RouteInfoRow("Duration", "%.0f minutes".format(it))
            }

            if (currentRange.isNotBlank()) {
                RouteInfoRow("Range", currentRange)
            }

            if (routePreferences.isNotBlank()) {
                Text(
                    text = routePreferences,
                    color = MutedText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Orange),
                    onClick = onChangeDetailsClick
                ) {
                    Text("Details")
                }

                Button(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Orange,
                        contentColor = Color.White
                    ),
                    onClick = onStartTripClick
                ) {
                    Text("Start trip", fontWeight = FontWeight.Bold)
                }
            }

            TextButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onCancelClick
            ) {
                Text("Cancel", color = Orange, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun RouteDetailsDialog(
    currentRange: String,
    routePreferences: String,
    isLoadingRoute: Boolean,
    onCurrentRangeChange: (String) -> Unit,
    onRoutePreferencesChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NavyCard,
        titleContentColor = SoftWhite,
        textContentColor = SoftWhite,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Route details",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = currentRange,
                    onValueChange = onCurrentRangeChange,
                    label = { Text("Current range") },
                    placeholder = { Text("Ex: 120 km") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoadingRoute,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SoftWhite,
                        unfocusedTextColor = SoftWhite,
                        cursorColor = Orange,

                        focusedBorderColor = Orange,
                        unfocusedBorderColor = Color(0xFF35566B),

                        focusedLabelColor = Orange,
                        unfocusedLabelColor = MutedText,

                        focusedPlaceholderColor = MutedText,
                        unfocusedPlaceholderColor = MutedText
                    )
                )

                OutlinedTextField(
                    value = routePreferences,
                    onValueChange = onRoutePreferencesChange,
                    label = { Text("Route preferences") },
                    placeholder = {
                        Text("Ex: chargers, restaurants, gas stations")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoadingRoute,
                    minLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SoftWhite,
                        unfocusedTextColor = SoftWhite,
                        cursorColor = Orange,

                        focusedBorderColor = Orange,
                        unfocusedBorderColor = Color(0xFF35566B),

                        focusedLabelColor = Orange,
                        unfocusedLabelColor = MutedText,

                        focusedPlaceholderColor = MutedText,
                        unfocusedPlaceholderColor = MutedText
                    )
                )

                if (isLoadingRoute) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = Orange
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isLoadingRoute,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Orange,
                    contentColor = Color.White
                ),
                onClick = onConfirm
            ) {
                Text(
                    text = if (isLoadingRoute) "Searching..." else "Find route",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                enabled = !isLoadingRoute,
                onClick = onDismiss
            ) {
                Text("Cancel", color = Orange)
            }
        }
    )
}
