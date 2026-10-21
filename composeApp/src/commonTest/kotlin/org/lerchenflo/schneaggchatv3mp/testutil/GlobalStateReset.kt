package org.lerchenflo.schneaggchatv3mp.testutil

import org.lerchenflo.schneaggchatv3mp.app.AppLifecycleManager
import org.lerchenflo.schneaggchatv3mp.app.OpenChatTracker
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.utilities.IncomingDataManager
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager

/**
 * Puts the app-wide singletons `SessionCache`, `SnackbarManager`, `AppLifecycleManager`,
 * `IncomingDataManager` and `OpenChatTracker` back to their initial state (plan D19). Call it from `@BeforeTest` in
 * every test that touches one of them.
 *
 * They are global, so tests that use them must not run in parallel. Injecting them instead is a
 * follow-up in `plans/Todo.md`.
 */
fun resetGlobalState() {
    SessionCache.logout()
    SessionCache.updateOnline(true)

    SnackbarManager.resetForTest()

    AppLifecycleManager.updateAppForegroundState(false)
    AppLifecycleManager.consumePendingNotificationOpen()
    AppLifecycleManager.resetStartupRoutingDoneForTest()

    IncomingDataManager.clearAllData()

    OpenChatTracker.current.value?.let { OpenChatTracker.onChatClosed(it.chatId, it.isGroup) }
}
