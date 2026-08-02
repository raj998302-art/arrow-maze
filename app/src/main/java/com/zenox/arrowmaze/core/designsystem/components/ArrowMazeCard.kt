package com.zenox.arrowmaze.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.zenox.arrowmaze.core.designsystem.theme.BrandBlue
import com.zenox.arrowmaze.core.designsystem.theme.BrandViolet
import com.zenox.arrowmaze.core.designsystem.tokens.ElevationTokens
import com.zenox.arrowmaze.core.designsystem.tokens.SpacingTokens

/**
 * Visual variants for [ArrowMazeCard].
 *
 * - [Elevated] — surface + soft shadow for prominent content.
 * - [Outlined] — flat surface with 1dp outlineVariant border.
 * - [Gradient] — surface wrapped in a 1dp brand-blue→violet gradient border.
 */
enum class CardVariant { Elevated, Outlined, Gradient }

/**
 * Reusable content card with optional header and three visual variants.
 * 16.dp rounded corners, [SpacingTokens.lg] inner padding by default.
 */
@Composable
fun ArrowMazeCard(
    modifier: Modifier = Modifier,
    variant: CardVariant = CardVariant.Elevated,
    header: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    val innerPadding = PaddingValues(all = SpacingTokens.lg)

    when (variant) {
        CardVariant.Elevated -> {
            Surface(
                modifier = modifier.fillMaxWidth(),
                shape = shape,
                color = cs.surface,
                tonalElevation = ElevationTokens.Level1,
                shadowElevation = ElevationTokens.Level2,
            ) {
                CardContent(header = header, content = content, padding = innerPadding)
            }
        }

        CardVariant.Outlined -> {
            Surface(
                modifier = modifier.fillMaxWidth(),
                shape = shape,
                color = cs.surface,
                border = BorderStroke(1.dp, cs.outlineVariant),
            ) {
                CardContent(header = header, content = content, padding = innerPadding)
            }
        }

        CardVariant.Gradient -> {
            // Outer box paints the gradient border; inner Surface sits 1.dp
            // inside so the gradient shows through as a thin frame.
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(colors = listOf(BrandBlue, BrandViolet)),
                        shape = shape,
                    )
                    .padding(1.dp),
            ) {
                Surface(
                    shape = shape,
                    color = cs.surface,
                ) {
                    CardContent(header = header, content = content, padding = innerPadding)
                }
            }
        }
    }
}

@Composable
private fun CardContent(
    header: (@Composable () -> Unit)?,
    content: @Composable () -> Unit,
    padding: PaddingValues,
) {
    Column(modifier = Modifier.padding(padding)) {
        header?.invoke()
        if (header != null) {
            Spacer(Modifier.height(SpacingTokens.md))
        }
        content()
    }
}
