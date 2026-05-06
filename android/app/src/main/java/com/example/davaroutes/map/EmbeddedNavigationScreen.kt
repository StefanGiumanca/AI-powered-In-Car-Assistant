package com.example.davaroutes.map

import android.os.Bundle
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.Image
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

@Composable
fun EmbeddedNavigationScreen(
    activity: MainActivity,
    destinationName: String,
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
                        destinationLng = destinationLng
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
            modifier = Modifier.fillMaxSize(),
            factory = { navigationView }
        )

        NavigationInstructionHeader(
            instruction = instruction,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth()
        )
    }
}

private fun startEmbeddedGuidance(
    activity: MainActivity,
    navigator: Navigator,
    destinationName: String,
    destinationLat: Double,
    destinationLng: Double
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
                    navigator.setAudioGuidance(
                        Navigator.AudioGuidance.VOICE_ALERTS_AND_GUIDANCE
                    )

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
