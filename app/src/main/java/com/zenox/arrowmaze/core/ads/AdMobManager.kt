package com.zenox.arrowmaze.core.ads

import android.app.Activity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAd.OnNativeAdLoadedListener
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.zenox.arrowmaze.BuildConfig
import com.zenox.arrowmaze.core.common.AppError
import com.zenox.arrowmaze.core.common.Result
import com.zenox.arrowmaze.core.data.repository.SessionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Central facade over the five AdMob ad types used by Arrow Maze:
 *
 *  - Banner (`AdView`) — shown at the bottom of menu screens
 *  - Interstitial — full-screen ad shown between levels (with a per-user cooldown)
 *  - Rewarded — full-screen ad shown for opt-in rewards (e.g. double coins)
 *  - App Open — full-screen ad shown when the app is foregrounded (managed by [AppOpenAdManager])
 *  - Native — in-feed ad rendered via [com.zenox.arrowmaze.core.ads.components.NativeAdView]
 *
 * Every load is wrapped in [suspendCancellableCoroutine] over the SDK's
 * listener-based callbacks. Every show is wrapped in a separate coroutine
 * that completes when the full-screen ad is dismissed (so callers can `await`
 * the user dismissing the ad before continuing).
 *
 * The interstitial path respects a per-user cooldown surfaced via
 * [SessionRepository.lastInterstitialEpochMsFlow]. The cooldown length is
 * pulled from Remote Config key [AppConstants.RC_INTERSTITIAL_COOLDOWN_SECONDS]
 * (default 60s) — the actual Remote Config lookup is left to the caller so
 * this class doesn't need to inject [RemoteConfigManager]; we read the value
 * from [AppConstants] as a static default (60s) which the orchestrator can
 * override by passing a custom cooldown to [showInterstitial].
 */
