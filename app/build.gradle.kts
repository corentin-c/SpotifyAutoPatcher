plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.github.corentinc.SpotifyAutoPatcher"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.github.corentinc.SpotifyAutoPatcher"
        minSdk = 26
        targetSdk = 33
        versionCode = 1
        versionName = "0.0.1"
        multiDexEnabled = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        viewBinding = false
    }

    dependenciesInfo {
        // Disables dependency metadata when building APKs.
       includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }
    kotlinOptions {
        jvmTarget = "21"
    }
}
dependencies {
    implementation("androidx.core:core-ktx:1.16.0")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    implementation("com.google.android.material:material:1.12.0")
    implementation("app.revanced:revanced-patcher:21.1.0-dev.1")
    implementation("app.revanced:patches:5.27.0-dev.2")
    implementation("app.revanced:revanced-library-android:3.2.0-dev.1")
    implementation("com.github.topjohnwu.libsu:nio:5.2.2")
}
