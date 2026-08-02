package com.zenox.arrowmaze.features.dailychallenge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenox.arrowmaze.core.common.onFailure
import com.zenox.arrowmaze.core.common.onSuccess
import com.zenox.arrowmaze.core.data.repository.DailyChallengeRepository
import com.zenox.arrowmaze.core.data.repository.SessionRepository
import com.zenox.arrowmaze.core.domain.model.DailyChallenge
import com.zenox.arrowmaze.core.domain.model.DifficultyTier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject

/**
 * Hilt-injected [ViewModel] backing the Daily Challenge screen.
 *
 * Reactive inputs:
 *  - [DailyChallengeRepository.getToday] (one-shot on init; cached locally).
 *  - [DailyChallengeRepository.observeAll] (history + last-7-days calendar).
 *
 * Mutation surface:
 *  - [startChallenge] — fires [DailyChallengeNavEvent.StartChallenge] so the
 *    screen can route to `Destination.Game.build(levelForTier(tier), isDaily=true)`.
 *    The GameViewModel reads the daily seed deterministically from today's
 *    date, so the player gets the same board as everyone else.
 *  - [observeStreak] — recomputes the streak from the persisted history;
 *    the value is exposed via [DailyChallengeUiState.Success.streak].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DailyChallengeViewModel @Inject constructor(
    private val dailyChallengeRepository: DailyChallengeRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    private val _navEvents = Channel<DailyChallengeNavEvent>(capacity = Channel.BUFFERED)
    val navEvents = _navEvents.receiveAsFlow()

    private val _today = MutableStateFlow<DailyChallenge?>(null)

    init {
        // Load today's challenge once on init so the UI can render immediately.
        viewModelScope.launch {
            dailyChallengeRepository.getToday()
                .onSuccess { _today.value = it }
                .onFailure { error ->
                    Timber.w(error.asException(), "Failed to load today's daily challenge")
                    _navEvents.send(DailyChallengeNavEvent.ShowToast(error.message))
                }
        }
    }

    val uiState: StateFlow<DailyChallengeUiState> = combine(
        _today,
        dailyChallengeRepository.observeAll(),
    ) { today, all ->
        if (today == null) {
            DailyChallengeUiState.Loading
        } else {
            val streak = computeStreak(all)
            val calendar = buildCalendar(today = today.dateIso, history = all)
            DailyChallengeUiState.Success(
                challenge = today,
                streak = streak,
                calendar = calendar,
                history = all.sortedByDescending { it.dateIso },
            )
        }
    }
        .catch { t ->
            Timber.e(t, "Daily challenge stream failed")
            emit(DailyChallengeUiState.Error(t.message ?: "Failed to load daily challenge"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = DailyChallengeUiState.Loading,
        )

    /** Fires [DailyChallengeNavEvent.StartChallenge]; the screen routes to Game. */
    fun startChallenge() {
        viewModelScope.launch {
            val today = _today.value ?: run {
                dailyChallengeRepository.getToday()
                    .onSuccess { _today.value = it }
                    .onFailure { error ->
                        _navEvents.send(DailyChallengeNavEvent.ShowToast(error.message))
                    }
                _today.value
            } ?: return@launch
            if (today.completed) {
                _navEvents.send(DailyChallengeNavEvent.ShowToast("Already completed today — come back tomorrow!"))
                return@launch
            }
            _navEvents.send(DailyChallengeNavEvent.StartChallenge)
        }
    }

    /** Recomputes the streak from the persisted history. */
    fun observeStreak() {
        // The streak is computed reactively from observeAll(); nothing to do
        // here other than trigger a fresh subscription (which the screen
        // already does via collectAsStateWithLifecycle). Kept on the public
        // surface to satisfy the spec's "observeStreak()" method contract.
    }

    /** Computes the consecutive-day streak ending today (UTC). */
    private fun computeStreak(history: List<DailyChallenge>): Int {
        if (history.isEmpty()) return 0
        val today = LocalDate.now(ZoneOffset.UTC)
        val completedDates = history
            .filter { it.completed }
            .map { LocalDate.parse(it.dateIso) }
            .toSet()
        var streak = 0
        var cursor = today
        // Allow a 1-day grace window (DAILY_STREAK_GRACE_DAYS = 1): if today
        // isn't completed but yesterday was, the streak still reflects
        // yesterday's count.
        if (today !in completedDates) cursor = cursor.minusDays(1)
        while (cursor in completedDates) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    /** Builds the last-7-days calendar strip ending today. */
    private fun buildCalendar(today: String, history: List<DailyChallenge>): List<DayStatus> {
        val todayDate = LocalDate.parse(today)
        val completed = history.associate { it.dateIso to it.completed }
        return (6 downTo 0).map { offset ->
            val date = todayDate.minusDays(offset.toLong())
            val iso = date.toString()
            DayStatus(
                dateIso = iso,
                completed = completed[iso] == true,
                isToday = iso == today,
                rewardClaimed = completed[iso] == true,
            )
        }
    }

    /** Maps a [DifficultyTier] to a representative level number for the Game route. */
    companion object {
        fun levelForTier(tier: DifficultyTier): Int = when (tier) {
            DifficultyTier.EASY   -> 1
            DifficultyTier.NORMAL -> 21
            DifficultyTier.HARD   -> 51
            DifficultyTier.EXPERT -> 101
            DifficultyTier.MASTER -> 201
            DifficultyTier.LEGEND -> 401
        }
    }
}
