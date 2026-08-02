package com.zenox.arrowmaze.features.dailychallenge

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zenox.arrowmaze.core.designsystem.components.ArrowMazeButton
import com.zenox.arrowmaze.core.designsystem.components.ArrowMazeIconButton
import com.zenox.arrowmaze.core.designsystem.components.ButtonStyle
import com.zenox.arrowmaze.core.designsystem.components.EmptyState
import com.zenox.arrowmaze.core.designsystem.components.ErrorState
import com.zenox.arrowmaze.core.designsystem.components.LoadingState
import com.zenox.arrowmaze.core.designsystem.icons.ArrowMazeIcons
import com.zenox.arrowmaze.core.designsystem.tokens.ElevationTokens
import com.zenox.arrowmaze.core.designsystem.tokens.SpacingTokens
import com.zenox.arrowmaze.core.domain.model.DailyChallenge
import com.zenox.arrowmaze.core.domain.model.DifficultyTier

/**
 * Root daily-challenge screen. Header shows today's date + tier; streak
 * row shows the day streak with a flame icon; calendar row shows the last
 * 7 days; rewards card shows coins + XP + a special badge; the big
 * "Start Challenge" button fires [onStartChallenge].
 *
 * @param onStartChallenge Called when the user taps the Start button. The
 *   NavHost routes to `Destination.Game.build(level, isDaily=true)` using
 *   the tier-derived level (see [DailyChallengeViewModel.levelForTier]).
 * @param onBack Called when the user taps the back arrow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyChallengeScreen(
    viewModel: DailyChallengeViewModel = hiltViewModel(),
    onStartChallenge: () -> Unit,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.navEvents.collect { event ->
            when (event) {
                is DailyChallengeNavEvent.StartChallenge -> onStartChallenge()
                is DailyChallengeNavEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Daily Challenge",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                },
                navigationIcon = {
                    ArrowMazeIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = onBack,
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { inner ->
        Box(modifier = Modifier.fillMaxSize().padding(inner)) {
            when (val state = uiState) {
                DailyChallengeUiState.Loading -> LoadingState(message = "Loading today's challenge…")
                is DailyChallengeUiState.Error -> ErrorState(message = state.message, onRetry = onBack)
                is DailyChallengeUiState.Success -> DailyChallengeContent(
                    state = state,
                    onStartChallenge = viewModel::startChallenge,
                )
            }
        }
    }
}

@Composable
private fun DailyChallengeContent(
    state: DailyChallengeUiState.Success,
    onStartChallenge: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val challenge = state.challenge

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(SpacingTokens.lg),
        verticalArrangement = Arrangement.spacedBy(SpacingTokens.lg),
    ) {
        // Header card: date + tier badge + board size
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = cs.surface,
            tonalElevation = ElevationTokens.Level1,
            shadowElevation = ElevationTokens.Level2,
        ) {
            Column(
                modifier = Modifier.padding(SpacingTokens.lg),
                verticalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
            ) {
                Text(
                    text = formatToday(challenge.dateIso),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = cs.onSurface,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TierBadge(tier = challenge.tier)
                    Spacer(Modifier.width(SpacingTokens.md))
                    Text(
                        text = "${challenge.boardSize}×${challenge.boardSize} board",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant,
                    )
                }
            }
        }

        // Streak row
        StreakRow(streak = state.streak)

        // Calendar strip
        CalendarStrip(days = state.calendar)

        // Rewards card
        RewardsCard(challenge = challenge)

        // Start button (disabled if already completed)
        ArrowMazeButton(
            text = if (challenge.completed) "Already Completed" else "Start Challenge",
            onClick = onStartChallenge,
            style = ButtonStyle.Primary,
            enabled = !challenge.completed,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = ArrowMazeIcons.Target,
        )

        // History
        if (state.history.isNotEmpty()) {
            Text(
                text = "History",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = cs.onSurface,
            )
            state.history.take(10).forEach { entry ->
                HistoryRow(entry = entry)
            }
        }

        Spacer(Modifier.height(SpacingTokens.lg))
    }
}

@Composable
private fun TierBadge(tier: DifficultyTier) {
    val cs = MaterialTheme.colorScheme
    val bgColor = runCatching { Color(android.graphics.Color.parseColor(tier.colorHex)) }
        .getOrElse { cs.primary }
    Surface(
        shape = RoundedCornerShape(50),
        color = bgColor,
    ) {
        Text(
            text = tier.displayName.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            modifier = Modifier.padding(horizontal = SpacingTokens.sm, vertical = 4.dp),
        )
    }
}

@Composable
private fun StreakRow(streak: Int) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = cs.surfaceVariant.copy(alpha = 0.5f),
    ) {
        Row(
            modifier = Modifier.padding(SpacingTokens.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.LocalFireDepartment,
                    contentDescription = null,
                    tint = if (streak > 0) Color(0xFFFF5722) else cs.onSurfaceVariant,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(Modifier.width(SpacingTokens.sm))
                Column {
                    Text(
                        text = if (streak > 0) "$streak day streak" else "No streak yet",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = cs.onSurface,
                    )
                    Text(
                        text = if (streak > 0) "Keep it going!" else "Solve today to start a streak.",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                    )
                }
            }
            if (streak >= 7) {
                Icon(
                    imageVector = ArrowMazeIcons.Sparkle,
                    contentDescription = null,
                    tint = cs.tertiary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
private fun CalendarStrip(days: List<DayStatus>) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = cs.surface,
        tonalElevation = ElevationTokens.Level1,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SpacingTokens.md),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            days.forEach { day ->
                DayCircle(day = day)
            }
        }
    }
}

@Composable
private fun DayCircle(day: DayStatus) {
    val cs = MaterialTheme.colorScheme
    val bgColor = when {
        day.isToday && day.completed -> cs.tertiary
        day.completed -> cs.primary
        day.isToday -> cs.primary.copy(alpha = 0.3f)
        else -> cs.surfaceVariant
    }
    val fgColor = when {
        day.completed -> Color.White
        day.isToday -> cs.onSurface
        else -> cs.onSurfaceVariant
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    brush = if (day.completed) {
                        Brush.radialGradient(listOf(bgColor, bgColor))
                    } else {
                        Brush.linearGradient(listOf(bgColor, bgColor))
                    },
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = day.dateIso.takeLast(2).trimStart('0').ifEmpty { "0" },
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = fgColor,
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = dayLabel(day.dateIso),
            style = MaterialTheme.typography.labelSmall,
            color = cs.onSurfaceVariant,
        )
    }
}

@Composable
private fun RewardsCard(challenge: DailyChallenge) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = cs.surface,
        tonalElevation = ElevationTokens.Level1,
        shadowElevation = ElevationTokens.Level2,
    ) {
        Column(
            modifier = Modifier.padding(SpacingTokens.lg),
            verticalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
        ) {
            Text(
                text = "Rewards",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = cs.onSurface,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                RewardBlock(icon = ArrowMazeIcons.Coin, value = "+${challenge.rewardCoins}", label = "Coins", tint = cs.tertiary)
                RewardBlock(icon = ArrowMazeIcons.Sparkle, value = "+${challenge.rewardXp}", label = "XP", tint = cs.primary)
                RewardBlock(icon = ArrowMazeIcons.Trophy, value = "Daily", label = "Badge", tint = cs.secondary)
            }
        }
    }
}

@Composable
private fun RewardBlock(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    tint: Color,
) {
    val cs = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = cs.onSurface,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = cs.onSurfaceVariant,
        )
    }
}

@Composable
private fun HistoryRow(entry: DailyChallenge) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = cs.surfaceVariant.copy(alpha = 0.4f),
    ) {
        Row(
            modifier = Modifier.padding(SpacingTokens.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatHistoryDate(entry.dateIso),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = cs.onSurface,
                )
                Text(
                    text = entry.tier.displayName + " • " + entry.boardSize + "×" + entry.boardSize,
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant,
                )
            }
            if (entry.completed) {
                Icon(
                    imageVector = ArrowMazeIcons.Trophy,
                    contentDescription = null,
                    tint = cs.tertiary,
                    modifier = Modifier.size(20.dp),
                )
                if (entry.solvedInSeconds != null) {
                    Spacer(Modifier.width(SpacingTokens.xs))
                    Text(
                        text = "${entry.solvedInSeconds}s",
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.onSurfaceVariant,
                    )
                }
            } else {
                Text(
                    text = "—",
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant,
                )
            }
        }
    }
}

/** Formats an ISO date as "5 Oct 2024" for the header card. */
private fun formatToday(iso: String): String = runCatching {
    val date = java.time.LocalDate.parse(iso)
    val day = date.dayOfMonth
    val month = date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    val year = date.year
    "$day $month $year"
}.getOrDefault(iso)

/** Formats an ISO date as "Oct 5" for the history rows. */
private fun formatHistoryDate(iso: String): String = runCatching {
    val date = java.time.LocalDate.parse(iso)
    val day = date.dayOfMonth
    val month = date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    "$month $day"
}.getOrDefault(iso)

/** Returns "Mon", "Tue", etc. for an ISO date. */
private fun dayLabel(iso: String): String = runCatching {
    val date = java.time.LocalDate.parse(iso)
    date.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
}.getOrDefault("—")
