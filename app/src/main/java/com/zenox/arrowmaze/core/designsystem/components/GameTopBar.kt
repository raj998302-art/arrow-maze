package com.zenox.arrowmaze.core.designsystem.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zenox.arrowmaze.R

/**
 * Top app bar for the game screen. Centered level title (with optional
 * subtitle for daily challenge / level number), back button on the left,
 * settings icon on the right. Uses Material 3 [CenterAlignedTopAppBar].
 *
 * Touch targets on both icons are 48.dp via [ArrowMazeIconButton].
 */
@Composable
fun GameTopBar(
    title: String,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    val cs = MaterialTheme.colorScheme
    CenterAlignedTopAppBar(
        modifier = modifier,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = cs.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
                if (subtitle != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
        },
        navigationIcon = {
            ArrowMazeIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.cd_back_button),
                onClick = onBack,
            )
        },
        actions = {
            ArrowMazeIconButton(
                icon = Icons.Rounded.Settings,
                contentDescription = "Settings",
                onClick = onSettings,
            )
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = cs.surface,
            titleContentColor = cs.onSurface,
            navigationIconContentColor = cs.onSurface,
            actionIconContentColor = cs.onSurfaceVariant,
        ),
    )
}