@Singleton
class AdMobManager @Inject constructor(
    @ApplicationContext private val context: android.content.Context,
    private val adMobInitializer: AdMobInitializer,
    private val sessionRepo: SessionRepository,
) {

    /** Currently loaded interstitial — `null` until [loadInterstitial] succeeds. */
    @Volatile private var interstitialAd: InterstitialAd? = null

    /** Currently loaded rewarded ad — `null` until [loadRewarded] succeeds. */
    @Volatile private var rewardedAd: RewardedAd? = null

    /** Currently loaded app-open ad — `null` until [loadAppOpen] succeeds. */
    @Volatile private var appOpenAd: AppOpenAd? = null

    /** Background scope for DataStore writes kicked off from main-thread callbacks. */
    private val bgScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Builds a fresh [AdView] configured with the banner ad unit id from
     * [BuildConfig.ADMOB_BANNER]. Caller is responsible for adding the view
     * to a Compose tree (via `AndroidView`) and calling `loadAd()` on it.
     */
    fun createBannerAdView(): AdView {
        ensureInitialized()
        return AdView(context).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = BuildConfig.ADMOB_BANNER
        }
    }

    /**
     * Pre-loads an interstitial ad. Returns `null` on failure (the caller can
     * retry later). Safe to call multiple times — only one ad is cached at a
     * time.
     */
    suspend fun loadInterstitial(): Result<InterstitialAd?> {
        ensureInitialized()

        interstitialAd?.let { return Result.Success(it) }

        return suspendCancellableCoroutine { cont ->
            InterstitialAd.load(
                context,
                BuildConfig.ADMOB_INTERSTITIAL,
                AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Timber.w("Interstitial load failed: code=${error.code} msg=${error.message}")
                        if (cont.isActive) {
                            cont.resume(
                                Result.Failure(
                                    AppError.Ads("Interstitial load failed: ${error.code} ${error.message}")
                                )
                            )
                        }
                    }

                    override fun onAdLoaded(ad: InterstitialAd) {
                        Timber.i("Interstitial loaded.")
                        interstitialAd = ad
                        if (cont.isActive) cont.resume(Result.Success(ad))
                    }
                },
            )
        }
    }

    /**
     * Shows the loaded interstitial on [activity]. Respects the per-user
     * cooldown: if the last show was less than [AppConstants.RC_INTERSTITIAL_COOLDOWN_SECONDS]
     * seconds ago (default 60s), returns [Result.Failure] with [AppError.Ads]
     * ("cooldown"). The orchestrator can override the cooldown by setting a
     * different value in Remote Config — but this class reads the *static*
     * default of 60s; pass [cooldownSeconds] to override per-call.
     *
     * Records the show time via [SessionRepository.setLastInterstitialEpochMs]
     * once the ad is dismissed.
     */
    suspend fun showInterstitial(
        activity: Activity,
        cooldownSeconds: Long = DEFAULT_INTERSTITIAL_COOLDOWN_SECONDS,
    ): Result<Unit> {
        ensureInitialized()

        val lastShow = sessionRepo.lastInterstitialEpochMsFlow.first()
        val now = System.currentTimeMillis()
        val cooldownMs = cooldownSeconds * 1000L
        if (now - lastShow < cooldownMs) {
            val remaining = (cooldownMs - (now - lastShow)) / 1000L
            Timber.d("Interstitial in cooldown — ${remaining}s remaining.")
            return Result.Failure(AppError.Ads("Interstitial in cooldown (${remaining}s remaining)"))
        }

        val ad = interstitialAd
            ?: when (val load = loadInterstitial()) {
                is Result.Success -> load.data
                is Result.Failure -> return load as Result<Unit>
                Result.Loading -> return Result.Loading as Result<Unit>
            }
            ?: return Result.Failure(AppError.Ads("Interstitial ad unavailable after load"))

        return suspendCancellableCoroutine { cont ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Timber.i("Interstitial dismissed.")
                    interstitialAd = null
                    bgScope.launch {
                        try {
                            sessionRepo.setLastInterstitialEpochMs(System.currentTimeMillis())
                        } catch (t: Throwable) {
                            Timber.w(t, "Failed to record interstitial show time")
                        }
                        if (cont.isActive) cont.resume(Result.Success(Unit))
                    }
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    Timber.w("Interstitial show failed: code=${error.code} msg=${error.message}")
                    interstitialAd = null
                    if (cont.isActive) {
                        cont.resume(
                            Result.Failure(
                                AppError.Ads("Interstitial show failed: ${error.code} ${error.message}")
                            )
                        )
                    }
                }

                override fun onAdShowedFullScreenContent() {
                    Timber.d("Interstitial showed.")
                }
            }
            ad.show(activity)
        }
    }

    /** Pre-loads a rewarded ad. Returns `null` on failure. */
    suspend fun loadRewarded(): Result<RewardedAd?> {
        ensureInitialized()

        rewardedAd?.let { return Result.Success(it) }

        return suspendCancellableCoroutine { cont ->
            RewardedAd.load(
                context,
                BuildConfig.ADMOB_REWARDED,
                AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Timber.w("Rewarded load failed: code=${error.code} msg=${error.message}")
                        if (cont.isActive) {
                            cont.resume(
                                Result.Failure(
                                    AppError.Ads("Rewarded load failed: ${error.code} ${error.message}")
                                )
                            )
                        }
                    }

                    override fun onAdLoaded(ad: RewardedAd) {
                        Timber.i("Rewarded loaded.")
                        rewardedAd = ad
                        if (cont.isActive) cont.resume(Result.Success(ad))
                    }
                },
            )
        }
    }

    /**
     * Shows the loaded rewarded ad on [activity]. Invokes [onReward] when the
     * user earns the reward (i.e. completes watching). Resumes with
     * [Result.Success] once the ad is dismissed, regardless of whether the
     * reward was earned.
     */
    suspend fun showRewarded(activity: Activity, onReward: () -> Unit): Result<Unit> {
        ensureInitialized()

        val ad = rewardedAd
            ?: when (val load = loadRewarded()) {
                is Result.Success -> load.data
                is Result.Failure -> return load as Result<Unit>
                Result.Loading -> return Result.Loading as Result<Unit>
            }
            ?: return Result.Failure(AppError.Ads("Rewarded ad unavailable after load"))

        return suspendCancellableCoroutine { cont ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Timber.i("Rewarded dismissed.")
                    rewardedAd = null
                    if (cont.isActive) cont.resume(Result.Success(Unit))
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    Timber.w("Rewarded show failed: code=${error.code} msg=${error.message}")
                    rewardedAd = null
                    if (cont.isActive) {
                        cont.resume(
                            Result.Failure(
                                AppError.Ads("Rewarded show failed: ${error.code} ${error.message}")
                            )
                        )
                    }
                }

                override fun onAdShowedFullScreenContent() {
                    Timber.d("Rewarded showed.")
                }
            }
            ad.show(activity) { rewardItem ->
                Timber.i("Reward earned: ${rewardItem.amount} ${rewardItem.type}")
                onReward()
            }
        }
    }

    /** Pre-loads an app-open ad. Returns `null` on failure. */
    suspend fun loadAppOpen(): Result<AppOpenAd?> {
        ensureInitialized()

        appOpenAd?.let { return Result.Success(it) }

        return suspendCancellableCoroutine { cont ->
            AppOpenAd.load(
                context,
                BuildConfig.ADMOB_APP_OPEN,
                AdRequest.Builder().build(),
                AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
                object : AppOpenAd.AppOpenAdLoadCallback() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Timber.w("AppOpen load failed: code=${error.code} msg=${error.message}")
                        if (cont.isActive) {
                            cont.resume(
                                Result.Failure(
                                    AppError.Ads("AppOpen load failed: ${error.code} ${error.message}")
                                )
                            )
                        }
                    }

                    override fun onAdLoaded(ad: AppOpenAd) {
                        Timber.i("AppOpen loaded.")
                        appOpenAd = ad
                        if (cont.isActive) cont.resume(Result.Success(ad))
                    }
                },
            )
        }
    }

    /**
     * Shows the loaded app-open ad on [activity]. Does *not* enforce a cooldown
     * — the orchestrator ([AppOpenAdManager]) decides whether to show app-open
     * ads based on the 4-hour cooldown + foreground-during-gameplay guard.
     */
    suspend fun showAppOpen(activity: Activity): Result<Unit> {
        ensureInitialized()

        val ad = appOpenAd
            ?: when (val load = loadAppOpen()) {
                is Result.Success -> load.data
                is Result.Failure -> return load as Result<Unit>
                Result.Loading -> return Result.Loading as Result<Unit>
            }
            ?: return Result.Failure(AppError.Ads("AppOpen ad unavailable after load"))

        return suspendCancellableCoroutine { cont ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Timber.i("AppOpen dismissed.")
                    appOpenAd = null
                    if (cont.isActive) cont.resume(Result.Success(Unit))
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    Timber.w("AppOpen show failed: code=${error.code} msg=${error.message}")
                    appOpenAd = null
                    if (cont.isActive) {
                        cont.resume(
                            Result.Failure(
                                AppError.Ads("AppOpen show failed: ${error.code} ${error.message}")
                            )
                        )
                    }
                }

                override fun onAdShowedFullScreenContent() {
                    Timber.d("AppOpen showed.")
                }
            }
            ad.show(activity)
        }
    }

    /**
     * Builds a fresh [AdLoader] configured for native ads. The caller is
     * responsible for invoking `loadAd()` on it and forwarding the loaded
     * [NativeAd] to [com.zenox.arrowmaze.core.ads.components.NativeAdView]
     * for rendering.
     */
    fun createNativeAdLoader(
        onLoaded: (NativeAd) -> Unit,
        onFailed: (LoadAdError) -> Unit = { Timber.w("Native load failed: ${it.code} ${it.message}") },
    ): AdLoader {
        ensureInitialized()
        return AdLoader.Builder(context, BuildConfig.ADMOB_NATIVE)
            .forNativeAd(OnNativeAdLoadedListener { ad ->
                Timber.i("Native ad loaded.")
                onLoaded(ad)
            })
            .withNativeAdOptions(NativeAdOptions.Builder().build())
            .withAdListener(object : com.google.android.gms.ads.AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) = onFailed(error)
            })
            .build()
    }

    /** Drops all cached ad references — called by [AppOpenAdManager] on cold-start resets. */
    fun clearCache() {
        interstitialAd = null
        rewardedAd = null
        appOpenAd = null
    }

    private fun ensureInitialized() {
        if (!adMobInitializer.isInitialized.value) {
            // The SDK accepts ad requests before initialisation completes — they'll
            // be queued and dispatched once the SDK is ready. We still call
            // initialize() defensively so the queue starts draining.
            adMobInitializer.initialize()
        }
    }

    companion object {
        /**
         * Static default interstitial cooldown — mirrors the Remote Config
         * default of 60s so the manager doesn't need to inject
         * [com.zenox.arrowmaze.core.firebase.config.RemoteConfigManager]
         * directly. The orchestrator can pass a custom [cooldownSeconds]
         * to [showInterstitial] if Remote Config overrides this.
         */
        const val DEFAULT_INTERSTITIAL_COOLDOWN_SECONDS: Long = 60L
    }
}
