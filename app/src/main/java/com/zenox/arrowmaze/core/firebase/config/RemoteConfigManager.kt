package com.zenox.arrowmaze.core.firebase.config

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.zenox.arrowmaze.core.common.AppConstants
import com.zenox.arrowmaze.core.common.AppError
import com.zenox.arrowmaze.core.common.Result
import com.zenox.arrowmaze.core.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central facade over [FirebaseRemoteConfig].
 *
 * - Sets sensible defaults (mirroring [AppConstants.RC_*] keys) on first use.
 * - Sets a 1-hour cache expiry via [FirebaseRemoteConfigSettings].
 * - Exposes [fetchAndActivate] (always fetches + activates) and
 *   [fetchAndActivateIfNeeded] (only fetches if the cache is older than
 *   the 1-hour minimum fetch interval).
 * - Exposes typed getters [getString] / [getLong] / [getBoolean] / [getDouble]
 *   that read from the activated config (no I/O).
 *
 * All fetch / activate operations run on the [IoDispatcher] — the underlying
 * Firebase Tasks use `kotlinx-coroutines-play-services`'s `await()` extension
 * so we can suspend on them directly.
 *
 * Defaults are intentionally inlined here (rather than in a separate XML
 * resource) so they live next to the keys in [AppConstants] and are easy to
 * audit. The orchestrator (Application.onCreate) calls [fetchAndActivateIfNeeded]
 * once at startup; UI layers then read via the typed getters without paying
 * any I/O cost.
 */
