package com.example.davaroutes

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.davaroutes.ui.theme.DavaRoutesTheme

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
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = {
                            val createTripIntent = Intent(
                                this@MainActivity,
                                CreateTripActivity::class.java
                            )

                            createTripIntent.putExtra("access_token", accessToken)
                            createTripIntent.putExtra("token_type", tokenType)
                            createTripIntent.putExtra("user_id", userId)
                            createTripIntent.putExtra("email", email)
                            createTripIntent.putExtra("full_name", fullName)

                            startActivity(createTripIntent)
                        }
                    ) {
                        Text("Create trip")
                    }
                }
            }
        }
    }
}