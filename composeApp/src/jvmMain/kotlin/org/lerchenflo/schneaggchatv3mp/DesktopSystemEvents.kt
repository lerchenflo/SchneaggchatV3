package org.lerchenflo.schneaggchatv3mp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.lerchenflo.schneaggchatv3mp.app.AppLifecycleManager
import org.lerchenflo.schneaggchatv3mp.datasource.network.socket.SocketConnectionManager
import java.awt.Desktop
import java.awt.desktop.SystemSleepEvent
import java.awt.desktop.SystemSleepListener
import kotlin.time.Duration.Companion.seconds

/**
 * Hooks into the operating system's app events, each only where the platform delivers it:
 * - system sleep and wake (macOS, Windows): reported to [AppLifecycleManager], which closes the
 *   socket before sleep and reconnects and syncs after wake;
 * - the system's quit request (macOS Cmd+Q, Dock "Quit", logout, shutdown): [onQuitRequest] gets a
 *   callback to run once the app is ready to end, the system quits only then;
 * - logout and shutdown elsewhere (Windows, Linux) end the JVM: a shutdown hook still closes the
 *   socket, so the server marks the user offline at once instead of after its keepalive sweep.
 */
@Composable
internal fun DesktopSystemEvents(
    socketConnectionManager: SocketConnectionManager,
    onQuitRequest: (performQuit: () -> Unit) -> Unit
) {
    val currentOnQuitRequest by rememberUpdatedState(onQuitRequest)

    DisposableEffect(Unit) {
        val desktop = Desktop.getDesktop().takeIf { Desktop.isDesktopSupported() }

        val sleepListener = object : SystemSleepListener {
            override fun systemAboutToSleep(e: SystemSleepEvent) = AppLifecycleManager.notifySystemSleep()
            override fun systemAwoke(e: SystemSleepEvent) = AppLifecycleManager.notifySystemWake()
        }
        val listensToSleep = desktop?.isSupported(Desktop.Action.APP_EVENT_SYSTEM_SLEEP) == true
        if (listensToSleep) desktop.addAppEventListener(sleepListener)

        val handlesQuit = desktop?.isSupported(Desktop.Action.APP_QUIT_HANDLER) == true
        if (handlesQuit) {
            desktop.setQuitHandler { _, response -> currentOnQuitRequest { response.performQuit() } }
        }

        val shutdownHook = Thread { closeSocketBeforeExit(socketConnectionManager) }
        Runtime.getRuntime().addShutdownHook(shutdownHook)

        onDispose {
            if (listensToSleep) desktop.removeAppEventListener(sleepListener)
            if (handlesQuit) desktop.setQuitHandler(null)
            // Throws while the JVM is already shutting down; the hook then runs anyway
            runCatching { Runtime.getRuntime().removeShutdownHook(shutdownHook) }
        }
    }
}

/** Bounded, so a dead network never delays the end of the process. A closed socket returns at once. */
private fun closeSocketBeforeExit(socketConnectionManager: SocketConnectionManager) {
    runCatching {
        runBlocking { withTimeoutOrNull(1.seconds) { socketConnectionManager.close() } }
    }
}
