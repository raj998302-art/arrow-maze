package com.zenox.arrowmaze.features.achievements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zenox.arrowmaze.core.designsystem.components.ArrowMazeIconButton
import com.zenox.arrowmaze.core.designsystem.components.EmptyState
import com.zenox.arrowmaze.core.designsystem.components.ErrorState
import com.zenox.arrowmaze.core.designsystem.components.LoadingState
import com.zenox.arrowmaze.core.designsystem.icons.ArrowMazeIcons
import com.zenox.arrowmaze.core.designsystem.tokens.ElevationTokens
import com.zenox.arrowmaze.core.designsystem.tokens.SpacingTokens
import com.zenox.arrowmaze.core.domain.model.AchievementCategory
import com.zenox.arrowmaze.features.achievements.components.AchievementRow
import com.zenox.arrowmaze.features.achievements.components.AchievementUnlockPopup

/**
 * Display metadata for an achievement-category filter chip.
 */
private data class CategoryChip(val category: AchievementCategory?, val label: String)

private val categoryChips: List<CategoryChip> = listOf(
    CategoryChip(null, "All"),
    CategoryChip(AchievementCategory.GAMEPLAY, "Gameplay"),
    CategoryChip(AchievementCategory.PROGRESSION, "Progression"),
    CategoryChip(AchievementCategory.COLLECTION, "Collection"),
    CategoryChip(AchievementCategory.SOCIAL, "Social"),
    CategoryChip(AchievementCategory.SPECIAL, "Special"),
)

/**
 * Root achievements screen. Top bar shows the unlocked count + a back
 * button. Filter chips below the top bar scope the list to a category.
 * The body is a LazyColumn of [AchievementRow]s.
 *
 * When [AchievementsViewModel.unlockPopup] emits a new achievement (because
 * the player just unlocked it elsewhere in the app), the screen surfaces
 * the [AchievementUnlockPopup] overlay.
 *
 * @param onBack Called when the user taps the back arrow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    viewModel: AchievementsViewModel = hiltViewModel(),
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedCategory by rememberSaveable { mutableStateOf<AchievementCategory?>(null) }
    var popupAchievement by remember { mutableStateOf<com.zenox.arrowmaze.core.domain.model.Achievement?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.unlockPopup.collect { ach ->
            popupAchievement = ach
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Achievements",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        val state = uiState as? AchievementsUiState.Success
                        if (state != null) {
                            Text(
                                text = "${state.unlockedCount}/${state.totalCount} unlocked",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
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
                AchievementsUiState.Loading -> LoadingState(message = "Loading achievements…")
                is AchievementsUiState.Error -> ErrorState(message = state.message, onRetry = onBack)
                is AchievementsUiState.Success -> AchievementsContent(
                    state = state,
                    selectedCategory = selectedCategory,
                    onSelectCategory = { selectedCategory = it },
                )
            }

            AchievementUnlockPopup(
                achievement = popupAchievement,
                onDismiss = {
                    popupAchievement = null
                    viewModel.unlockPopupDismissed()
                },
            )
        }
    }
}

@Composable
private fun AchievementsContent(
    state: AchievementsUiState.Success,
    selectedCategory: AchievementCategory?,
    onSelectCategory: (AchievementCategory?) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val filtered = if (selectedCategory == null) {
        state.achievements
    } else {
        state.achievements.filter { it.achievement.category == selectedCategory }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Filter chips
        androidx.compose.foundation.lazy.LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = SpacingTokens.lg, vertical = SpacingTokens.sm),
            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
        ) {
            items(categoryChips, key = { it.label }) { chip ->
                CategoryChipPill(
                    label = chip.label,
                    selected = chip.category == selectedCategory,
                    onClick = { onSelectCategory(chip.category) },
                )
            }
        }

        // Progress summary card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SpacingTokens.lg, vertical = SpacingTokens.xs),
            shape = RoundedCornerShape(16.dp),
            color = cs.surface,
            tonalElevation = ElevationTokens.Level1,
        ) {
            Row(
                modifier = Modifier.padding(SpacingTokens.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "Progress",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = cs.onSurface,
                    )
                    Text(
                        text = "${state.unlockedCount} of ${state.totalCount} unlocked",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "+${state.xpFromAchievements}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = cs.primary,
                    )
                    Text(
                        text = "XP earned",
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.onSurfaceVariant,
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            EmptyState(
                icon = ArrowMazeIcons.Trophy,
                title = "No achievements here",
                subtitle = "Try a different category filter.",
                modifier = Modifier.fillMaxSize(),
            )
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = SpacingTokens.lg, vertical = SpacingTokens.sm),
            verticalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
        ) {
            items(filtered, key = { it.achievement.id }) { display ->
                AchievementRow(display = display)
            }
            item {
                Spacer(Modifier.height(SpacingTokens.lg))
            }
        }
    }
}

@Composable
private fun CategoryChipPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val bg = if (selected) cs.primary else cs.surfaceVariant.copy(alpha = 0.6f)
    val fg = if (selected) cs.onPrimary else cs.onSurfaceVariant
    Surface(
        shape = RoundedCornerShape(50),
        color = bg,
        onClick = onClick,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = fg,
            modifier = Modifier.padding(horizontal = SpacingTokens.lg, vertical = SpacingTokens.sm),
        )
    }
}
