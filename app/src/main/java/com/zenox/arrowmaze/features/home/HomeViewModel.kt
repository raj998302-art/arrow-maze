package com.zenox.arrowmaze.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenox.arrowmaze.core.common.AppConstants
import com.zenox.arrowmaze.core.common.onFailure
import com.zenox.arrowmaze.core.common.onSuccess
import com.zenox.arrowmaze.core.data.repository.AchievementRepository
import com.zenox.arrowmaze.core.data.repository.DailyChallengeRepository
import com.zenox.arrowmaze.core.data.repository.LevelProgressRepository
import com.zenox.arrowmaze.core.data.repository.ProfileRepository
import com.zenox.arrowmaze.core.data.repository.SessionRepository
import com.zenox.arrowmaze.core.domain.model.DailyChallenge
import com.zenox.arrowmaze.core.domain.model.LevelProgression
import com.zenox.arrowmaze.core.domain.model.Profile
import com.zenox.arrowmaze.core.firebase.auth.ArrowMazeAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject

/**
 * Hilt-injected [ViewModel] backing the Home screen.
 *
 * Reactive inputs:
 *  - [SessionRepository.currentUidFlow] — drives a `flatMapLatest` so the
 *    surface re-attaches to the right profile on sign-in / sign-out / guest
 *    upgrade.
 *  - [ProfileRepository.observeProfile] — live profile (coins, hints, lives,
 *    level, xp, highest level).
 *  - [DailyChallengeRepository.observeAll] — calendar + today's challenge.
 *  - [AchievementRepository.observeUnlocked] — unlocked count for the
 *    Achievements mode-card badge.
 *
 * Mutation surface:
 *  - [checkLifeRegen] — if the profile's `lastLifeRegenEpochMs` is older
 *    than [AppConstants.LIFE_REGEN_MINUTES], grant one life per elapsed
 *    window (capped at [AppConstants.MAX_LIVES]) and bump the timestamp.
 *    Called once on init (against the live profile flow) and on every
 *    [refresh].
 *  - [refresh] — pulls a fresh daily-challenge snapshot from
 *    [DailyChallengeRepository.getToday] and re-runs [checkLifeRegen].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val profileRepository: ProfileRepository,
    @Suppress("unused") // spec-required: reserved for level-progress-derived "Continue" badge state
    private val levelProgressRepository: LevelProgressRepository,
    private val dailyChallengeRepository: DailyChallengeRepository,
    private val achievementRepository: AchievementRepository,
    @Suppress("unused") // reserved for future sign-out wiring on the home top bar
    private val auth: ArrowMazeAuth,
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    private val _todayChallenge = MutableStateFlow<DailyChallenge?>(null)

    /** Ticker that re-emits on a fixed cadence so the life-regen countdown stays fresh. */
    private val _nowMs = MutableStateFlow(System.currentTimeMillis())

    private var refreshJob: Job? = null
    private var tickerJob: Job? = null
    private var lifeRegenJob: Job? = null

    init {
        // Prime today's daily challenge immediately so the home surface can
        // show the "Completed" badge without waiting for the user to pull
        // to refresh.
        viewModelScope.launch {
            dailyChallengeRepository.getToday()
                .onSuccess { _todayChallenge.value = it }
                .onFailure { Timber.w(it.asException(), "Failed to load today's daily challenge on home") }
        }

        // Drive the life-regen countdown timer — the displayed countdown
        // only needs ~15s resolution (the regen window is 30 minutes).
        tickerJob = viewModelScope.launch {
            while (true) {
                _nowMs.value = System.currentTimeMillis()
                delay(LIFE_REGEN_TICK_MS)
            }
        }

        // Run a life-regen pass as soon as we have a profile snapshot.
        // .catch() prevents a Room/DataStore failure from crashing the app.
        lifeRegenJob = viewModelScope.launch {
            sessionRepository.currentUidFlow
                .flatMapLatest { uid ->
                    uid?.let { profileRepository.observeProfile(it) } ?: flowOf<Profile?>(null)
                }
                .catch { t ->
                    Timber.e(t, "HomeViewModel: life-regen profile stream failed — skipping regen.")
                }
                .collect { profile ->
                    if (profile != null) checkLifeRegen(profile)
                }
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        sessionRepository.currentUidFlow.flatMapLatest { uid ->
            uid?.let { profileRepository.observeProfile(it) } ?: flowOf<Profile?>(null)
        },
        dailyChallengeRepository.observeAll(),
        achievementRepository.observeUnlocked(),
        _todayChallenge,
        _isRefreshing,
    ) { profile, allDailies, unlockedIds, today, isRefreshing ->
        if (profile == null) {
            HomeUiState.Loading
        } else {
            val currentLevel = computeCurrentLevel(profile)
            val todayChallenge = today ?: allDailies.firstOrNull { it.dateIso == todayIso() }
            val canPlayDaily = todayChallenge?.completed != true
            val streak = computeStreak(allDailies)
            val nextRegenMs = computeNextRegenMs(profile, _nowMs.value)
            HomeUiState.Success(
                profile = profile,
                currentLevel = currentLevel,
                currentTier = LevelProgression.tierFor(currentLevel),
                dailyChallenge = todayChallenge,
                canPlayDaily = canPlayDaily,
                dailyStreak = streak,
                lives = profile.lives,
                unlockedAchievements = unlockedIds.size,
                totalAchievements = achievementRepository.allAchievements.size,
                todayIso = todayIso(),
                nextLifeRegenMs = nextRegenMs,
                isRefreshing = isRefreshing,
            )
        }
    }
        .combine(_nowMs) { state, nowMs ->
            // Re-evaluate the next-life-regen countdown whenever the ticker
            // fires — without forcing a profile re-read.
            if (state is HomeUiState.Success) {
                state.copy(nextLifeRegenMs = computeNextRegenMs(state.profile, nowMs))
            } else {
                state
            }
        }
        .catch { t ->
            Timber.e(t, "Home stream failed")
            emit(HomeUiState.Error(t.message ?: "Failed to load home"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = HomeUiState.Loading,
        )

    /**
     * If `profile.lastLifeRegenEpochMs` is older than [AppConstants.LIFE_REGEN_MINUTES],
     * grants one life per elapsed window (capped at [AppConstants.MAX_LIVES])
     * and bumps the timestamp. Safe to call repeatedly — it short-circuits
     * when the player is at MAX_LIVES or the next regen is still in the future.
     */
    fun checkLifeRegen() {
        val state = uiState.value as? HomeUiState.Success ?: return
        checkLifeRegen(state.profile)
    }

    /** Overload that re-checks a specific profile snapshot (used by the init collector). */
    private fun checkLifeRegen(profile: Profile) {
        val now = System.currentTimeMillis()
        if (profile.lives >= AppConstants.MAX_LIVES) {
            // At cap — ensure the timestamp is current so the timer doesn't
            // immediately grant a life if the player spends one.
            if (now - profile.lastLifeRegenEpochMs > LIFE_REGEN_WINDOW_MS) {
                updateProfileLifeRegen(profile, lives = profile.lives, now = now)
            }
            return
        }
        val elapsed = now - profile.lastLifeRegenEpochMs
        if (elapsed < LIFE_REGEN_WINDOW_MS) return

        val regenCycles = (elapsed / LIFE_REGEN_WINDOW_MS).toInt().coerceAtLeast(1)
        val newLives = (profile.lives + regenCycles).coerceAtMost(AppConstants.MAX_LIVES)
        val consumedCycles = newLives - profile.lives
        val newTimestamp = profile.lastLifeRegenEpochMs + consumedCycles * LIFE_REGEN_WINDOW_MS
        updateProfileLifeRegen(profile, lives = newLives, now = newTimestamp)
    }

    /** Helper: writes the new (lives, lastLifeRegenEpochMs) atomically via the profile repo. */
    private fun updateProfileLifeRegen(profile: Profile, lives: Int, now: Long) {
        viewModelScope.launch {
            profileRepository.saveProfile(profile.copy(lives = lives, lastLifeRegenEpochMs = now))
                .onFailure { Timber.w(it.asException(), "Life regen save failed") }
        }
    }

    /** Pulls a fresh daily challenge snapshot + re-runs life-regen. */
    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _isRefreshing.value = true
            try {
                dailyChallengeRepository.getToday()
                    .onSuccess { _todayChallenge.value = it }
                    .onFailure { Timber.w(it.asException(), "Refresh: daily challenge fetch failed") }

                // Re-run life regen against the freshest profile snapshot.
                val uid = sessionRepository.currentUidFlow.first()
                if (uid != null) {
                    profileRepository.getProfile(uid)
                        .onSuccess { checkLifeRegen(it) }
                        .onFailure { Timber.w(it.asException(), "Refresh: profile read failed") }
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        tickerJob?.cancel()
        lifeRegenJob?.cancel()
        refreshJob?.cancel()
    }

    // ---------- helpers ----------

    private fun computeCurrentLevel(profile: Profile): Int {
        // Resume at highest completed + 1, or the player's stored level
        // (whichever is greater), but never below 1.
        val fromHighest = profile.highestLevel + 1
        return maxOf(profile.level.coerceAtLeast(1), fromHighest).coerceAtLeast(1)
    }

    /** Consecutive-day streak ending today (UTC) with a 1-day grace window. */
    private fun computeStreak(history: List<DailyChallenge>): Int {
        if (history.isEmpty()) return 0
        val today = LocalDate.now(ZoneOffset.UTC)
        val completedDates = history
            .filter { it.completed }
            .mapNotNull { runCatching { LocalDate.parse(it.dateIso) }.getOrNull() }
            .toSet()
        var streak = 0
        var cursor = today
        if (today !in completedDates) cursor = cursor.minusDays(1)
        while (cursor in completedDates) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    /** Returns ms until the next life regenerates; 0 when at MAX_LIVES. */
    private fun computeNextRegenMs(profile: Profile, nowMs: Long): Long {
        if (profile.lives >= AppConstants.MAX_LIVES) return 0L
        val next = profile.lastLifeRegenEpochMs + LIFE_REGEN_WINDOW_MS
        return (next - nowMs).coerceAtLeast(0L)
    }

    private fun todayIso(): String = LocalDate.now(ZoneOffset.UTC).toString()

    private companion object {
        const val LIFE_REGEN_TICK_MS = 15_000L
        val LIFE_REGEN_WINDOW_MS: Long =
            AppConstants.LIFE_REGEN_MINUTES.toLong() * 60L * 1_000L
    }
}
