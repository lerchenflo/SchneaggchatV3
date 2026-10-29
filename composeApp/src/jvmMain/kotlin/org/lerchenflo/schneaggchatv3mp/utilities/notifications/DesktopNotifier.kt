package org.lerchenflo.schneaggchatv3mp.utilities.notifications

import androidx.compose.ui.window.Notification
import androidx.compose.ui.window.TrayState
import androidx.compose.ui.window.isTraySupported
import java.io.File
import java.nio.file.Files
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

private const val APP_NAME = "SchneaggchatV3"
private const val ICON_RESOURCE_PATH =
    "composeResources/schneaggchatv3mp.composeapp.generated.resources/drawable/schneaggchat_logo_v3.png"

/** notify-send / gdbus return right after their D-Bus call; anything longer means the bus is gone. */
private const val PROCESS_TIMEOUT_SECONDS = 2L

/**
 * @param trayState the state of the app's tray icon (main.kt): outside Linux, notifications show as
 * that icon's balloon, so no second tray icon is needed for them
 */
class DesktopNotifier(private val trayState: TrayState) : Notifier {

    private val isLinux: Boolean = System.getProperty("os.name")?.lowercase()?.contains("linux") == true

    /**
     * Linux only: [NotificationContent.id] -> id the desktop's notification server assigned to it.
     * Lets a re-shown notification replace the previous one instead of stacking, and lets
     * cancel* close it again (e.g. message notifications when that chat gets opened).
     */
    private val nativeIds = ConcurrentHashMap<Int, Int>()

    /** App logo copied out of the classpath so notify-send can reference it by path. Null when missing. */
    private val iconFile: File? by lazy { extractIconFile() }

    override suspend fun getToken(): String? = null // No FCM on desktop — skipped
    override suspend fun removeToken() = Unit // No FCM on desktop — skipped
    override suspend fun hasPermission(): Boolean = isLinux || isTraySupported

    override fun showLocalNotification(content: NotificationContent) {
        // Linux: libnotify first. java.awt.SystemTray reports itself as supported on most modern
        // desktops (KDE's xembedsniproxy, GNOME with an XEmbed tray extension, ...), but its
        // balloon is a bare AWT popup anchored to the tray icon's X window - which those proxies
        // keep hidden at (0,0), so the balloon lands in the top-left screen corner for a few
        // seconds and never reaches the desktop's notification center. notify-send talks to
        // org.freedesktop.Notifications and yields a real, dismissable notification.
        if (isLinux && showViaNotifySend(content)) return

        // Windows / macOS (and Linux without libnotify): the tray balloon maps to a native toast.
        if (showViaSystemTray(content)) return

        println("[Notifier.jvm] skipped notification: ${content.title} — ${content.body}")
    }

    // Only Linux notifications can be dismissed programmatically (via D-Bus); once shown, a
    // java.awt.TrayIcon balloon can't be, so on other platforms these are no-ops.
    override fun cancelNotification(id: Int) = closeNative(id)
    override fun cancelNotifications(ids: List<Int>) = ids.forEach { closeNative(it) }
    override fun cancelAllNotifications() = nativeIds.keys.toList().forEach { closeNative(it) }
    override fun cancelMessageNotifications(ids: List<Int>) = ids.forEach { closeNative(it) }

    private fun showViaNotifySend(content: NotificationContent): Boolean {
        val command = buildList {
            add("notify-send")
            add("--app-name=$APP_NAME")
            add("--print-id")
            iconFile?.let { add("--icon=${it.absolutePath}") }
            nativeIds[content.id]?.let { add("--replace-id=$it") }
            // End of options: a title or body starting with '-' must not be parsed as a flag.
            add("--")
            add(content.title)
            // The body is rendered as markup by most notification servers; the title is plain text.
            add(escapeMarkup(content.body))
        }
        return try {
            val process = ProcessBuilder(command).redirectErrorStream(true).start()
            if (!process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                println("[Notifier.jvm] notify-send timed out")
                return false
            }
            val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
            if (process.exitValue() != 0) {
                println("[Notifier.jvm] notify-send failed: $output")
                return false
            }
            output.toIntOrNull()?.let { nativeIds[content.id] = it }
            true
        } catch (e: Exception) {
            println("[Notifier.jvm] notify-send unavailable: ${e.message}")
            false
        }
    }

    private fun closeNative(id: Int) {
        val nativeId = nativeIds.remove(id) ?: return
        try {
            ProcessBuilder(
                "gdbus", "call", "--session",
                "--dest", "org.freedesktop.Notifications",
                "--object-path", "/org/freedesktop/Notifications",
                "--method", "org.freedesktop.Notifications.CloseNotification",
                nativeId.toString(),
            ).redirectErrorStream(true).start().also { process ->
                if (!process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS)) process.destroyForcibly()
            }
        } catch (e: Exception) {
            println("[Notifier.jvm] closing notification via gdbus failed: ${e.message}")
        }
    }

    private fun escapeMarkup(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

    /** Shown by the tray icon in main.kt, which only exists where [isTraySupported]. */
    private fun showViaSystemTray(content: NotificationContent): Boolean {
        if (!isTraySupported) return false
        trayState.sendNotification(Notification(content.title, content.body, Notification.Type.Info))
        return true
    }

    private fun extractIconFile(): File? = try {
        DesktopNotifier::class.java.classLoader?.getResourceAsStream(ICON_RESOURCE_PATH)?.use { input ->
            Files.createTempFile("schneaggchat_notification_icon", ".png").toFile().apply {
                deleteOnExit()
                outputStream().use { input.copyTo(it) }
            }
        }
    } catch (e: Exception) {
        println("[Notifier.jvm] could not extract notification icon: ${e.message}")
        null
    }
}
