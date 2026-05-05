package com.example.davaroutes.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.location.Location
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.davaroutes.MainActivity
import com.example.davaroutes.UserDashboard
import com.example.davaroutes.data.EXTRA_VEHICLES_JSON
import com.example.davaroutes.data.TripRequest
import com.example.davaroutes.network.RetrofitClient
import com.example.davaroutes.ui.theme.DarkNavy
import com.example.davaroutes.ui.theme.NavyCard
import com.example.davaroutes.ui.theme.Orange
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.maps.android.PolyUtil
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
import java.time.LocalDateTime

@Composable
fun MapScreen(
    accessToken: String,
    tokenType: String,
    userId: String,
    email: String,
    fullName: String,
    vehiclesJson: String,
    activity: MainActivity
) {
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var destinationLocation by remember { mutableStateOf<LatLng?>(null) }
    var destinationName by remember { mutableStateOf("") }
    var permissionGranted by remember { mutableStateOf(false) }

    var showRouteForm by remember { mutableStateOf(false) }
    var currentRange by remember { mutableStateOf("") }
    var routePreferences by remember { mutableStateOf("") }

    var routePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var isLoadingRoute by remember { mutableStateOf(false) }
    var distanceKm by remember { mutableStateOf<Double?>(null) }
    var durationMinutes by remember { mutableStateOf<Double?>(null) }

    var isDarkMap by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState()

    fun clearDestination() {
        destinationLocation = null
        destinationName = ""
        routePoints = emptyList()
        distanceKm = null
        durationMinutes = null
        showRouteForm = false
        currentRange = ""
        routePreferences = ""

        userLocation?.let { location ->
            activity.lifecycleScope.launch {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(location, 16f)
                )
            }
        }
    }

    val placesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == android.app.Activity.RESULT_OK && result.data != null) {
            val place = Autocomplete.getPlaceFromIntent(result.data!!)
            val latLng = place.latLng

            if (latLng != null) {
                destinationLocation = latLng
                destinationName = place.name ?: place.address ?: "Selected destination"

                routePoints = emptyList()
                distanceKm = null
                durationMinutes = null

                activity.lifecycleScope.launch {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(latLng, 15f)
                    )
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted =
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted =
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        permissionGranted = fineLocationGranted || coarseLocationGranted

        if (permissionGranted) {
            getUserLocation(activity, cameraPositionState) { location ->
                userLocation = location
            }
        } else {
            Toast.makeText(
                activity,
                "Permisiunea pentru locație nu a fost acordată",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavy)
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = permissionGranted,
                mapStyleOptions = if (isDarkMap) {
                    MapStyleOptions(MapStyles.DARK_MAP_STYLE)
                } else {
                    null
                }
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                compassEnabled = false,
                myLocationButtonEnabled = false,
                mapToolbarEnabled = false
            )
        ) {
            destinationLocation?.let { destination ->
                Marker(
                    state = MarkerState(position = destination),
                    title = destinationName
                )
            }

            if (routePoints.isNotEmpty()) {
                Polyline(
                    points = routePoints,
                    color = Orange,
                    width = 12f
                )
            }
        }

        SearchDestinationCard(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 56.dp, start = 16.dp, end = 78.dp)
                .fillMaxWidth(),
            destinationName = destinationName,
            onSearchClick = {
                val fields = listOf(
                    Place.Field.ID,
                    Place.Field.NAME,
                    Place.Field.ADDRESS,
                    Place.Field.LAT_LNG
                )

                val intent = Autocomplete.IntentBuilder(
                    AutocompleteActivityMode.OVERLAY,
                    fields
                ).build(activity)

                placesLauncher.launch(intent)
            },
            onClearClick = {
                clearDestination()
            }
        )

        FloatingActionButton(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 56.dp, end = 16.dp)
                .size(52.dp),
            containerColor = Orange,
            contentColor = Color.White,
            onClick = {
                val intent = Intent(activity, UserDashboard::class.java)

                intent.putExtra("access_token", accessToken)
                intent.putExtra("token_type", tokenType)
                intent.putExtra("user_id", userId)
                intent.putExtra("email", email)
                intent.putExtra("full_name", fullName)
                intent.putExtra(EXTRA_VEHICLES_JSON, vehiclesJson)

                activity.startActivity(intent)
            }
        ) {
            Text("☰")
        }

        destinationLocation?.let {
            RouteSummaryCard(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = 92.dp)
                    .fillMaxWidth(),
                destinationName = destinationName,
                distanceKm = distanceKm,
                durationMinutes = durationMinutes,
                isLoadingRoute = isLoadingRoute,
                onCancelClick = {
                    clearDestination()
                },
                onGoClick = {
                    showRouteForm = true
                }
            )
        }

        FloatingActionButton(
            modifier = Modifier
                .padding(start = 16.dp, bottom = 16.dp)
                .align(Alignment.BottomStart),
            containerColor = Orange,
            contentColor = Color.White,
            onClick = {
                if (permissionGranted) {
                    getUserLocation(activity, cameraPositionState) { location ->
                        userLocation = location
                    }
                } else {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            }
        ) {
            Text("📍")
        }

        FloatingActionButton(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = 16.dp,
                    bottom = if (destinationLocation != null) 260.dp else 16.dp
                )
                .size(52.dp),
            containerColor = NavyCard,
            contentColor = Orange,
            onClick = {
                isDarkMap = !isDarkMap
            }
        ) {
            Text(if (isDarkMap) "☀️" else "🌙")
        }

        if (showRouteForm && destinationLocation != null) {
            RouteDetailsDialog(
                currentRange = currentRange,
                routePreferences = routePreferences,
                isLoadingRoute = isLoadingRoute,
                onCurrentRangeChange = { currentRange = it },
                onRoutePreferencesChange = { routePreferences = it },
                onDismiss = {
                    if (!isLoadingRoute) {
                        showRouteForm = false
                    }
                },
                onConfirm = {
                    val origin = userLocation
                    val destination = destinationLocation

                    if (userId.isBlank()) {
                        Toast.makeText(activity, "User ID lipsă", Toast.LENGTH_SHORT).show()
                        return@RouteDetailsDialog
                    }

                    if (origin == null) {
                        Toast.makeText(activity, "Locația curentă lipsește", Toast.LENGTH_SHORT).show()
                        return@RouteDetailsDialog
                    }

                    if (destination == null) {
                        Toast.makeText(activity, "Destinația lipsește", Toast.LENGTH_SHORT).show()
                        return@RouteDetailsDialog
                    }

                    if (currentRange.isBlank()) {
                        Toast.makeText(activity, "Introdu range-ul actual", Toast.LENGTH_SHORT).show()
                        return@RouteDetailsDialog
                    }

                    val trip = TripRequest(
                        user_id = userId,
                        vehicle_id = "",
                        driver_profile_id = null,

                        origin_label = "Current location",
                        origin_lat = origin.latitude,
                        origin_lng = origin.longitude,

                        destination_label = destinationName,
                        destination_lat = destination.latitude,
                        destination_lng = destination.longitude,

                        departure_time = LocalDateTime.now().toString(),
                        requested_mode = "driver",

                        current_range = currentRange,
                        route_preferences = routePreferences
                    )

                    activity.lifecycleScope.launch {
                        try {
                            isLoadingRoute = true

                            val response = RetrofitClient.api.previewRoute(
                                trip = trip
                            )

                            if (response.isSuccessful) {
                                val route = response.body()

                                if (route != null) {
                                    routePoints = PolyUtil.decode(route.polyline)
                                    distanceKm = route.distance_km
                                    durationMinutes = route.duration_minutes

                                    showRouteForm = false

                                    if (routePoints.isNotEmpty()) {
                                        cameraPositionState.animate(
                                            CameraUpdateFactory.newLatLngZoom(
                                                routePoints.first(),
                                                12f
                                            )
                                        )
                                    }

                                    Toast.makeText(
                                        activity,
                                        "Ruta a fost generată",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        activity,
                                        "Răspuns gol de la server",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } else {
                                Toast.makeText(
                                    activity,
                                    "Eroare server: ${response.code()}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(
                                activity,
                                "Eroare: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        } finally {
                            isLoadingRoute = false
                        }
                    }
                }
            )
        }
    }
}

@SuppressLint("MissingPermission")
private fun getUserLocation(
    activity: MainActivity,
    cameraPositionState: CameraPositionState,
    onLocationReceived: (LatLng?) -> Unit
) {
    val fusedLocationClient =
        LocationServices.getFusedLocationProviderClient(activity)

    fusedLocationClient.getCurrentLocation(
        Priority.PRIORITY_HIGH_ACCURACY,
        null
    )
        .addOnSuccessListener { location: Location? ->
            if (location != null) {
                val latLng = LatLng(location.latitude, location.longitude)

                activity.lifecycleScope.launch {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(latLng, 16f)
                    )
                    onLocationReceived(latLng)
                }
            } else {
                Toast.makeText(
                    activity,
                    "Nu s-a putut obține locația. Verifică dacă GPS-ul este pornit.",
                    Toast.LENGTH_SHORT
                ).show()
                onLocationReceived(null)
            }
        }
        .addOnFailureListener { e ->
            Toast.makeText(
                activity,
                "Eroare la obținerea locației: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
            onLocationReceived(null)
        }
}