package com.zenox.arrowmaze.features.game

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenox.arrowmaze.core.common.AppConstants
import com.zenox.arrowmaze.core.common.Result
import com.zenox.arrowmaze.core.common.onFailure
import com.zenox.arrowmaze.core.data.repository.AchievementRepository
import com.zenox.arrowmaze.core.data.repository.LevelProgressRepository
import com.zenox.arrowmaze.core.data.repository.ProfileRepository
import com.zenox.arrowmaze.core.data.repository.SessionRepository
import com.zenox.arrowmaze.core.data.repository.StatsRepository
import com.zenox.arrowmaze.core.di.IoDispatcher
import com.zenox.arrowmaze.core.domain.engine.GameEngine
import com.zenox.arrowmaze.core.domain.engine.PuzzleGenerator
import com.zenox.arrowmaze.core.domain.model.Board
import com.zenox.arrowmaze.core.domain.model.GameEvent
import com.zenox.arrowmaze.core.domain.model.LevelConfig
import com.zenox.arrowmaze.core.domain.model.LevelProgression
import com.zenox.arrowmaze.core.domain.model.LoseReason
import com.zenox.arrowmaze.core.domain.model.Position
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Hilt-injected [ViewModel] backing the Game screen.
 *
 * Owns the [GameEngine], drives the [GameUiState] lifecycle, persists wins /
 * losses through the four data repositories (profile / stats / level-progress
 * / achievements), and emits haptic + audio cues via injected façades.
 *
 * Saved state (process-death survival):
 *  - `level: Int`            — the level being played.
 *  - `isDaily: Boolean`      — whether this is a Daily Challenge.
 *  - `seed: Long`            — the deterministic board seed so a restarted
 *                              process regenerates the same board. For Daily
 *                              this is derived from today's date; for regular
 *                              levels it's a `System.currentTimeMillis()`
 *                              snapshot captured at the first initialize.
 *
 * The VM is non-reentrant: a single [GameEngine] is held in [engine] for the
 * lifetime of the session. `onRestart()` rebuilds the engine with the same
 * seed; `onContinueAfterWin()` is handled by navigation.
 */
