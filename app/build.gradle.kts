plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.g22.orbitsoundkotlin"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.g22.orbitsoundkotlin"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // 🔑 Variables de entorno para Spotify
        buildConfigField("String", "SPOTIFY_CLIENT_ID", "\"${getSpotifyClientId()}\"")
        buildConfigField("String", "SPOTIFY_CLIENT_SECRET", "\"${getSpotifyClientSecret()}\"")
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // 🔥 Use the Compose BOM platform from your version catalog
    implementation(platform(libs.androidx.compose.bom))

    // ✅ Now, implement Compose libraries WITHOUT specifying versions
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.foundation:foundation-layout") // No version needed
    implementation("androidx.compose.material:material-icons-core") // No version needed
    implementation("androidx.compose.material:material-icons-extended") // No version needed

    // Other dependencies (look fine)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // 🔥 Firebase con BOM
    implementation(platform(libs.firebase.bom))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-analytics")

    // Play Services y coroutines extra
    implementation(libs.play.services.auth)
    implementation(libs.kotlinx.coroutines.play.services)

    // Test dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom)) // BOM for tests too
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

fun getSpotifyClientId(): String {
    return project.findProperty("SPOTIFY_CLIENT_ID") as? String ?: "YOUR_SPOTIFY_CLIENT_ID"
}

fun getSpotifyClientSecret(): String {
    return project.findProperty("SPOTIFY_CLIENT_SECRET") as? String ?: "YOUR_SPOTIFY_CLIENT_SECRET"
}
