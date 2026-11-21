import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Our own Maps key lives only in the untracked local.properties (MAPS_API_KEY=...); blank leaves the Place map empty
val mapsApiKey: String = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}.getProperty("MAPS_API_KEY", "").trim()

android {
    namespace = "com.quizango.gallery"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.quizango.gallery"
        minSdk = 24
        targetSdk = 36
        versionCode = 150
        versionName = "2.7.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
    }

    buildTypes {
        debug {
        }
        release {
            // R8: shrinks and optimizes code; unminified Compose renders noticeably slower
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.biometric)
    implementation("com.android.installreferrer:installreferrer:2.2")
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Storage & Preferences
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.security.crypto)
    implementation("androidx.print:print:1.0.0")

    // Media Loading & Playback
    implementation(libs.coil.compose)
    implementation(libs.coil.video)
    implementation(libs.coil.gif)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)

    // Place map (UI-SPEC §7, user decision 2026-11-21)
    implementation("com.google.android.gms:play-services-maps:20.1.0")

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Lottie Animation
    implementation(libs.lottie.compose)

    // ML Kit Subject Segmentation
    implementation(libs.play.services.mlkit.subject.segmentation)
    implementation(libs.mlkit.vision.common)
    implementation(libs.play.services.mlkit.image.labeling)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
