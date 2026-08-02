package com.zenox.arrowmaze.core.billing

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module for the billing graph.
 *
 * Both [BillingManager] and [PurchaseProcessor] are `@Inject constructor`-able
 * `@Singleton` classes — Hilt auto-builds them without `@Provides`. This module
 * exists as a single point of documentation for the billing dependency graph
 * and as a future extension point (e.g. for binding a fake implementation in
 * instrumented tests via `@TestInstallIn`).
 */
@Module
@InstallIn(SingletonComponent::class)
object BillingModule
