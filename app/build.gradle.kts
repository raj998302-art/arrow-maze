import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
    alias(libs.plugins.crashlytics)
    alias(libs.plugins.firebase.perf)
}

// ---- Signing config (loaded from keystore.properties if present) ----
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.zenox.arrowmaze"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.zenox.arrowmaze"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        // Native build config fields consumed by Ads/Billing/Firebase managers
        buildConfigField("String", "ADMOB_APP_ID_DEBUG", "\"ca-app-pub-3940256099942544~3347511713\"")
        buildConfigField("String", "ADMOB_APP_ID_RELEASE", "\"ca-app-pub-0000000000000000~0000000000\"")
        buildConfigField("String", "ADMOB_BANNER_DEBUG", "\"ca-app-pub-3940256099942544/6300978111\"")
        buildConfigField("String", "ADMOB_BANNER_RELEASE", "\"ca-app-pub-0000000000000000/0000000000\"")
        buildConfigField("String", "ADMOB_INTERSTITIAL_DEBUG", "\"ca-app-pub-3940256099942544/1033173712\"")
        buildConfigField("String", "ADMOB_INTERSTITIAL_RELEASE", "\"ca-app-pub-0000000000000000/0000000000\"")
        buildConfigField("String", "ADMOB_REWARDED_DEBUG", "\"ca-app-pub-3940256099942544/5224354917\"")
        buildConfigField("String", "ADMOB_REWARDED_RELEASE", "\"ca-app-pub-0000000000000000/0000000000\"")
        buildConfigField("String", "ADMOB_NATIVE_DEBUG", "\"ca-app-pub-3940256099942544/2247696110\"")
        buildConfigField("String", "ADMOB_NATIVE_RELEASE", "\"ca-app-pub-0000000000000000/0000000000\"")
        buildConfigField("String", "ADMOB_APP_OPEN_DEBUG", "\"ca-app-pub-3940256099942544/9257395921\"")
        buildConfigField("String", "ADMOB_APP_OPEN_RELEASE", "\"ca-app-pub-0000000000000000/0000000000\"")
        buildConfigField("String", "PLAY_BILLING_PREMIUM_SKU", "\"arrow_maze_premium\"")
        buildConfigField("String", "PLAY_BILLING_COINS_SMALL_SKU", "\"coins_small\"")
        buildConfigField("String", "PLAY_BILLING_COINS_MEDIUM_SKU", "\"coins_medium\"")
        buildConfigField("String", "PLAY_BILLING_COINS_LARGE_SKU", "\"coins_large\"")
        buildConfigField("String", "PLAY_BILLING_HINTS_PACK_SKU", "\"hints_pack\"")
        buildConfigField("String", "PLAY_BILLING_VIP_MONTHLY_SKU", "\"vip_monthly\"")
    }

    signingConfigs {
        create("release") {
            if (keystoreProperties.isNotEmpty()) {
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
            }
        }
    }

    buildTypes {
        debug {
            // NOTE: applicationIdSuffix intentionally omitted so that the debug build's
            // applicationId stays `com.zenox.arrowmaze` — matching the package registered
            // in the user-provided google-services.json. To use a `.debug` suffix you must
            // register `com.zenox.arrowmaze.debug` as a second Android app in the Firebase
            // console and regenerate google-services.json.
            versionNameSuffix = "-debug"
            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
            buildConfigField("boolean", "USE_DEBUG_ADS", "true")
            buildConfigField("String", "ADMOB_APP_ID", "\"ca-app-pub-3940256099942544~3347511713\"")
            buildConfigField("String", "ADMOB_BANNER", "\"ca-app-pub-3940256099942544/6300978111\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "ADMOB_REWARDED", "\"ca-app-pub-3940256099942544/5224354917\"")
            buildConfigField("String", "ADMOB_NATIVE", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "ADMOB_APP_OPEN", "\"ca-app-pub-3940256099942544/9257395921\"")
        }
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig =
                if (keystoreProperties.isNotEmpty()) signingConfigs.getByName("release")
                else null
            buildConfigField("boolean", "USE_DEBUG_ADS", "false")
            buildConfigField("String", "ADMOB_APP_ID", "\"ca-app-pub-0000000000000000~0000000000\"")
            buildConfigField("String", "ADMOB_BANNER", "\"ca-app-pub-0000000000000000/0000000000\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL", "\"ca-app-pub-0000000000000000/0000000000\"")
            buildConfigField("String", "ADMOB_REWARDED", "\"ca-app-pub-0000000000000000/0000000000\"")
            buildConfigField("String", "ADMOB_NATIVE", "\"ca-app-pub-0000000000000000/0000000000\"")
            buildConfigField("String", "ADMOB_APP_OPEN", "\"ca-app-pub-0000000000000000/0000000000\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "META-INF/*.kotlin_module"
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.splashscreen)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.runtime)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Kotlinx
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.datetime)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Firebase (BOM-managed)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.analytics.ktx)
    implementation(libs.firebase.crashlytics.ktx)
    implementation(libs.firebase.messaging.ktx)
    implementation(libs.firebase.config.ktx)
    implementation(libs.firebase.storage.ktx)

    // Google Sign-In via Credentials API
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.play.services.auth)

    // Image / Animation
    implementation(libs.coil.compose)
    implementation(libs.lottie.compose)

    // Media
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.common)

    // Ads
    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)

    // Billing
    implementation(libs.play.billing.ktx)

    // Utils
    implementation(libs.timber)
}