@Singleton
class RemoteConfigManager @Inject constructor(
    private val remoteConfig: FirebaseRemoteConfig,
    @IoDispatcher private val io: CoroutineDispatcher,
) {

    /** Whether [applyDefaults] + [applySettings] have been called. */
    @Volatile private var configured: Boolean = false

    /**
     * Fetches the latest config from the Firebase backend with a 60-second
     * server-side timeout and activates it. Always makes a network call —
     * use [fetchAndActivateIfNeeded] to skip when the cache is fresh.
     */
    suspend fun fetchAndActivate(): Result<Unit> = withContext(io) {
        ensureConfigured()
        try {
            Timber.i("RemoteConfig: fetchAndActivate start.")
            val activated = remoteConfig.fetchAndActivate().await()
            Timber.i("RemoteConfig: fetchAndActivate done. activatedNew=$activated")
            Result.Success(Unit)
        } catch (t: Throwable) {
            Timber.w(t, "RemoteConfig: fetchAndActivate failed.")
            Result.Failure(AppError.from(t))
        }
    }

    /**
     * Fetches + activates only if the cache is older than the 1-hour minimum
     * fetch interval. Use this on app cold-start to avoid hammering the
     * backend on every launch.
     */
    suspend fun fetchAndActivateIfNeeded(): Result<Unit> = withContext(io) {
        ensureConfigured()
        val info = remoteConfig.info
        val lastFetchMs = info.fetchTimeMillis
        val now = System.currentTimeMillis()
        val cacheAge = now - lastFetchMs
        if (lastFetchMs > 0 && cacheAge < MIN_FETCH_INTERVAL_MS) {
            Timber.d("RemoteConfig: cache fresh (${cacheAge / 1000}s old) — skipping fetch.")
            return@withContext Result.Success(Unit)
        }
        try {
            Timber.i("RemoteConfig: fetch needed (cache age=${cacheAge / 1000}s).")
            remoteConfig.fetch(FETCH_TIMEOUT_SECONDS).await()
            val activated = remoteConfig.activate().await()
            Timber.i("RemoteConfig: fetch+activate done. activatedNew=$activated")
            Result.Success(Unit)
        } catch (t: Throwable) {
            Timber.w(t, "RemoteConfig: fetch+activate failed.")
            Result.Failure(AppError.from(t))
        }
    }

    /** Returns the activated string value for [key] (or the default if unset). */
    fun getString(key: String): String = ensureConfiguredAndGetString(key)

    /** Returns the activated long value for [key] (or the default if unset). */
    fun getLong(key: String): Long = ensureConfiguredAndGetLong(key)

    /** Returns the activated boolean value for [key] (or the default if unset). */
    fun getBoolean(key: String): Boolean = ensureConfiguredAndGetBoolean(key)

    /** Returns the activated double value for [key] (or the default if unset). */
    fun getDouble(key: String): Double = ensureConfiguredAndGetDouble(key)

    // ---- Internals ----

    private fun ensureConfigured() {
        if (!configured) {
            synchronized(this) {
                if (!configured) {
                    applyDefaults()
                    applySettings()
                    configured = true
                }
            }
        }
    }

    private fun applyDefaults() {
        val defaults = mapOf<String, Any>(
            AppConstants.RC_MIN_APP_VERSION to DEFAULT_MIN_APP_VERSION,
            AppConstants.RC_DAILY_REWARD_COINS to DEFAULT_DAILY_REWARD_COINS,
            AppConstants.RC_DAILY_REWARD_XP to DEFAULT_DAILY_REWARD_XP,
            AppConstants.RC_INTERSTITIAL_COOLDOWN_SECONDS to DEFAULT_INTERSTITIAL_COOLDOWN_SECONDS,
            AppConstants.RC_FEATURE_FLAGS_JSON to DEFAULT_FEATURE_FLAGS_JSON,
        )
        try {
            // setDefaultsAsync returns a Task<Void> — block on it via await().
            remoteConfig.setDefaultsAsync(defaults)
            Timber.i("RemoteConfig defaults applied: $defaults")
        } catch (t: Throwable) {
            Timber.w(t, "RemoteConfig setDefaultsAsync failed — defaults will be unavailable if backend doesn't return them.")
        }
    }

    private fun applySettings() {
        try {
            val settings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(MIN_FETCH_INTERVAL_SECONDS)
                .setFetchTimeoutInSeconds(FETCH_TIMEOUT_SECONDS)
                .build()
            remoteConfig.setConfigSettingsAsync(settings)
            Timber.i("RemoteConfig settings applied (minFetch=${MIN_FETCH_INTERVAL_SECONDS}s, timeout=${FETCH_TIMEOUT_SECONDS}s).")
        } catch (t: Throwable) {
            Timber.w(t, "RemoteConfig setConfigSettingsAsync failed — using SDK defaults.")
        }
    }

    private fun ensureConfiguredAndGetString(key: String): String {
        ensureConfigured()
        return try {
            remoteConfig.getString(key)
        } catch (t: Throwable) {
            Timber.w(t, "RemoteConfig.getString($key) failed.")
            ""
        }
    }

    private fun ensureConfiguredAndGetLong(key: String): Long {
        ensureConfigured()
        return try {
            remoteConfig.getLong(key)
        } catch (t: Throwable) {
            Timber.w(t, "RemoteConfig.getLong($key) failed.")
            0L
        }
    }

    private fun ensureConfiguredAndGetBoolean(key: String): Boolean {
        ensureConfigured()
        return try {
            remoteConfig.getBoolean(key)
        } catch (t: Throwable) {
            Timber.w(t, "RemoteConfig.getBoolean($key) failed.")
            false
        }
    }

    private fun ensureConfiguredAndGetDouble(key: String): Double {
        ensureConfigured()
        return try {
            remoteConfig.getDouble(key)
        } catch (t: Throwable) {
            Timber.w(t, "RemoteConfig.getDouble($key) failed.")
            0.0
        }
    }

    companion object {
        /** Cache expiry (1 hour) — matches Google's recommended production value. */
        const val MIN_FETCH_INTERVAL_SECONDS: Long = 60 * 60L
        /** Server-side fetch timeout (60s). */
        const val FETCH_TIMEOUT_SECONDS: Long = 60L
        /** [MIN_FETCH_INTERVAL_SECONDS] in milliseconds, used by [fetchAndActivateIfNeeded]. */
        const val MIN_FETCH_INTERVAL_MS: Long = MIN_FETCH_INTERVAL_SECONDS * 1000L

        // ---- Default values (mirror the Remote Config backend defaults) ----
        const val DEFAULT_MIN_APP_VERSION: String = "1.0.0"
        const val DEFAULT_DAILY_REWARD_COINS: Long = 50L
        const val DEFAULT_DAILY_REWARD_XP: Long = 100L
        const val DEFAULT_INTERSTITIAL_COOLDOWN_SECONDS: Long = 60L
        const val DEFAULT_FEATURE_FLAGS_JSON: String = "{}"
    }
}
