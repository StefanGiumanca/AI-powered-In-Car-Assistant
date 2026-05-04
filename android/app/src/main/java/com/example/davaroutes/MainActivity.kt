package com.example.davaroutes

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
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
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

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

            destinationLocation?.let { destination ->
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

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                val createTripIntent = Intent(
                                    activity,
                                    CreateTripActivity::class.java
                                )

                                createTripIntent.putExtra("access_token", accessToken)
                                createTripIntent.putExtra("token_type", tokenType)
                                createTripIntent.putExtra("user_id", userId)
                                createTripIntent.putExtra("email", email)
                                createTripIntent.putExtra("full_name", fullName)

                                userLocation?.let {
                                    createTripIntent.putExtra("origin_lat", it.latitude)
                                    createTripIntent.putExtra("origin_lng", it.longitude)
                                }

                                createTripIntent.putExtra("destination_lat", destination.latitude)
                                createTripIntent.putExtra("destination_lng", destination.longitude)
                                createTripIntent.putExtra("destination_name", destinationName)

                                startActivity(createTripIntent)
                            }
                        ) {
                            Text("Go here")
                        }
                    }
                }
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
        ).addOnSuccessListener { location: Location? ->
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
        }.addOnFailureListener { e ->
            Toast.makeText(
                activity,
                "Eroare la obținerea locației: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
            onLocationReceived(null)
        }
    }
}