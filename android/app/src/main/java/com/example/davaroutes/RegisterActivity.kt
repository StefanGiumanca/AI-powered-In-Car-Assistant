package com.example.davaroutes

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.davaroutes.data.RegisterRequest
import com.example.davaroutes.network.RetrofitClient
import com.example.davaroutes.ui.theme.DavaRoutesTheme
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.example.davaroutes.ui.components.ModernInput
import com.example.davaroutes.ui.components.SectionTitle
import com.example.davaroutes.ui.components.modernTextFieldColors
import com.example.davaroutes.ui.theme.*

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

        var profileName by remember { mutableStateOf("") }
        var profileType by remember { mutableStateOf("balanced") }

        var vehicleModel by remember { mutableStateOf("") }
        var vehicleYear by remember { mutableStateOf("") }
        var powertrain by remember { mutableStateOf("ICE") }

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
                            text = "Set up your driving profile",
                            color = MutedText
                        )

                        ModernInput(email, { email = it }, "Email")
                        ModernInput(password, { password = it }, "Password", true)
                        ModernInput(fullName, { fullName = it }, "Full name")

                        SectionTitle("Driver profile")

                        ModernInput(profileName, { profileName = it }, "Profile name")
                        ModernInput(profileType, { profileType = it }, "Profile type")

                        SectionTitle("Vehicle")

                        ModernInput(vehicleModel, { vehicleModel = it }, "Vehicle model")
                        ModernInput(vehicleYear, { vehicleYear = it }, "Vehicle year")
                        ModernInput(powertrain, { powertrain = it }, "Powertrain")

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
                                            full_name = fullName,
                                            profile_name = profileName.ifBlank { null },
                                            profile_type = profileType.ifBlank { null },
                                            vehicle_model = vehicleModel.ifBlank { null },
                                            vehicle_year = vehicleYear.toIntOrNull(),
                                            powertrain = powertrain.ifBlank { null }
                                        )

                                        val response = RetrofitClient.api.register(request)

                                        if (response.isSuccessful) {
                                            response.body()?.let { body ->
                                                val intent = Intent(
                                                    this@RegisterActivity,
                                                    MainActivity::class.java
                                                )

                                                intent.putExtra("access_token", body.access_token)
                                                intent.putExtra("token_type", body.token_type)
                                                intent.putExtra("user_id", body.user.id)
                                                intent.putExtra("email", body.user.email)
                                                intent.putExtra("full_name", body.user.full_name)

                                                startActivity(intent)
                                                finish()
                                            }
                                        } else {
                                            Toast.makeText(
                                                this@RegisterActivity,
                                                "Eroare register: ${response.code()}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(
                                            this@RegisterActivity,
                                            "Eroare: ${e.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        ) {
                            Text("Register", fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { finish() }
                        ) {
                            Text("Already have an account? Login", color = Orange)
                        }
                    }
                }
            }
        }
    }
}