package org.lerchenflo.schneaggchatv3mp.utilities.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.domain.Logger
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageRepository
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.utilities.NotificationManager
import org.lerchenflo.schneaggchatv3mp.utilities.getCurrentTimeMillisString

const val ACTION_MARK_AS_READ = "org.lerchenflo.schneaggchatv3mp.MARK_AS_READ"
const val EXTRA_CHAT_ID = "chat_id"
const val EXTRA_GROUP_CHAT = "group_chat"

/**
 * Handles the "mark as read" action button on message notifications.
 * Marks the whole chat as read on the server (the sync pulls the read state back), then
 * dismisses the notification.
 */
class MarkAsReadReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_MARK_AS_READ) return

        val chatId = intent.getStringExtra(EXTRA_CHAT_ID) ?: return
        val groupChat = intent.getBooleanExtra(EXTRA_GROUP_CHAT, false)

        //Dismiss immediately so the action feels instant, the sync happens in the background.
        //Takes the chat's reaction notifications along.
        NotificationManager.removeChatNotifications(chatId)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val prefs = KoinPlatform.getKoin().get<Preferencemanager>()
                val messageRepository = KoinPlatform.getKoin().get<MessageRepository>()

                //The app process may have been started just for this broadcast
                if (SessionCache.authState.value !is SessionCache.AuthState.LoggedIn) {
                    SessionCache.loginIfValid(tokens = prefs.getTokens(), developer = false)
                }
                //Bail rather than fire an unauthenticated request the server would reject
                if (SessionCache.authState.value !is SessionCache.AuthState.LoggedIn) {
                    logError("Chat $chatId not marked as read: no valid session")
                    return@runCatching
                }

                messageRepository.setAllChatMessagesRead(
                    chatid = chatId,
                    gruppe = groupChat,
                    timestamp = getCurrentTimeMillisString()
                )
            }.onFailure { e ->
                logError("Marking chat $chatId as read failed: ${e::class.simpleName}: ${e.message}")
            }
            pendingResult.finish()
        }
    }

    private suspend fun logError(message: String) {
        println("[MarkAsReadReceiver] $message")
        runCatching { KoinPlatform.getKoin().get<Logger>().logError("[MarkAsReadReceiver] $message") }
    }
}
