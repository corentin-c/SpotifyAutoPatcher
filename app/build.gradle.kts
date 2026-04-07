plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.0"
    // remove if you don't want firebase
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    buildFeatures {
        compose = true
        buildConfig = true
        viewBinding = false
    }

    packaging {
        resources {
            // Useless files
            excludes += "/XPP3_*_VERSION"
            excludes += "/font-awesome-license.txt"
            excludes += "/smali.properties"
            excludes += "/baksmali.properties"
            excludes += "/properties/apktool.properties"
            excludes += "/org/antlr/**"
            excludes += "/org/mockito/**"
            excludes += "/org/bouncycastle/pqc/**.properties"
            excludes += "/org/bouncycastle/x509/**.properties"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/**/*.txt"
            excludes += "/META-INF/**/*.properties"
            excludes += "/META-INF/DEPENDENCIES"

            // AAPT
            excludes += "/prebuilt/**/*"
        }
        jniLibs {
            // 32-bit x86 is dead
            excludes += "/lib/x86/*.so"

            // Equivalent of AndroidManifest's extractNativeLibs=true to ensure libs are compressed
            useLegacyPackaging = true
        }
    }

    namespace = "com.github.corentinc.SpotifyAutoPatcher"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.github.corentinc.SpotifyAutoPatcher"
        minSdk = 26
        targetSdk = 36
        versionCode = 7
        versionName = "2.2.0"
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
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }

    packaging {
        resources {
            // Useless files
            excludes += "/XPP3_*_VERSION"
            excludes += "/font-awesome-license.txt"
            excludes += "/smali.properties"
            excludes += "/baksmali.properties"
            excludes += "/properties/apktool.properties"
            excludes += "/org/antlr/**"
            excludes += "/org/mockito/**"
            excludes += "/org/bouncycastle/pqc/**.properties"
            excludes += "/org/bouncycastle/x509/**.properties"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/**/*.txt"
            excludes += "/META-INF/**/*.properties"
            excludes += "/META-INF/DEPENDENCIES"

            // AAPT
            excludes += "/prebuilt/**/*"
        }
        jniLibs {
            // 32-bit x86 is dead
            excludes += "/lib/x86/*.so"

            // Equivalent of AndroidManifest's extractNativeLibs=true to ensure libs are compressed
            useLegacyPackaging = true
        }
    }

}

configurations {
    all {
        // ReVanced Library has a dependency which conflicts with whatever this is. We don't use protobuf, so it should be fine.
        exclude(group = "com.google.api.grpc", module = "proto-google-common-protos")
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xexplicit-backing-fields",
            "-Xcontext-parameters",
            "-Xskip-prerelease-check"
        )
    }
}

androidComponents {
    onVariants(selector().withBuildType("release")) {
        it.packaging.resources.excludes.apply {
            // Debug metadata
            add("/META-INF/*.version")
            add("/META-INF/*.kotlin_module")
            add("/kotlin-tooling-metadata.json")

            // Kotlin debugging (https://github.com/Kotlin/kotlinx.coroutines/issues/2274)
            add("/DebugProbesKt.bin")
        }
    }
}
dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    implementation("com.google.android.material:material:1.13.0")
    implementation("app.revanced:patcher:22.0.2-dev.1")
    implementation("app.revanced:library-android:4.0.0")
    implementation("com.github.topjohnwu.libsu:nio:6.0.0")
    implementation("com.google.code.gson:gson:2.13.2")

    // remove if you don't want firebase
    implementation(platform("com.google.firebase:firebase-bom:34.8.0"))
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-analytics")


    // Core Compose libraries
    implementation(platform("androidx.compose:compose-bom:2026.01.01"))
    implementation("androidx.activity:activity-compose")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.hilt:hilt-navigation-compose:1.3.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.59")
    ksp("com.google.dagger:hilt-compiler:2.59")
}
