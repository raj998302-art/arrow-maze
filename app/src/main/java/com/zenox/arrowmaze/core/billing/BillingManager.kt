package com.zenox.arrowmaze.core.billing

import android.app.Activity
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.SkuDetails
import com.android.billingclient.api.SkuDetailsParams
import com.zenox.arrowmaze.core.common.AppError
import com.zenox.arrowmaze.core.common.Result
import com.zenox.arrowmaze.core.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Central facade over the Play Billing client.
 *
 * Wraps [BillingClient] with a [PurchasesUpdatedListener] + [BillingClientStateListener]
 * and exposes every operation as a suspend function returning the app's [Result] type.
 * Listener-driven callbacks (purchase updates, billing-setup completion, sku-detail
 * responses, ack responses, query-purchases responses) are bridged into suspending
 * calls via [suspendCancellableCoroutine].
 *
 * Connection lifecycle:
 * - The manager auto-starts the connection on first use (`startConnection` is
 *   idempotent if already connected). Callers may also call [ensureConnected]
 *   explicitly before issuing commands.
 * - [close] tears down the client — used from `Application.onTerminate()` /
 *   process death hooks. After [close] the manager is no longer usable.
 *
 * Purchase-flow results (success / cancelled / error) are surfaced via the hot
 * [purchaseResults] [SharedFlow]. The [PurchaseProcessor] collects this flow to
 * apply rewards + acknowledge; UI layers may also collect it to dismiss spinners.
 *
 * All billing response codes are mapped to [AppError.Billing] via [billingError].
 *
 * @property connectionState Hot state of the underlying client. UI layers can
 *   collect this to disable purchase buttons while [BillingConnectionState.Connecting].
 */
