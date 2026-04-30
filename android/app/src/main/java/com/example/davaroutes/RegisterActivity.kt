package com.example.davaroutes

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.davaroutes.data.RegisterRequest
import com.example.davaroutes.network.RetrofitClient
import com.example.davaroutes.ui.components.ModernInput
import com.example.davaroutes.ui.theme.*
import kotlinx.coroutines.launch

class RegisterActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DavaRoutesTheme {
                RegisterScreen()
            }
        }
    }

    @Composable
    fun RegisterScreen() {
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var fullName by remember { mutableStateOf("") }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkNavy)
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = NavyCard),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Create account",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = SoftWhite
                        )

                        Text(
                            text = "Register with email, password and name",
                            color = MutedText
                        )

                        ModernInput(
                            value = email,
                            onValueChange = { email = it },
                            label = "Email"
                        )

                        ModernInput(
                            value = password,
                            onValueChange = { password = it },
                            label = "Password",
                            isPassword = true
                        )

                        ModernInput(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = "Full name"
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
                                        val request = RegisterRequest(
                                            email = email,
                                            password = password,
                                            full_name = fullName
                                        )

                                        val response = RetrofitClient.api.register(request)

                                        if (response.isSuccessful) {
                                            Toast.makeText(
                                                this@RegisterActivity,
                                                "Cont creat cu succes. Te poți loga.",
                                                Toast.LENGTH_SHORT
                                            ).show()

                                            finish()
                                        } else {
                                            val errorBody = response.errorBody()?.string()

                                            Toast.makeText(
                                                this@RegisterActivity,
                                                "Eroare register ${response.code()}: $errorBody",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(
                                            this@RegisterActivity,
                                            "Eroare: ${e.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        ) {
                            Text(
                                text = "Register",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { finish() }
                        ) {
                            Text(
                                text = "Already have an account? Login",
                                color = Orange
                            )
                        }
                    }
                }
            }
        }
    }
}