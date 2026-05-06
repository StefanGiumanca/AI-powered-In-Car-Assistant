package com.example.davaroutes.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import java.time.LocalTime
import java.time.format.DateTimeFormatter

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
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onChangeDetailsClick: () -> Unit,
    onStartTripClick: () -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCard.copy(alpha = 0.96f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = destinationName,
                        color = SoftWhite,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = formatPreviewEta(durationMinutes),
                        color = Orange,
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }

                IconButton(onClick = onToggleExpanded) {
                    Icon(
                        imageVector = if (isExpanded) {
                            Icons.Filled.KeyboardArrowDown
                        } else {
                            Icons.Filled.KeyboardArrowUp
                        },
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = Orange
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
    }
}

private fun formatPreviewEta(durationMinutes: Double?): String {
    if (durationMinutes == null) {
        return "ETA --:--"
    }

    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    val arrivalTime = LocalTime.now().plusMinutes(durationMinutes.toLong())
    return "ETA ${arrivalTime.format(formatter)}"
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
