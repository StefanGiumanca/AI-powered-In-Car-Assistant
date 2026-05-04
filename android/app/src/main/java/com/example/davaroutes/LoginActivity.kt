package com.example.davaroutes

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.davaroutes.data.LoginRequest
import com.example.davaroutes.network.RetrofitClient
import com.example.davaroutes.ui.theme.DavaRoutesTheme
import kotlinx.coroutines.launch
import kotlin.jvm.java
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.example.davaroutes.ui.components.modernTextFieldColors
import com.example.davaroutes.ui.theme.*

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DavaRoutesTheme {
                LoginScreen()
            }
        }
    }

    @Composable
    fun LoginScreen() {
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkNavy)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = NavyCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Welcome back",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = SoftWhite
                    )

                    Text(
                        text = "Login to continue your routes",
                        color = MutedText
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = modernTextFieldColors()
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = modernTextFieldColors()
                    )

                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Orange),
                        onClick = {
                            lifecycleScope.launch {
                                try {
                                    val response = RetrofitClient.api.login(
                                        LoginRequest(email = email, password = password)
                                    )

                                    if (response.isSuccessful) {
                                        response.body()?.let { body ->
                                            openMainActivity(
                                                accessToken = body.access_token,
                                                tokenType = body.token_type,
                                                userId = body.user.id,
                                                email = body.user.email,
                                                fullName = body.user.full_name
                                            )
                                        }
                                    } else {
                                        Toast.makeText(
                                            this@LoginActivity,
                                            "Email sau parola gresita",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        this@LoginActivity,
                                        "Eroare: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    ) {
                        Text("Login", fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            startActivity(
                                Intent(this@LoginActivity, RegisterActivity::class.java)
                            )
                        }
                    ) {
                        Text("Create account", color = Orange)
                    }
                }
            }
        }
    }

    private fun openMainActivity(
        accessToken: String,
        tokenType: String,
        userId: String,
        email: String,
        fullName: String
    ) {
        val intent = Intent(this, MainActivity::class.java)

        intent.putExtra("access_token", accessToken)
        intent.putExtra("token_type", tokenType)
        intent.putExtra("user_id", userId)
        intent.putExtra("email", email)
        intent.putExtra("full_name", fullName)

        startActivity(intent)
        finish()
    }
}