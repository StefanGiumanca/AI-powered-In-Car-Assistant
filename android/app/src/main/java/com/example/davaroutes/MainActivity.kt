package com.example.davaroutes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.davaroutes.data.EXTRA_VEHICLES_JSON
import com.example.davaroutes.map.MapScreen
import com.example.davaroutes.ui.theme.DavaRoutesTheme
import com.google.android.libraries.places.api.Places

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
        val vehiclesJson = intent.getStringExtra(EXTRA_VEHICLES_JSON) ?: "[]"

        setContent {
            DavaRoutesTheme {
                MapScreen(
                    accessToken = accessToken,
                    tokenType = tokenType,
                    userId = userId,
                    email = email,
                    fullName = fullName,
                    vehiclesJson = vehiclesJson,
                    activity = this
                )
            }
        }
    }
}