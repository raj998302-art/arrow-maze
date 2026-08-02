package com.zenox.arrowmaze.core.firebase.crashlytics

import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin façade over [FirebaseCrashlytics] so the rest of the app never
 * imports the Firebase SDK directly. All public methods are safe to call
 * from any thread and degrade to no-ops if Crashlytics isn't initialised
 * (e.g. on the emulator with no google-services.json for the actual app
 * package).
 *
 * Convention:
 *  - Call [log] for breadcrumbs ("User tapped Play", "Level 42 started").
 *  - Call [recordException] for caught exceptions you want surfaced in
 *    the Crashlytics console without crashing the app.
 *  - Call [setUserId] on sign-in / sign-out so crashes are attributed to
 *    a player.
 *  - Call [setCustomKey] for any tag you want to slice crashes by in the
 *    console (e.g. `currentLevel = 42`, `isGuest = true`).
 */
@Singleton
class CrashlyticsManager @Inject constructor() {

    private val crashlytics: FirebaseCrashlytics by lazy { FirebaseCrashlytics.getInstance() }

    /** Appends a breadcrumb log line that appears with the next crash report. */
    fun log(message: String) {
        runCatching { crashlytics.log(message) }
            .onFailure { Timber.w(it, "Crashlytics.log failed") }
    }

    /** Records a non-fatal exception so it surfaces in the Crashlytics console. */
    fun recordException(t: Throwable) {
        runCatching { crashlytics.recordException(t) }
            .onFailure { Timber.w(it, "Crashlytics.recordException failed") }
    }

    /** Sets the user id for crash attribution. Pass `null`/empty to clear. */
    fun setUserId(uid: String?) {
        runCatching { crashlytics.setUserId(uid.orEmpty()) }
            .onFailure { Timber.w(it, "Crashlytics.setUserId failed") }
    }

    /** Sets a string custom key — used for crash slicing in the console. */
    fun setCustomKey(key: String, value: String) {
        runCatching { crashlytics.setCustomKey(key, value) }
            .onFailure { Timber.w(it, "Crashlytics.setCustomKey(String) failed") }
    }

    /** Sets a boolean custom key. */
    fun setCustomKey(key: String, value: Boolean) {
        runCatching { crashlytics.setCustomKey(key, value) }
            .onFailure { Timber.w(it, "Crashlytics.setCustomKey(Boolean) failed") }
    }

    /** Sets an integer custom key. */
    fun setCustomKey(key: String, value: Int) {
        runCatching { crashlytics.setCustomKey(key, value) }
            .onFailure { Timber.w(it, "Crashlytics.setCustomKey(Int) failed") }
    }

    /**
     * Enables / disables automatic data collection. Pass `false` in debug
     * builds (the DebugTree handles logging instead) and `true` in release.
     */
    fun setCollectionEnabled(enabled: Boolean) {
        runCatching { crashlytics.isCrashlyticsCollectionEnabled = enabled }
            .onFailure { Timber.w(it, "Crashlytics.setCollectionEnabled failed") }
    }
}
