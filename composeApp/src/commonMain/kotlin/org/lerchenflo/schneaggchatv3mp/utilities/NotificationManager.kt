package org.lerchenflo.schneaggchatv3mp.utilities

import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.koin.mp.KoinPlatform
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageType
import org.lerchenflo.schneaggchatv3mp.utilities.notifications.NotificationChannels
import org.lerchenflo.schneaggchatv3mp.utilities.notifications.NotificationContent
import org.lerchenflo.schneaggchatv3mp.utilities.notifications.Notifier
import org.lerchenflo.schneaggchatv3mp.utilities.notifications.toNotificationId
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.audio
import schneaggchatv3mp.composeapp.generated.resources.image
import schneaggchatv3mp.composeapp.generated.resources.poll
import kotlin.math.absoluteValue

object NotificationManager {

    private var initialized = false

    sealed interface NotiId {
        val asInt: Int

        data class Integ(val value: Int) : NotiId {
            override val asInt: Int get() = value
        }

        data class HexString(val value: String) : NotiId {
            override val asInt: Int get() = value.toNotificationId()
        }
    }

    enum class NotiIdType(val baseId: Int) {
        ERROR(1),
        SERVERMESSAGE(2),
        FRIEND_REQUEST(3)
    }

    fun initialize() {
        if (initialized) return
        initialized = true
        notifier().initialize()
    }

    fun showNotification(titletext: String, bodytext: String, notiId: NotiId) {
        notifier().show(
            NotificationContent(
                id = notiId.asInt,
                title = titletext,
                body = bodytext,
                channelId = NotificationChannels.MESSAGES,
            )
        )
    }

    fun showNotification(message: Message) {
        runBlocking {
            val body = when (message.msgType) {
                MessageType.TEXT -> message.content
                MessageType.IMAGE -> getString(Res.string.image)
                MessageType.POLL -> getString(Res.string.poll)
                MessageType.AUDIO -> getString(Res.string.audio)
            }
            showNotification(message.senderAsString, body, NotiId.HexString(message.id!!))
        }
    }

    fun removeNotification(notiId: Int? = null) {
        if (notiId == null) {
            notifier().cancelAll()
        } else {
            notifier().cancel(notiId)
        }
    }

    fun removeNotifications(ids: List<Int>) {
        ids.forEach { notifier().cancel(it) }
    }

    suspend fun getToken(): String = notifier().getToken()

    suspend fun removeToken() = notifier().deleteToken()

    private fun notifier(): Notifier = KoinPlatform.getKoin().get()
}
