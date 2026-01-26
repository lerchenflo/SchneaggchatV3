package org.lerchenflo.schneaggchatv3mp

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.lerchenflo.schneaggchatv3mp.app.App
import java.awt.Dimension

fun main() = application {

    onAppStart()


    Window(
        onCloseRequest = ::exitApplication,
        title = "SchneaggchatV3 Desktop",

    ) {
        window.setSize(1600, 1000)
        window.minimumSize = Dimension(400, 400)
        App()
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