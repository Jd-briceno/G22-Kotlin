// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.services) apply false
    // You've correctly defined the Hilt plugin version here.
    // The error about hiltVersion was a red herring; the real issue is the kapt plugin.
    id("com.google.dagger.hilt.android") version "2.51.1" apply false
}