@HiltViewModel
class GameViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val sessionRepository: SessionRepository,
    private val profileRepository: ProfileRepository,
    private val statsRepository: StatsRepository,
    private val levelProgressRepository: LevelProgressRepository,
    private val achievementRepository: AchievementRepository,
    private val hapticManager: com.zenox.arrowmaze.features.game.components.HapticManager,
    private val audioManager: com.zenox.arrowmaze.features.game.components.GameAudioManager,
    @IoDispatcher private val io: CoroutineDispatcher,
) : ViewModel() {

    // ---------- Saved-state keys ----------

    private object Keys {
        const val LEVEL = "level"
        const val IS_DAILY = "isDaily"
        const val SEED = "seed"
    }

    // ---------- Mutable session state ----------

    /** Live engine; non-null between [initialize] and VM clear. */
    private var engine: GameEngine? = null

    /** Seed used by [PuzzleGenerator] for the current session. */
    private var seed: Long = 0L

    /** Active level config; frozen once [initialize] runs. */
    private var config: LevelConfig? = null

    /** Daily flag, surfaced via [isDaily]. */
    private var daily: Boolean = false

    /** Player uid driving profile / stats writes. May be null briefly during guest bootstrap. */
    private var uid: String? = null

    /** Snapshot of player economy — kept here so [onCellTapped] can short-circuit reads. */
    @Volatile private var coins: Int = AppConstants.STARTING_COINS
    @Volatile private var hints: Int = AppConstants.STARTING_HINTS
    @Volatile private var lives: Int = AppConstants.STARTING_LIVES

    /** Background profile subscription so the HUD stays in sync with profile writes. */
    private var profileJob: Job? = null

    /** Per-second tick driving [GameSession.elapsedMs]. Cancelled on Win/Lost. */
    private var timerJob: Job? = null

    private val _uiState = MutableStateFlow<GameUiState>(GameUiState.Loading(level = 1))
    /** Public, read-only state stream consumed by [GameScreen]. */
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    // ---------- Lifecycle ----------

    init {
        // Restore from SavedStateHandle if available (process-death recovery).
        val restoredLevel = savedStateHandle.get<Int>(Keys.LEVEL)
        val restoredIsDaily = savedStateHandle.get<Boolean>(Keys.IS_DAILY) ?: false
        val restoredSeed = savedStateHandle.get<Long>(Keys.SEED)
        if (restoredLevel != null) {
            initialize(restoredLevel, restoredIsDaily, restoredSeed)
        }
    }

    /**
     * Bootstraps the session: derives the seed, generates the board, builds
     * the engine, and transitions to [GameUiState.Playing].
     *
     * If [seedOverride] is non-null (process-death recovery), it's used
     * verbatim; otherwise the seed is computed from the daily flag.
     */
    fun initialize(level: Int, isDaily: Boolean, seedOverride: Long? = null) {
        this.daily = isDaily
        savedStateHandle[Keys.LEVEL] = level
        savedStateHandle[Keys.IS_DAILY] = isDaily

        _uiState.value = GameUiState.Loading(level = level)

        viewModelScope.launch {
            try {
                val cfg = LevelProgression.configFor(level)
                config = cfg

                val resolvedSeed = seedOverride ?: computeSeed(isDaily)
                seed = resolvedSeed
                savedStateHandle[Keys.SEED] = resolvedSeed

                // Generate the board on the IO dispatcher — the carving
                // random-walk can take a few ms on big boards.
                val board = generateBoard(cfg, resolvedSeed)

                val eng = GameEngine(
                    initialBoard = board,
                    config = cfg,
                    maxMoves = cfg.suggestedMaxMoves,
                )
                engine = eng

                // Subscribe to the live profile so HUD coins/hints/lives
                // update in real time as we deduct hints or grant rewards.
                subscribeProfile()

                // Start the per-second timer.
                startTimer()

                Timber.i("Game initialised: level=%d daily=%s seed=%d board=%dx%d",
                    level, isDaily, resolvedSeed, cfg.boardSize, cfg.boardSize)

                _uiState.value = GameUiState.Playing(
                    session = eng.session,
                    path = eng.currentPath(),
                    lastRotatedCell = null,
                    coins = coins,
                    hints = hints,
                    lives = lives,
                    config = cfg,
                )
            } catch (t: Throwable) {
                Timber.e(t, "Failed to initialise game for level=%d", level)
                _uiState.value = GameUiState.Error(
                    message = "Could not load level $level: ${t.message ?: "Unknown error"}",
                )
            }
        }
    }

    // ---------- Player actions ----------

    /**
     * Rotates the arrow at [position]. Updates the [Playing] state with the
     * new session, plays haptics + SFX, and — when the engine signals a
     * terminal event — transitions to [Won] / [Lost] and persists the
     * outcome through the data repositories.
     *
     * Tapping a non-arrow cell, an out-of-bounds cell, or any cell after
     * the session has ended is a silent no-op.
     */
    fun onCellTapped(position: Position) {
        val eng = engine ?: return
        if (eng.isComplete()) return

        val event = eng.rotateCell(position) ?: return

        when (event) {
            is GameEvent.CellRotated -> {
                hapticManager.rotate()
                audioManager.playRotate()
                publishPlaying(position)
            }
            is GameEvent.HintUsed -> {
                // Hint auto-rotation produced a board change — surface it.
                publishPlaying(position)
            }
            is GameEvent.PathAdvanced,
            is GameEvent.PathRetreated -> {
                publishPlaying(null)
            }
            is GameEvent.Won -> handleWin(eng)
            is GameEvent.Lost -> handleLost(eng, LoseReason.OUT_OF_MOVES)
        }
    }

    /**
     * Consumes a hint. If the player has free hints (`hints > 0`), one is
     * deducted; otherwise the player must have at least
     * [AppConstants.HINT_COST_COINS] coins, which are spent in exchange for
     * a single hint. The engine is then asked to apply the hint rotation.
     *
     * No-op if the player can't afford a hint, the game is terminal, or no
     * useful hint exists.
     */
    fun onHintUsed() {
        val eng = engine ?: return
        if (eng.isComplete()) return

        // Determine the source of the hint.
        val useFreeHint = hints > 0
        val canAffordCoinHint = coins >= AppConstants.HINT_COST_COINS
        if (!useFreeHint && !canAffordCoinHint) {
            Timber.d("Hint rejected: no free hints and coins=%d < cost=%d",
                coins, AppConstants.HINT_COST_COINS)
            return
        }

        viewModelScope.launch {
            // Compute the post-hint economy.
            //  - Free hint: deduct 1 hint, coins unchanged.
            //  - Bought hint: deduct HINT_COST_COINS, hints unchanged
            //    (the purchased hint is consumed immediately by the engine).
            val finalCoins: Int
            val finalHints: Int
            if (useFreeHint) {
                finalCoins = coins
                finalHints = (hints - 1).coerceAtLeast(0)
            } else {
                finalCoins = (coins - AppConstants.HINT_COST_COINS).coerceAtLeast(0)
                finalHints = hints
            }

            val currentUid = uid
            if (currentUid != null) {
                profileRepository.updateEconomy(currentUid, finalCoins, finalHints, lives)
                    .onFailure { Timber.w(it.message ?: "updateEconomy failed") }
            }

            // Apply locally before the Flow propagates so the UI feels instant.
            coins = finalCoins
            hints = finalHints

            val event = eng.useHint()
            if (event == null) {
                Timber.d("Hint had no effect (no useful rotation).")
                publishPlaying(null)
                return@launch
            }

            hapticManager.tap()
            audioManager.playHint()

            when (event) {
                is GameEvent.HintUsed -> publishPlaying(event.at)
                is GameEvent.Won -> handleWin(eng)
                is GameEvent.Lost -> handleLost(eng, LoseReason.OUT_OF_MOVES)
                is GameEvent.CellRotated,
                is GameEvent.PathAdvanced,
                is GameEvent.PathRetreated -> publishPlaying(null)
            }
        }
    }

    /**
     * Regenerates the same level with the same seed — preserves difficulty
     * and Daily parity. The engine, timer, and state are reset; the player's
     * economy is left untouched.
     */
    fun onRestart() {
        val cfg = config ?: return
        val currentLevel = cfg.level
        Timber.i("Restart requested: level=%d seed=%d", currentLevel, seed)
        engine = null
        timerJob?.cancel()
        timerJob = null
        _uiState.value = GameUiState.Loading(level = currentLevel)

        viewModelScope.launch {
            try {
                val board = generateBoard(cfg, seed)
                val eng = GameEngine(
                    initialBoard = board,
                    config = cfg,
                    maxMoves = cfg.suggestedMaxMoves,
                )
                engine = eng
                startTimer()
                _uiState.value = GameUiState.Playing(
                    session = eng.session,
                    path = eng.currentPath(),
                    lastRotatedCell = null,
                    coins = coins,
                    hints = hints,
                    lives = lives,
                    config = cfg,
                )
            } catch (t: Throwable) {
                Timber.e(t, "Restart failed")
                _uiState.value = GameUiState.Error(
                    message = "Could not restart: ${t.message ?: "Unknown error"}",
                )
            }
        }
    }

    /**
     * Hook for the screen's "Continue" button. The actual navigation is
     * performed by the screen (which owns the NavController); the VM just
     * cancels the timer and lets the state settle.
     */
    fun onContinueAfterWin() {
        timerJob?.cancel()
        timerJob = null
    }

    // ---------- Win / lose handling ----------

    /**
     * Persists the win outcome through every relevant repository and
     * transitions to [GameUiState.Won].
     */
    private fun handleWin(eng: GameEngine) {
        val cfg = config ?: return
        val currentUid = uid
        val elapsedMs = eng.session.elapsedMs
        val moves = eng.movesPlayed
        val hintsUsed = eng.hintsUsed
        val level = cfg.level

        val coinsEarned = AppConstants.COIN_REWARD_PER_LEVEL + level * 2
        val xpEarned = AppConstants.XP_REWARD_PER_LEVEL + level * 5
        val stars = computeStars(cfg, moves, hintsUsed)

        hapticManager.success()
        audioManager.playWin()
        audioManager.duckMusic()
        timerJob?.cancel()
        timerJob = null

        // Optimistically transition so the win overlay appears immediately.
        _uiState.value = GameUiState.Won(
            session = eng.session,
            path = eng.currentPath(),
            coinsEarned = coinsEarned,
            xpEarned = xpEarned,
            timeMs = elapsedMs,
            moves = moves,
        )

        if (currentUid == null) {
            Timber.w("Win persisted without uid — guest profile not yet loaded")
            return
        }

        viewModelScope.launch(io) {
            // 1. Economy + progress on the profile.
            val newCoins = coins + coinsEarned
            profileRepository.updateEconomy(currentUid, newCoins, hints, lives)
                .onFailure { Timber.w(it.message ?: "updateEconomy failed (win)") }

            // Bump player level / XP by reading the latest profile then writing back.
            val profile = profileRepository.getProfile(currentUid).getOrNullSafe()
            if (profile != null) {
                val updatedXp = profile.xp + xpEarned
                val updatedLevel = max(profile.level, level + 1)
                val updatedHighest = max(profile.highestLevel, level)
                profileRepository.updateProgress(currentUid, updatedLevel, updatedXp)
                    .onFailure { Timber.w(it.message ?: "updateProgress failed (win)") }
                // highestLevel isn't exposed via updateProgress; save full profile.
                profileRepository.saveProfile(
                    profile.copy(
                        xp = updatedXp,
                        level = updatedLevel,
                        highestLevel = updatedHighest,
                        gamesWon = profile.gamesWon + 1,
                        gamesPlayed = profile.gamesPlayed + 1,
                        currentStreak = profile.currentStreak + 1,
                        bestStreak = max(profile.bestStreak, profile.currentStreak + 1),
                        coins = newCoins,
                        hints = hints,
                        lives = lives,
                    )
                ).onFailure { Timber.w(it.message ?: "saveProfile failed (win)") }

                // 2. Stats.
                statsRepository.recordGamePlayed(
                    uid = currentUid,
                    won = true,
                    timeMs = elapsedMs,
                    moves = moves,
                    hintsUsed = hintsUsed,
                    level = level,
                ).onFailure { Timber.w(it.message ?: "recordGamePlayed failed (win)") }

                // 3. Level progress (best-of).
                levelProgressRepository.recordCompletion(
                    level = level,
                    moves = moves,
                    timeMs = elapsedMs,
                    stars = stars,
                ).onFailure { Timber.w(it.message ?: "recordCompletion failed (win)") }

                // 4. Achievements (subset — full engine lands in Phase 8).
                evaluateWinAchievements(
                    level = level,
                    moves = moves,
                    hintsUsed = hintsUsed,
                    elapsedMs = elapsedMs,
                    tier = cfg.tier,
                    totalWins = profile.gamesWon + 1,
                    currentStreak = profile.currentStreak + 1,
                    totalGames = profile.gamesPlayed + 1,
                    playerLevel = updatedLevel,
                )
            }
        }
    }

    /**
     * Persists the loss outcome and transitions to [GameUiState.Lost].
     */
    private fun handleLost(eng: GameEngine, reason: LoseReason) {
        val cfg = config ?: return
        val currentUid = uid
        val elapsedMs = eng.session.elapsedMs
        val moves = eng.movesPlayed
        val hintsUsed = eng.hintsUsed
        val level = cfg.level

        hapticManager.error()
        audioManager.playLose()
        audioManager.duckMusic()
        timerJob?.cancel()
        timerJob = null

        _uiState.value = GameUiState.Lost(
            session = eng.session,
            reason = reason,
            coins = coins,
            hints = hints,
        )

        if (currentUid == null) {
            Timber.w("Loss persisted without uid — guest profile not yet loaded")
            return
        }

        viewModelScope.launch(io) {
            statsRepository.recordGamePlayed(
                uid = currentUid,
                won = false,
                timeMs = elapsedMs,
                moves = moves,
                hintsUsed = hintsUsed,
                level = level,
            ).onFailure { Timber.w(it.message ?: "recordGamePlayed failed (loss)") }

            // Update the profile's gamesPlayed + reset current streak.
            val profile = profileRepository.getProfile(currentUid).getOrNullSafe()
            if (profile != null) {
                profileRepository.saveProfile(
                    profile.copy(
                        gamesPlayed = profile.gamesPlayed + 1,
                        currentStreak = 0,
                    )
                ).onFailure { Timber.w(it.message ?: "saveProfile failed (loss)") }

                evaluateLossAchievements(
                    totalGames = profile.gamesPlayed + 1,
                    playerLevel = profile.level,
                )
            }
        }
    }

    // ---------- Achievement evaluation (Phase 6 subset) ----------

    /**
     * Lightweight achievement check covering the "obvious" wins: first win,
     * solve-count, win-streak, play-count, no-hints, under-time, and
     * reach-level. The full achievements engine (with predicate registry)
     * is Phase 8; this stub unlocks the most common unlocks so the player
     * sees immediate progress.
     */
    private suspend fun evaluateWinAchievements(
        level: Int,
        moves: Int,
        hintsUsed: Int,
        elapsedMs: Long,
        tier: com.zenox.arrowmaze.core.domain.model.DifficultyTier,
        totalWins: Int,
        currentStreak: Int,
        totalGames: Int,
        playerLevel: Int,
    ) {
        // solve_levels_<n>
        val solveCounts = listOf(1, 5, 10, 25, 50, 100, 200, 400, 600, 1000)
        solveCounts.forEach { n ->
            val id = "solve_levels_$n"
            if (totalWins >= n) {
                achievementRepository.unlock(id).onFailure {
                    Timber.v("unlock %s failed: %s", id, it.message)
                }
            } else {
                achievementRepository.setProgress(id, totalWins).onFailure {
                    Timber.v("setProgress %s failed: %s", id, it.message)
                }
            }
        }

        // win_streak_<n>
        val winStreaks = listOf(5, 10, 25, 50, 100)
        winStreaks.forEach { n ->
            val id = "win_streak_$n"
            if (currentStreak >= n) achievementRepository.unlock(id)
            else achievementRepository.setProgress(id, currentStreak)
        }

        // play_games_<n>
        val playCounts = listOf(10, 50, 100, 250, 500, 1000, 2500, 5000, 10000, 25000)
        playCounts.forEach { n ->
            val id = "play_games_$n"
            if (totalGames >= n) achievementRepository.unlock(id)
            else achievementRepository.setProgress(id, totalGames)
        }

        // reach_level_<n>
        val reachLevels = listOf(10, 25, 50, 100, 200, 300, 400, 500, 750, 1000)
        reachLevels.forEach { n ->
            val id = "reach_level_$n"
            if (playerLevel >= n) achievementRepository.unlock(id)
            else achievementRepository.setProgress(id, playerLevel)
        }

        // reach_tier_<tier> (uses Custom predicateId, so force-unlock).
        val tierId = "reach_tier_${tier.name.lowercase()}"
        achievementRepository.unlock(tierId).onFailure {
            Timber.v("unlock %s failed: %s", tierId, it.message)
        }

        // no_hints_<tier> — unlocked only when the player solved without hints.
        if (hintsUsed == 0) {
            val noHintTiers = listOf(
                com.zenox.arrowmaze.core.domain.model.DifficultyTier.EASY,
                com.zenox.arrowmaze.core.domain.model.DifficultyTier.NORMAL,
                com.zenox.arrowmaze.core.domain.model.DifficultyTier.HARD,
                com.zenox.arrowmaze.core.domain.model.DifficultyTier.EXPERT,
                com.zenox.arrowmaze.core.domain.model.DifficultyTier.MASTER,
            )
            if (tier in noHintTiers) {
                val id = "no_hints_${tier.name.lowercase()}"
                achievementRepository.unlock(id).onFailure {
                    Timber.v("unlock %s failed: %s", id, it.message)
                }
            }
        }

        // under_time_<tier>_<secs>s — unlock the highest threshold cleared.
        val seconds = (elapsedMs / 1000.0).roundToInt()
        val underTimeSpecs = listOf(
            com.zenox.arrowmaze.core.domain.model.DifficultyTier.EASY to 60,
            com.zenox.arrowmaze.core.domain.model.DifficultyTier.NORMAL to 60,
            com.zenox.arrowmaze.core.domain.model.DifficultyTier.NORMAL to 45,
            com.zenox.arrowmaze.core.domain.model.DifficultyTier.HARD to 45,
            com.zenox.arrowmaze.core.domain.model.DifficultyTier.HARD to 30,
            com.zenox.arrowmaze.core.domain.model.DifficultyTier.EXPERT to 30,
            com.zenox.arrowmaze.core.domain.model.DifficultyTier.EXPERT to 20,
            com.zenox.arrowmaze.core.domain.model.DifficultyTier.MASTER to 20,
            com.zenox.arrowmaze.core.domain.model.DifficultyTier.MASTER to 10,
            com.zenox.arrowmaze.core.domain.model.DifficultyTier.LEGEND to 10,
        )
        underTimeSpecs.forEach { (t, secs) ->
            if (tier == t && seconds < secs) {
                val id = "under_time_${t.name.lowercase()}_${secs}s"
                achievementRepository.unlock(id).onFailure {
                    Timber.v("unlock %s failed: %s", id, it.message)
                }
            }
        }

        // Touch the catalogue so dead-code elimination doesn't strip the
        // requirement-mapping helpers (Phase 8 will replace this stub).
        achievementRepository.allAchievements.size.let { /* no-op */ }
    }

    /** Loss-side achievements: only the play-count updates reset their streak. */
    private suspend fun evaluateLossAchievements(
        totalGames: Int,
        playerLevel: Int,
    ) {
        val playCounts = listOf(10, 50, 100, 250, 500, 1000, 2500, 5000, 10000, 25000)
        playCounts.forEach { n ->
            val id = "play_games_$n"
            if (totalGames >= n) achievementRepository.unlock(id)
            else achievementRepository.setProgress(id, totalGames)
        }
    }

    // ---------- Internals ----------

    /** Generates a board on the IO dispatcher. */
    private suspend fun generateBoard(cfg: LevelConfig, seedValue: Long): Board =
        kotlinx.coroutines.withContext(io) {
            PuzzleGenerator.generate(cfg, seedValue)
        }

    /** Daily: deterministic per-calendar-day seed. Regular: wall-clock snapshot. */
    private fun computeSeed(isDaily: Boolean): Long =
        if (isDaily) {
            Clock.System.todayIn(TimeZone.currentSystemDefault()).toEpochDays().toLong()
        } else {
            System.currentTimeMillis()
        }

    /**
     * Subscribes to the player profile flow so [coins] / [hints] / [lives]
     * stay in sync with repository writes. Cancelled in [onCleared].
     */
    private fun subscribeProfile() {
        profileJob?.cancel()
        profileJob = sessionRepository.currentUidFlow
            .onEach { id ->
                uid = id
                if (id == null) {
                    // Guest fallback: keep the default economy.
                    return@onEach
                }
                val profile = profileRepository.observeProfile(id).first()
                if (profile != null) {
                    coins = profile.coins
                    hints = profile.hints
                    lives = profile.lives
                    // Refresh the current state with the updated balances.
                    refreshPlayingEconomy()
                }
            }
            .launchIn(viewModelScope)
    }

    /** Patches the latest Playing state with the live economy snapshot. */
    private fun refreshPlayingEconomy() {
        val state = _uiState.value
        if (state is GameUiState.Playing) {
            _uiState.value = state.copy(coins = coins, hints = hints, lives = lives)
        }
    }

    /** Emits a fresh [GameUiState.Playing] from the engine's current session. */
    private fun publishPlaying(lastRotated: Position?) {
        val eng = engine ?: return
        val cfg = config ?: return
        val state = _uiState.value as? GameUiState.Playing
        val next = GameUiState.Playing(
            session = eng.session,
            path = eng.currentPath(),
            lastRotatedCell = lastRotated ?: state?.lastRotatedCell,
            coins = coins,
            hints = hints,
            lives = lives,
            config = cfg,
        )
        _uiState.value = next
    }

    /** Starts the per-second elapsed-time ticker. Idempotent. */
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1_000L)
                val eng = engine ?: break
                if (eng.isComplete()) break
                val state = _uiState.value
                if (state is GameUiState.Playing) {
                    val newElapsed = state.session.elapsedMs + 1_000L
                    eng.updateElapsed(newElapsed)
                    _uiState.value = state.copy(session = eng.session)
                } else {
                    break
                }
            }
        }
    }

    /**
     * Computes 1–3 stars for a level completion based on move efficiency
     * and hint usage. Three stars require no hints AND ≤50% of the move cap;
     * two stars require ≤75%; one star otherwise.
     */
    private fun computeStars(cfg: LevelConfig, moves: Int, hintsUsed: Int): Int {
        val maxMoves = cfg.suggestedMaxMoves.coerceAtLeast(1)
        val moveFraction = moves.toFloat() / maxMoves.toFloat()
        return when {
            hintsUsed == 0 && moveFraction <= 0.50f -> 3
            moveFraction <= 0.75f -> 2
            else -> 1
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        profileJob?.cancel()
    }

    // ---------- Result helpers ----------

    /** Lightweight `getOrNull` shim that avoids importing the extension on every call site. */
    private fun <T> Result<T>.getOrNullSafe(): T? = (this as? Result.Success)?.data
}
