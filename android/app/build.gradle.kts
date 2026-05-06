import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.davaroutes"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.davaroutes"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val localProperties = Properties().apply {
            val localPropsFile = rootProject.file("local.properties")
            if (localPropsFile.exists()) {
                load(localPropsFile.inputStream())
            }
        }
        val mapsApiKey = localProperties.getProperty("google_maps_api_key", "")
        val googleWebClientId = localProperties.getProperty("google_web_client_id", "")

        buildConfigField(
            "String",
            "GOOGLE_MAPS_API_KEY",
            "\"${localProperties.getProperty("google_maps_api_key", "")}\""
        )
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")

        manifestPlaceholders["GOOGLE_MAPS_API_KEY"] = mapsApiKey
        resValue("string", "google_web_client_id", googleWebClientId)
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }
}

configurations.configureEach {
    exclude(group = "com.google.android.gms", module = "play-services-maps")

    resolutionStrategy {
        force("org.chromium.net:cronet-api:143.7445.0")
        force("org.chromium.net:cronet-common:143.7445.0")
        force("org.chromium.net:cronet-fallback:143.7445.0")
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    // Google Navigation SDK bundles the Maps SDK classes used by the existing map preview.
    implementation("com.google.android.libraries.navigation:navigation:7.5.0")

    // Google services
    implementation("com.google.maps.android:maps-compose:4.3.3")
    implementation("com.google.android.gms:play-services-location:21.1.0")
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    //Places api
    implementation("com.google.android.libraries.places:places:4.3.1")

    // Permissions
    implementation("com.google.accompanist:accompanist-permissions:0.33.2-alpha")

    // Material Icons
    implementation("androidx.compose.material:material-icons-extended:1.6.8")

    implementation("com.google.maps.android:android-maps-utils:3.8.2")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs_nio:2.0.3")
}
