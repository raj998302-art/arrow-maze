package com.zenox.arrowmaze.core.ads

import android.app.Activity
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.zenox.arrowmaze.BuildConfig
import com.zenox.arrowmaze.core.common.AppError
import com.zenox.arrowmaze.core.common.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Wraps the User Messaging Platform (UMP) consent SDK so the rest of the app
 * doesn't have to deal with its listener-based API.
 *
 * Flow:
 * 1. On app first launch (before any ad request) call [requestConsent].
 *    This calls `ConsentInformation.requestConsentInfoUpdate` and then
 *    `UserMessagingPlatform.loadAndShowConsentFormIfRequired`.
 * 2. After [requestConsent] returns, call [canRequestAds] to gate ad
 *    initialisation. The SDK says "you can request ads at any point after
 *    `requestConsentInfoUpdate` returns, even if the user hasn't yet
 *    consented" — [canRequestAds] returns `true` once the SDK reports that
 *    either (a) consent was obtained or (b) consent is not required.
 * 3. If [isPrivacyOptionsRequired] returns `true`, surface a "Privacy
 *    Options" entry in the settings screen that calls [showPrivacyOptions]
 *    so the user can change their consent decision.
 *
 * In debug builds, [ConsentDebugSettings] forces EEA geography so the consent
 * form is always shown (otherwise the form is skipped on devices outside the
 * EEA + UK + Switzerland region).
 */
@Singleton
class ConsentManager @Inject constructor(
    @ApplicationContext private val context: android.content.Context,
) {

    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(context)

    /**
     * Requests the latest consent info from the UMP backend and (if required)
     * loads + shows the consent form on [activity].
     *
     * Returns [Result.Success] with `true` if the consent flow completed and
     * ads can be requested, `false` if the form failed to load/show but the
     * SDK still permits ad requests. Returns [Result.Failure] on hard errors
     * (network / form unavailable).
     */
    suspend fun requestConsent(activity: Activity): Result<Boolean> =
        suspendCancellableCoroutine { cont ->

            val paramsBuilder = ConsentRequestParameters.Builder()
                .setTagForUnderAgeOfConsent(false)

            if (BuildConfig.USE_DEBUG_ADS) {
                // Force the EEA debug geography so the consent form always
                // appears in debug builds, even on devices outside the EEA.
                val debug = ConsentDebugSettings.Builder(context)
                    .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                    // Add hashed device IDs here if you want to debug on
                    // physical devices without resetting advertising ID.
                    .build()
                paramsBuilder.setConsentDebugSettings(debug)
            }

            val params = paramsBuilder.build()

            consentInformation.requestConsentInfoUpdate(
                activity,
                params,
                {
                    // requestConsentInfoUpdate success → load + show form if required.
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                        if (formError != null) {
                            Timber.w("Consent form error: ${formError.errorCode} ${formError.message}")
                            // Even on form-error, the SDK still permits ad requests if
                            // the prior consent state was already obtained or not required.
                            if (cont.isActive) {
                                cont.resume(Result.Success(canRequestAds()))
                            }
                        } else {
                            Timber.i("Consent form shown + dismissed. canRequestAds=${canRequestAds()}")
                            if (cont.isActive) {
                                cont.resume(Result.Success(canRequestAds()))
                            }
                        }
                    }
                },
                { requestError ->
                    Timber.w("Consent info update failed: ${requestError.errorCode} ${requestError.message}")
                    val err = AppError.Ads(
                        "Consent info update failed: ${requestError.errorCode} ${requestError.message}"
                    )
                    if (cont.isActive) {
                        cont.resume(Result.Failure(err))
                    }
                },
            )
        }

    /** Returns `true` if AdMob can be initialised + ad requests can be made. */
    fun canRequestAds(): Boolean = consentInformation.canRequestAds()

    /** Returns `true` if the app should show a "Privacy Options" entry point. */
    fun isPrivacyOptionsRequired(): Boolean =
        consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    /** Shows the privacy-options form (re-opens the consent sheet). */
    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            if (formError != null) {
                Timber.w("Privacy options form error: ${formError.errorCode} ${formError.message}")
            } else {
                Timber.i("Privacy options form dismissed.")
            }
        }
    }

    /** Resets all consent state — for debugging / "reset ads consent" settings entry. */
    fun reset() {
        consentInformation.reset()
        Timber.i("Consent state reset.")
    }
}
