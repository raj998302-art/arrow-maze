package com.zenox.arrowmaze.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zenox.arrowmaze.R
import com.zenox.arrowmaze.core.designsystem.tokens.SpacingTokens

/**
 * Centred error state with icon + message + optional retry button.
 * Drops in wherever a `Result.Failure` needs to be surfaced (network,
 * Firestore, billing, etc.).
 */
@Composable
fun ErrorState(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(SpacingTokens.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint = cs.error,
            modifier = Modifier.size(56.dp),
        )
        Spacer(Modifier.height(SpacingTokens.lg))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = cs.onSurface,
            textAlign = TextAlign.Center,
        )
        if (onRetry != null) {
            Spacer(Modifier.height(SpacingTokens.lg))
            ArrowMazeButton(
                text = stringResource(R.string.common_retry),
                onClick = onRetry,
                style = ButtonStyle.Primary,
            )
        }
    }
}
