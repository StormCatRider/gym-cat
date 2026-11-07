// ╔══════════════════════════════════════════════════════════════════╗
// ║ PLAIN-ENGLISH GUIDE — app/build.gradle.kts                        ║
// ║ The recipe for building the Gym Cat app:                          ║
// ║  • android { } = app id, Android versions it supports, Java 17.   ║
// ║  • dependencies { } = the libraries the app uses.                 ║
// ║ minSdk 29 = Android 10 or newer. Same setup as Gym & Ñam.         ║
// ╚══════════════════════════════════════════════════════════════════╝
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)   // turns on Jetpack Compose (the UI toolkit)
    alias(libs.plugins.ksp)              // code generator Room needs
}

android {
    namespace = "com.gymcat.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.gymcat.app"   // unique id on the phone (different from Gym & Ñam)
        minSdk = 29
        targetSdk = 35
        versionCode = 4
        versionName = "1.0.3"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
}

// Room writes a copy of the database layout here (useful for future migrations).
ksp { arg("room.schemaLocation", "$projectDir/schemas") }

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.fragment)          // needed by the fingerprint prompt

    implementation(platform(libs.compose.bom))      // one version for all Compose parts
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.navigation.compose)         // moving between screens
    implementation(libs.room.runtime)               // local database
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.datastore.preferences)      // settings storage

    implementation(libs.camerax.core)               // camera for QR scanning
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)
    implementation(libs.zxing.core)                 // QR make + read (on the phone, no internet)

    implementation(libs.glance.appwidget)           // home-screen widgets
    implementation(libs.glance.material3)
    implementation(libs.work.runtime)               // reminders + weekly auto-backup
    implementation(libs.biometric)                  // fingerprint / PIN lock
    implementation(libs.health.connect)             // Health Connect link with Gym & Ñam
    implementation(libs.coroutines.android)
}
