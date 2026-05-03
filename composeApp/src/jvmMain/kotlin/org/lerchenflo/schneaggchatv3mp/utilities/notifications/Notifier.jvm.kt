package org.lerchenflo.schneaggchatv3mp.utilities.notifications

actual class Notifier {
    actual suspend fun getToken(): String? = null
    actual suspend fun removeToken() = Unit
    actual suspend fun hasPermission(): Boolean = false
    actual fun showLocalNotification(content: NotificationContent) {
        println("[Notifier.jvm] skipped notification: ${content.title} — ${content.body}")
    }
    actual fun cancelNotification(id: Int) = Unit
    actual fun cancelNotifications(ids: List<Int>) = Unit
}
