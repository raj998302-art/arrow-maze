package com.zenox.arrowmaze.core.ads

import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.zenox.arrowmaze.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single point of initialisation for the Google Mobile Ads SDK.
 *
 * `MobileAds.initialize` is async (the SDK continues initialising on a
 * background thread after the call returns). The completion listener is
 * wrapped so callers can collect [isInitialized] before issuing ad requests.
 *
 * In debug builds ([BuildConfig.USE_DEBUG_ADS]) the [RequestConfiguration]
 * is set with the test device IDs (and hashed IDs of any devices you want
 * to whitelist). The Google-provided test ad unit IDs in [BuildConfig.ADMOB_*]
 * are used directly so you never accidentally serve real impressions during
 * development.
 *
 * UMP consent is *not* handled here — see [ConsentManager]. Call
 * `ConsentManager.requestConsent()` *before* [initialize] on first launch.
 */
@Singleton
class AdMobInitializer @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    /** Guards against double-initialisation (MobileAds allows it but it's wasteful). */
    @Volatile private var started: Boolean = false

    /**
     * Initialises the Mobile Ads SDK. Idempotent — calling it a second time
     * is a no-op (the SDK initialises only once per process).
     *
     * Sets the global [RequestConfiguration] with test device IDs in debug
     * builds (so test ad units never produce real impressions).
     */
    fun initialize() {
        if (started) {
            Timber.d("AdMob already initialised — skipping.")
            return
        }
        started = true

        if (BuildConfig.USE_DEBUG_ADS) {
            // In debug builds, set the current device as a test device so
            // real ad unit IDs (if a developer ever flips BuildConfig flags)
            // never produce billable impressions.
            // NOTE: RequestConfiguration.DEVICE_ID_EMULATOR was removed in
            // Play Services Ads 23.x — use the well-known emulator test
            // device ID literal instead.
            val testDeviceIds = listOf(
                "B3EEABB8EE11C2BE770B684D95219EC1",
                // Add hashed device IDs from logcat ("To get test ads on this
                // device, set ... as a test device ID") as needed.
            )
            val config = RequestConfiguration.Builder()
                .setTestDeviceIds(testDeviceIds)
                .build()
            MobileAds.setRequestConfiguration(config)
            Timber.i("AdMob debug RequestConfiguration applied (test devices=$testDeviceIds).")
        }

        MobileAds.initialize(context) { status ->
            val adapterStates = status?.adapterStatusMap
                ?.map { (name, s) -> "$name=${s.initializationState}" }
                ?.joinToString(", ")
                ?: "(no status)"
            Timber.i("AdMob initialised. adapters=$adapterStates")
            _isInitialized.value = true
        }
    }
}
