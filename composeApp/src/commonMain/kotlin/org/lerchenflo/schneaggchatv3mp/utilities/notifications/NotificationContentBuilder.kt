package org.lerchenflo.schneaggchatv3mp.utilities.notifications

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import org.jetbrains.compose.resources.getString
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageType
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils
import org.lerchenflo.schneaggchatv3mp.utilities.CryptoUtil
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.audio
import schneaggchatv3mp.composeapp.generated.resources.image
import schneaggchatv3mp.composeapp.generated.resources.new_friend_accepted_noti
import schneaggchatv3mp.composeapp.generated.resources.new_friend_accepted_noti_body
import schneaggchatv3mp.composeapp.generated.resources.new_friend_request_noti
import schneaggchatv3mp.composeapp.generated.resources.new_friend_request_noti_body
import schneaggchatv3mp.composeapp.generated.resources.new_message_noti_group_title
import schneaggchatv3mp.composeapp.generated.resources.new_message_noti_single_title
import schneaggchatv3mp.composeapp.generated.resources.poll

object NotificationContentBuilder {

    private val json = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "type"
    }

    suspend fun build(
        response: NetworkUtils.NotificationResponse,
        encryptionKey: String,
    ): NotificationContent? = when (response) {
        is NetworkUtils.NotificationResponse.MessageNotificationResponse -> buildMessage(response, encryptionKey)
        is NetworkUtils.NotificationResponse.FriendRequestNotificationResponse -> buildFriendRequest(response)
        is NetworkUtils.NotificationResponse.SystemNotificationResponse -> buildSystem(response)
    }

    private suspend fun buildMessage(
        r: NetworkUtils.NotificationResponse.MessageNotificationResponse,
        encryptionKey: String,
    ): NotificationContent {
        val title = if (r.groupMessage) {
            getString(Res.string.new_message_noti_group_title, r.senderName, r.groupName)
        } else {
            getString(Res.string.new_message_noti_single_title, r.senderName)
        }

        val body = when (r.messageType) {
            MessageType.TEXT -> if (encryptionKey.isNotEmpty()) {
                try { CryptoUtil.decrypt(r.encodedContent, encryptionKey) } catch (_: Exception) { "..." }
            } else "..."
            MessageType.IMAGE -> getString(Res.string.image)
            MessageType.POLL -> getString(Res.string.poll)
            MessageType.AUDIO -> getString(Res.string.audio)
        }

        return NotificationContent(
            id = r.msgId.toNotificationId(),
            title = title,
            body = body,
            channelId = NotificationChannels.MESSAGES,
        )
    }

    private suspend fun buildFriendRequest(
        r: NetworkUtils.NotificationResponse.FriendRequestNotificationResponse,
    ): NotificationContent {
        val title: String
        val body: String
        if (r.accepted) {
            title = getString(Res.string.new_friend_accepted_noti)
            body = getString(Res.string.new_friend_accepted_noti_body, r.requesterName)
        } else {
            title = getString(Res.string.new_friend_request_noti, r.requesterName)
            body = getString(Res.string.new_friend_request_noti_body, r.requesterName)
        }
        return NotificationContent(
            id = r.requesterId.toNotificationId(),
            title = title,
            body = body,
            channelId = NotificationChannels.FRIEND_REQUESTS,
        )
    }

    private fun buildSystem(r: NetworkUtils.NotificationResponse.SystemNotificationResponse): NotificationContent =
        NotificationContent(
            id = "system".toNotificationId(),
            title = r.title,
            body = r.message,
            channelId = NotificationChannels.SYSTEM,
        )

    fun fromMap(data: Map<String, Any?>, encryptionKey: String): NotificationContent? {
        val response = parseMap(data) ?: return null
        return runBlocking { build(response, encryptionKey) }
    }

    fun parseMap(data: Map<String, Any?>): NetworkUtils.NotificationResponse? {
        return try {
            val jsonObject = JsonObject(
                data.mapValues { (_, v) ->
                    when (v) {
                        is String -> JsonPrimitive(v)
                        is Boolean -> JsonPrimitive(v)
                        is Number -> JsonPrimitive(v)
                        null -> JsonNull
                        else -> JsonPrimitive(v.toString())
                    }
                }
            )
            json.decodeFromJsonElement<NetworkUtils.NotificationResponse>(jsonObject)
        } catch (_: Exception) {
            null
        }
    }
}
