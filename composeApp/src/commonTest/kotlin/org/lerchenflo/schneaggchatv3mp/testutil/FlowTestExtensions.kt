package org.lerchenflo.schneaggchatv3mp.testutil

import app.cash.turbine.test
import kotlinx.coroutines.flow.Flow

/**
 * Collects this flow with Turbine until it emits a value matching [predicate] and returns that
 * value. Turbine waits in wall-clock time (3 s per item), so this also works for flows that hop to
 * `Dispatchers.Default` (e.g. a `flowOn` inside a ViewModel) and fails with a timeout instead of
 * hanging when the value never comes.
 */
suspend fun <T> Flow<T>.awaitMatching(predicate: (T) -> Boolean): T {
    val match = mutableListOf<T>()
    test {
        var item = awaitItem()
        while (!predicate(item)) item = awaitItem()
        match += item
        cancelAndIgnoreRemainingEvents()
    }
    return match.single()
}