@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: android.content.Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) {

    private val scope = CoroutineScope(io + SupervisorJob())

    private val _connectionState =
        MutableStateFlow<BillingConnectionState>(BillingConnectionState.Disconnected)
    val connectionState: StateFlow<BillingConnectionState> = _connectionState.asStateFlow()

    private val _purchaseResults = MutableSharedFlow<PurchaseResult>(extraBufferCapacity = 8)
    val purchaseResults: SharedFlow<PurchaseResult> = _purchaseResults.asSharedFlow()

    /** Cached SkuDetails lookup so we don't re-query the Play Store on every purchase. */
    private val skuDetailsCache = mutableMapOf<String, SkuDetails>()

    private val purchasesUpdatedListener = PurchasesUpdatedListener { result, purchases ->
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                Timber.i("Purchase success: sku=${purchase.skus.joinToString()}, tokens=${purchase.purchaseToken.take(8)}…")
                scope.launch { _purchaseResults.emit(PurchaseResult.Success(purchase)) }
            }
        } else if (result.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Timber.i("Purchase flow cancelled by user.")
            scope.launch { _purchaseResults.emit(PurchaseResult.Cancelled) }
        } else {
            val msg = "Purchase flow error: code=${result.responseCode} msg=${result.debugMessage}"
            Timber.w(msg)
            scope.launch {
                _purchaseResults.emit(PurchaseResult.Error(result.responseCode, result.debugMessage))
            }
        }
    }

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    /**
     * Connects to the Play Billing service if not already connected.
     * Idempotent: returns immediately if [BillingConnectionState.Connected] or
     * [BillingConnectionState.Connecting].
     */
    suspend fun ensureConnected(): Result<Unit> = withContext(io) {
        when (_connectionState.value) {
            BillingConnectionState.Connected -> Result.Success(Unit)
            BillingConnectionState.Closed -> Result.Failure(
                AppError.Billing("Billing client is closed", null)
            )
            BillingConnectionState.Connecting -> waitForConnection()
            BillingConnectionState.Disconnected -> {
                startConnectionSuspend()
                waitForConnection()
            }
        }
    }

    private suspend fun startConnectionSuspend() {
        if (_connectionState.value == BillingConnectionState.Disconnected) {
            _connectionState.value = BillingConnectionState.Connecting
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        Timber.i("Billing client connected.")
                        _connectionState.value = BillingConnectionState.Connected
                    } else {
                        Timber.w("Billing setup failed: code=${billingResult.responseCode} msg=${billingResult.debugMessage}")
                        _connectionState.value = BillingConnectionState.Disconnected
                    }
                }

                override fun onBillingServiceDisconnected() {
                    Timber.w("Billing service disconnected.")
                    _connectionState.value = BillingConnectionState.Disconnected
                }
            })
        }
    }

    private suspend fun waitForConnection(): Result<Unit> = suspendCancellableCoroutine { cont ->
        val job = scope.launch {
            _connectionState.collect { state ->
                when (state) {
                    BillingConnectionState.Connected -> {
                        if (cont.isActive) cont.resume(Result.Success(Unit))
                        return@collect
                    }
                    BillingConnectionState.Closed -> {
                        if (cont.isActive) {
                            cont.resume(Result.Failure(AppError.Billing("Billing client closed", null)))
                        }
                        return@collect
                    }
                    BillingConnectionState.Disconnected -> {
                        if (cont.isActive) {
                            cont.resume(Result.Failure(AppError.Billing("Billing client disconnected", null)))
                        }
                        return@collect
                    }
                    BillingConnectionState.Connecting -> { /* keep waiting */ }
                }
            }
        }
        cont.invokeOnCancellation { job.cancel() }
    }

    /**
     * Queries the Play Store for [SkuDetails] of the given [skus] (INAPP type).
     * Caches results in [skuDetailsCache] so subsequent purchases don't re-query.
     */
    suspend fun querySkuDetails(skus: List<String>): Result<List<SkuDetails>> = withContext(io) {
        val conn = ensureConnected()
        if (conn is Result.Failure) return@withContext conn

        val params = SkuDetailsParams.newBuilder()
            .setType(BillingClient.SkuType.INAPP)
            .setSkusList(skus)
            .build()

        suspendCancellableCoroutine<Result<List<SkuDetails>>> { cont ->
            client.querySkuDetailsAsync(params) { result, detailsList ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    val safe = detailsList ?: emptyList()
                    safe.forEach { d -> skuDetailsCache[d.sku] = d }
                    Timber.i("SkuDetails query OK: ${safe.size} items.")
                    if (cont.isActive) cont.resume(Result.Success(safe))
                } else {
                    val err = billingError("querySkuDetails", result.responseCode, result.debugMessage)
                    Timber.w(err.message)
                    if (cont.isActive) cont.resume(Result.Failure(err))
                }
            }
        }
    }

    /**
     * Launches the Play Store purchase sheet for [sku]. Resumes with the
     * resulting [Purchase] once the [PurchasesUpdatedListener] reports success;
     * fails with [AppError.Billing] on cancel / error.
     *
     * Pre-loads the SkuDetails from [skuDetailsCache] (or queries the Play
     * Store if missing) so the purchase flow has a valid [BillingFlowParams].
     */
    suspend fun launchPurchaseFlow(activity: Activity, sku: String): Result<Purchase> =
        withContext(io) {
            val conn = ensureConnected()
            if (conn is Result.Failure) return@withContext conn

            val details = skuDetailsCache[sku] ?: when (val q = querySkuDetails(listOf(sku))) {
                is Result.Success -> q.data.firstOrNull()
                is Result.Failure -> return@withContext q as Result<Purchase>
                Result.Loading -> return@withContext Result.Loading as Result<Purchase>
            }
            if (details == null) {
                return@withContext Result.Failure(
                    AppError.Billing("SkuDetails not found for sku=$sku", null)
                )
            }

            val flowParams = BillingFlowParams.newBuilder()
                .setSkuDetails(details)
                .build()

            val launchResult = client.launchBillingFlow(activity, flowParams)
            if (launchResult.responseCode != BillingClient.BillingResponseCode.OK) {
                val err = billingError(
                    "launchBillingFlow",
                    launchResult.responseCode,
                    launchResult.debugMessage,
                )
                Timber.w(err.message)
                return@withContext Result.Failure(err)
            }

            // The actual purchase result arrives via `purchaseResults` SharedFlow.
            // Wait for the first emission matching this sku.
            suspendCancellableCoroutine { cont ->
                val job = scope.launch {
                    _purchaseResults.collect { res ->
                        when (res) {
                            is PurchaseResult.Success -> {
                                if (res.purchase.skus.contains(sku)) {
                                    if (cont.isActive) cont.resume(Result.Success(res.purchase))
                                    return@collect
                                }
                            }
                            is PurchaseResult.Cancelled -> {
                                if (cont.isActive) {
                                    cont.resume(Result.Failure(AppError.Billing("Purchase cancelled by user", null)))
                                }
                                return@collect
                            }
                            is PurchaseResult.Error -> {
                                if (cont.isActive) {
                                    cont.resume(Result.Failure(billingError("purchaseFlow", res.code, res.message)))
                                }
                                return@collect
                            }
                        }
                    }
                }
                cont.invokeOnCancellation { job.cancel() }
            }
        }

    /** Acknowledges a purchase — required for INAPP purchases within 3 days. */
    suspend fun acknowledgePurchase(purchase: Purchase): Result<Unit> = withContext(io) {
        val conn = ensureConnected()
        if (conn is Result.Failure) return@withContext conn

        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) {
            return@withContext Result.Failure(
                AppError.Billing("Purchase not in PURCHASED state (state=${purchase.purchaseState})", null)
            )
        }
        if (purchase.isAcknowledged) {
            Timber.d("Purchase ${purchase.orderId} already acknowledged.")
            return@withContext Result.Success(Unit)
        }

        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        suspendCancellableCoroutine<Result<Unit>> { cont ->
            client.acknowledgePurchase(params, AcknowledgePurchaseResponseListener { result ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    Timber.i("Purchase acknowledged: ${purchase.orderId}")
                    if (cont.isActive) cont.resume(Result.Success(Unit))
                } else {
                    val err = billingError("acknowledgePurchase", result.responseCode, result.debugMessage)
                    Timber.w(err.message)
                    if (cont.isActive) cont.resume(Result.Failure(err))
                }
            })
        }
    }

    /** Returns all active INAPP purchases the user currently owns. */
    suspend fun queryPurchases(): Result<List<Purchase>> = withContext(io) {
        val conn = ensureConnected()
        if (conn is Result.Failure) return@withContext conn

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.SkuType.INAPP)
            .build()

        suspendCancellableCoroutine<Result<List<Purchase>>> { cont ->
            client.queryPurchasesAsync(params) { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val list = purchases ?: emptyList()
                    Timber.i("QueryPurchases OK: ${list.size} items.")
                    if (cont.isActive) cont.resume(Result.Success(list))
                } else {
                    val err = billingError("queryPurchases", billingResult.responseCode, billingResult.debugMessage)
                    Timber.w(err.message)
                    if (cont.isActive) cont.resume(Result.Failure(err))
                }
            }
        }
    }

    /** Tears down the underlying client. Idempotent. */
    fun close() {
        if (_connectionState.value != BillingConnectionState.Closed) {
            Timber.i("Closing billing client.")
            try {
                client.endConnection()
            } catch (t: Throwable) {
                Timber.w(t, "endConnection threw")
            }
            _connectionState.value = BillingConnectionState.Closed
            scope.cancel()
        }
    }

    /** Maps a Play Billing response code to an [AppError.Billing]. */
    private fun billingError(op: String, code: Int, debug: String): AppError.Billing {
        val msg = when (code) {
            BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED -> "$op: feature not supported ($debug)"
            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> "$op: service disconnected ($debug)"
            BillingClient.BillingResponseCode.OK -> "$op: ok (no error)"
            BillingClient.BillingResponseCode.USER_CANCELED -> "$op: user cancelled ($debug)"
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> "$op: service unavailable ($debug)"
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> "$op: billing unavailable ($debug)"
            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> "$op: item unavailable ($debug)"
            BillingClient.BillingResponseCode.DEVELOPER_ERROR -> "$op: developer error ($debug)"
            BillingClient.BillingResponseCode.ERROR -> "$op: generic error ($debug)"
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> "$op: item already owned ($debug)"
            BillingClient.BillingResponseCode.ITEM_NOT_OWNED -> "$op: item not owned ($debug)"
            else -> "$op: unknown response code $code ($debug)"
        }
        return AppError.Billing(msg, code)
    }

    companion object {
        @Suppress("unused")
        private const val TAG = "BillingManager"
    }
}
