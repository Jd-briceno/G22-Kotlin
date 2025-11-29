plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    id("com.google.dagger.hilt.android")
    id("kotlin-kapt")
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
        // 🔑 Variable de entorno para Straico (defínela en gradle.properties o como -PSTRAICO_API_KEY)
        buildConfigField("String", "STRAICO_API_KEY", "\"${getStraicoKey()}\"")
        // 🔑 Variable de entorno para Gemini
        buildConfigField("String", "GEMINI_API_KEY", "\"${getGeminiKey()}\"")
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
        isCoreLibraryDesugaringEnabled = true
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


    implementation("androidx.compose.foundation:foundation-layout")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material:material")
    // Other dependencies (look fine)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Security Crypto (for encrypted session cache)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Add OkHttp for calling the OpenAI API
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

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

    implementation("com.google.dagger:hilt-android:2.51.1")
    kapt("com.google.dagger:hilt-android-compiler:2.51.1")

    // This provides the @Inject annotation
    implementation("javax.inject:javax.inject:1")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

}

fun getSpotifyClientId(): String {
    return project.findProperty("SPOTIFY_CLIENT_ID") as? String ?: "YOUR_SPOTIFY_CLIENT_ID"
}

fun getSpotifyClientSecret(): String {
    return project.findProperty("SPOTIFY_CLIENT_SECRET") as? String ?: "YOUR_SPOTIFY_CLIENT_SECRET"
}

// Helper to fetch Straico key from project properties (or fallback placeholder). Define STRAICO_API_KEY in gradle.properties or pass -PSTRAICO_API_KEY.
fun getStraicoKey(): String {
    return project.findProperty("STRAICO_API_KEY") as? String ?: "YOUR_STRAICO_API_KEY"
}

fun getGeminiKey(): String {
    return project.findProperty("GEMINI_API_KEY") as? String ?: "YOUR_GEMINI_API_KEY"
}
