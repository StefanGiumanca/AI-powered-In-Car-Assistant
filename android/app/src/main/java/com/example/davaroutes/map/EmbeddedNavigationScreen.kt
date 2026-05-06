package com.example.davaroutes.map

import android.os.Bundle
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import com.example.davaroutes.BuildConfig
import com.example.davaroutes.MainActivity
import com.example.davaroutes.ui.theme.MutedText
import com.example.davaroutes.ui.theme.NavyCard
import com.example.davaroutes.ui.theme.Orange
import com.example.davaroutes.ui.theme.SoftWhite
import com.google.android.libraries.mapsplatform.turnbyturn.model.Maneuver
import com.google.android.gms.maps.GoogleMap.CameraPerspective
import com.google.android.libraries.navigation.ListenableResultFuture
import com.google.android.libraries.navigation.NavigationApi
import com.google.android.libraries.navigation.NavigationUpdatesOptions
import com.google.android.libraries.navigation.NavigationView
import com.google.android.libraries.navigation.Navigator
import com.google.android.libraries.navigation.RoutingOptions
import com.google.android.libraries.navigation.SimulationOptions
import com.google.android.libraries.navigation.Waypoint
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun EmbeddedNavigationScreen(
    activity: MainActivity,
    destinationName: String,
    destinationAddress: String,
    destinationLat: Double,
    destinationLng: Double,
    distanceKm: Double?,
    durationMinutes: Double?,
    onExitNavigation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val navigationView = remember { NavigationView(activity) }
    val navigatorRef = remember { arrayOfNulls<Navigator>(1) }
    val instruction by NavigationInstructionStore.instruction.collectAsState()
    var isSoundEnabled by remember { mutableStateOf(true) }
    var isNavigationCardExpanded by remember { mutableStateOf(false) }
    val compactNavigationCardHeight = 112.dp

    DisposableEffect(destinationLat, destinationLng) {
        navigationView.onCreate(Bundle())
        navigationView.onStart()
        navigationView.onResume()
        navigationView.setNavigationUiEnabled(true)
        navigationView.setHeaderEnabled(false)
        navigationView.setEtaCardEnabled(false)
        navigationView.setTrafficPromptsEnabled(false)
        navigationView.setTrafficIncidentCardsEnabled(false)
        NavigationInstructionStore.reset()

        NavigationApi.getNavigator(
            activity,
            object : NavigationApi.NavigatorListener {
                override fun onNavigatorReady(readyNavigator: Navigator) {
                    navigatorRef[0] = readyNavigator

                    val updateOptions = NavigationUpdatesOptions.builder()
                        .setNumNextStepsToPreview(2)
                        .setGeneratedStepImagesType(
                            NavigationUpdatesOptions.GeneratedStepImagesType.BITMAP
                        )
                        .setDisplayMetrics(activity.resources.displayMetrics)
                        .build()

                    readyNavigator.registerServiceForNavUpdates(
                        activity.packageName,
                        NavInfoReceivingService::class.java.name,
                        updateOptions
                    )

                    navigationView.getMapAsync { googleMap ->
                        googleMap.followMyLocation(CameraPerspective.TILTED)
                    }

                    startEmbeddedGuidance(
                        activity = activity,
                        navigator = readyNavigator,
                        destinationName = destinationName,
                        destinationLat = destinationLat,
                        destinationLng = destinationLng,
                        isSoundEnabled = { isSoundEnabled }
                    )
                }

                override fun onError(errorCode: Int) {
                    Toast.makeText(
                        activity,
                        embeddedNavigationErrorMessage(errorCode),
                        Toast.LENGTH_LONG
                    ).show()
                    onExitNavigation()
                }
            }
        )

        onDispose {
            navigatorRef[0]?.unregisterServiceForNavUpdates()
            navigatorRef[0]?.stopGuidance()
            navigatorRef[0]?.cleanup()
            navigatorRef[0] = null
            NavigationInstructionStore.reset()

            navigationView.onPause()
            navigationView.onStop()
            navigationView.onDestroy()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = compactNavigationCardHeight),
            factory = { navigationView }
        )

        NavigationInstructionHeader(
            instruction = instruction,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth()
        )

        NavigationBottomCard(
            destinationName = destinationName,
            destinationAddress = destinationAddress,
            distanceKm = distanceKm,
            durationMinutes = durationMinutes,
            isExpanded = isNavigationCardExpanded,
            isSoundEnabled = isSoundEnabled,
            onToggleExpanded = {
                isNavigationCardExpanded = !isNavigationCardExpanded
            },
            onToggleSound = {
                val soundEnabled = !isSoundEnabled
                isSoundEnabled = soundEnabled
                navigatorRef[0]?.applyAudioGuidance(soundEnabled)
            },
            onStop = {
                navigatorRef[0]?.stopGuidance()
                onExitNavigation()
            },
            onResume = {
                navigatorRef[0]?.startGuidance()
                isNavigationCardExpanded = false
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        )
    }
}

