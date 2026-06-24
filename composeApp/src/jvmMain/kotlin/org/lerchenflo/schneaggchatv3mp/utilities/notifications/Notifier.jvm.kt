package org.lerchenflo.schneaggchatv3mp.utilities.notifications

import java.awt.Image
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

private const val APP_NAME = "SchneaggchatV3"
private const val TRAY_ICON_RESOURCE_PATH =
    "composeResources/schneaggchatv3mp.composeapp.generated.resources/drawable/schneaggchat_logo_v3.png"

actual class Notifier {

    // Lazily created and reused — a TrayIcon can only be added to the SystemTray once.
    private var trayIcon: TrayIcon? = null

    actual suspend fun getToken(): String? = null // No FCM on desktop — skipped
    actual suspend fun removeToken() = Unit // No FCM on desktop — skipped
    actual suspend fun hasPermission(): Boolean = SystemTray.isSupported() || isLinux()

    actual fun showLocalNotification(content: NotificationContent) {
        if (SystemTray.isSupported()) {
            try {
                val icon = getOrCreateTrayIcon()
                icon.displayMessage(content.title, content.body, TrayIcon.MessageType.INFO)
                return
            } catch (e: Exception) {
                println("[Notifier.jvm] SystemTray notification failed: ${e.message}")
            }
        }

        // Many Linux desktops (e.g. Cinnamon, some GNOME setups) don't implement the
        // legacy XEmbed tray protocol java.awt.SystemTray relies on, but still support
        // libnotify-based notifications via notify-send.
        if (isLinux() && showViaNotifySend(content)) return

        println("[Notifier.jvm] skipped notification: ${content.title} — ${content.body}")
    }

    private fun isLinux(): Boolean = System.getProperty("os.name")?.lowercase()?.contains("linux") == true

    private fun showViaNotifySend(content: NotificationContent): Boolean {
        return try {
            ProcessBuilder("notify-send", "--app-name=$APP_NAME", content.title, content.body).start()
            true
        } catch (e: Exception) {
            println("[Notifier.jvm] notify-send fallback failed: ${e.message}")
            false
        }
    }

    // Per-id cancellation isn't supported by java.awt.TrayIcon — once shown, a tray
    // notification can't be dismissed programmatically, so these are skipped.
    actual fun cancelNotification(id: Int) = Unit
    actual fun cancelNotifications(ids: List<Int>) = Unit
    actual fun cancelAllNotifications() = Unit
    actual fun cancelMessageNotifications(ids: List<Int>) = Unit

    private fun getOrCreateTrayIcon(): TrayIcon {
        trayIcon?.let { return it }

        val icon = TrayIcon(loadTrayImage(), APP_NAME).apply {
            isImageAutoSize = true
        }
        SystemTray.getSystemTray().add(icon)
        trayIcon = icon
        return icon
    }

    private fun loadTrayImage(): Image {
        val classLoaderImage = Thread.currentThread().contextClassLoader
            ?.getResourceAsStream(TRAY_ICON_RESOURCE_PATH)
            ?.use { ImageIO.read(it) }

        if (classLoaderImage != null) return classLoaderImage

        // Fallback: a small solid-color placeholder, in case the resource can't be loaded.
        return BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB).apply {
            createGraphics().apply {
                color = java.awt.Color.WHITE
                fillRect(0, 0, 16, 16)
                dispose()
            }
        }
    }
}
