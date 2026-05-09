package org.lerchenflo.schneaggchatv3mp.utilities.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.koin.mp.KoinPlatform
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.logging.LoggingRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageType
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.utilities.CryptoUtil
import org.lerchenflo.schneaggchatv3mp.utilities.LanguageService
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.audio
import schneaggchatv3mp.composeapp.generated.resources.image
import schneaggchatv3mp.composeapp.generated.resources.friend_birthday_noti_body
import schneaggchatv3mp.composeapp.generated.resources.friend_birthday_noti_title
import schneaggchatv3mp.composeapp.generated.resources.new_friend_accepted_noti
import schneaggchatv3mp.composeapp.generated.resources.new_friend_accepted_noti_body
import schneaggchatv3mp.composeapp.generated.resources.new_friend_request_noti
import schneaggchatv3mp.composeapp.generated.resources.new_friend_request_noti_body
import schneaggchatv3mp.composeapp.generated.resources.own_birthday_noti_body
import schneaggchatv3mp.composeapp.generated.resources.own_birthday_noti_title
import schneaggchatv3mp.composeapp.generated.resources.poll
import schneaggchatv3mp.composeapp.generated.resources.you_have_new_messages
import org.lerchenflo.schneaggchatv3mp.utilities.NotificationManager

class AppFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                KoinPlatform.getKoin().get<AppRepository>().setNotificationToken(token)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val decoded = PayloadDecoder.decode(data) ?: return

        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val languageService = KoinPlatform.getKoin().get<LanguageService>()
                languageService.applyLanguage(languageService.getCurrentLanguage())

                val notifier = KoinPlatform.getKoin().get<Notifier>()
                val logger = KoinPlatform.getKoin().get<LoggingRepository>()

                when (decoded) {
                    is DecodedNotification.Message -> {
                        val prefs = KoinPlatform.getKoin().get<Preferencemanager>()
                        val encryptionKey = prefs.getEncryptionKey()

                        val body = if (encryptionKey.isNotEmpty()) {
                            runCatching {
                                when (decoded.messageType) {
                                    MessageType.TEXT -> CryptoUtil.decrypt(decoded.encodedContent, encryptionKey)
                                    MessageType.IMAGE -> getString(Res.string.image)
                                    MessageType.AUDIO -> getString(Res.string.audio)
                                    MessageType.POLL  -> getString(Res.string.poll)
                                }
                            }.getOrElse { getString(Res.string.you_have_new_messages) }
                        } else {
                            getString(Res.string.you_have_new_messages)
                        }

                        val msg = Message(
                            msgType = decoded.messageType,
                            content = body,
                            senderId = "",
                            receiverId = "",
                            groupMessage = decoded.groupMessage,
                            senderAsString = decoded.senderName,
                            myMessage = false,
                            readByMe = false,
                            readers = emptyList(),
                            id = decoded.msgId,
                        )
                        notifier.showLocalNotification(
                            msg.toNotificationContent(
                                fallbackGroupName = decoded.groupName.ifEmpty { null }
                            )
                        )

                        val appRepository = KoinPlatform.getKoin().get<AppRepository>()
                        val preferenceManager = KoinPlatform.getKoin().get<Preferencemanager>()
                        SessionCache.login(tokens = preferenceManager.getTokens(), developer = false)
                        appRepository.messageIdSync()
                    }

                    is DecodedNotification.FriendRequest -> {
                        val title: String
                        val body: String
                        if (decoded.accepted) {
                            title = getString(Res.string.new_friend_accepted_noti)
                            body = getString(Res.string.new_friend_accepted_noti_body, decoded.requesterName)
                        } else {
                            title = getString(Res.string.new_friend_request_noti, decoded.requesterName)
                            body = getString(Res.string.new_friend_request_noti_body, decoded.requesterName)
                        }
                        notifier.showLocalNotification(
                            NotificationContent(
                                id = NotificationManager.NotiIdType.FRIEND_REQUEST.baseId,
                                title = title,
                                body = body,
                            )
                        )
                    }

                    is DecodedNotification.System -> {
                        notifier.showLocalNotification(
                            NotificationContent(
                                id = NotificationManager.NotiIdType.SERVERMESSAGE.baseId,
                                title = decoded.title,
                                body = decoded.message,
                            )
                        )
                    }

                    is DecodedNotification.Birthday -> {
                        val title: String
                        val body: String
                        if (decoded.ownBirthday) {
                            title = getString(Res.string.own_birthday_noti_title)
                            body = getString(Res.string.own_birthday_noti_body)
                        } else {
                            title = getString(Res.string.friend_birthday_noti_title, decoded.birthdayUserName)
                            body = getString(Res.string.friend_birthday_noti_body)
                        }
                        notifier.showLocalNotification(
                            NotificationContent(
                                id = NotificationManager.NotiIdType.BIRTHDAY.baseId,
                                title = title,
                                body = body,
                            )
                        )
                    }
                }
            }.onFailure { e ->
                println("[AppFirebaseMessagingService] Error handling push: ${e.message}")
            }
        }
    }
}
