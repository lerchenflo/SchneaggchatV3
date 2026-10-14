package org.lerchenflo.schneaggchatv3mp.testutil

import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.lerchenflo.schneaggchatv3mp.fakes.FakeNotifier
import org.lerchenflo.schneaggchatv3mp.utilities.notifications.Notifier

/**
 * Starts a global Koin that only knows [notifier]. Some code under test still looks the
 * `Notifier` up with `KoinPlatform.getKoin()` instead of getting it injected
 * (`ChatSelectorViewModel`'s init, `NotificationManager`), see `plans/Todo.md`.
 *
 * Global like the objects of `resetGlobalState()`: call [stopTestKoin] in `@AfterTest`.
 */
fun startTestKoin(notifier: Notifier = FakeNotifier()) {
    stopKoin() // a test that failed before its @AfterTest may have left one running
    startKoin {
        modules(module { single<Notifier> { notifier } })
    }
}

fun stopTestKoin() = stopKoin()
