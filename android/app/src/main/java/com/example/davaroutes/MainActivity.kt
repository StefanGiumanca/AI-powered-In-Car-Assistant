package com.example.davaroutes

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.location.Location
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.davaroutes.ui.theme.DavaRoutesTheme
import com.example.davaroutes.ui.theme.Orange
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.rememberCameraPositionState
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import android.widget.Toast

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
        var permissionGranted by remember { mutableStateOf(false) }

        val cameraPositionState = rememberCameraPositionState()

        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

            permissionGranted = fineLocationGranted || coarseLocationGranted

            if (permissionGranted) {
                getUserLocation(activity, cameraPositionState) { location ->
                    userLocation = location
                }
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
                cameraPositionState = cameraPositionState
            )

            FloatingActionButton(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.BottomEnd),
                containerColor = Orange,
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

                    startActivity(createTripIntent)
                }
            ) {
                Text("+")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getUserLocation(
        activity: MainActivity,
        cameraPositionState: com.google.maps.android.compose.CameraPositionState,
        onLocationReceived: (LatLng?) -> Unit
    ) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity)

        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val latLng = LatLng(location.latitude, location.longitude)

                    lifecycleScope.launch {
                        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 15f)
                        cameraPositionState.move(cameraUpdate)
                        onLocationReceived(latLng)
                    }
                } else {
                    Toast.makeText(
                        activity,
                        "Nu s-a găsit locația curentă",
                        Toast.LENGTH_SHORT
                    ).show()
                    onLocationReceived(null)
                }
            }.addOnFailureListener { e: Exception ->
                Toast.makeText(
                    activity,
                    "Eroare la obținerea locației: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                onLocationReceived(null)
            }
        } catch (e: Exception) {
            Toast.makeText(
                activity,
                "Eroare: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
            onLocationReceived(null)
        }
    }
}