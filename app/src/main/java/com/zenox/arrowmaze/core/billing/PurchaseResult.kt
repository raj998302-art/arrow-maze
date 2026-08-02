package com.zenox.arrowmaze.core.billing

import com.android.billingclient.api.Purchase

/**
 * Outcome of a `launchPurchaseFlow` invocation, surfaced via
 * [BillingManager.purchaseResults].
 *
 * The PurchaseProcessor only consumes [Success] entries — cancelled flows
 * are surfaced to the UI (e.g. to dismiss a spinner) but otherwise no-op'd.
 */
sealed interface PurchaseResult {

    /** The Play Store successfully completed the purchase. */
    data class Success(val purchase: Purchase) : PurchaseResult

    /** The user backed out of the Play Store purchase sheet. */
    data object Cancelled : PurchaseResult

    /** The purchase flow failed with the given Play Billing response code. */
    data class Error(val code: Int, val message: String) : PurchaseResult
}
