package com.example.davaroutes.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.davaroutes.MainActivity
import com.example.davaroutes.UserDashboard
import com.example.davaroutes.data.EXTRA_VEHICLES_JSON
import com.example.davaroutes.data.RouteLocationDto
import com.example.davaroutes.data.RoutePreviewRequest
import com.example.davaroutes.network.RetrofitClient
import com.example.davaroutes.ui.theme.DarkNavy
import com.example.davaroutes.ui.theme.NavyCard
import com.example.davaroutes.ui.theme.Orange
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

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
    var destinationAddress by remember { mutableStateOf("") }
    var permissionGranted by remember { mutableStateOf(false) }

    var showRouteForm by remember { mutableStateOf(false) }
    var currentRange by remember { mutableStateOf("") }
    var routePreferences by remember { mutableStateOf("") }
    var showRoutePreview by remember { mutableStateOf(false) }
    var isRoutePreviewExpanded by remember { mutableStateOf(false) }

    var routePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var isLoadingRoute by remember { mutableStateOf(false) }
    var distanceKm by remember { mutableStateOf<Double?>(null) }
    var durationMinutes by remember { mutableStateOf<Double?>(null) }

    var destinationQuery by remember { mutableStateOf("") }
    var destinationPredictions by remember { mutableStateOf<List<PlacePredictionUi>>(emptyList()) }
    var isLoadingPredictions by remember { mutableStateOf(false) }

    var isDarkMap by remember { mutableStateOf(false) }
    var isNavigationMode by remember { mutableStateOf(false) }

    var isListening by remember { mutableStateOf(false) }
    var recognizedSpeech by remember { mutableStateOf("") }

    var pendingVoiceDestination by remember { mutableStateOf("") }
    var pendingVoiceNeedsRange by remember { mutableStateOf(false) }

    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var listeningTimeoutJob by remember { mutableStateOf<Job?>(null) }
    var textToSpeech by remember { mutableStateOf<TextToSpeech?>(null) }

    val cameraPositionState = rememberCameraPositionState()
    val context = LocalContext.current

    val placesClient = remember {
        com.google.android.libraries.places.api.Places.createClient(context)
    }

    var autocompleteSessionToken by remember {
        mutableStateOf(AutocompleteSessionToken.newInstance())
    }

    lateinit var startVoiceInput: () -> Unit

    DisposableEffect(Unit) {
        val tts = TextToSpeech(activity) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale("ro", "RO")
            }
        }

        textToSpeech = tts

        onDispose {
            listeningTimeoutJob?.cancel()
            speechRecognizer?.destroy()
            tts.stop()
            tts.shutdown()
        }
    }

    fun speak(message: String) {
        if (message.isBlank()) return

        textToSpeech?.speak(
            message,
            TextToSpeech.QUEUE_FLUSH,
            null,
            UUID.randomUUID().toString()
        )
    }

    fun clearDestination() {
        destinationLocation = null
        destinationName = ""
        destinationAddress = ""
        pendingVoiceDestination = ""
        pendingVoiceNeedsRange = false
        routePoints = emptyList()
        distanceKm = null
        durationMinutes = null
        showRouteForm = false
        showRoutePreview = false
        currentRange = ""
        routePreferences = ""
        isRoutePreviewExpanded = false

        userLocation?.let { location ->
            activity.lifecycleScope.launch {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(location, 16f)
                )
            }
        }
    }

    fun previewRoute() {
        val origin = userLocation
        val destination = destinationLocation

        if (userId.isBlank()) {
            Toast.makeText(activity, "User ID lipsă", Toast.LENGTH_SHORT).show()
            return
        }

        if (origin == null) {
            Toast.makeText(activity, "Locația curentă lipsește", Toast.LENGTH_SHORT).show()
            speak("Nu am locația ta curentă.")
            return
        }

        if (destination == null) {
            Toast.makeText(activity, "Destinația lipsește", Toast.LENGTH_SHORT).show()
            speak("Nu am găsit destinația.")
            return
        }

        if (currentRange.isBlank()) {
            Toast.makeText(activity, "Range-ul este obligatoriu", Toast.LENGTH_SHORT).show()
            speak("Am nevoie de autonomia curentă.")
            return
        }

        val routePreviewRequest = RoutePreviewRequest(
            origin = RouteLocationDto(
                label = "Current location",
                lat = origin.latitude,
                lng = origin.longitude
            ),
            destination = RouteLocationDto(
                label = destinationName,
                lat = destination.latitude,
                lng = destination.longitude
            ),
            current_range = currentRange,
            route_preferences = routePreferences
        )

        activity.lifecycleScope.launch {
            try {
                isLoadingRoute = true

                val response = RetrofitClient.api.previewRoute(
                    trip = routePreviewRequest
                )

                if (response.isSuccessful) {
                    val route = response.body()

                    if (route != null) {
                        routePoints = PolyUtil.decode(route.polyline)
                        distanceKm = route.distance_km
                        durationMinutes = route.duration_minutes

                        showRouteForm = false
                        showRoutePreview = true
                        pendingVoiceNeedsRange = false

                        Toast.makeText(
                            activity,
                            "Ruta a fost generată",
                            Toast.LENGTH_SHORT
                        ).show()

                        speak("Ruta a fost generată.")
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

    fun extractRangeFromSpeech(text: String): String {
        val regex = Regex(
            """(\d+)\s*(km|kilometri|kilometrii|kilometru|autonomie)?""",
            RegexOption.IGNORE_CASE
        )

        val match = regex.find(text)

        return match
            ?.groupValues
            ?.getOrNull(1)
            ?.let { "$it km" }
            .orEmpty()
    }

    fun extractDestinationFromSpeech(text: String): String {
        if (pendingVoiceNeedsRange) return ""

        val patterns = listOf(
            Regex(
                """(?:vreau sa merg|vreau să merg|merg|ma duc|mă duc|du-ma|du-mă|navigheaza|navighează|ruta catre|ruta către|catre|către|la)\s+(.+?)(?:,| si | și | am | cu | avand | având |$)""",
                RegexOption.IGNORE_CASE
            ),
            Regex(
                """(?:destinatia|destinația)\s+(.+?)(?:,| si | și | am | cu |$)""",
                RegexOption.IGNORE_CASE
            )
        )

        for (pattern in patterns) {
            val result = pattern.find(text)
                ?.groupValues
                ?.getOrNull(1)
                ?.trim()

            if (!result.isNullOrBlank()) return result
        }

        val fallbackDestination = text
            .replace(
                Regex("""\b\d+\s*(km|kilometri|kilometrii|kilometru|autonomie)?\b""", RegexOption.IGNORE_CASE),
                ""
            )
            .replace(
                Regex("""\b(am|cu|autonomie|range)\b""", RegexOption.IGNORE_CASE),
                ""
            )
            .trim(' ', ',', '.', ';', ':')

        if (fallbackDestination.isNotBlank()) {
            return fallbackDestination
        }

        return ""
    }

    fun extractPreferencesFromSpeech(text: String): String {
        if (
            text.equals("nu", ignoreCase = true) ||
            text.contains("nu am", ignoreCase = true) ||
            text.contains("fara", ignoreCase = true) ||
            text.contains("fără", ignoreCase = true)
        ) {
            return ""
        }

        val patterns = listOf(
            Regex(
                """(?:preferinte|preferințe|prefer|vreau sa trec pe la|vreau să trec pe la|trec pe la|opreste la|oprește la|cu oprire la)\s+(.+)$""",
                RegexOption.IGNORE_CASE
            ),
            Regex(
                """(restaurant|benzinarie|benzinărie|incarcator|încărcător|charger|cafenea|hotel|parcare|supermarket).*$""",
                RegexOption.IGNORE_CASE
            )
        )

        for (pattern in patterns) {
            val result = pattern.find(text)?.value?.trim()

            if (!result.isNullOrBlank()) return result
        }

        return ""
    }

    fun findDestinationAndPreview(destinationQueryFromVoice: String) {
        if (destinationQueryFromVoice.isBlank()) {
            speak("Nu am înțeles destinația. Spune unde vrei să mergi.")
            Toast.makeText(activity, "Nu am înțeles destinația", Toast.LENGTH_SHORT).show()
            return
        }

        val request = FindAutocompletePredictionsRequest
            .builder()
            .setQuery(destinationQueryFromVoice)
            .build()

        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response ->
                val firstPrediction = response.autocompletePredictions.firstOrNull()

                if (firstPrediction == null) {
                    speak("Nu am găsit destinația.")
                    Toast.makeText(
                        activity,
                        "Nu am găsit destinația: $destinationQueryFromVoice",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addOnSuccessListener
                }

                val fields = listOf(
                    Place.Field.ID,
                    Place.Field.NAME,
                    Place.Field.ADDRESS,
                    Place.Field.LAT_LNG
                )

                val fetchRequest = FetchPlaceRequest
                    .builder(firstPrediction.placeId, fields)
                    .build()

                placesClient.fetchPlace(fetchRequest)
                    .addOnSuccessListener { fetchResponse ->
                        val place = fetchResponse.place
                        val latLng = place.latLng

                        if (latLng == null) {
                            speak("Nu am putut obține coordonatele destinației.")
                            Toast.makeText(
                                activity,
                                "Nu s-au putut obține coordonatele destinației",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@addOnSuccessListener
                        }

                        destinationLocation = latLng
                        destinationName = place.name
                            ?: place.address
                                    ?: destinationQueryFromVoice

                        routePoints = emptyList()
                        distanceKm = null
                        durationMinutes = null
                        showRoutePreview = false
                        showRouteForm = false

                        activity.lifecycleScope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(latLng, 15f)
                            )
                        }

                        if (currentRange.isBlank()) {
                            pendingVoiceNeedsRange = true
                            speak("Am găsit destinația. Ce autonomie ai acum?")
                            activity.lifecycleScope.launch {
                                delay(3000)
                                startVoiceInput()
                            }
                        } else {
                            speak("Am găsit destinația. Generez ruta.")
                            previewRoute()
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            activity,
                            "Eroare la selectarea destinației: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    activity,
                    "Eroare la căutarea destinației: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    fun handleVoiceCommand(spokenText: String) {
        recognizedSpeech = spokenText

        val extractedDestination = extractDestinationFromSpeech(spokenText)
        val extractedRange = extractRangeFromSpeech(spokenText)
        val extractedPreferences = extractPreferencesFromSpeech(spokenText)

        if (extractedDestination.isNotBlank()) {
            pendingVoiceDestination = extractedDestination
        }

        if (extractedRange.isNotBlank()) {
            currentRange = extractedRange
        }

        if (extractedPreferences.isNotBlank()) {
            routePreferences = extractedPreferences
        }

        if (pendingVoiceDestination.isBlank()) {
            speak("Nu am înțeles destinația. Spune unde vrei să mergi.")
            activity.lifecycleScope.launch {
                delay(3000)
                startVoiceInput()
            }
            return
        }

        if (currentRange.isBlank()) {
            pendingVoiceNeedsRange = true
            speak("Am înțeles destinația. Ce autonomie ai acum?")
            activity.lifecycleScope.launch {
                delay(3000)
                startVoiceInput()
            }
            return
        }

        pendingVoiceNeedsRange = false

        speak("Perfect. Generez ruta.")
        findDestinationAndPreview(pendingVoiceDestination)
    }

    startVoiceInput = startVoiceInput@{
        if (isListening) {
            listeningTimeoutJob?.cancel()
            speechRecognizer?.stopListening()
            isListening = false
            return@startVoiceInput
        }

        val hasAudioPermission = ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasAudioPermission) {
            Toast.makeText(
                activity,
                "Acordă permisiunea pentru microfon și apasă din nou.",
                Toast.LENGTH_SHORT
            ).show()
            return@startVoiceInput
        }

        if (!SpeechRecognizer.isRecognitionAvailable(activity)) {
            Toast.makeText(
                activity,
                "Speech recognition nu este disponibil pe acest dispozitiv",
                Toast.LENGTH_SHORT
            ).show()
            return@startVoiceInput
        }

        speechRecognizer?.destroy()

        val newSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(activity)
        speechRecognizer = newSpeechRecognizer

        val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ro-RO")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ro-RO")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 5000L)
            putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                "Spune destinația și autonomia curentă"
            )
        }

        newSpeechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                recognizedSpeech = ""
                Toast.makeText(activity, "Te ascult...", Toast.LENGTH_SHORT).show()
            }

            override fun onBeginningOfSpeech() {
                isListening = true
            }

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                isListening = false
            }

            override fun onError(error: Int) {
                listeningTimeoutJob?.cancel()
                isListening = false
                newSpeechRecognizer.destroy()
                speechRecognizer = null

                val message = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Eroare audio sau microfon."
                    SpeechRecognizer.ERROR_CLIENT -> "Eroare client SpeechRecognizer."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permisiune microfon lipsă."
                    SpeechRecognizer.ERROR_NETWORK -> "Eroare de rețea."
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Timeout rețea."
                    SpeechRecognizer.ERROR_NO_MATCH -> "Nu am înțeles ce ai spus."
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer ocupat. Încearcă din nou."
                    SpeechRecognizer.ERROR_SERVER -> "Eroare server speech."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Nu am detectat voce."
                    else -> "Eroare speech: $error"
                }

                Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
            }

            override fun onResults(results: Bundle?) {
                listeningTimeoutJob?.cancel()
                isListening = false

                val matches = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

                val spokenText = matches
                    ?.firstOrNull { it.isNotBlank() }
                    .orEmpty()

                recognizedSpeech = spokenText

                newSpeechRecognizer.destroy()
                speechRecognizer = null

                if (spokenText.isBlank()) {
                    Toast.makeText(
                        activity,
                        "Nu am auzit nimic clar",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }

                Toast.makeText(activity, spokenText, Toast.LENGTH_LONG).show()
                handleVoiceCommand(spokenText)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

                recognizedSpeech = matches?.firstOrNull().orEmpty()
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        newSpeechRecognizer.startListening(speechIntent)

        listeningTimeoutJob?.cancel()
        listeningTimeoutJob = activity.lifecycleScope.launch {
            delay(8000)
            if (isListening) {
                newSpeechRecognizer.stopListening()
                isListening = false
            }
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startVoiceInput()
        } else {
            Toast.makeText(
                activity,
                "Permisiunea pentru microfon nu a fost acordată",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun requestOrStartVoiceInput() {
        val hasAudioPermission = ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasAudioPermission) {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            startVoiceInput()
        }
    }

    fun searchDestinationPredictions(query: String) {
        destinationQuery = query

        if (query.length < 2) {
            destinationPredictions = emptyList()
            isLoadingPredictions = false
            return
        }

        isLoadingPredictions = true

        val request = FindAutocompletePredictionsRequest
            .builder()
            .setSessionToken(autocompleteSessionToken)
            .setQuery(query)
            .build()

        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response ->
                destinationPredictions = response.autocompletePredictions.map { prediction ->
                    PlacePredictionUi(
                        placeId = prediction.placeId,
                        primaryText = prediction.getPrimaryText(null).toString(),
                        secondaryText = prediction.getSecondaryText(null).toString()
                    )
                }
                isLoadingPredictions = false
            }
            .addOnFailureListener { e ->
                destinationPredictions = emptyList()
                isLoadingPredictions = false

                Toast.makeText(
                    activity,
                    "Eroare la căutare: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    fun selectDestinationPrediction(prediction: PlacePredictionUi) {
        val fields = listOf(
            Place.Field.ID,
            Place.Field.NAME,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG
        )

        val request = FetchPlaceRequest
            .builder(prediction.placeId, fields)
            .setSessionToken(autocompleteSessionToken)
            .build()

        placesClient.fetchPlace(request)
            .addOnSuccessListener { response ->
                val place = response.place
                val latLng = place.latLng

                if (latLng == null) {
                    Toast.makeText(
                        activity,
                        "Nu s-au putut obține coordonatele destinației",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addOnSuccessListener
                }

                destinationLocation = latLng
                destinationName = place.name ?: place.address ?: prediction.primaryText
                destinationAddress = place.address ?: ""

                routePoints = emptyList()
                distanceKm = null
                durationMinutes = null
                showRoutePreview = false
                isRoutePreviewExpanded = false
                showRouteForm = false

                destinationQuery = ""
                destinationPredictions = emptyList()
                autocompleteSessionToken = AutocompleteSessionToken.newInstance()

                activity.lifecycleScope.launch {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(latLng, 15f)
                    )
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    activity,
                    "Eroare la selectarea destinației: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    val placesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK && result.data != null) {
            val place = Autocomplete.getPlaceFromIntent(result.data!!)
            val latLng = place.latLng

            if (latLng != null) {
                destinationLocation = latLng
                destinationName = place.name ?: place.address ?: "Selected destination"
                destinationAddress = place.address ?: ""

                routePoints = emptyList()
                distanceKm = null
                durationMinutes = null
                showRoutePreview = false
                isRoutePreviewExpanded = false

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

    LaunchedEffect(showRoutePreview, routePoints) {
        if (showRoutePreview && routePoints.isNotEmpty()) {
            cameraPositionState.animate(buildRouteCameraUpdate(routePoints))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavy)
    ) {
        if (isNavigationMode && destinationLocation != null) {
            val destination = destinationLocation

            EmbeddedNavigationScreen(
                activity = activity,
                destinationName = destinationName,
                destinationAddress = destinationAddress,
                destinationLat = destination!!.latitude,
                destinationLng = destination.longitude,
                distanceKm = distanceKm,
                durationMinutes = durationMinutes,
                onExitNavigation = {
                    isNavigationMode = false
                    showRoutePreview = true
                }
            )
            return@Box
        }

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

        FloatingActionButton(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 56.dp, start = 16.dp)
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

        if (destinationLocation != null && !showRoutePreview) {
            RouteSummaryCard(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
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

        if (destinationLocation != null && showRoutePreview) {
            RoutePreviewActionsCard(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
                    .fillMaxWidth(),
                destinationName = destinationName,
                distanceKm = distanceKm,
                durationMinutes = durationMinutes,
                currentRange = currentRange,
                routePreferences = routePreferences,
                isExpanded = isRoutePreviewExpanded,
                onToggleExpanded = {
                    isRoutePreviewExpanded = !isRoutePreviewExpanded
                },
                onChangeDetailsClick = {
                    showRouteForm = true
                },
                onStartTripClick = {
                    val destination = destinationLocation

                    if (destination == null) {
                        Toast.makeText(
                            activity,
                            "Destinatia lipseste",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@RoutePreviewActionsCard
                    }

                    isNavigationMode = true
                },
                onCancelClick = {
                    clearDestination()
                }
            )
        }

        if (!showRoutePreview && destinationLocation == null) {
            FloatingActionButton(
                modifier = Modifier
                    .padding(start = 16.dp, bottom = 158.dp)
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
        }

        if (!showRoutePreview && destinationLocation == null) {
            FloatingActionButton(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = 16.dp,
                        bottom = 158.dp
                    )
                    .size(52.dp),
                containerColor = NavyCard,
                contentColor = Orange,
                onClick = {
                    isDarkMap = !isDarkMap
                }
            ) {
                Text(if (isDarkMap) "Light" else "Dark")
            }
        }

        if (destinationLocation == null) {
            SearchDestinationCard(
                modifier = Modifier
                    .fillMaxSize(),
                destinationName = destinationName,
                query = destinationQuery,
                predictions = destinationPredictions,
                isLoading = isLoadingPredictions,
                onQueryChange = { query ->
                    searchDestinationPredictions(query)
                },
                onPredictionClick = { prediction ->
                    selectDestinationPrediction(prediction)
                },
                onVoiceClick = {
                    requestOrStartVoiceInput()
                },
            )
        }

        if (recognizedSpeech.isNotBlank() && !showRoutePreview) {
            Text(
                text = recognizedSpeech,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 88.dp, start = 16.dp, end = 16.dp)
                    .background(NavyCard)
                    .padding(12.dp),
                color = Color.White
            )
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
                    previewRoute()
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

private fun buildRouteCameraUpdate(routePoints: List<LatLng>): com.google.android.gms.maps.CameraUpdate {
    if (routePoints.size == 1) {
        return CameraUpdateFactory.newLatLngZoom(routePoints.first(), 15f)
    }

    val boundsBuilder = LatLngBounds.builder()
    routePoints.forEach { point ->
        boundsBuilder.include(point)
    }

    return CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 140)
}
