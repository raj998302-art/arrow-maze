package com.zenox.arrowmaze.core.designsystem.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Custom Arrow Maze icon set.
 *
 * Built directly with [ImageVector.Builder] so the app doesn't depend on
 * the auto-mirroring behaviour of `Icons.Filled.*` (our arrows are
 * explicitly directional — they MUST NOT flip in RTL locales).
 *
 * All icons use a 24×24 viewport, `Color.Black` default fill (consumers
 * tint via `LocalContentColor` / `tint` parameter).
 *
 * Usage: `Icon(imageVector = ArrowMazeIcons.Coin, ...)`.
 */
object ArrowMazeIcons {

    private const val DefaultStrokeLineWidth = 1f

    /** Thin wrapper that gives every icon consistent metadata. */
    private fun iconBuilder(
        name: String,
        block: Builder.() -> Unit,
    ): ImageVector = Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply(block).build()

    val ArrowUp: ImageVector by lazy {
    iconBuilder("ArrowUp") {
        path(
            fill = SolidColor(Color.Black),
            stroke = null,
            strokeLineWidth = DefaultStrokeLineWidth,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(12f, 4f)
            lineTo(21f, 12f)
            lineTo(16f, 12f)
            lineTo(16f, 20f)
            lineTo(8f, 20f)
            lineTo(8f, 12f)
            lineTo(3f, 12f)
            close()
        }
    }
}

val ArrowDown: ImageVector by lazy {
    iconBuilder("ArrowDown") {
        path(
            fill = SolidColor(Color.Black),
            stroke = null,
            strokeLineWidth = DefaultStrokeLineWidth,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(12f, 20f)
            lineTo(3f, 12f)
            lineTo(8f, 12f)
            lineTo(8f, 4f)
            lineTo(16f, 4f)
            lineTo(16f, 12f)
            lineTo(21f, 12f)
            close()
        }
    }
}

val ArrowLeft: ImageVector by lazy {
    iconBuilder("ArrowLeft") {
        path(
            fill = SolidColor(Color.Black),
            stroke = null,
            strokeLineWidth = DefaultStrokeLineWidth,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(4f, 12f)
            lineTo(12f, 3f)
            lineTo(12f, 8f)
            lineTo(20f, 8f)
            lineTo(20f, 16f)
            lineTo(12f, 16f)
            lineTo(12f, 21f)
            close()
        }
    }
}

val ArrowRight: ImageVector by lazy {
    iconBuilder("ArrowRight") {
        path(
            fill = SolidColor(Color.Black),
            stroke = null,
            strokeLineWidth = DefaultStrokeLineWidth,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(20f, 12f)
            lineTo(12f, 21f)
            lineTo(12f, 16f)
            lineTo(4f, 16f)
            lineTo(4f, 8f)
            lineTo(12f, 8f)
            lineTo(12f, 3f)
            close()
        }
    }
}

/**
 * Coin: a filled disc with a 4-point star cut-out (EvenOdd) so it reads
 * as a coin face from any tint colour.
 */
val Coin: ImageVector by lazy {
    iconBuilder("Coin") {
        path(
            fill = SolidColor(Color.Black),
            stroke = null,
            strokeLineWidth = DefaultStrokeLineWidth,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            pathFillType = PathFillType.EvenOdd,
        ) {
            // outer disc (r=10, centre 12,12) drawn with 4 cubic arcs
            moveTo(12f, 2f)
            curveTo(17.523f, 2f, 22f, 6.477f, 22f, 12f)
            curveTo(22f, 17.523f, 17.523f, 22f, 12f, 22f)
            curveTo(6.477f, 22f, 2f, 17.523f, 2f, 12f)
            curveTo(2f, 6.477f, 6.477f, 2f, 12f, 2f)
            close()
            // 4-point star cut-out (sparkle) centred at 12,12
            moveTo(12f, 6.5f)
            curveTo(12f, 9.2f, 9.2f, 12f, 6.5f, 12f)
            curveTo(9.2f, 12f, 12f, 14.8f, 12f, 17.5f)
            curveTo(12f, 14.8f, 14.8f, 12f, 17.5f, 12f)
            curveTo(14.8f, 12f, 12f, 9.2f, 12f, 6.5f)
            close()
        }
    }
}

/** Hint: classic lightbulb silhouette. */
val Hint: ImageVector by lazy {
    iconBuilder("Hint") {
        path(
            fill = SolidColor(Color.Black),
            stroke = null,
            strokeLineWidth = DefaultStrokeLineWidth,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(9f, 21f)
            curveTo(9f, 21.55f, 9.45f, 22f, 10f, 22f)
            lineTo(14f, 22f)
            curveTo(14.55f, 22f, 15f, 21.55f, 15f, 21f)
            lineTo(15f, 20f)
            lineTo(9f, 20f)
            lineTo(9f, 21f)
            close()
            moveTo(12f, 2f)
            curveTo(8.14f, 2f, 5f, 5.14f, 5f, 9f)
            curveTo(5f, 11.38f, 6.19f, 13.47f, 8f, 14.74f)
            lineTo(8f, 17f)
            curveTo(8f, 17.55f, 8.45f, 18f, 9f, 18f)
            lineTo(15f, 18f)
            curveTo(15.55f, 18f, 16f, 17.55f, 16f, 17f)
            lineTo(16f, 14.74f)
            curveTo(17.81f, 13.47f, 19f, 11.38f, 19f, 9f)
            curveTo(19f, 5.14f, 15.86f, 2f, 12f, 2f)
            close()
        }
    }
}

/** Life: filled heart silhouette. */
val Life: ImageVector by lazy {
    iconBuilder("Life") {
        path(
            fill = SolidColor(Color.Black),
            stroke = null,
            strokeLineWidth = DefaultStrokeLineWidth,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(12f, 21.35f)
            lineTo(10.55f, 20.03f)
            curveTo(5.4f, 15.36f, 2f, 12.28f, 2f, 8.5f)
            curveTo(2f, 5.42f, 4.42f, 3f, 7.5f, 3f)
            curveTo(9.24f, 3f, 10.91f, 3.81f, 12f, 5.09f)
            curveTo(13.09f, 3.81f, 14.76f, 3f, 16.5f, 3f)
            curveTo(19.58f, 3f, 22f, 5.42f, 22f, 8.5f)
            curveTo(22f, 12.28f, 18.6f, 15.36f, 13.45f, 20.03f)
            lineTo(12f, 21.35f)
            close()
        }
    }
}

/** Trophy: cup + handles + base. */
val Trophy: ImageVector by lazy {
    iconBuilder("Trophy") {
        path(
            fill = SolidColor(Color.Black),
            stroke = null,
            strokeLineWidth = DefaultStrokeLineWidth,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(19f, 5f)
            horizontalLineTo(17f)
            verticalLineTo(3f)
            horizontalLineTo(7f)
            verticalLineTo(5f)
            horizontalLineTo(5f)
            curveTo(3.9f, 5f, 3f, 5.9f, 3f, 7f)
            verticalLineTo(8f)
            curveTo(3f, 10.55f, 4.92f, 12.63f, 7.39f, 12.94f)
            curveTo(8.02f, 14.44f, 9.37f, 15.57f, 11f, 15.9f)
            verticalLineTo(19f)
            horizontalLineTo(7f)
            verticalLineTo(21f)
            horizontalLineTo(17f)
            verticalLineTo(19f)
            horizontalLineTo(13f)
            verticalLineTo(15.9f)
            curveTo(14.63f, 15.57f, 15.98f, 14.44f, 16.61f, 12.94f)
            curveTo(19.08f, 12.63f, 21f, 10.55f, 21f, 8f)
            verticalLineTo(7f)
            curveTo(21f, 5.9f, 20.1f, 5f, 19f, 5f)
            close()
            moveTo(5f, 8f)
            verticalLineTo(7f)
            horizontalLineTo(7f)
            verticalLineTo(10.82f)
            curveTo(5.84f, 10.4f, 5f, 9.3f, 5f, 8f)
            close()
            moveTo(19f, 8f)
            curveTo(19f, 9.3f, 18.16f, 10.4f, 17f, 10.82f)
            verticalLineTo(7f)
            horizontalLineTo(19f)
            verticalLineTo(8f)
            close()
        }
    }
}

/**
 * Target: 3 concentric rings + centre dot. Drawn with EvenOdd so a
 * single path produces alternating filled/empty bands.
 */
val Target: ImageVector by lazy {
    iconBuilder("Target") {
        // outer ring (r=10 outer, r=7.5 inner)
        path(
            fill = SolidColor(Color.Black),
            stroke = null,
            strokeLineWidth = DefaultStrokeLineWidth,
            pathFillType = PathFillType.EvenOdd,
        ) {
            // outer circle r=10
            moveTo(12f, 2f)
            curveTo(17.523f, 2f, 22f, 6.477f, 22f, 12f)
            curveTo(22f, 17.523f, 17.523f, 22f, 12f, 22f)
            curveTo(6.477f, 22f, 2f, 17.523f, 2f, 12f)
            curveTo(2f, 6.477f, 6.477f, 2f, 12f, 2f)
            close()
            // inner hole r=7.5
            moveTo(12f, 4.5f)
            curveTo(16.142f, 4.5f, 19.5f, 7.858f, 19.5f, 12f)
            curveTo(19.5f, 16.142f, 16.142f, 19.5f, 12f, 19.5f)
            curveTo(7.858f, 19.5f, 4.5f, 16.142f, 4.5f, 12f)
            curveTo(4.5f, 7.858f, 7.858f, 4.5f, 12f, 4.5f)
            close()
        }
        // middle ring (r=5 outer, r=2.5 inner)
        path(
            fill = SolidColor(Color.Black),
            stroke = null,
            strokeLineWidth = DefaultStrokeLineWidth,
            pathFillType = PathFillType.EvenOdd,
        ) {
            moveTo(12f, 7f)
            curveTo(14.761f, 7f, 17f, 9.239f, 17f, 12f)
            curveTo(17f, 14.761f, 14.761f, 17f, 12f, 17f)
            curveTo(9.239f, 17f, 7f, 14.761f, 7f, 12f)
            curveTo(7f, 9.239f, 9.239f, 7f, 12f, 7f)
            close()
            moveTo(12f, 9.5f)
            curveTo(10.619f, 9.5f, 9.5f, 10.619f, 9.5f, 12f)
            curveTo(9.5f, 13.381f, 10.619f, 14.5f, 12f, 14.5f)
            curveTo(13.381f, 14.5f, 14.5f, 13.381f, 14.5f, 12f)
            curveTo(14.5f, 10.619f, 13.381f, 9.5f, 12f, 9.5f)
            close()
        }
        // centre dot (r=1.5)
        path(
            fill = SolidColor(Color.Black),
            stroke = null,
            strokeLineWidth = DefaultStrokeLineWidth,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(12f, 10.5f)
            curveTo(12.828f, 10.5f, 13.5f, 11.172f, 13.5f, 12f)
            curveTo(13.5f, 12.828f, 12.828f, 13.5f, 12f, 13.5f)
            curveTo(11.172f, 13.5f, 10.5f, 12.828f, 10.5f, 12f)
            curveTo(10.5f, 11.172f, 11.172f, 10.5f, 12f, 10.5f)
            close()
        }
    }
}

/** Sparkle: 4-point concave star used for reward / celebration accents. */
val Sparkle: ImageVector by lazy {
    iconBuilder("Sparkle") {
        path(
            fill = SolidColor(Color.Black),
            stroke = null,
            strokeLineWidth = DefaultStrokeLineWidth,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(12f, 2f)
            curveTo(12f, 7f, 17f, 12f, 22f, 12f)
            curveTo(17f, 12f, 12f, 17f, 12f, 22f)
            curveTo(12f, 17f, 7f, 12f, 2f, 12f)
            curveTo(7f, 12f, 12f, 7f, 12f, 2f)
            close()
        }
        // tiny companion sparkle
        path(
            fill = SolidColor(Color.Black),
            stroke = null,
            strokeLineWidth = DefaultStrokeLineWidth,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(19f, 3f)
            curveTo(19f, 4.5f, 20.5f, 6f, 22f, 6f)
            curveTo(20.5f, 6f, 19f, 7.5f, 19f, 9f)
            curveTo(19f, 7.5f, 17.5f, 6f, 16f, 6f)
            curveTo(17.5f, 6f, 19f, 4.5f, 19f, 3f)
            close()
        }
    }
}

/** Lock: padlock body + shackle. */
val Lock: ImageVector by lazy {
    iconBuilder("Lock") {
        path(
            fill = SolidColor(Color.Black),
            stroke = null,
            strokeLineWidth = DefaultStrokeLineWidth,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            pathFillType = PathFillType.NonZero,
        ) {
            // body + shackle as one fill
            moveTo(18f, 8f)
            horizontalLineTo(17f)
            verticalLineTo(6f)
            curveTo(17f, 3.24f, 14.76f, 1f, 12f, 1f)
            curveTo(9.24f, 1f, 7f, 3.24f, 7f, 6f)
            verticalLineTo(8f)
            horizontalLineTo(6f)
            curveTo(4.9f, 8f, 4f, 8.9f, 4f, 10f)
            verticalLineTo(20f)
            curveTo(4f, 21.1f, 4.9f, 22f, 6f, 22f)
            horizontalLineTo(18f)
            curveTo(19.1f, 22f, 20f, 21.1f, 20f, 20f)
            verticalLineTo(10f)
            curveTo(20f, 8.9f, 19.1f, 8f, 18f, 8f)
            close()
            moveTo(9f, 6f)
            curveTo(9f, 4.34f, 10.34f, 3f, 12f, 3f)
            curveTo(13.66f, 3f, 15f, 4.34f, 15f, 6f)
            verticalLineTo(8f)
            horizontalLineTo(9f)
            close()
            // keyhole (cut-out via EvenOdd would change fill rule; instead
            // draw a subtle hole as a separate path with the background
            // colour — kept as one fill so the icon stays single-colour).
        }
    }
}
}
