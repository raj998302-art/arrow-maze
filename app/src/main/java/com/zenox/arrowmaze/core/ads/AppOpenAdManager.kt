package com.zenox.arrowmaze.core.ads

import android.app.Activity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.zenox.arrowmaze.core.common.AppError
import com.zenox.arrowmaze.core.common.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Drives the AppOpenAd lifecycle.
 *
 * The owning Activity (or [com.zenox.arrowmaze.MainActivity]) attaches this
 * manager as a [DefaultLifecycleObserver]; on `ON_START` we kick off
 * [onAppForegrounded], which loads (if needed) + shows an app-open ad if:
 *
 *  1. The 4-hour cooldown since the last show has elapsed.
 *  2. No other full-screen ad is currently showing ([isShowingAd] == `false`).
 *  3. The app is not in the middle of a game session ([isGameplayActive] == `false`).
 *
 * The orchestrator calls [setIsShowingAd] when other full-screen ads (interstitial
 * / rewarded) are visible, and [setIsGameplayActive] when the player is mid-game,
 * so we don't interrupt gameplay with an app-open splash.
 */
@Singleton
class AppOpenAdManager @Inject constructor(
    private val adMobManager: AdMobManager,
) {

    /** Cooldown between consecutive app-open ad shows (4 hours, per Google's guidance). */
    private val cooldownMs: Long = 4 * 60 * 60 * 1000L

    /** Epoch-ms of the last successful app-open ad show. */
    @Volatile private var lastShowEpochMs: Long = 0L

    /** True when any full-screen ad is currently on screen (interstitial / rewarded / app-open). */
    private val _isShowingAd = MutableStateFlow(false)
    val isShowingAd: StateFlow<Boolean> = _isShowingAd.asStateFlow()

    /** True when the player is in an active game session — app-open ads are suppressed. */
    private val _isGameplayActive = MutableStateFlow(false)
    val isGameplayActive: StateFlow<Boolean> = _isGameplayActive.asStateFlow()

    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /**
     * Called by the owning Activity's `LifecycleObserver` on `ON_START`.
     * Loads + shows an app-open ad if all preconditions hold.
     */
    fun onAppForegrounded(activity: Activity) {
        if (_isShowingAd.value) {
            Timber.d("AppOpen skipped — another ad is showing.")
            return
        }
        if (_isGameplayActive.value) {
            Timber.d("AppOpen skipped — gameplay active.")
            return
        }
        val now = System.currentTimeMillis()
        if (now - lastShowEpochMs < cooldownMs) {
            val remainingMs = cooldownMs - (now - lastShowEpochMs)
            Timber.d("AppOpen skipped — cooldown (${remainingMs / 1000}s remaining).")
            return
        }

        scope.launch {
            _isShowingAd.value = true
            try {
                val loadResult = adMobManager.loadAppOpen()
                val ad = when (loadResult) {
                    is Result.Success -> loadResult.data
                    is Result.Failure -> {
                        Timber.w(loadResult.error.message, "AppOpen load failed — skipping show.")
                        return@launch
                    }
                    Result.Loading -> return@launch
                } ?: return@launch

                val showResult = adMobManager.showAppOpen(activity)
                if (showResult is Result.Success) {
                    lastShowEpochMs = System.currentTimeMillis()
                    Timber.i("AppOpen shown.")
                } else if (showResult is Result.Failure) {
                    val err = showResult.error
                    Timber.w(err.message, "AppOpen show failed.")
                }
            } catch (t: Throwable) {
                Timber.w(t, "AppOpen foregrounded block threw")
            } finally {
                _isShowingAd.value = false
            }
        }
    }

    /**
     * Sets the "another ad is showing" guard. AdMobManager calls this when
     * an interstitial / rewarded / app-open ad becomes visible (true) and
     * when it is dismissed (false).
     */
    fun setIsShowingAd(value: Boolean) {
        _isShowingAd.value = value
    }

    /**
     * Sets the "player is mid-game" guard. The Game screen sets this to
     * `true` on `onStart` and `false` on `onStop` so we don't splash an
     * app-open ad over an active game session.
     */
    fun setIsGameplayActive(value: Boolean) {
        _isGameplayActive.value = value
    }

    /**
     * Convenience entry point for the owning Activity to attach this manager
     * as a [DefaultLifecycleObserver] without having to write the boilerplate
     * itself. The Activity just calls:
     *
     * ```
     * lifecycle.addObserver(appOpenAdManager.lifecycleObserver(this))
     * ```
     *
     * The returned observer delegates `ON_START` to [onAppForegrounded].
     */
    fun lifecycleObserver(activityProvider: () -> Activity): DefaultLifecycleObserver =
        object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                onAppForegrounded(activityProvider())
            }
        }
}
