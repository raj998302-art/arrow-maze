package com.zenox.arrowmaze.core.ads.components

import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import timber.log.Timber

/**
 * Renders a loaded [NativeAd] inside a Compose tree.
 *
 * Wraps a `NativeAdView` (the legacy View container the AdMob SDK requires
 * for native ads) inside an `AndroidView`. The container's child views
 * (headline / body / icon / CTA) are inflated from a simple inline layout
 * — built programmatically rather than from an XML resource so we don't have
 * to ship a separate layout file.
 *
 * The [NativeAd] is *not* destroyed on dispose — the caller (typically a
 * ViewModel) is responsible for `nativeAd.destroy()` when the ad is scrolled
 * off-screen or the screen is destroyed. This matches AdMob's recommended
 * lifecycle (the NativeAdView should not own the NativeAd — the data source
 * does).
 */
@Composable
fun NativeAdView(
    ad: NativeAd,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(8.dp),
            factory = { ctx ->
                Timber.d("NativeAdView: factory creating container.")
                val container = android.widget.LinearLayout(ctx).apply {
                    orientation = android.widget.LinearLayout.HORIZONTAL
                    setPadding(8, 8, 8, 8)
                }
                val iconView = ImageView(ctx).apply {
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        96.dpToPx(ctx),
                        96.dpToPx(ctx),
                    ).apply { setMargins(0, 0, 16.dpToPx(ctx), 0) }
                }
                val textColumn = android.widget.LinearLayout(ctx).apply {
                    orientation = android.widget.LinearLayout.VERTICAL
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        0,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f,
                    )
                }
                val headlineView = TextView(ctx).apply {
                    textSize = 16f
                    setTextColor(android.graphics.Color.BLACK)
                }
                val bodyView = TextView(ctx).apply {
                    textSize = 13f
                    setTextColor(android.graphics.Color.DKGRAY)
                    maxLines = 2
                }
                val ctaView = Button(ctx).apply {
                    textSize = 12f
                }
                textColumn.addView(headlineView)
                textColumn.addView(bodyView)
                textColumn.addView(ctaView)
                container.addView(iconView)
                container.addView(textColumn)

                val nativeAdView = NativeAdView(ctx).apply {
                    addView(container)
                    this.headlineView = headlineView
                    this.bodyView = bodyView
                    this.iconView = iconView
                    this.callToActionView = ctaView
                }
                nativeAdView
            },
            update = { nativeAdView ->
                // Re-bind the ad to the view every time the ad changes.
                nativeAdView.headlineView?.let { (it as TextView).text = ad.headline }
                nativeAdView.bodyView?.let { (it as TextView).text = ad.body }
                nativeAdView.callToActionView?.let { (it as Button).text = ad.callToAction }
                nativeAdView.iconView?.let { iv ->
                    val drawable = ad.icon?.drawable
                    if (drawable != null) {
                        (iv as ImageView).setImageDrawable(drawable)
                        iv.visibility = View.VISIBLE
                    } else {
                        iv.visibility = View.GONE
                    }
                }
                nativeAdView.setNativeAd(ad)
            },
            onRelease = { /* Caller owns the NativeAd's lifecycle — no destroy here. */ },
        )
    }
}

/** Tiny helper to convert dp to px without pulling in `androidx.core.content.res.ResourcesCompat`. */
private fun Int.dpToPx(ctx: android.content.Context): Int =
    (this * ctx.resources.displayMetrics.density).toInt()
