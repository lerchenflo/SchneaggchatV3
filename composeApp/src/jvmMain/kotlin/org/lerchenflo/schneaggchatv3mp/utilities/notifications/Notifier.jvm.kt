package org.lerchenflo.schneaggchatv3mp.utilities.notifications

actual class Notifier {
    actual fun show(content: NotificationContent) {}
    actual suspend fun getToken(): String = ""
    actual suspend fun deleteToken() {}
    actual fun initialize() {}
    actual fun cancel(id: Int) {}
    actual fun cancelAll() {}
}
