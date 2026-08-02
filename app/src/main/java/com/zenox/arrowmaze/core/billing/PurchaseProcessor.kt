package com.zenox.arrowmaze.core.billing

import android.app.Activity
import com.zenox.arrowmaze.BuildConfig
import com.zenox.arrowmaze.core.common.AppError
import com.zenox.arrowmaze.core.common.Result
import com.zenox.arrowmaze.core.data.repository.ProfileRepository
import com.zenox.arrowmaze.core.data.repository.SessionRepository
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sealed reward granted by [PurchaseProcessor.processSku]. The Shop / coin-pack
 * UI consumes [Coins] / [Hints] to show a "you got X coins!" toast, and the
 * premium / VIP rewards simply flip profile flags.
 */
sealed interface Reward {
    data class Coins(val amount: Int) : Reward
    data class Hints(val amount: Int) : Reward
    data object Premium : Reward
    data object Vip : Reward
}

/**
 * Maps SKUs to rewards, applies them to the user's [Profile], and acknowledges
 * the purchase via [BillingManager].
 *
 * Flow:
 * 1. `launchPurchaseFlow(activity, sku)` — surfaces the Play Store sheet.
 * 2. On success, applies the reward to the profile (`saveProfile` after
 *    mutating the matching field) via [ProfileRepository].
 * 3. Acknowledges the purchase via [BillingManager.acknowledgePurchase] so
 *    Google Play doesn't auto-refund after 3 days.
 * 4. Returns the [Reward] so the calling screen can show a success toast.
 *
 * For [PLAY_BILLING_PREMIUM_SKU] / [PLAY_BILLING_VIP_MONTHLY_SKU], the
 * corresponding `isPremium` / `isVip` flag is flipped on the profile.
 *
 * @property billingManager Wraps the Play [BillingClient].
 * @property profileRepository Reads / writes the player [Profile].
 * @property sessionRepository Source of truth for the current-user uid.
 */
@Singleton
class PurchaseProcessor @Inject constructor(
    private val billingManager: BillingManager,
    private val profileRepository: ProfileRepository,
    private val sessionRepository: SessionRepository,
) {

    /**
     * Surfaces the Play Store purchase sheet for [sku], applies the matching
     * reward to the current user's profile, and acknowledges the purchase.
     */
    suspend fun processSku(sku: String, activity: Activity): Result<Reward> {
        val reward = rewardForSku(sku)
            ?: return Result.Failure(
                AppError.Billing("Unknown SKU: $sku", null)
            )

        when (val purchase = billingManager.launchPurchaseFlow(activity, sku)) {
            is Result.Success -> {
                val apply = applyReward(reward)
                if (apply is Result.Failure) return apply
                val ack = billingManager.acknowledgePurchase(purchase.data)
                if (ack is Result.Failure) {
                    Timber.w(ack.error.message, "Ack failed for sku=%s", sku)
                    // Reward already applied — surface the ack error but the user keeps the reward.
                    return ack
                }
                return Result.Success(reward)
            }
            is Result.Failure -> return purchase
            Result.Loading -> return Result.Loading
        }
    }

    /** Maps a SKU string to its [Reward] (or `null` if unknown). */
    fun rewardForSku(sku: String): Reward? = when (sku) {
        BuildConfig.PLAY_BILLING_COINS_SMALL_SKU -> Reward.Coins(COINS_SMALL_AMOUNT)
        BuildConfig.PLAY_BILLING_COINS_MEDIUM_SKU -> Reward.Coins(COINS_MEDIUM_AMOUNT)
        BuildConfig.PLAY_BILLING_COINS_LARGE_SKU -> Reward.Coins(COINS_LARGE_AMOUNT)
        BuildConfig.PLAY_BILLING_HINTS_PACK_SKU -> Reward.Hints(HINTS_PACK_AMOUNT)
        BuildConfig.PLAY_BILLING_PREMIUM_SKU -> Reward.Premium
        BuildConfig.PLAY_BILLING_VIP_MONTHLY_SKU -> Reward.Vip
        else -> null
    }

    /** Applies [reward] to the current user's profile via [ProfileRepository]. */
    private suspend fun applyReward(reward: Reward): Result<Unit> {
        val uid = sessionRepository.currentUidFlow.first()
            ?: return Result.Failure(AppError.Auth("No current user", null))

        val profile = when (val r = profileRepository.getProfile(uid)) {
            is Result.Success -> r.data
            is Result.Failure -> return r as Result<Unit>
            Result.Loading -> return Result.Loading as Result<Unit>
        }

        val updated = when (reward) {
            is Reward.Coins -> profile.copy(coins = profile.coins + reward.amount)
            is Reward.Hints -> profile.copy(hints = profile.hints + reward.amount)
            Reward.Premium -> profile.copy(isPremium = true)
            Reward.Vip -> profile.copy(isVip = true)
        }

        Timber.i("Applying reward=$reward to uid=$uid (coins ${profile.coins}→${updated.coins}, hints ${profile.hints}→${updated.hints})")
        return profileRepository.saveProfile(updated)
    }

    companion object {
        const val COINS_SMALL_AMOUNT = 1000
        const val COINS_MEDIUM_AMOUNT = 5000
        const val COINS_LARGE_AMOUNT = 12000
        const val HINTS_PACK_AMOUNT = 50
    }
}
