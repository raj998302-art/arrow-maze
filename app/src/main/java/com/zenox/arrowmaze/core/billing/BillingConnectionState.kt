package com.zenox.arrowmaze.core.billing

/**
 * Connection state of the Play [BillingManager]'s underlying [BillingClient].
 *
 * The state is exposed as a hot `StateFlow` so UI layers can show a spinner
 * while the connection is being established and disable purchase buttons when
 * the client is disconnected.
 */
sealed interface BillingConnectionState {

    /** Initial state — `startConnection` has not yet been called (or the client was closed). */
    data object Disconnected : BillingConnectionState

    /** `startConnection` was called and we are waiting for `onBillingSetupFinished`. */
    data object Connecting : BillingConnectionState

    /** `onBillingSetupFinished` returned `OK`. The client is ready to query / purchase. */
    data object Connected : BillingConnectionState

    /** `endConnection` was called — terminal state, the manager is no longer usable. */
    data object Closed : BillingConnectionState
}
