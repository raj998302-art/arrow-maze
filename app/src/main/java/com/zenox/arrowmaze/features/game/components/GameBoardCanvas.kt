package com.zenox.arrowmaze.features.game.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.zenox.arrowmaze.core.designsystem.theme.LocalGamePalette
import com.zenox.arrowmaze.core.designsystem.theme.LocalHighContrast
import com.zenox.arrowmaze.core.designsystem.tokens.MotionTokens
import com.zenox.arrowmaze.core.domain.model.ArrowDirection
import com.zenox.arrowmaze.core.domain.model.Board
import com.zenox.arrowmaze.core.domain.model.Cell
import com.zenox.arrowmaze.core.domain.model.Position
import kotlin.math.min

/**
 * Renders the Arrow Maze board on a Compose [Canvas] and forwards single-tap
 * gestures to [onCellTap] as discrete [Position]s.
 *
 * Layout
 * ------
 * The composable wraps a [BoxWithConstraints] so the board is sized as a
 * square based on `min(maxWidth, maxHeight)`, and the per-cell size is
 * `boardPx / board.size` (clamped to a minimum of 32.dp so 9×9 boards stay
 * tappable on small phones). The Canvas itself is a fixed-size square.
 *
 * Drawing
 * -------
 *  - `EmptyCell` — subtle rounded-rect background using [GamePalette.cellEmpty].
 *  - `ArrowCell` — rounded-rect background + a thick arrow shape (Path)
 *    rotated to `direction.angleDegrees`, filled with [GamePalette.arrowFill]
 *    and outlined with [GamePalette.arrowOutline]. The arrow at
 *    [lastRotatedCell] is animated from its previous orientation to the
 *    current one over [MotionTokens.DurationShort] using
 *    [MotionTokens.EmphasizedEasing] (gated by [animateRotation]).
 *  - `StartCell` — filled circle with a radial glow ([GamePalette.startGlow]).
 *  - `GoalCell` — filled star/target shape with a radial glow
 *    ([GamePalette.goalGlow]).
 *
 * The current solution path is drawn on top as a connected polyline with a
 * [Brush.linearGradient] from [GamePalette.trailStart] to
 * [GamePalette.trailEnd], thick rounded stroke, and an `Animatable` progress
 * that animates 0→1 every time the path size changes.
 *
 * Performance
 * -----------
 * The two reusable [Path] objects (arrow + glow) are `remember`ed so no
 * allocations happen inside the draw lambda. The animation `Animatable`s
 * live in composable state, not the draw lambda, so the draw stays
 * side-effect-free.
 *
 * Accessibility
 * -------------
 * The Canvas carries a content description (`"Arrow maze board, N by N"`)
 * and a `testTag("gameBoard")` for UI Automator tests. High-contrast mode
 * ([LocalHighContrast]) thickens arrow outlines and frame strokes.
 */
