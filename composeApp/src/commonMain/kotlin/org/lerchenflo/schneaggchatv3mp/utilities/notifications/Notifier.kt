package org.lerchenflo.schneaggchatv3mp.utilities.notifications

expect class Notifier {
    fun show(content: NotificationContent)
    suspend fun getToken(): String
    suspend fun deleteToken()
    fun initialize()
    fun cancel(id: Int)
    fun cancelAll()
}
