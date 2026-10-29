package org.lerchenflo.schneaggchatv3mp.testutil

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/**
 * Swaps `Dispatchers.Main` (used by `viewModelScope`) for [testDispatcher]. kotlin-test has no
 * rules, so call [setUp] / [tearDown] yourself:
 *
 * ```
 * private val mainDispatcher = MainDispatcherRule()
 *
 * @BeforeTest fun setUp() = mainDispatcher.setUp()
 * @AfterTest fun tearDown() = mainDispatcher.tearDown()
 * ```
 *
 * - Default [UnconfinedTestDispatcher]: coroutines run eagerly, fine for most ViewModel tests.
 * - Lock, dedup and sync-order tests pass a `StandardTestDispatcher()` instead (plan R12) and run
 *   `runTest(mainDispatcher.testDispatcher) { … }`, so `viewModelScope` and the test body share one
 *   scheduler and `advanceUntilIdle()` / `runCurrent()` drive both.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) {
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    fun tearDown() {
        Dispatchers.resetMain()
    }
}