@Composable
fun GameBoardCanvas(
    board: Board,
    path: List<Position>,
    lastRotatedCell: Position?,
    onCellTap: (Position) -> Unit,
    modifier: Modifier = Modifier,
    animateRotation: Boolean = true,
) {
    val palette = LocalGamePalette.current
    val highContrast = LocalHighContrast.current
    val density = LocalDensity.current

    // Per-frame rotation angle of the [lastRotatedCell]. Non-rotating cells
    // read their angle directly from `cell.direction.angleDegrees`.
    val rotationAnimatable = remember { Animatable(0f) }
    LaunchedEffect(lastRotatedCell, board) {
        val target = lastRotatedCell ?: return@LaunchedEffect
        if (!animateRotation) {
            val cell = board.cellAtOrNull(target)
            if (cell is Cell.ArrowCell) {
                rotationAnimatable.snapTo(cell.direction.angleDegrees)
            }
            return@LaunchedEffect
        }
        val cell = board.cellAtOrNull(target)
        if (cell !is Cell.ArrowCell) return@LaunchedEffect
        val newAngle = cell.direction.angleDegrees
        val oldAngle = cell.direction.rotateCCW().angleDegrees
        rotationAnimatable.snapTo(oldAngle)
        rotationAnimatable.animateTo(
            targetValue = newAngle,
            animationSpec = tween(
                durationMillis = MotionTokens.DurationShort,
                easing = MotionTokens.EmphasizedEasing,
            ),
        )
    }

    // Path draw progress — animates 0→1 every time the path size changes,
    // producing a smooth "trail extends" effect.
    val pathProgressAnimatable = remember(path.size) { Animatable(0f) }
    LaunchedEffect(path.size) {
        pathProgressAnimatable.snapTo(0f)
        pathProgressAnimatable.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = MotionTokens.DurationShort,
                easing = MotionTokens.EmphasizedDecelerateEasing,
            ),
        )
    }

    // Reusable Path objects — never allocate inside the draw lambda.
    val arrowPath = remember { Path() }
    val glowPath = remember { Path() }
    val trailPath = remember { Path() }

    BoxWithConstraints(modifier = modifier) {
        val maxPx = min(constraints.maxWidth, constraints.maxHeight)
        val minCellPx = with(density) { 32.dp.toPx().toInt() }
        val rawCellPx = if (board.size > 0) maxPx / board.size else 0
        val cellPx = rawCellPx.coerceAtLeast(minCellPx).toFloat()
        val boardPx = cellPx * board.size

        Canvas(
            modifier = Modifier
                .size(with(density) { boardPx.toDp() })
                .pointerInput(board.size) {
                    detectTapGestures { offset ->
                        val col = (offset.x / cellPx).toInt()
                            .coerceIn(0, board.size - 1)
                        val row = (offset.y / cellPx).toInt()
                            .coerceIn(0, board.size - 1)
                        onCellTap(Position(row, col))
                    }
                }
                .semantics {
                    contentDescription = "Arrow maze board, ${board.size} by ${board.size}"
                }
                .testTag("gameBoard"),
        ) {
            // --- Background: every cell gets its own rounded-rect backdrop.
            for (row in 0 until board.size) {
                for (col in 0 until board.size) {
                    val pos = Position(row, col)
                    val cell = board.cellAt(pos)
                    val cx = (col + 0.5f) * cellPx
                    val cy = (row + 0.5f) * cellPx
                    drawCellBackground(cell, cx, cy, cellPx, palette, highContrast)
                }
            }

            // --- Arrows on top of backgrounds (so they read clearly).
            for (row in 0 until board.size) {
                for (col in 0 until board.size) {
                    val pos = Position(row, col)
                    val cell = board.cellAt(pos)
                    if (cell !is Cell.ArrowCell) continue
                    val cx = (col + 0.5f) * cellPx
                    val cy = (row + 0.5f) * cellPx
                    val angle = if (pos == lastRotatedCell) {
                        rotationAnimatable.value
                    } else {
                        cell.direction.angleDegrees
                    }
                    drawArrow(
                        arrowPath = arrowPath,
                        cx = cx,
                        cy = cy,
                        cellPx = cellPx,
                        angleDeg = angle,
                        fill = palette.arrowFill,
                        outline = palette.arrowOutline,
                        highContrast = highContrast,
                    )
                }
            }

            // --- Start + goal decorations on top of arrows (so the player
            // always sees the entry / exit points even when arrows overlap).
            drawStartGoal(board, cellPx, palette, glowPath)

            // --- Grid frame.
            drawGridFrame(board.size, cellPx, palette.boardFrame, highContrast)

            // --- Path trail on top of everything.
            drawTrail(
                trailPath = trailPath,
                path = path,
                cellPx = cellPx,
                palette = palette,
                progress = pathProgressAnimatable.value,
                highContrast = highContrast,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Draw helpers (private DrawScope extensions)
// ---------------------------------------------------------------------------

/** Draws the rounded-rect backdrop for one cell, including start/goal glows. */
private fun DrawScope.drawCellBackground(
    cell: Cell,
    cx: Float,
    cy: Float,
    cellPx: Float,
    palette: com.zenox.arrowmaze.core.designsystem.theme.GamePalette,
    highContrast: Boolean,
) {
    val inset = cellPx * 0.06f
    val cornerRadius = cellPx * 0.18f
    val bg = when (cell) {
        is Cell.ArrowCell -> palette.cellEmpty
        is Cell.StartCell -> palette.startFill
        is Cell.GoalCell  -> palette.goalFill
        is Cell.EmptyCell -> palette.cellEmpty
    }
    val topLeft = Offset(cx - cellPx / 2 + inset, cy - cellPx / 2 + inset)
    val size = Size(cellPx - 2 * inset, cellPx - 2 * inset)

    if (cell is Cell.StartCell) {
        // Radial glow first so the start cell pops.
        drawRect(
            color = palette.cellEmpty,
            topLeft = topLeft,
            size = size,
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(palette.startGlow, palette.startGlow.copy(alpha = 0f)),
                center = Offset(cx, cy),
                radius = cellPx * 0.55f,
            ),
            center = Offset(cx, cy),
            radius = cellPx * 0.55f,
        )
        drawCircle(
            color = palette.startFill,
            center = Offset(cx, cy),
            radius = cellPx * 0.28f,
        )
    } else if (cell is Cell.GoalCell) {
        drawRect(
            color = palette.cellEmpty,
            topLeft = topLeft,
            size = size,
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(palette.goalGlow, palette.goalGlow.copy(alpha = 0f)),
                center = Offset(cx, cy),
                radius = cellPx * 0.55f,
            ),
            center = Offset(cx, cy),
            radius = cellPx * 0.55f,
        )
        // Concentric target rings (gold disc + inner ring + centre dot).
        drawCircle(
            color = palette.goalFill,
            center = Offset(cx, cy),
            radius = cellPx * 0.30f,
        )
        drawCircle(
            color = palette.cellEmpty,
            center = Offset(cx, cy),
            radius = cellPx * 0.20f,
        )
        drawCircle(
            color = palette.goalFill,
            center = Offset(cx, cy),
            radius = cellPx * 0.10f,
        )
    } else {
        // Arrow or empty: shared rounded-rect backdrop.
        drawRoundRect(
            color = bg,
            topLeft = topLeft,
            size = size,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius),
        )
        if (highContrast) {
            drawRoundRect(
                color = palette.boardFrame,
                topLeft = topLeft,
                size = size,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius),
                style = Stroke(width = 2f),
            )
        }
    }
}

