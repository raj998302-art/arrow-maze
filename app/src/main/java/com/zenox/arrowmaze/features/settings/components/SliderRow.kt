package com.zenox.arrowmaze.features.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zenox.arrowmaze.core.designsystem.tokens.SpacingTokens

/**
 * Label + horizontal slider + numeric value. Used for music / SFX volume
 * and font-scale settings.
 *
 * @param label     Row label (e.g. "Music volume").
 * @param value     Current slider value.
 * @param valueText Human-readable value rendered to the right of the slider
 *                  (e.g. "70%"). Kept separate from [value] so callers can
 *                  format however they like.
 * @param onValueChange Called on every drag tick.
 * @param onValueChangeFinished Called when the user releases the slider.
 * @param valueRange Inclusive range of the slider.
 * @param steps Number of discrete steps between min and max (0 = continuous).
 */
@Composable
fun SliderRow(
    label: String,
    value: Float,
    valueText: String,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onValueChangeFinished: () -> Unit = {},
    valueRange: ClosedFloatingPointRange<Float> = 0f..100f,
    steps: Int = 0,
    subtitle: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = SpacingTokens.sm),
        verticalArrangement = Arrangement.spacedBy(SpacingTokens.xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.width(SpacingTokens.sm))
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.semantics {
                    contentDescription = "$label, $valueText"
                    role = Role.Tab
                },
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                thumbColor = MaterialTheme.colorScheme.primary,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .semantics { contentDescription = "$label slider" },
        )
    }
}