private fun startEmbeddedGuidance(
    activity: MainActivity,
    navigator: Navigator,
    destinationName: String,
    destinationLat: Double,
    destinationLng: Double,
    isSoundEnabled: () -> Boolean
) {
    val destination = Waypoint.builder()
        .setLatLng(destinationLat, destinationLng)
        .setTitle(destinationName)
        .build()

    val routingOptions = RoutingOptions().apply {
        travelMode(RoutingOptions.TravelMode.DRIVING)
    }

    navigator.setDestination(destination, routingOptions).setOnResultListener(
        ListenableResultFuture.OnResultListener { routeStatus ->
            when (routeStatus) {
                Navigator.RouteStatus.OK -> {
                    navigator.applyAudioGuidance(isSoundEnabled())

                    if (BuildConfig.DEBUG) {
                        navigator.getSimulator()
                            .simulateLocationsAlongExistingRoute(
                                SimulationOptions().speedMultiplier(5f)
                            )
                    }

                    navigator.startGuidance()
                }

                Navigator.RouteStatus.NO_ROUTE_FOUND -> {
                    Toast.makeText(activity, "Nu a fost gasita o ruta.", Toast.LENGTH_LONG).show()
                }

                Navigator.RouteStatus.NETWORK_ERROR -> {
                    Toast.makeText(activity, "Eroare de retea la navigatie.", Toast.LENGTH_LONG)
                        .show()
                }

                Navigator.RouteStatus.ROUTE_CANCELED -> {
                    Toast.makeText(activity, "Calcularea rutei a fost anulata.", Toast.LENGTH_LONG)
                        .show()
                }

                else -> {
                    Toast.makeText(
                        activity,
                        "Navigatia nu a putut porni: $routeStatus",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    )
}

private fun Navigator.applyAudioGuidance(isSoundEnabled: Boolean) {
    setAudioGuidance(
        if (isSoundEnabled) {
            Navigator.AudioGuidance.VOICE_ALERTS_AND_GUIDANCE
        } else {
            Navigator.AudioGuidance.SILENT
        }
    )
}

@Composable
private fun NavigationInstructionHeader(
    instruction: NavigationInstruction,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCard.copy(alpha = 0.96f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            instruction.maneuverBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = 2.dp)
                )
            } ?: Text(
                text = maneuverLabel(instruction.maneuver),
                color = Orange,
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.headlineMedium
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = formatStepDistance(instruction.distanceMeters),
                    color = Orange,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = instruction.instruction,
                    color = SoftWhite,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium
                )
                instruction.roadName?.let { roadName ->
                    Text(
                        text = roadName,
                        color = MutedText,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun NavigationBottomCard(
    destinationName: String,
    destinationAddress: String,
    distanceKm: Double?,
    durationMinutes: Double?,
    isExpanded: Boolean,
    isSoundEnabled: Boolean,
    onToggleExpanded: () -> Unit,
    onToggleSound: () -> Unit,
    onStop: () -> Unit,
    onResume: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.animateContentSize(),
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCard.copy(alpha = 0.97f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpanded),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(42.dp))

                Text(
                    modifier = Modifier.weight(1f),
                    text = formatEta(durationMinutes),
                    color = SoftWhite,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.headlineSmall
                )

                IconButton(onClick = onToggleSound) {
                    Icon(
                        imageVector = if (isSoundEnabled) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                        contentDescription = if (isSoundEnabled) "Sound on" else "Sound off",
                        tint = Orange
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
                        tint = MutedText
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatRemainingTime(durationMinutes),
                    color = MutedText,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    text = "|",
                    color = MutedText
                )

                Text(
                    text = formatRemainingDistance(distanceKm),
                    color = SoftWhite,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = formatDestinationLabel(destinationName, destinationAddress),
                        color = SoftWhite,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Orange),
                            onClick = onStop
                        ) {
                            Text("Stop", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Orange,
                                contentColor = Color.White
                            ),
                            onClick = onResume
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Resume", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

private fun formatStepDistance(distanceMeters: Int?): String {
    if (distanceMeters == null) {
        return "Continue"
    }

    return if (distanceMeters < 1000) {
        "In $distanceMeters m"
    } else {
        "In %.1f km".format(distanceMeters / 1000.0)
    }
}

private fun formatEta(durationMinutes: Double?): String {
    if (durationMinutes == null) {
        return "ETA --:--"
    }

    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    val arrivalTime = LocalTime.now().plusMinutes(durationMinutes.toLong())
    return "ETA ${arrivalTime.format(formatter)}"
}

private fun formatRemainingTime(durationMinutes: Double?): String {
    if (durationMinutes == null) {
        return "-- min"
    }

    return "%.0f min".format(durationMinutes)
}

private fun formatRemainingDistance(distanceKm: Double?): String {
    if (distanceKm == null) {
        return "-- km"
    }

    return "%.1f km".format(distanceKm)
}

private fun formatDestinationLabel(
    destinationName: String,
    destinationAddress: String
): String {
    val name = destinationName.trim()
    val address = destinationAddress.trim()

    if (name.isBlank()) {
        return address.ifBlank { "Selected destination" }
    }

    if (address.isBlank() || name.equals(address, ignoreCase = true)) {
        return name
    }

    return "$name\n$address"
}

private fun maneuverLabel(maneuver: Int): String =
    when (maneuver) {
        Maneuver.TURN_LEFT,
        Maneuver.TURN_KEEP_LEFT,
        Maneuver.TURN_SLIGHT_LEFT,
        Maneuver.TURN_SHARP_LEFT,
        Maneuver.ON_RAMP_LEFT,
        Maneuver.OFF_RAMP_LEFT,
        Maneuver.MERGE_LEFT,
        Maneuver.FORK_LEFT -> "LEFT"

        Maneuver.TURN_RIGHT,
        Maneuver.TURN_KEEP_RIGHT,
        Maneuver.TURN_SLIGHT_RIGHT,
        Maneuver.TURN_SHARP_RIGHT,
        Maneuver.ON_RAMP_RIGHT,
        Maneuver.OFF_RAMP_RIGHT,
        Maneuver.MERGE_RIGHT,
        Maneuver.FORK_RIGHT -> "RIGHT"

        Maneuver.STRAIGHT,
        Maneuver.DEPART -> "GO"

        Maneuver.DESTINATION,
        Maneuver.DESTINATION_LEFT,
        Maneuver.DESTINATION_RIGHT -> "ARRIVE"

        else -> "NAV"
    }

private fun embeddedNavigationErrorMessage(errorCode: Int): String =
    when (errorCode) {
        NavigationApi.ErrorCode.NOT_AUTHORIZED ->
            "Cheia API nu este autorizata pentru Google Navigation SDK."

        NavigationApi.ErrorCode.TERMS_NOT_ACCEPTED ->
            "Termenii Google Navigation SDK nu au fost acceptati."

        NavigationApi.ErrorCode.NETWORK_ERROR ->
            "Eroare de retea la initializarea Navigation SDK."

        NavigationApi.ErrorCode.LOCATION_PERMISSION_MISSING ->
            "Permisiunea de locatie lipseste."

        else ->
            "Eroare la initializarea Navigation SDK: $errorCode"
    }
