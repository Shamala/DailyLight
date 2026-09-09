plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.shamala.dailylight"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.shamala.dailylight"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

// Intentionally no dependencies: the widget uses only the platform SDK, so
// there is nothing to resolve beyond the Android Gradle Plugin itself.
dependencies {
}
