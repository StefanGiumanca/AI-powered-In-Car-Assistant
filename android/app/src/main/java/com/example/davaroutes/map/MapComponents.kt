package com.example.davaroutes.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
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
    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val collapsedHeight = 136.dp
        val maxSheetHeight = maxHeight - 104.dp
        val minHeightPx = with(density) { collapsedHeight.toPx() }
        val maxHeightPx = with(density) { maxSheetHeight.toPx() }
        val halfHeightPx = (minHeightPx + maxHeightPx) / 2f
        var sheetHeightPx by remember { mutableFloatStateOf(minHeightPx) }
        var sheetStage by remember { mutableIntStateOf(0) }

        LaunchedEffect(maxHeightPx) {
            sheetHeightPx = sheetHeightPx.coerceIn(minHeightPx, maxHeightPx)
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(with(density) { sheetHeightPx.toDp() }),
                shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = NavyCard.copy(alpha = 0.96f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(start = 12.dp, top = 10.dp, end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .width(54.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MutedText.copy(alpha = 0.72f))
                            .clickable {
                                sheetStage = (sheetStage + 1) % 3
                                sheetHeightPx = when (sheetStage) {
                                    1 -> halfHeightPx
                                    2 -> maxHeightPx
                                    else -> minHeightPx
                                }
                            }
                            .pointerInput(Unit) {
                                detectVerticalDragGestures { change, dragAmount ->
                                    change.consume()
                                    sheetHeightPx = (sheetHeightPx - dragAmount)
                                        .coerceIn(minHeightPx, maxHeightPx)
                                    sheetStage = when {
                                        sheetHeightPx >= maxHeightPx - 12f -> 2
                                        sheetHeightPx >= halfHeightPx - 12f -> 1
                                        else -> 0
                                    }
                                }
                            }
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(onClick = onSearchClick)
                            .background(Color(0xFF35566B))
                            .padding(start = 16.dp, end = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = Orange
                        )

                        Text(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 10.dp),
                            text = destinationName.ifBlank { "Search destination" },
                            color = SoftWhite,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyLarge
                        )

                        IconButton(onClick = onSearchClick) {
                            Icon(
                                imageVector = Icons.Filled.Mic,
                                contentDescription = "Voice search",
                                tint = SoftWhite
                            )
                        }
                    }

                    SearchHistoryContent(
                        showEmptyState = sheetHeightPx > minHeightPx + with(density) { 44.dp.toPx() }
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchHistoryContent(
    showEmptyState: Boolean
) {
    AnimatedVisibility(
        visible = true,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it })
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Search History",
                color = Orange,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )

            if (showEmptyState) {
                Text(
                    text = "No recent searches",
                    color = MutedText,
                    style = MaterialTheme.typography.bodyMedium
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
