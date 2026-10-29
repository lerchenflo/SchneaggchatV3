package org.lerchenflo.schneaggchatv3mp.testutil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.job
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.reflect.KClass

/**
 * Owns the ViewModels of one test, like a screen's `ViewModelStore`, so [clear] can end them the
 * way the app does: `onCleared()` runs and `viewModelScope` is cancelled. Holds one ViewModel per
 * class. Use it through [runViewModelTest].
 */
class TestViewModelStore {

    private val store = ViewModelStore()
    private val scopeJobs = mutableListOf<Job>()

    fun <VM : ViewModel> add(viewModel: VM): VM {
        scopeJobs += viewModel.viewModelScope.coroutineContext.job
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T = viewModel as T
        }
        @Suppress("UNCHECKED_CAST")
        return ViewModelProvider.create(store, factory)[viewModel::class as KClass<VM>]
    }

    /**
     * Clears every ViewModel and waits until all their coroutines have ended. The waiting matters
     * for ViewModels with a `flowOn(Dispatchers.Default)`: the cancelled producer finishes on a
     * `Default` thread and then resumes its collector on `Dispatchers.Main`. If that happened
     * after the test swapped `Dispatchers.Main` back, it would hit the next test's dispatcher.
     */
    suspend fun clear() {
        store.clear()
        scopeJobs.forEach { it.join() }
        scopeJobs.clear()
    }
}

/**
 * `runTest` for ViewModel tests: when [testBody] ends (also by failing), the ViewModels in
 * [viewModels] are cleared and their coroutines awaited, still inside the test and while the
 * test's `Dispatchers.Main` is set. Call `MainDispatcherRule.tearDown()` in `@AfterTest` as usual.
 */
fun runViewModelTest(viewModels: TestViewModelStore, testBody: suspend TestScope.() -> Unit): TestResult =
    runTest {
        try {
            testBody()
        } finally {
            withContext(NonCancellable) { viewModels.clear() }
        }
    }
