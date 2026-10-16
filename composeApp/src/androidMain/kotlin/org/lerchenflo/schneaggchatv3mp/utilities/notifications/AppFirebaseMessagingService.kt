package org.lerchenflo.schneaggchatv3mp.utilities.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.jetbrains.compose.resources.getString
import org.koin.mp.KoinPlatform
import org.lerchenflo.schneaggchatv3mp.app.ApplicationScope
import org.lerchenflo.schneaggchatv3mp.app.OpenChatTracker
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.domain.DataSyncCoordinator
import org.lerchenflo.schneaggchatv3mp.app.domain.Logger
import org.lerchenflo.schneaggchatv3mp.login.domain.AuthRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageRepository
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.utilities.LanguageService
import org.lerchenflo.schneaggchatv3mp.utilities.wake.WakeAlarmService
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.wake_notification_title
import schneaggchatv3mp.composeapp.generated.resources.wake_notification_title_group
import schneaggchatv3mp.composeapp.generated.resources.you_have_new_messages
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.seconds

class AppFirebaseMessagingService : FirebaseMessagingService() {

    private companion object {
        //A high priority FCM message buys roughly 10s of execution, stay well inside it
        val NOTIFICATION_TIMEOUT = 8.seconds
    }

    override fun onNewToken(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                //Before login there is no session to register the token with - the login uploads it then
                if (!restoreSession()) {
                    logInfo("New notification token while logged out, it is uploaded at the next login")
                    return@launch
                }
                KoinPlatform.getKoin().get<AuthRepository>().setNotificationToken(token)
            }.onFailure { e -> logError("Uploading the new notification token failed", e) }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val decoded = PayloadDecoder.decode(message.data)
        if (decoded == null) {
            //Unknown or incomplete payload (newer server, missing id) - nothing can be shown, so at least log it
            runBlocking(Dispatchers.IO) {
                logError("Push dropped, its payload could not be read (_class=${message.data["_class"]}, keys=${message.data.keys.sorted()})")
            }
            return
        }

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
            //Set once the notification was posted or deliberately skipped - a failure after that
            //point must not post a second, fallback notification
            val handled = AtomicBoolean(false)
            runCatching {
                withTimeout(NOTIFICATION_TIMEOUT) { showNotification(decoded, handled) }
            }.onFailure { e ->
                logError("Handling push failed", e)
                if (!handled.get()) showFallbackNotification(decoded)
            }
        }

        //The provisional row only speeds up an open chat, a later sync brings the message anyway
        if (decoded is DecodedNotification.Message) {
            KoinPlatform.getKoin().get<ApplicationScope>().launch {
                runCatching {
                    if (restoreSession()) {
                        // Instantly upsert a provisional row so an already-open (or now-opened)
                        // chat shows the message before the sync completes.
                        KoinPlatform.getKoin().get<MessageRepository>().applyPushMessage(decoded)
                    }
                }.onFailure { e -> logError("Storing the pushed message failed", e) }
            }
        }
    }

    /**
     * FCM dropped pushes it could not deliver (device offline for long, too many waiting). The docs
     * ask for a full sync; a generic notification tells the user that something is waiting.
     */
    override fun onDeletedMessages() {
        runBlocking(Dispatchers.IO) {
            logError("FCM dropped pending pushes, showing a generic notification and syncing")
            try {
                KoinPlatform.getKoin().get<Notifier>().showLocalNotification(
                    NotificationContent(
                        id = "fcm_deleted_messages".hashCode().absoluteValue,
                        title = NOTIFICATION_FALLBACK_TITLE,
                        body = runCatching { getString(Res.string.you_have_new_messages) }.getOrDefault(""),
                        kind = NotificationKind.MESSAGE,
                    )
                )
            } catch (e: Throwable) {
                logError("Notification about the dropped pushes failed", e)
            }
        }

        KoinPlatform.getKoin().get<ApplicationScope>().launch {
            runCatching {
                if (restoreSession()) {
                    KoinPlatform.getKoin().get<DataSyncCoordinator>().dataSync(reason = "fcmDeletedMessages")
                }
            }.onFailure { e -> logError("Sync after the dropped pushes failed", e) }
        }
    }

    private suspend fun showNotification(decoded: DecodedNotification, handled: AtomicBoolean) {
        val languageService = KoinPlatform.getKoin().get<LanguageService>()
        languageService.applyLanguage(languageService.getCurrentLanguage())

        //A push can start a fresh process (app killed) - the session is needed to download a
        //missing profile picture and to tell our own id apart from others
        restoreSession()

        //The server pushes a message the socket already delivered when the ack came too late -
        //it is stored and was notified already. Not for reactions: their msgId is the existing
        //message they react to.
        if (decoded is DecodedNotification.Message && !decoded.reaction &&
            KoinPlatform.getKoin().get<MessageRepository>().getMessageById(decoded.msgId) != null) {
            handled.set(true)
            logInfo("Push for message ${decoded.msgId} skipped, the message is already stored (socket or sync was first)")
            return
        }

        val content = resolveLocalizedContent(decoded)
        if (content == null) {
            handled.set(true)
            logInfo("Push ${decoded::class.simpleName} has nothing to show (e.g. a system message)")
            return
        }

        //Suppress only messages, and only when the chat they belong to is on screen and
        //still is after the confirm delay - not while the user is leaving the app
        val suppressNotification = decoded is DecodedNotification.Message
            && decoded.chatTargetId?.let { OpenChatTracker.isSeenAfterConfirmDelay(chatId = it, isGroup = decoded.groupMessage) } == true

        if (suppressNotification) {
            handled.set(true)
            logDebug("Push for an open chat, no notification")
            return
        }

        //Picture of whoever the notification is about, downloaded first if this device doesn't have it yet
        ensureProfilePictureForNotification(content.avatarUserId ?: content.senderId)

        handled.set(true)
        KoinPlatform.getKoin().get<Notifier>().showLocalNotification(content)
    }

    /** Plain notification from the raw push fields after the normal path failed or timed out. Never throws. */
    private suspend fun showFallbackNotification(decoded: DecodedNotification) {
        try {
            val content = resolveFallbackContent(decoded) ?: return
            KoinPlatform.getKoin().get<Notifier>().showLocalNotification(content)
            logInfo("Showed a fallback notification for ${decoded::class.simpleName}")
        } catch (e: Throwable) {
            logError("Fallback notification failed, nothing could be shown", e)
        }
    }

    /**
     * Logged in already, or restores the session from the stored tokens (a push into a killed app
     * starts without one). False when there is no valid session. Never throws.
     */
    private suspend fun restoreSession(): Boolean {
        if (SessionCache.isLoggedIn()) return true
        return runCatching {
            val prefs = KoinPlatform.getKoin().get<Preferencemanager>()
            SessionCache.loginIfValid(tokens = prefs.getTokens(), developer = false)
        }.onFailure { e -> logError("Restoring the session for a push failed", e) }
            .getOrDefault(false)
    }

    /**
     * Start the alarm immediately, then log. The service start must not wait on the database:
     * a high priority FCM message only buys a short background start window, and missing it
     * throws ForegroundServiceStartNotAllowedException.
     */
    private fun handleWake(wake: DecodedNotification.Wake) {
        val started = runCatching {
            WakeAlarmService.start(
                context = this,
                senderName = wake.senderName,
                reason = wake.reason,
                groupName = wake.groupName,
                wokenUserCount = wake.wokenUserCount,
                wokenDeviceCount = wake.wokenDeviceCount,
            )
        }

        started.exceptionOrNull()?.let { e ->
            //No alarm - at least tell the user someone tried to wake them
            runBlocking(Dispatchers.IO) {
                logError("Could not start the wake alarm, showing a plain notification", e)
                showWakeFallbackNotification(wake)
            }
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val who = if (wake.isGroupWake) "${wake.senderName} (${wake.groupName})" else wake.senderName
                logInfo("Woken by $who: ${wake.reason}")
            }
        }
    }

    private suspend fun showWakeFallbackNotification(wake: DecodedNotification.Wake) {
        try {
            val title = runCatching {
                if (wake.isGroupWake) getString(Res.string.wake_notification_title_group, wake.senderName, wake.groupName)
                else getString(Res.string.wake_notification_title, wake.senderName)
            }.getOrDefault(wake.senderName.ifBlank { NOTIFICATION_FALLBACK_TITLE })

            KoinPlatform.getKoin().get<Notifier>().showLocalNotification(
                NotificationContent(
                    id = "wake:${wake.senderId}".hashCode().absoluteValue,
                    title = title,
                    body = wake.reason,
                    avatarUserId = wake.senderId,
                )
            )
        } catch (e: Throwable) {
            logError("Wake fallback notification failed, nothing could be shown", e)
        }
    }

    //Push problems go to the app log (not only the console), so a missing notification can be traced. Never throw.

    private suspend fun logError(message: String, e: Throwable? = null) {
        val text = if (e != null) "$message: ${e::class.simpleName}: ${e.message}" else message
        println("[AppFirebaseMessagingService] $text")
        runCatching { KoinPlatform.getKoin().get<Logger>().logError("[Push] $text") }
    }

    private suspend fun logInfo(message: String) {
        runCatching { KoinPlatform.getKoin().get<Logger>().logInfo("[Push] $message") }
    }

    private suspend fun logDebug(message: String) {
        runCatching { KoinPlatform.getKoin().get<Logger>().logDebug("[Push] $message") }
    }
}
