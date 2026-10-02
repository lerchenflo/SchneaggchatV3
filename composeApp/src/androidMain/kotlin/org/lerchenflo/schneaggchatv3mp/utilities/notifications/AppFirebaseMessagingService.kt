package org.lerchenflo.schneaggchatv3mp.utilities.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.koin.mp.KoinPlatform
import org.lerchenflo.schneaggchatv3mp.app.ApplicationScope
import org.lerchenflo.schneaggchatv3mp.app.OpenChatTracker
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.logging.LoggingRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.MessageRepository
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.utilities.LanguageService
import org.lerchenflo.schneaggchatv3mp.utilities.wake.WakeAlarmService
import kotlin.time.Duration.Companion.seconds

class AppFirebaseMessagingService : FirebaseMessagingService() {

    private companion object {
        //A high priority FCM message buys roughly 10s of execution, stay well inside it
        val NOTIFICATION_TIMEOUT = 8.seconds
    }

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

        //Blocking on purpose: onMessageReceived runs on a worker thread, and returning hands the
        //process back to the OS - a fire-and-forget coroutine could be frozen or killed before
        //the notification is posted. The seen-check below can wait up to SEEN_CONFIRM_DELAY.
        runBlocking(Dispatchers.IO) {
            runCatching {
                withTimeout(NOTIFICATION_TIMEOUT) { showNotification(decoded) }
            }.onFailure { e -> logPushError(e) }
        }

        //The provisional row only speeds up an open chat, a later sync brings the message anyway
        if (decoded is DecodedNotification.Message) {
            KoinPlatform.getKoin().get<ApplicationScope>().launch {
                runCatching {
                    val prefs = KoinPlatform.getKoin().get<Preferencemanager>()
                    if (SessionCache.loginIfValid(tokens = prefs.getTokens(), developer = false)) {
                        // Instantly upsert a provisional row so an already-open (or now-opened)
                        // chat shows the message before the sync completes.
                        KoinPlatform.getKoin().get<AppRepository>().applyPushMessage(decoded)
                    }
                }.onFailure { e -> logPushError(e) }
            }
        }
    }

    private suspend fun showNotification(decoded: DecodedNotification) {
        val languageService = KoinPlatform.getKoin().get<LanguageService>()
        languageService.applyLanguage(languageService.getCurrentLanguage())

        //The server pushes a message the socket already delivered when the ack came too late -
        //it is stored and was notified already. Not for reactions: their msgId is the existing
        //message they react to.
        if (decoded is DecodedNotification.Message && !decoded.reaction &&
            KoinPlatform.getKoin().get<MessageRepository>().getMessageById(decoded.msgId) != null) {
            return
        }

        val content = resolveLocalizedContent(decoded) ?: return

        //Suppress only messages, and only when the chat they belong to is on screen and
        //still is after the confirm delay - not while the user is leaving the app
        val suppressNotification = decoded is DecodedNotification.Message
            && decoded.chatTargetId?.let { OpenChatTracker.isSeenAfterConfirmDelay(chatId = it, isGroup = decoded.groupMessage) } == true

        if (!suppressNotification) {
            KoinPlatform.getKoin().get<Notifier>().showLocalNotification(content)
        }
    }

    private fun logPushError(e: Throwable) {
        println("[AppFirebaseMessagingService] Error handling push: ${e.message}")
        KoinPlatform.getKoin().get<ApplicationScope>().launch {
            runCatching {
                KoinPlatform.getKoin().get<LoggingRepository>().logError("Handling push failed: ${e::class.simpleName}: ${e.message}")
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
