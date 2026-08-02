package com.zenox.arrowmaze.core.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Abstraction over [Dispatchers] so that dispatchers can be swapped in tests
 * and so that no module ever touches [Dispatchers] directly.
 */
interface DispatcherProvider {
    val main: CoroutineDispatcher
    val mainImmediate: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
    val unconfined: CoroutineDispatcher
}

class DefaultDispatcherProvider : DispatcherProvider {
    override val main: CoroutineDispatcher get() = Dispatchers.Main
    override val mainImmediate: CoroutineDispatcher get() = Dispatchers.Main.immediate
    override val io: CoroutineDispatcher get() = Dispatchers.IO
    override val default: CoroutineDispatcher get() = Dispatchers.Default
    override val unconfined: CoroutineDispatcher get() = Dispatchers.Unconfined
}
