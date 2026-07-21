package org.lerchenflo.schneaggchatv3mp.utilities.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform
import org.lerchenflo.schneaggchatv3mp.app.AppLifecycleManager
import org.lerchenflo.schneaggchatv3mp.app.OpenChatTracker
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.logging.LoggingRepository
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.utilities.LanguageService
import org.lerchenflo.schneaggchatv3mp.utilities.wake.WakeAlarmService

class AppFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                KoinPlatform.getKoin().get<AppRepository>().setNotificationToken(token)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val decoded = PayloadDecoder.decode(message.data) ?: return

        //A wake bypasses the whole notification pipeline - the alarm service shows its own
        //foreground notification and plays the alarm itself.
        if (decoded is DecodedNotification.Wake) {
            handleWake(decoded)
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val languageService = KoinPlatform.getKoin().get<LanguageService>()
                languageService.applyLanguage(languageService.getCurrentLanguage())

                val notifier = KoinPlatform.getKoin().get<Notifier>()
                val prefs = KoinPlatform.getKoin().get<Preferencemanager>()

                val encryptionKey = prefs.getEncryptionKey().ifEmpty { null }
                val content = resolveLocalizedContent(decoded, encryptionKey) ?: return@launch

                val suppressNotification = AppLifecycleManager.isAppInForeground
                    && decoded is DecodedNotification.Message //Suppress only messages
                    && decoded.senderId.isNotEmpty() && decoded.receiverId.isNotEmpty()
                    && run {
                        //Suppress only when the chat is currently open on screen
                        OpenChatTracker.isChatOpen(chatId = decoded.senderId, isGroup = decoded.groupMessage) //Single messages
                                || OpenChatTracker.isChatOpen(chatId = decoded.receiverId, isGroup = decoded.groupMessage) //Group messages
                    }

                if (!suppressNotification) {
                    notifier.showLocalNotification(content)
                }

                if (decoded is DecodedNotification.Message) {
                    val appRepository = KoinPlatform.getKoin().get<AppRepository>()
                    SessionCache.login(tokens = prefs.getTokens(), developer = false)
                    appRepository.messageIdSync()
                }
            }.onFailure { e ->
                println("[AppFirebaseMessagingService] Error handling push: ${e.message}")
            }
        }
    }

    /**
     * Start the alarm immediately, then log. The service start must not wait on the database:
     * a high priority FCM message only buys a short background start window, and missing it
     * throws ForegroundServiceStartNotAllowedException.
     */
    private fun handleWake(wake: DecodedNotification.Wake) {
        runCatching {
            WakeAlarmService.start(
                context = this,
                senderName = wake.senderName,
                reason = wake.reason,
                groupName = wake.groupName,
                wokenUserCount = wake.wokenUserCount,
                wokenDeviceCount = wake.wokenDeviceCount,
            )
        }.onFailure { e ->
            println("[AppFirebaseMessagingService] Could not start wake alarm: ${e.message}")
        }

        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val loggingRepository = KoinPlatform.getKoin().get<LoggingRepository>()
                val who = if (wake.isGroupWake) "${wake.senderName} (${wake.groupName})" else wake.senderName
                loggingRepository.logInfo("Woken by $who: ${wake.reason}")
            }
        }
    }
}
