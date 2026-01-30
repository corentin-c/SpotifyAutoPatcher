plugins {
	id("com.android.application")
	id("org.jetbrains.kotlin.android")
	id("org.jetbrains.kotlin.plugin.compose") version "2.2.0"
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
	}

	android {
		packaging {
			jniLibs {
				useLegacyPackaging = true
			}
		}
	}


	namespace = "com.github.corentinc.SpotifyAutoPatcher"
	compileSdk = 35

	defaultConfig {
		applicationId = "com.github.corentinc.SpotifyAutoPatcher"
		minSdk = 26
		targetSdk = 33
		versionCode = 4
		versionName = "1.2.0"
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
	buildFeatures {
		viewBinding = false
	}

	dependenciesInfo {
		// Disables dependency metadata when building APKs.
		includeInApk = false
		// Disables dependency metadata when building Android App Bundles.
		includeInBundle = false
	}
}
dependencies {
	implementation("androidx.core:core-ktx:1.16.0")
	coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
	implementation("com.google.android.material:material:1.12.0")
	implementation("app.revanced:revanced-patcher:21.1.0-dev.1")
	implementation("app.revanced:revanced-library-android:3.2.0-dev.1")
	implementation("com.github.topjohnwu.libsu:nio:5.2.2")
	implementation("com.google.code.gson:gson:2.13.1")

	// remove if you don't want firebase
	implementation(platform("com.google.firebase:firebase-bom:34.0.0"))
	implementation("com.google.firebase:firebase-crashlytics")
	implementation("com.google.firebase:firebase-analytics")


	// Core Compose libraries
	implementation(platform("androidx.compose:compose-bom:2025.07.00"))
	implementation("androidx.activity:activity-compose")
	implementation("androidx.compose.ui:ui")
	implementation("androidx.compose.ui:ui-tooling-preview")
	implementation("androidx.compose.material3:material3")
	implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
	debugImplementation("androidx.compose.ui:ui-tooling")
	debugImplementation("androidx.compose.ui:ui-test-manifest")

	// Hilt
	implementation("com.google.dagger:hilt-android:2.57")
	ksp("com.google.dagger:hilt-compiler:2.57")
}
