package org.lerchenflo.schneaggchatv3mp.utilities.notifications

import kotlin.math.absoluteValue

interface Notifier {
    suspend fun getToken(): String?
    suspend fun removeToken()
    suspend fun hasPermission(): Boolean
    fun showLocalNotification(content: NotificationContent)
    fun cancelNotification(id: Int)
    fun cancelNotifications(ids: List<Int>)
    fun cancelAllNotifications()
    fun cancelMessageNotifications(ids: List<Int>)

    /**
     * Everything shown for the chat [chatId] (the group id for group chats, the other user's id
     * otherwise): its messages and the reactions in it. By default only the chat's message
     * notification, keyed by the chat id (see Message.toNotificationContent).
     */
    fun cancelChatNotifications(chatId: String) {
        cancelMessageNotifications(listOf(chatId.hashCode().absoluteValue))
    }
}
