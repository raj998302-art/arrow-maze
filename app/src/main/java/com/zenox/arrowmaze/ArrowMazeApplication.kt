package com.zenox.arrowmaze

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.auth.FirebaseAuth
import com.zenox.arrowmaze.core.audio.AudioManager
import com.zenox.arrowmaze.core.firebase.crashlytics.CrashlyticsManager
import com.zenox.arrowmaze.core.firebase.crashlytics.CrashlyticsTree
import dagger.Lazy
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
 * AUDIT-1: foreground/background audio lifecycle. We track the count of
 * started Activities so we can pause background music when the app is
 * backgrounded (releases the audio focus implicitly) and resume when the
 * user returns. On [onTerminate] (emulator only) we fully release the
 * [AudioManager] — production process death reclaims native resources via
 * the OS, but the explicit release documents intent and keeps instrumented
 * tests leak-free.
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

    /**
     * Lazy handle to the [AudioManager] singleton. Wrapped in `dagger.Lazy`
     * so the singleton (and therefore the SoundPool + settings-flow
     * collector) is NOT created at app launch — only when the first
     * feature screen injects AudioManager and calls a play method.
     */
    @Inject lateinit var audioManagerLazy: Lazy<AudioManager>

    /** Number of Activities currently in STARTED state. 0 → app backgrounded. */
    private var startedActivityCount: Int = 0

    override fun onCreate() {
        super.onCreate()

        // ---- Global uncaught-exception handler ----
        // Install a last-resort handler that logs the stack trace to Timber
        // (→ Crashlytics in release) BEFORE the process dies. This ensures
        // every crash is visible in the Crashlytics console even if the
        // SDK's own auto-init races with the crash.
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                Timber.e(throwable, "UNCAUGHT EXCEPTION on %s", thread.name)
            } catch (_: Throwable) { /* never crash inside the crash handler */ }
            previousHandler?.uncaughtException(thread, throwable)
        }

        // ---- Logging: Timber trees ----
        // Wrapped in runCatching so a missing Crashlytics init (e.g. if
        // google-services.json wasn't processed) doesn't prevent the app
        // from booting.
        runCatching {
            if (BuildConfig.DEBUG) {
                if (Timber.treeCount == 0) Timber.plant(Timber.DebugTree())
                crashlyticsManager.setCollectionEnabled(false)
            } else {
                if (Timber.treeCount == 0) Timber.plant(crashlyticsTree)
                crashlyticsManager.setCollectionEnabled(true)
            }
            crashlyticsManager.setCustomKey("build_type", BuildConfig.BUILD_TYPE)
            crashlyticsManager.setCustomKey("debug", BuildConfig.DEBUG)
            crashlyticsManager.setCustomKey("version_code", BuildConfig.VERSION_CODE)
            crashlyticsManager.setCustomKey("version_name", BuildConfig.VERSION_NAME)
        }.onFailure { Timber.w(it, "Crashlytics/Timber init failed — continuing without crash reporting.") }

        // ---- Analytics: app_open event + auth-state listener ----
        // Every call here is best-effort — a Firebase init failure must
        // NEVER prevent the app from reaching the first screen.
        runCatching {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.APP_OPEN) {
                param("build_type", BuildConfig.BUILD_TYPE)
            }
        }.onFailure { Timber.w(it, "FirebaseAnalytics.logEvent(APP_OPEN) failed.") }

        runCatching {
            FirebaseAuth.getInstance().addAuthStateListener { auth ->
                val user = auth.currentUser
                runCatching {
                    if (user != null) {
                        firebaseAnalytics.setUserId(user.uid)
                        firebaseAnalytics.setUserProperty("is_guest", user.isAnonymous.toString())
                        crashlyticsManager.setUserId(user.uid)
                    } else {
                        firebaseAnalytics.setUserId(null)
                        firebaseAnalytics.setUserProperty("is_guest", null)
                        crashlyticsManager.setUserId(null)
                    }
                }.onFailure { Timber.w(it, "Auth-state analytics sync failed.") }
            }
        }.onFailure { Timber.w(it, "FirebaseAuth.AuthStateListener registration failed.") }

        // ---- Audio lifecycle: pause music when the app is backgrounded ----
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                if (startedActivityCount == 0) {
                    runCatching { audioManagerLazy.get().resumeMusic() }
                        .onFailure { Timber.w(it, "AudioManager.resumeMusic on foreground failed.") }
                }
                startedActivityCount++
            }

            override fun onActivityStopped(activity: Activity) {
                startedActivityCount = (startedActivityCount - 1).coerceAtLeast(0)
                if (startedActivityCount == 0) {
                    runCatching { audioManagerLazy.get().pauseMusic() }
                        .onFailure { Timber.w(it, "AudioManager.pauseMusic on background failed.") }
                }
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    override fun onTerminate() {
        super.onTerminate()
        // Emulator-only callback (production processes are killed, not
        // terminated). Still — release the AudioManager so instrumented
        // tests that drive a full application lifecycle don't leak the
        // SoundPool / MediaPlayer across test cases.
        runCatching { audioManagerLazy.get().release() }
            .onFailure { Timber.w(it, "AudioManager.release on terminate failed.") }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) android.util.Log.DEBUG else android.util.Log.INFO)
            .build()
}
