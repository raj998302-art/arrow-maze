package com.zenox.arrowmaze

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.auth.FirebaseAuth
import com.zenox.arrowmaze.core.firebase.crashlytics.CrashlyticsManager
import com.zenox.arrowmaze.core.firebase.crashlytics.CrashlyticsTree
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

/**
 * Arrow Maze application entry point.
 *
 * Phase 1: minimal Hilt-enabled application with WorkManager custom config.
 * Phase 10b: plants the [CrashlyticsTree] (release only), boots
 * [FirebaseAnalytics] with default user properties, and seeds the
 * [CrashlyticsManager] with app-level custom keys (build type, version).
 *
 * The WorkManager [Configuration.Provider] is preserved so the
 * `HiltWorkerFactory` is used for every `@HiltWorker`-annotated worker.
 */
@HiltAndroidApp
class ArrowMazeApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var crashlyticsManager: CrashlyticsManager
    @Inject lateinit var crashlyticsTree: CrashlyticsTree
    @Inject lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreate() {
        super.onCreate()

        // ---- Logging: Timber trees ----
        // Debug: plant DebugTree (full logcat output, no Crashlytics).
        // Release: plant CrashlyticsTree (breadcrumbs + non-fatals in the
        // Crashlytics console).
        if (BuildConfig.DEBUG) {
            if (Timber.treeCount == 0) Timber.plant(Timber.DebugTree())
            crashlyticsManager.setCollectionEnabled(false)
        } else {
            if (Timber.treeCount == 0) Timber.plant(crashlyticsTree)
            crashlyticsManager.setCollectionEnabled(true)
        }

        // ---- Crashlytics: app-level custom keys ----
        // Slice crashes in the console by build type + version so we can
        // tell dev crashes from production crashes at a glance.
        crashlyticsManager.setCustomKey("build_type", BuildConfig.BUILD_TYPE)
        crashlyticsManager.setCustomKey("debug", BuildConfig.DEBUG)
        crashlyticsManager.setCustomKey("version_code", BuildConfig.VERSION_CODE)
        crashlyticsManager.setCustomKey("version_name", BuildConfig.VERSION_NAME)

        // ---- Analytics: app_open event + user properties ----
        // The user_id is set/cleared reactively from FirebaseAuth so every
        // subsequent analytics event is attributed to the right player.
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.APP_OPEN) {
            param("build_type", BuildConfig.BUILD_TYPE)
        }
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            val user = auth.currentUser
            if (user != null) {
                firebaseAnalytics.setUserId(user.uid)
                firebaseAnalytics.setUserProperty("is_guest", user.isAnonymous.toString())
                crashlyticsManager.setUserId(user.uid)
            } else {
                firebaseAnalytics.setUserId(null)
                firebaseAnalytics.setUserProperty("is_guest", null)
                crashlyticsManager.setUserId(null)
            }
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) android.util.Log.DEBUG else android.util.Log.INFO)
            .build()
}
