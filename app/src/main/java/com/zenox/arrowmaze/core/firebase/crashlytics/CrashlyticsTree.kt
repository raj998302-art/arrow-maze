package com.zenox.arrowmaze.core.firebase.crashlytics

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [Timber.Tree] that mirrors logcat output into Firebase Crashlytics so
 * crash reports in the console include the same breadcrumbs the developer
 * sees locally.
 *
 *  - `Log.INFO` and above are forwarded to [FirebaseCrashlytics.log] as
 *    short breadcrumbs (truncated to the 64 KB Crashlytics per-message cap
 *    to be safe).
 *  - `Log.ERROR` and `Log.ASSERT` are additionally recorded as non-fatal
 *    exceptions via [FirebaseCrashlytics.recordException] (wrapped in a
 *    synthetic [CrashlyticsLoggedException] so they don't lose their
 *    stacktrace).
 *
 * In debug builds this tree is NOT planted — `Timber.DebugTree` is used
 * instead so developers see full logcat output without polluting the
 * Crashlytics dashboard with dev crashes.
 *
 * The class is `@Singleton`-annotated so Hilt can inject it into the
 * [com.zenox.arrowmaze.ArrowMazeApplication] for planting on app start.
 */
@Singleton
class CrashlyticsTree @Inject constructor() : Timber.Tree() {

    private val crashlytics: FirebaseCrashlytics by lazy { FirebaseCrashlytics.getInstance() }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        // Forward every breadcrumb to Crashlytics so the next crash report
        // includes the same trail the developer sees in logcat.
        val formatted = if (tag.isNullOrBlank()) message else "[$tag] $message"
        runCatching { crashlytics.log(formatted.take(MAX_BREADCRUMB_LEN)) }

        // Errors are also recorded as non-fatal exceptions so they appear
        // in the Crashlytics "Issues" view. If the caller supplied a
        // throwable we record it verbatim; otherwise we synthesise one so
        // the breadcrumb + priority make it into the report.
        if (priority >= Log.ERROR) {
            val throwable = t ?: CrashlyticsLoggedException(formatted)
            runCatching { crashlytics.recordException(throwable) }
        }
    }

    override fun isLoggable(priority: Int): Boolean = priority >= Log.INFO

    /** Synthetic exception used when a log line is recorded without a throwable. */
    private class CrashlyticsLoggedException(message: String) : RuntimeException(message)

    private companion object {
        const val MAX_BREADCRUMB_LEN = 64_000
    }
}
