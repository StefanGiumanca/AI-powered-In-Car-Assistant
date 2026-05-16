package com.example.davaroutes

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.davaroutes.data.GoogleLoginRequest
import com.example.davaroutes.data.LoginRequest
import com.example.davaroutes.data.LoginResponse
import com.example.davaroutes.data.driverProfilesToJson
import com.example.davaroutes.data.vehiclesToJson
import com.example.davaroutes.network.RetrofitClient
import com.example.davaroutes.ui.theme.DavaRoutesTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch
import retrofit2.Response
import kotlin.jvm.java
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.example.davaroutes.ui.components.modernTextFieldColors
import com.example.davaroutes.ui.theme.*

class LoginActivity : ComponentActivity() {
    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken.isNullOrBlank()) {
                Toast.makeText(
                    this,
                    "Google login is not configured.",
                    Toast.LENGTH_SHORT
                ).show()
                return@registerForActivityResult
            }

            lifecycleScope.launch {
                loginWithGoogle(idToken)
            }
        } catch (e: ApiException) {
            Toast.makeText(
                this,
                "Google login failed: ${e.statusCode}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureGoogleSignIn()

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
        var showGoogleConfirmation by remember { mutableStateOf(false) }

        if (showGoogleConfirmation) {
            AlertDialog(
                onDismissRequest = { showGoogleConfirmation = false },
                containerColor = Color(0xFF123149),
                titleContentColor = SoftWhite,
                textContentColor = MutedText,
                title = { Text("Continue with Google?") },
                text = {
                    Text("You will choose a Google account, then DavaRoutes will create or open your profile.")
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = Orange),
                        onClick = {
                            showGoogleConfirmation = false
                            googleSignInLauncher.launch(googleSignInClient.signInIntent)
                        }
                    ) {
                        Text("Continue", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showGoogleConfirmation = false }) {
                        Text("Cancel", color = Orange)
                    }
                }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF06111C))
                .padding(horizontal = 24.dp, vertical = 32.dp)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF123149)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
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
                        label = { Text("Email", color = MutedText) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = loginTextFieldColors()
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password", color = MutedText) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = loginTextFieldColors()
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
                                        response.body()?.let { body -> openAuthenticatedSession(body) }
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

                    OutlinedButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, Color(0xFFE7EDF5)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF1E2A32)
                        ),
                        onClick = {
                            if (getString(R.string.google_web_client_id).isBlank()) {
                                Toast.makeText(
                                    this@LoginActivity,
                                    "Missing google_web_client_id in local.properties",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                showGoogleConfirmation = true
                            }
                        }
                    ) {
                        Text("Continue with Google", fontWeight = FontWeight.Bold)
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

    @Composable
    private fun loginTextFieldColors() = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Orange,
        unfocusedBorderColor = Color(0xFF6D8798),
        focusedLabelColor = Orange,
        unfocusedLabelColor = MutedText,
        cursorColor = Orange,
        focusedTextColor = SoftWhite,
        unfocusedTextColor = SoftWhite,
        focusedContainerColor = Color(0xFF173B55),
        unfocusedContainerColor = Color(0xFF173B55)
    )

    private fun configureGoogleSignIn() {
        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.google_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)
    }

    private suspend fun loginWithGoogle(idToken: String) {
        try {
            val response = RetrofitClient.api.googleLogin(GoogleLoginRequest(id_token = idToken))
            if (response.isSuccessful) {
                response.body()?.let { body -> openAuthenticatedSession(body) }
            } else {
                Toast.makeText(
                    this@LoginActivity,
                    googleLoginErrorMessage(response),
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

    private fun googleLoginErrorMessage(response: Response<LoginResponse>): String {
        val body = response.errorBody()?.string().orEmpty()
        val detail = Regex(""""detail"\s*:\s*"([^"]+)"""")
            .find(body)
            ?.groupValues
            ?.getOrNull(1)

        return detail ?: "Google login failed (${response.code()})"
    }

    private suspend fun openAuthenticatedSession(body: LoginResponse) {
        val authHeader = "Bearer ${body.access_token}"
        val bootstrapResponse = RetrofitClient.api.bootstrap(authHeader)
        val bootstrap = bootstrapResponse.body()

        openMainActivity(
            accessToken = body.access_token,
            tokenType = body.token_type,
            userId = bootstrap?.user?.id ?: body.user.id,
            email = bootstrap?.user?.email ?: body.user.email,
            fullName = bootstrap?.user?.full_name ?: body.user.full_name,
            vehiclesJson = vehiclesToJson(bootstrap?.vehicles ?: emptyList()),
            driverProfilesJson = driverProfilesToJson(bootstrap?.driver_profiles ?: emptyList())
        )
    }

    private fun openMainActivity(
        accessToken: String,
        tokenType: String,
        userId: String,
        email: String,
        fullName: String,
        vehiclesJson: String,
        driverProfilesJson: String
    ) {
        val intent = Intent(this, MainActivity::class.java)

        intent.putExtra("access_token", accessToken)
        intent.putExtra("token_type", tokenType)
        intent.putExtra("user_id", userId)
        intent.putExtra("email", email)
        intent.putExtra("full_name", fullName)
        intent.putExtra("vehicles_json", vehiclesJson)
        intent.putExtra("driver_profiles_json", driverProfilesJson)

        startActivity(intent)
        finish()
    }
}
