plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.medicinereminderapp"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.medicinereminderapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // Sign the release build with the debug key so the APK is directly
        // installable (e.g. via a GitHub Release) without a dedicated release
        // keystore. Not suitable for Play Store distribution.
        getByName("debug") {}
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("debug")
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
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.activity:activity:1.9.3")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // --- Added libraries for reporting UI (pie chart + live update) ---
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")
}
