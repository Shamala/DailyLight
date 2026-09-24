import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Release signing lives outside the repo. Create keystore.properties next to
// this file (it is gitignored) with storeFile / storePassword / keyAlias /
// keyPassword, or set the same four as environment variables in CI. Without
// it the release build still assembles — unsigned — so a debug-only machine
// is never blocked.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun signingValue(key: String, env: String): String? =
    keystoreProperties.getProperty(key) ?: System.getenv(env)

android {
    namespace = "com.shamala.dailylight"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.shamala.dailylight"
        minSdk = 26
        targetSdk = 36
        versionCode = 5
        versionName = "1.0"
        resourceConfigurations += listOf("en")
    }

    signingConfigs {
        create("release") {
            val store = signingValue("storeFile", "DAILYLIGHT_STORE_FILE")
            if (store != null) {
                storeFile = file(store)
                storePassword = signingValue("storePassword", "DAILYLIGHT_STORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "DAILYLIGHT_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "DAILYLIGHT_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
                .takeIf { it.storeFile != null }
        }
        debug {
            // So a debug build can sit beside a Play install on the same phone.
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    // Play shows the SDK/AGP versions from this block in the console. Nothing
    // here is secret, but there is no reason to publish a dependency manifest
    // for an app that has no dependencies.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    lint {
        // A shipping build should not carry known-broken resources.
        warningsAsErrors = false
        abortOnError = true
        checkReleaseBuilds = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

// The app itself pulls in nothing: the widget uses only the platform SDK.
// JUnit is test-only and never reaches the APK.
dependencies {
    testImplementation("junit:junit:4.13.2")
}
