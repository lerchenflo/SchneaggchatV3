package org.lerchenflo.schneaggchatv3mp.utilities

import kotlinx.coroutines.runBlocking
import org.koin.mp.KoinPlatform
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.utilities.notifications.NotificationContent
import org.lerchenflo.schneaggchatv3mp.utilities.notifications.Notifier
import org.lerchenflo.schneaggchatv3mp.utilities.notifications.toNotificationContent
import kotlin.math.absoluteValue

object NotificationManager {

    private var initialized = false

    fun initialize() {
        if (initialized) return
        initialized = true
        println("NotificationManager initialized")
    }

    sealed interface NotiId {
        val asInt: Int

        data class Integ(val value: Int) : NotiId {
            override val asInt: Int get() = value
        }

        data class HexString(val value: String) : NotiId {
            override val asInt: Int get() = value.hashCode().absoluteValue
        }
    }

    enum class NotiIdType(val baseId: Int) {
        ERROR(1),
        SERVERMESSAGE(2),
        FRIEND_REQUEST(3)
    }

    fun showNotification(titletext: String, bodytext: String, notiId: NotiId) {
        notifier().showLocalNotification(NotificationContent(notiId.asInt, titletext, bodytext))
    }

    fun showNotification(message: Message) {
        runBlocking {
            notifier().showLocalNotification(message.toNotificationContent())
        }
    }

    fun removeNotification(notiId: Int? = null) {
        if (notiId == null) {
            NotiIdType.entries.forEach { notifier().cancelNotification(it.baseId) }
        } else {
            notifier().cancelNotification(notiId)
        }
    }

    fun removeNotifications(ids: List<Int>) {
        notifier().cancelNotifications(ids)
    }

    suspend fun getToken(): String = notifier().getToken() ?: ""

    suspend fun removeToken() {
        notifier().removeToken()
    }

    private fun notifier(): Notifier = KoinPlatform.getKoin().get()
}
