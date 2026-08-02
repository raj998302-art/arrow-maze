package com.zenox.arrowmaze.features.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenox.arrowmaze.core.data.repository.AchievementRepository
import com.zenox.arrowmaze.core.data.repository.SessionRepository
import com.zenox.arrowmaze.core.domain.model.Achievement
import com.zenox.arrowmaze.core.domain.model.AchievementRequirement
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Hilt-injected [ViewModel] backing the Achievements screen.
 *
 * Reactive inputs:
 *  - [AchievementRepository.observeUnlocked] — the player's unlocked ids.
 *  - [AchievementRepository.observeAllProgress] — the player's per-id progress.
 *
 * The two streams are combined with the static catalogue
 * ([AchievementRepository.allAchievements]) to produce the
 * [AchievementsUiState.Success] surface.
 *
 * When a new achievement id appears in `observeUnlocked` that wasn't there
 * before, the VM emits an [Achievement] via [unlockPopup] for the screen to
 * surface in the [AchievementUnlockPopup] overlay.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AchievementsViewModel @Inject constructor(
    private val achievementRepository: AchievementRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    /** Previously-seen unlocked ids; used to detect fresh unlocks. */
    private val seenUnlocked = mutableSetOf<String>()

    private val _unlockPopup = MutableSharedFlow<Achievement>(extraBufferCapacity = 4)
    val unlockPopup = _unlockPopup.asSharedFlow()

    private val _navEvents = Channel<AchievementsNavEvent>(capacity = Channel.BUFFERED)
    val navEvents = _navEvents.receiveAsFlow()

    val uiState: StateFlow<AchievementsUiState> = sessionRepository.currentUidFlow
        .flatMapLatest { _ ->
            kotlinx.coroutines.flow.flow<AchievementsUiState> {
                combine(
                    achievementRepository.observeUnlocked(),
                    achievementRepository.observeAllProgress(),
                ) { unlocked, progress ->
                    unlocked to progress
                }.collect { (unlocked, progress) ->
                    val catalogue = achievementRepository.allAchievements
                    val unlockedSet = unlocked.toSet()
                    val displays = catalogue.map { ach ->
                        AchievementDisplay(
                            achievement = ach,
                            isUnlocked = ach.id in unlockedSet,
                            progress = progress[ach.id] ?: 0,
                            progressTarget = requirementTarget(ach.requirement) ?: 1,
                        )
                    }.sortedWith(
                        compareByDescending<AchievementDisplay> { it.isUnlocked }
                            .thenBy { it.achievement.category.ordinal }
                            .thenBy { it.progressTarget },
                    )
                    val xpFromAchievements = displays
                        .filter { it.isUnlocked }
                        .sumOf { it.achievement.xpReward }

                    // Detect fresh unlocks (ids not in seenUnlocked at the
                    // previous emission). The first emission seeds seenUnlocked
                    // without firing any popups (cold-start restoration).
                    if (seenUnlocked.isNotEmpty()) {
                        catalogue.forEach { ach ->
                            if (ach.id in unlockedSet && ach.id !in seenUnlocked) {
                                Timber.i("Achievement unlocked: %s", ach.id)
                                _unlockPopup.tryEmit(ach)
                            }
                        }
                    }
                    seenUnlocked.clear()
                    seenUnlocked.addAll(unlockedSet)

                    emit(
                        AchievementsUiState.Success(
                            achievements = displays,
                            unlockedCount = displays.count { it.isUnlocked },
                            totalCount = displays.size,
                            xpFromAchievements = xpFromAchievements,
                        ),
                    )
                }
            }
        }
        .catch { t ->
            Timber.e(t, "Achievements stream failed")
            emit(AchievementsUiState.Error(t.message ?: "Failed to load achievements"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = AchievementsUiState.Loading,
        )

    /** Called by the screen when the unlock popup's "Claim" button is tapped. */
    fun unlockPopupDismissed() {
        viewModelScope.launch {
            _navEvents.send(AchievementsNavEvent.DismissUnlockPopup)
        }
    }

    /** Extracts the integer target from a requirement, or null for Custom. */
    private fun requirementTarget(requirement: AchievementRequirement): Int? = when (requirement) {
        is AchievementRequirement.SolveLevels   -> requirement.count
        is AchievementRequirement.ReachLevel    -> requirement.level
        is AchievementRequirement.SolveUnderTime -> requirement.seconds
        is AchievementRequirement.DailyStreak   -> requirement.days
        is AchievementRequirement.UseCoins      -> requirement.amount
        is AchievementRequirement.PlayGames     -> requirement.count
        is AchievementRequirement.WinStreak     -> requirement.count
        is AchievementRequirement.OwnItems      -> requirement.count
        AchievementRequirement.WinWithoutHints  -> 1
        is AchievementRequirement.Custom        -> null
    }
}
