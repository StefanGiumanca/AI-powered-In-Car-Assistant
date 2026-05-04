package com.example.davaroutes

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.davaroutes.data.TripRequest
import com.example.davaroutes.network.RetrofitClient
import com.example.davaroutes.ui.theme.DavaRoutesTheme
import com.example.davaroutes.ui.theme.Orange
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.maps.android.PolyUtil
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, BuildConfig.GOOGLE_MAPS_API_KEY)
        }

        val accessToken = intent.getStringExtra("access_token") ?: ""
        val tokenType = intent.getStringExtra("token_type") ?: "bearer"
        val userId = intent.getStringExtra("user_id") ?: ""
        val email = intent.getStringExtra("email") ?: ""
        val fullName = intent.getStringExtra("full_name") ?: ""

        setContent {
            DavaRoutesTheme {
                MapScreen(
                    accessToken = accessToken,
                    tokenType = tokenType,
                    userId = userId,
                    email = email,
                    fullName = fullName,
                    activity = this@MainActivity
                )
            }
        }
    }

    @Composable
    fun MapScreen(
        accessToken: String,
        tokenType: String,
        userId: String,
        email: String,
        fullName: String,
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

        val cameraPositionState = rememberCameraPositionState()

        val placesLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val place = Autocomplete.getPlaceFromIntent(result.data!!)
                val latLng = place.latLng

                if (latLng != null) {
                    destinationLocation = latLng
                    destinationName = place.name ?: place.address ?: "Destinație selectată"

                    routePoints = emptyList()
                    distanceKm = null
                    durationMinutes = null

                    lifecycleScope.launch {
                        cameraPositionState.move(
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

        Box(modifier = Modifier.fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = permissionGranted
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

            Button(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth()
                    .height(56.dp),
                onClick = {
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
                }
            ) {
                Text(destinationName.ifBlank { "Caută destinația" })
            }

            destinationLocation?.let {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(start = 16.dp, end = 16.dp, bottom = 88.dp)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text("Destinație selectată")
                        Text(destinationName)

                        distanceKm?.let { distance ->
                            Text("Distanță: %.2f km".format(distance))
                        }

                        durationMinutes?.let { duration ->
                            Text("Durată: %.0f minute".format(duration))
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoadingRoute,
                            onClick = {
                                showRouteForm = true
                            }
                        ) {
                            Text(
                                if (isLoadingRoute) {
                                    "Se caută ruta..."
                                } else {
                                    "Go there"
                                }
                            )
                        }
                    }
                }
            }

            if (showRouteForm && destinationLocation != null) {
                AlertDialog(
                    onDismissRequest = {
                        if (!isLoadingRoute) {
                            showRouteForm = false
                        }
                    },
                    title = {
                        Text("Detalii traseu")
                    },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = currentRange,
                                onValueChange = { currentRange = it },
                                label = { Text("Range actual") },
                                placeholder = { Text("Ex: 120 km") },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isLoadingRoute
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = routePreferences,
                                onValueChange = { routePreferences = it },
                                label = { Text("Preferințe pe traseu") },
                                placeholder = {
                                    Text("Ex: benzinării, restaurante, stații de încărcare")
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isLoadingRoute
                            )

                            if (isLoadingRoute) {
                                Spacer(modifier = Modifier.height(16.dp))
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            enabled = !isLoadingRoute,
                            onClick = {
                                val origin = userLocation
                                val destination = destinationLocation

                                if (userId.isBlank()) {
                                    Toast.makeText(
                                        activity,
                                        "User ID lipsă",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@Button
                                }

                                if (origin == null) {
                                    Toast.makeText(
                                        activity,
                                        "Locația curentă lipsește",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@Button
                                }

                                if (destination == null) {
                                    Toast.makeText(
                                        activity,
                                        "Destinația lipsește",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@Button
                                }

                                if (currentRange.isBlank()) {
                                    Toast.makeText(
                                        activity,
                                        "Introdu range-ul actual",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@Button
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

                                lifecycleScope.launch {
                                    try {
                                        isLoadingRoute = true

                                        val authorization = "$tokenType $accessToken"

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
                                                    cameraPositionState.move(
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
                        ) {
                            Text(
                                if (isLoadingRoute) {
                                    "Se caută..."
                                } else {
                                    "Caută ruta"
                                }
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(
                            enabled = !isLoadingRoute,
                            onClick = {
                                showRouteForm = false
                            }
                        ) {
                            Text("Anulează")
                        }
                    }
                )
            }

            FloatingActionButton(
                modifier = Modifier
                    .padding(top = 112.dp, end = 16.dp)
                    .align(Alignment.TopEnd),
                containerColor = Orange,
                onClick = {
                    val intent = Intent(activity, UserDashboard::class.java)

                    intent.putExtra("access_token", accessToken)
                    intent.putExtra("token_type", tokenType)
                    intent.putExtra("user_id", userId)
                    intent.putExtra("email", email)
                    intent.putExtra("full_name", fullName)

                    startActivity(intent)
                }
            ) {
                Text("☰")
            }

            FloatingActionButton(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.BottomStart),
                containerColor = Orange,
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

                    lifecycleScope.launch {
                        cameraPositionState.move(
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
}