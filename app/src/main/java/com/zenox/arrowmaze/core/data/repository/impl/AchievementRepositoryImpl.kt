package com.zenox.arrowmaze.core.data.repository.impl

import com.zenox.arrowmaze.core.di.IoDispatcher
import com.zenox.arrowmaze.core.common.Result
import com.zenox.arrowmaze.core.common.resultOf
import com.zenox.arrowmaze.core.data.repository.AchievementRepository
import com.zenox.arrowmaze.core.database.dao.AchievementDao
import com.zenox.arrowmaze.core.database.entity.AchievementProgressEntity
import com.zenox.arrowmaze.core.domain.model.Achievement
import com.zenox.arrowmaze.core.domain.model.AchievementCategory
import com.zenox.arrowmaze.core.domain.model.AchievementRequirement
import com.zenox.arrowmaze.core.domain.model.DifficultyTier
import com.zenox.arrowmaze.core.domain.model.ShopCategory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

/**
 * Concrete [AchievementRepository]. The catalogue is built once at construction
 * (101 achievements across the 5 categories) and reused.
 *
 * Per-player progress lives in Room; Firestore sync is best-effort and lands
 * in Phase 10. Unlock decisions are made by callers (the achievements engine,
 * Phase 8) — this repository persists the result.
 */
class AchievementRepositoryImpl @Inject constructor(
    private val achievementDao: AchievementDao,
    @IoDispatcher private val io: CoroutineDispatcher,
) : AchievementRepository {

    override val allAchievements: List<Achievement> by lazy { buildCatalog() }

    override fun getById(id: String): Achievement? = allAchievements.firstOrNull { it.id == id }

    override fun observeUnlocked(): Flow<List<String>> =
        achievementDao.observeUnlocked().map { rows -> rows.map { it.achievementId } }

    override fun observeAllProgress(): Flow<Map<String, Int>> =
        achievementDao.observeAll().map { rows ->
            rows.associate { it.achievementId to it.progressInt }
        }

    override suspend fun getProgress(achievementId: String): Int = withContext(io) {
        achievementDao.get(achievementId)?.progressInt ?: 0
    }

    override suspend fun setProgress(achievementId: String, progress: Int): Result<Unit> =
        withContext(io) {
            resultOf {
                val existing = achievementDao.get(achievementId)
                val def = getById(achievementId)
                    ?: throw NoSuchElementException("Unknown achievement id=$achievementId")
                val clamped = progress.coerceAtLeast(0)
                val target = requirementTarget(def.requirement) ?: 0
                val alreadyUnlocked = existing?.unlocked == true
                val nowUnlocked = alreadyUnlocked || (target > 0 && clamped >= target)
                val entity = AchievementProgressEntity(
                    achievementId = achievementId,
                    unlocked = nowUnlocked,
                    unlockedAtEpochMs = existing?.unlockedAtEpochMs
                        ?: if (nowUnlocked) System.currentTimeMillis() else null,
                    progressInt = clamped,
                )
                achievementDao.upsert(entity)
                Timber.d("Achievement progress set: id=%s progress=%d unlocked=%s", achievementId, clamped, entity.unlocked)
                // Firestore sync: Phase 10
            }
        }

    override suspend fun unlock(achievementId: String): Result<Unit> = withContext(io) {
        resultOf {
            val existing = achievementDao.get(achievementId)
            if (existing?.unlocked == true) return@resultOf
            val now = System.currentTimeMillis()
            val entity = AchievementProgressEntity(
                achievementId = achievementId,
                unlocked = true,
                unlockedAtEpochMs = now,
                progressInt = existing?.progressInt ?: requirementTarget(
                    getById(achievementId)?.requirement
                ) ?: 0,
            )
            achievementDao.upsert(entity)
            Timber.i("Achievement unlocked: id=%s", achievementId)
            // Firestore sync: Phase 10
        }
    }

    override suspend fun isUnlocked(achievementId: String): Boolean = withContext(io) {
        achievementDao.get(achievementId)?.unlocked == true
    }

    // ---------- Catalogue builder ----------

    /**
     * Builds the static achievement catalogue. The structure mirrors the
     * requirement type breakdown in the project spec:
     *
     * - Solve N levels          (10)
     * - Reach tier X             (6)
     * - Reach level N            (10)
     * - Win without hints        (5)
     * - Solve under time         (10)
     * - Daily streak             (10)
     * - Use coins                (5)
     * - Own items per category   (10)
     * - Play games               (10)
     * - Win streak               (5)
     * - Collection               (5)
     * - Social                   (6)
     * - Special                  (9)
     *
     * Total: 101 achievements. About 10 are hidden.
     */
    @Suppress("LongMethod")
    private fun buildCatalog(): List<Achievement> {
        val list = mutableListOf<Achievement>()

        // ----- Solve N levels (10) -----
        val solveCounts = listOf(1, 5, 10, 25, 50, 100, 200, 400, 600, 1000)
        solveCounts.forEachIndexed { idx, n ->
            list += Achievement(
                id = "solve_levels_$n",
                title = when (n) {
                    1    -> "First Steps"
                    5    -> "Warming Up"
                    10   -> "Double Digits"
                    25   -> "Quarter Century"
                    50   -> "Halfway Hero"
                    100  -> "Centurion"
                    200  -> "Persistent Puzzler"
                    400  -> "Quadruple Threat"
                    600  -> "Marathon Maze Master"
                    else -> "Level Legend"
                },
                description = "Solve $n level${if (n == 1) "" else "s"}.",
                iconAsset = "ic_achievement_solve_$n",
                category = AchievementCategory.GAMEPLAY,
                requirement = AchievementRequirement.SolveLevels(n),
                xpReward = 50 + idx * 30,
                coinReward = 10 + idx * 15,
                isHidden = n == 1000,
            )
        }

        // ----- Reach tier X (6) -----
        DifficultyTier.entries.forEach { tier ->
            list += Achievement(
                id = "reach_tier_${tier.name.lowercase()}",
                title = "${tier.displayName} Contender",
                description = "Reach the ${tier.displayName} difficulty tier.",
                iconAsset = "ic_achievement_tier_${tier.name.lowercase()}",
                category = AchievementCategory.PROGRESSION,
                requirement = AchievementRequirement.Custom("reach_tier_${tier.name}"),
                xpReward = 80 + tier.sortOrder * 40,
                coinReward = 20 + tier.sortOrder * 20,
                isHidden = tier == DifficultyTier.LEGEND,
            )
        }

        // ----- Reach level N (10) -----
        val reachLevels = listOf(10, 25, 50, 100, 200, 300, 400, 500, 750, 1000)
        reachLevels.forEachIndexed { idx, lvl ->
            list += Achievement(
                id = "reach_level_$lvl",
                title = "Level $lvl Reached",
                description = "Reach player level $lvl.",
                iconAsset = "ic_achievement_level_$lvl",
                category = AchievementCategory.PROGRESSION,
                requirement = AchievementRequirement.ReachLevel(lvl),
                xpReward = 75 + idx * 30,
                coinReward = 15 + idx * 15,
                isHidden = lvl >= 750,
            )
        }

        // ----- Win without hints at each tier (5) -----
        val noHintTiers = listOf(
            DifficultyTier.EASY, DifficultyTier.NORMAL, DifficultyTier.HARD,
            DifficultyTier.EXPERT, DifficultyTier.MASTER,
        )
        noHintTiers.forEachIndexed { idx, tier ->
            list += Achievement(
                id = "no_hints_${tier.name.lowercase()}",
                title = "Untainted ${tier.displayName}",
                description = "Win a ${tier.displayName} level without using any hints.",
                iconAsset = "ic_achievement_no_hints_${tier.name.lowercase()}",
                category = AchievementCategory.GAMEPLAY,
                requirement = AchievementRequirement.Custom("no_hints_${tier.name}"),
                xpReward = 100 + idx * 40,
                coinReward = 25 + idx * 20,
                isHidden = tier == DifficultyTier.MASTER,
            )
        }

        // ----- Solve under time (10) -----
        // (tier, seconds) pairs at varying tiers.
        val solveUnderTimeSpecs = listOf(
            DifficultyTier.EASY to 60,
            DifficultyTier.NORMAL to 60,
            DifficultyTier.NORMAL to 45,
            DifficultyTier.HARD to 45,
            DifficultyTier.HARD to 30,
            DifficultyTier.EXPERT to 30,
            DifficultyTier.EXPERT to 20,
            DifficultyTier.MASTER to 20,
            DifficultyTier.MASTER to 10,
            DifficultyTier.LEGEND to 10,
        )
        solveUnderTimeSpecs.forEachIndexed { idx, (tier, secs) ->
            list += Achievement(
                id = "under_time_${tier.name.lowercase()}_${secs}s",
                title = "Speed Demon ${tier.displayName}",
                description = "Solve a ${tier.displayName} level in under $secs seconds.",
                iconAsset = "ic_achievement_speed_${tier.name.lowercase()}_$secs",
                category = AchievementCategory.GAMEPLAY,
                requirement = AchievementRequirement.SolveUnderTime(secs),
                xpReward = 120 + idx * 25,
                coinReward = 30 + idx * 15,
                isHidden = secs <= 10,
            )
        }

        // ----- Daily streak (10) -----
        val dailyStreaks = listOf(3, 7, 14, 30, 50, 100, 150, 200, 365, 500)
        dailyStreaks.forEachIndexed { idx, days ->
            list += Achievement(
                id = "daily_streak_$days",
                title = when (days) {
                    3    -> "Hat Trick"
                    7    -> "Week Warrior"
                    14   -> "Fortnight Fighter"
                    30   -> "Monthly Mind"
                    50   -> "Half-Century Habit"
                    100  -> "Triple Digit Dedication"
                    150  -> "Five-Month Streak"
                    200  -> "Almost a Year"
                    365  -> "Annual Arrowist"
                    else -> "Unbreakable"
                },
                description = "Maintain a $days-day daily challenge streak.",
                iconAsset = "ic_achievement_streak_$days",
                category = AchievementCategory.GAMEPLAY,
                requirement = AchievementRequirement.DailyStreak(days),
                xpReward = 75 + idx * 30,
                coinReward = 20 + idx * 15,
                isHidden = days >= 365,
            )
        }

        // ----- Use coins (5) -----
        val coinUsages = listOf(100, 500, 1000, 5000, 10000)
        coinUsages.forEachIndexed { idx, amount ->
            list += Achievement(
                id = "spend_coins_$amount",
                title = when (amount) {
                    100  -> "Pocket Change"
                    500  -> "Shopaholic"
                    1000 -> "Big Spender"
                    5000 -> "Coin Tycoon"
                    else -> "Mogul"
                },
                description = "Spend a total of $amount coins in the shop.",
                iconAsset = "ic_achievement_coins_$amount",
                category = AchievementCategory.COLLECTION,
                requirement = AchievementRequirement.UseCoins(amount),
                xpReward = 60 + idx * 40,
                coinReward = 0, // spent coins, no coin reward
                isHidden = amount >= 10000,
            )
        }

        // ----- Own items per category (10) -----
        // 5 for THEME, 5 for ARROW_SKIN.
        val ownItemsSpecs = listOf(
            ShopCategory.THEME to 1,
            ShopCategory.THEME to 5,
            ShopCategory.THEME to 10,
            ShopCategory.THEME to 25,
            ShopCategory.THEME to 50,
            ShopCategory.ARROW_SKIN to 1,
            ShopCategory.ARROW_SKIN to 5,
            ShopCategory.ARROW_SKIN to 10,
            ShopCategory.ARROW_SKIN to 25,
            ShopCategory.ARROW_SKIN to 50,
        )
        ownItemsSpecs.forEachIndexed { idx, (cat, count) ->
            list += Achievement(
                id = "own_${cat.name.lowercase()}_$count",
                title = "${cat.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }} Collector $count",
                description = "Own $count ${cat.name.lowercase().replace('_', ' ')} item${if (count == 1) "" else "s"}.",
                iconAsset = "ic_achievement_own_${cat.name.lowercase()}_$count",
                category = AchievementCategory.COLLECTION,
                requirement = AchievementRequirement.OwnItems(cat, count),
                xpReward = 80 + idx * 20,
                coinReward = 20 + idx * 15,
                isHidden = count >= 25,
            )
        }

        // ----- Play games (10) -----
        val playCounts = listOf(10, 50, 100, 250, 500, 1000, 2500, 5000, 10000, 25000)
        playCounts.forEachIndexed { idx, n ->
            list += Achievement(
                id = "play_games_$n",
                title = when (n) {
                    10     -> "Casual Player"
                    50     -> "Regular"
                    100    -> "Dedicated"
                    250    -> "Devoted"
                    500    -> "Veteran"
                    1000   -> "Hall of Famer"
                    2500   -> "Iron Player"
                    5000   -> "Five-Thousand Club"
                    10000  -> "Mythic Player"
                    else   -> "Endless Arrowist"
                },
                description = "Play $n games.",
                iconAsset = "ic_achievement_play_$n",
                category = AchievementCategory.GAMEPLAY,
                requirement = AchievementRequirement.PlayGames(n),
                xpReward = 50 + idx * 30,
                coinReward = 10 + idx * 15,
                isHidden = n >= 10000,
            )
        }

        // ----- Win streak (5) -----
        val winStreaks = listOf(5, 10, 25, 50, 100)
        winStreaks.forEachIndexed { idx, n ->
            list += Achievement(
                id = "win_streak_$n",
                title = when (n) {
                    5    -> "On Fire"
                    10   -> "Hot Streak"
                    25   -> "Unstoppable"
                    50   -> "Juggernaut"
                    else -> "Invincible"
                },
                description = "Win $n levels in a row.",
                iconAsset = "ic_achievement_streak_$n",
                category = AchievementCategory.GAMEPLAY,
                requirement = AchievementRequirement.WinStreak(n),
                xpReward = 100 + idx * 40,
                coinReward = 25 + idx * 20,
                isHidden = n >= 50,
            )
        }

        // ----- Collection (5) -----
        list += Achievement(
            id = "own_all_themes",
            title = "Fashionista",
            description = "Own every cosmetic theme.",
            iconAsset = "ic_achievement_all_themes",
            category = AchievementCategory.COLLECTION,
            requirement = AchievementRequirement.Custom("own_all_themes"),
            xpReward = 300,
            coinReward = 100,
            isHidden = true,
        )
        list += Achievement(
            id = "own_all_arrow_skins",
            title = "Sharp Dressed Arrows",
            description = "Own every arrow skin.",
            iconAsset = "ic_achievement_all_arrow_skins",
            category = AchievementCategory.COLLECTION,
            requirement = AchievementRequirement.Custom("own_all_arrow_skins"),
            xpReward = 250,
            coinReward = 80,
            isHidden = true,
        )
        list += Achievement(
            id = "own_all_trail_fx",
            title = "Trail Blazer",
            description = "Own every trail FX.",
            iconAsset = "ic_achievement_all_trail_fx",
            category = AchievementCategory.COLLECTION,
            requirement = AchievementRequirement.Custom("own_all_trail_fx"),
            xpReward = 250,
            coinReward = 80,
            isHidden = true,
        )
        list += Achievement(
            id = "own_all_board_backgrounds",
            title = "Background Check",
            description = "Own every board background.",
            iconAsset = "ic_achievement_all_backgrounds",
            category = AchievementCategory.COLLECTION,
            requirement = AchievementRequirement.Custom("own_all_board_backgrounds"),
            xpReward = 200,
            coinReward = 60,
            isHidden = false,
        )
        list += Achievement(
            id = "own_50_items_total",
            title = "Collector Supreme",
            description = "Own 50 items across every category combined.",
            iconAsset = "ic_achievement_own_50_total",
            category = AchievementCategory.COLLECTION,
            requirement = AchievementRequirement.OwnItems(ShopCategory.THEME, 50),
            xpReward = 350,
            coinReward = 120,
            isHidden = true,
        )

        // ----- Social (6) -----
        val friendCounts = listOf(1, 5, 10, 25, 50)
        friendCounts.forEachIndexed { idx, n ->
            list += Achievement(
                id = "friends_$n",
                title = when (n) {
                    1  -> "No Longer Alone"
                    5  -> "Inner Circle"
                    10 -> "Social Butterfly"
                    25 -> "Crowd Favourite"
                    else -> "Maze Mayor"
                },
                description = "Add $n friend${if (n == 1) "" else "s"}.",
                iconAsset = "ic_achievement_friends_$n",
                category = AchievementCategory.SOCIAL,
                requirement = AchievementRequirement.Custom("friends_$n"),
                xpReward = 60 + idx * 30,
                coinReward = 15 + idx * 20,
                isHidden = false,
            )
        }
        list += Achievement(
            id = "play_vs_friend",
            title = "Friendly Fire",
            description = "Play a head-to-head match against a friend.",
            iconAsset = "ic_achievement_vs_friend",
            category = AchievementCategory.SOCIAL,
            requirement = AchievementRequirement.Custom("play_vs_friend"),
            xpReward = 120,
            coinReward = 40,
            isHidden = false,
        )

        // ----- Special (9) -----
        list += Achievement(
            id = "login_weekend",
            title = "Weekend Warrior",
            description = "Complete a level on a Saturday or Sunday.",
            iconAsset = "ic_achievement_weekend",
            category = AchievementCategory.SPECIAL,
            requirement = AchievementRequirement.Custom("login_weekend"),
            xpReward = 80,
            coinReward = 20,
            isHidden = false,
        )
        list += Achievement(
            id = "login_new_year",
            title = "New Year Maze",
            description = "Log in on January 1st.",
            iconAsset = "ic_achievement_new_year",
            category = AchievementCategory.SPECIAL,
            requirement = AchievementRequirement.Custom("login_new_year"),
            xpReward = 200,
            coinReward = 100,
            isHidden = true,
        )
        list += Achievement(
            id = "login_halloween",
            title = "Trick or Trail",
            description = "Log in on Halloween (October 31st).",
            iconAsset = "ic_achievement_halloween",
            category = AchievementCategory.SPECIAL,
            requirement = AchievementRequirement.Custom("login_halloween"),
            xpReward = 150,
            coinReward = 75,
            isHidden = false,
        )
        list += Achievement(
            id = "login_christmas",
            title = "Mazey Christmas",
            description = "Log in on December 25th.",
            iconAsset = "ic_achievement_christmas",
            category = AchievementCategory.SPECIAL,
            requirement = AchievementRequirement.Custom("login_christmas"),
            xpReward = 150,
            coinReward = 75,
            isHidden = false,
        )
        list += Achievement(
            id = "complete_at_midnight",
            title = "Night Owl",
            description = "Complete a level between midnight and 1 AM.",
            iconAsset = "ic_achievement_midnight",
            category = AchievementCategory.SPECIAL,
            requirement = AchievementRequirement.Custom("complete_at_midnight"),
            xpReward = 100,
            coinReward = 40,
            isHidden = false,
        )
        list += Achievement(
            id = "complete_at_dawn",
            title = "Early Bird",
            description = "Complete a level between 5 AM and 7 AM.",
            iconAsset = "ic_achievement_dawn",
            category = AchievementCategory.SPECIAL,
            requirement = AchievementRequirement.Custom("complete_at_dawn"),
            xpReward = 100,
            coinReward = 40,
            isHidden = false,
        )
        list += Achievement(
            id = "play_7_days_in_a_week",
            title = "Seven-Day Streak",
            description = "Play on all 7 days of a single calendar week.",
            iconAsset = "ic_achievement_seven_days",
            category = AchievementCategory.SPECIAL,
            requirement = AchievementRequirement.Custom("play_7_days_in_a_week"),
            xpReward = 180,
            coinReward = 80,
            isHidden = false,
        )
        list += Achievement(
            id = "complete_first_daily",
            title = "Daily Debut",
            description = "Complete your first daily challenge.",
            iconAsset = "ic_achievement_first_daily",
            category = AchievementCategory.SPECIAL,
            requirement = AchievementRequirement.Custom("complete_first_daily"),
            xpReward = 90,
            coinReward = 30,
            isHidden = false,
        )
        list += Achievement(
            id = "use_10_hints_total",
            title = "Hint Hoarder",
            description = "Use a total of 10 hints across all games.",
            iconAsset = "ic_achievement_hint_hoarder",
            category = AchievementCategory.SPECIAL,
            requirement = AchievementRequirement.Custom("use_10_hints_total"),
            xpReward = 70,
            coinReward = 25,
            isHidden = true,
        )

        require(list.size >= 100) {
            "Achievement catalogue must have at least 100 entries, got ${list.size}"
        }
        return list
    }

    /** Extracts the integer target from a requirement, if applicable. */
    private fun requirementTarget(requirement: AchievementRequirement?): Int? = when (requirement) {
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
        null                                    -> null
    }
}
