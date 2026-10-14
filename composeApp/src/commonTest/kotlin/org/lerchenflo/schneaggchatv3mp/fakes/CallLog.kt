package org.lerchenflo.schneaggchatv3mp.fakes

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update

/**
 * The calls one fake method received, in call order. Thread-safe, because some code under test
 * calls its dependencies from `Dispatchers.IO` / `Dispatchers.Default`.
 *
 * Read [calls] after the code under test ran, or [awaitCount] when the call happens on another
 * thread (it waits in wall-clock time, so keep it inside `runTest`, whose own timeout ends a test
 * that waits for a call that never comes).
 */
class CallLog<T> {

    private val _calls = MutableStateFlow<List<T>>(emptyList())

    val calls: List<T> get() = _calls.value

    val count: Int get() = _calls.value.size

    fun record(call: T) {
        _calls.update { it + call }
    }

    /** Suspends until at least [count] calls were recorded, then returns all of them. */
    suspend fun awaitCount(count: Int): List<T> = _calls.first { it.size >= count }
}