/**
 * Draws a thick arrow shape (chevron tail + triangular head) rotated to
 * [angleDeg] (0 = up, growing clockwise). The arrow is built around the
 * cell centre (cx, cy) and sized to ~60% of [cellPx].
 */
private fun DrawScope.drawArrow(
    arrowPath: Path,
    cx: Float,
    cy: Float,
    cellPx: Float,
    angleDeg: Float,
    fill: Color,
    outline: Color,
    highContrast: Boolean,
) {
    val extent = cellPx * 0.30f // half-height of the arrow bounding box
    val headWidth = cellPx * 0.26f
    val tailWidth = cellPx * 0.12f
    val tailLength = cellPx * 0.18f

    arrowPath.reset()
    arrowPath.moveTo(cx, cy - extent)                    // tip
    arrowPath.lineTo(cx + headWidth, cy - extent + headWidth) // right wing
    arrowPath.lineTo(cx + tailWidth, cy - extent + headWidth) // right tail shoulder
    arrowPath.lineTo(cx + tailWidth, cy + tailLength)    // right tail bottom
    arrowPath.lineTo(cx - tailWidth, cy + tailLength)    // left tail bottom
    arrowPath.lineTo(cx - tailWidth, cy - extent + headWidth) // left tail shoulder
    arrowPath.lineTo(cx - headWidth, cy - extent + headWidth) // left wing
    arrowPath.close()

    rotate(degrees = angleDeg, pivot = Offset(cx, cy)) {
        drawPath(path = arrowPath, color = fill)
        if (highContrast) {
            drawPath(
                path = arrowPath,
                color = outline,
                style = Stroke(width = 2.5f, join = StrokeJoin.Round),
            )
        }
    }
}

/** Draws the start + goal decoration overlays. Currently a no-op as the
 *  backgrounds already paint them; kept for clarity / future skinning. */
private fun DrawScope.drawStartGoal(
    board: Board,
    cellPx: Float,
    palette: com.zenox.arrowmaze.core.designsystem.theme.GamePalette,
    glowPath: Path,
) {
    // No-op — start/goal are rendered by drawCellBackground.
}

