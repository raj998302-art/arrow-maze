package com.zenox.arrowmaze.core.ads.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.zenox.arrowmaze.BuildConfig
import com.zenox.arrowmaze.core.ads.AdMobManager
import timber.log.Timber

/**
 * Banner ad composable.
 *
 * Wraps an [AdView] in an `AndroidView` so the legacy View-based ad unit can be
 * embedded inside a Compose tree. The [AdView] is created via [AdMobManager.createBannerAdView]
 * (which sets the ad unit id + a sensible default [AdSize]); the composable
 * issues `loadAd()` once on first composition and `destroy()`s the view on
 * dispose.
 *
 * The view's height is hard-coded to `50.dp` (the standard banner height) so
 * the parent layout reserves the right amount of space — AdMob's banner ad
 * is always 320×50 dp at the smallest width.
 *
 * Usage:
 * ```
 * BannerAd(modifier = Modifier.fillMaxWidth())
 * ```
 */
@Composable
fun BannerAd(
    adMobManager: AdMobManager,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val adView = remember {
        adMobManager.createBannerAdView().apply {
            // Re-set the size to the standard 320x50 banner to be safe —
            // createBannerAdView() already sets BANNER, but ensure the
            // layout params are sane for the AndroidView host.
            setAdSize(AdSize.BANNER)
            adUnitId = BuildConfig.ADMOB_BANNER
        }
    }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        factory = { ctx ->
            Timber.d("BannerAd: factory creating AdView.")
            adView
        },
        update = { view ->
            // The view was created with the right ad unit id + size; just
            // issue a load on every update if it's not already loaded.
            if (!view.isLoading) {
                view.loadAd(AdRequest.Builder().build())
            }
        },
        onRelease = { view ->
            Timber.d("BannerAd: destroying AdView.")
            view.destroy()
        },
    )
}
