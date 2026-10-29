package org.lerchenflo.schneaggchatv3mp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.TrayState
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.isTraySupported
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.mp.KoinPlatform
import org.lerchenflo.schneaggchatv3mp.app.App
import org.maplibre.compose.desktop.ProvideMapPresentationHost
import org.maplibre.compose.desktop.rememberAwtComposeMapPresentationHost
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.desktop_tray_open
import schneaggchatv3mp.composeapp.generated.resources.desktop_tray_quit
import schneaggchatv3mp.composeapp.generated.resources.schneaggchat_logo_v3
import java.awt.Desktop
import java.awt.Dimension
import java.awt.desktop.AppReopenedListener

private const val TRAY_LOGO_PATH = "files/schneaggchat_logo_v3_transparent.png"

fun main() = application {

    onAppStart()

    // Desktop has no push: the socket is the only way notifications arrive. So while a tray icon can
    // bring the window back, closing it only hides it and the app (socket included) keeps running.
    // Without a tray (e.g. GNOME without a tray extension) closing quits, as before.
    var windowVisible by remember { mutableStateOf(true) }
    // Entry point, like the Android Application class: resolved directly. DesktopNotifier sends its
    // notifications through the same state, so they show as this icon's balloon.
    val trayState = remember { KoinPlatform.getKoin().get<TrayState>() }
    // The transparent logo, like the Linux desktop icon (the drawable one has a solid background).
    // A raw file, so it is read once; until then (a few ms) there is no tray icon yet.
    val trayLogo by produceState<Result<ImageBitmap>?>(null) {
        value = runCatching { Res.readBytes(TRAY_LOGO_PATH).decodeToImageBitmap() }
    }
    // One size for every way the window appears (start, shown from the tray, hot reload) and the
    // size the user resized it to
    val windowState = rememberWindowState(size = DpSize(1600.dp, 1000.dp))

    val logo = trayLogo
    if (isTraySupported && logo != null) {
        val logoBitmap = logo.getOrNull()
        Tray(
            icon = if (logoBitmap != null) remember(logoBitmap) { BitmapPainter(logoBitmap) } else painterResource(Res.drawable.schneaggchat_logo_v3),
            state = trayState,
            tooltip = "SchneaggchatV3",
            onAction = { windowVisible = true }, // click (Windows) / double click on the icon
            menu = {
                Item(stringResource(Res.string.desktop_tray_open), onClick = { windowVisible = true })
                Item(stringResource(Res.string.desktop_tray_quit), onClick = ::exitApplication)
            }
        )
    }

    // macOS: a click on the Dock icon brings the hidden window back
    DisposableEffect(Unit) {
        val desktop = Desktop.getDesktop().takeIf {
            Desktop.isDesktopSupported() && it.isSupported(Desktop.Action.APP_EVENT_REOPENED)
        }
        val listener = AppReopenedListener { windowVisible = true }
        desktop?.addAppEventListener(listener)
        onDispose { desktop?.removeAppEventListener(listener) }
    }

    Window(
        onCloseRequest = { if (isTraySupported) windowVisible = false else exitApplication() },
        state = windowState,
        visible = windowVisible,
        title = "SchneaggchatV3 Desktop",
        icon = painterResource(Res.drawable.schneaggchat_logo_v3) // taskbar / window icon (tested on linux)

    ) {
        LaunchedEffect(Unit) {
            window.minimumSize = Dimension(200, 400)
        }

        BringToFrontWhenShown(visible = { windowVisible })

        //Every AWT window owns its own GPU context, so the map needs this window's host to render
        ProvideMapPresentationHost(host = rememberAwtComposeMapPresentationHost(window)) {
            App()
        }
    }
}

/**
 * Brings the window in front of other windows when it is shown again from the tray. Its own
 * composable on purpose: [visible] is read here only, so showing or hiding the window doesn't
 * recompose the whole window content.
 */
@Composable
private fun FrameWindowScope.BringToFrontWhenShown(visible: () -> Boolean) {
    val shown = visible()
    LaunchedEffect(shown) {
        if (shown) window.toFront()
    }
}


//TRay icon (Working solution but horror)
/*
package org.lerchenflo.schneaggchatv3mp

import androidx.compose.runtime.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.lerchenflo.schneaggchatv3mp.app.App
import java.awt.*
import java.awt.image.BufferedImage
import kotlin.concurrent.thread

private var isShuttingDown = false
private var persistentTrayIcon: TrayIcon? = null

fun main() = application(exitProcessOnExit = false) {

    onAppStart()

    // Create tray icon once
    DisposableEffect(Unit) {
        if (SystemTray.isSupported()) {
            val systemTray = SystemTray.getSystemTray()
            val traySize = systemTray.trayIconSize

            val image = BufferedImage(traySize.width, traySize.height, BufferedImage.TYPE_INT_ARGB).apply {
                createGraphics().apply {
                    color = Color.RED
                    fillRect(0, 0, traySize.width, traySize.height)
                    color = Color.WHITE
                    fillRect(traySize.width / 4, traySize.height / 4, traySize.width / 2, traySize.height / 2)
                    dispose()
                }
            }

            val popupMenu = PopupMenu()
            val exitItem = MenuItem("Exit")
            exitItem.addActionListener {
                isShuttingDown = true
                exitApplication()
            }
            popupMenu.add(exitItem)

            persistentTrayIcon = TrayIcon(image, "SchneaggchatV3 Desktop", popupMenu).apply {
                isImageAutoSize = true
            }

            systemTray.add(persistentTrayIcon)
        }

        // Watchdog thread
        val watchdog = thread(isDaemon = true) {
            while (!isShuttingDown) {
                Thread.sleep(200)
                if (SystemTray.isSupported() && persistentTrayIcon != null) {
                    val systemTray = SystemTray.getSystemTray()
                    if (!systemTray.trayIcons.contains(persistentTrayIcon)) {
                        try {
                            systemTray.add(persistentTrayIcon)
                        } catch (e: Exception) {
                            // Ignore
                        }
                    }
                }
            }
        }

        onDispose {
            isShuttingDown = true
            persistentTrayIcon?.let {
                if (SystemTray.isSupported()) {
                    SystemTray.getSystemTray().remove(it)
                }
            }
        }
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "SchneaggchatV3 Desktop",
    ) {
        window.setSize(1600, 1000)
        window.minimumSize = Dimension(400, 400)
        App()
    }
}
 */