/** Draws the grid frame: outer border + inter-cell lines. */
private fun DrawScope.drawGridFrame(
    size: Int,
    cellPx: Float,
    color: Color,
    highContrast: Boolean,
) {
    val total = cellPx * size
    val stroke = if (highContrast) 2.5f else 1.2f

    // Outer border.
    drawRect(
        color = color,
        topLeft = Offset(0f, 0f),
        size = Size(total, total),
        style = Stroke(width = stroke),
    )

    // Inter-cell grid lines (skip the outer edges to avoid double-drawing
    // the border).
    for (i in 1 until size) {
        val pos = i * cellPx
        drawLine(
            color = color,
            start = Offset(pos, 0f),
            end = Offset(pos, total),
            strokeWidth = stroke * 0.6f,
        )
        drawLine(
            color = color,
            start = Offset(0f, pos),
            end = Offset(total, pos),
            strokeWidth = stroke * 0.6f,
        )
    }
}

/**
 * Draws the solution trail as a connected polyline through every cell
 * centre in [path], animated by [progress] (0 = nothing, 1 = full trail).
 *
 * The trail uses a linear gradient from [palette.trailStart] to
 * [palette.trailEnd] along the polyline's bounding box so the colour shifts
 * from blue at the start to violet at the goal.
 */
private fun DrawScope.drawTrail(
    trailPath: Path,
    path: List<Position>,
    cellPx: Float,
    palette: com.zenox.arrowmaze.core.designsystem.theme.GamePalette,
    progress: Float,
    highContrast: Boolean,
) {
    if (path.size < 2) return
    val clampedProgress = progress.coerceIn(0f, 1f)

    // How many full segments to draw? Fractional segments are drawn partially.
    val totalSegments = path.size - 1
    val drawnSegmentsFloat = totalSegments * clampedProgress
    val fullSegments = drawnSegmentsFloat.toInt()
    val partialFraction = drawnSegmentsFloat - fullSegments

    // Build the trail Path.
    trailPath.reset()
    val first = path.first()
    trailPath.moveTo(
        (first.col + 0.5f) * cellPx,
        (first.row + 0.5f) * cellPx,
    )
    for (i in 1..fullSegments) {
        val p = path[i]
        trailPath.lineTo(
            (p.col + 0.5f) * cellPx,
            (p.row + 0.5f) * cellPx,
        )
    }
    if (fullSegments < totalSegments && partialFraction > 0f) {
        val from = path[fullSegments]
        val to = path[fullSegments + 1]
        val fromX = (from.col + 0.5f) * cellPx
        val fromY = (from.row + 0.5f) * cellPx
        val toX = (to.col + 0.5f) * cellPx
        val toY = (to.row + 0.5f) * cellPx
        trailPath.lineTo(
            fromX + (toX - fromX) * partialFraction,
            fromY + (toY - fromY) * partialFraction,
        )
    }

    // Compute the polyline's bounding box for the gradient.
    val minX = path.minOf { (it.col + 0.5f) * cellPx }
    val maxX = path.maxOf { (it.col + 0.5f) * cellPx }
    val minY = path.minOf { (it.row + 0.5f) * cellPx }
    val maxY = path.maxOf { (it.row + 0.5f) * cellPx }

    val brush = Brush.linearGradient(
        colors = listOf(palette.trailStart, palette.trailEnd),
        start = Offset(minX, minY),
        end = Offset(maxX, maxY),
    )
    val strokeWidth = if (highContrast) cellPx * 0.18f else cellPx * 0.14f
    drawPath(
        path = trailPath,
        brush = brush,
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
            pathEffect = PathEffect.cornerPathEffect(cellPx * 0.25f),
        ),
    )
}

/** Convenience helper to draw a stroked round-rect. */
@Suppress("unused")
private fun DrawScope.strokeRoundRect(
    color: Color,
    topLeft: Offset,
    size: Size,
    cornerRadius: Float,
    strokeWidth: Float,
) {
    drawRoundRect(
        color = color,
        topLeft = topLeft,
        size = size,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius),
        style = Stroke(width = strokeWidth),
    )
}

/**
 * Maps an [ArrowDirection] to the angle convention used by Compose's
 * `rotate(degrees=)` modifier (0 = up, growing clockwise). Currently a
 * pass-through; kept as an explicit conversion point for future skinning
 * systems that may want to flip the convention.
 */
@Suppress("unused")
private fun ArrowDirection.toCanvasAngle(): Float = angleDegrees
