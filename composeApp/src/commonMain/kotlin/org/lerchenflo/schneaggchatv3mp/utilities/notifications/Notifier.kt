package org.lerchenflo.schneaggchatv3mp.utilities.notifications

expect class Notifier {
    suspend fun getToken(): String?
    suspend fun removeToken()
    suspend fun hasPermission(): Boolean
    fun showLocalNotification(content: NotificationContent)
    fun cancelNotification(id: Int)
    fun cancelNotifications(ids: List<Int>)
}
