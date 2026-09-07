package org.lerchenflo.schneaggchatv3mp.utilities.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.utilities.NotificationManager
import org.lerchenflo.schneaggchatv3mp.utilities.getCurrentTimeMillisString

const val ACTION_REPLY = "org.lerchenflo.schneaggchatv3mp.REPLY"
const val KEY_REPLY_TEXT = "reply_text"

/**
 * Handles the voice/typed "Reply" action on a message notification - the path Android Auto's
 * voice reply and the phone notification's inline reply both go through.
 */
class ReplyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REPLY) return

        val chatId = intent.getStringExtra(EXTRA_CHAT_ID) ?: return
        val groupChat = intent.getBooleanExtra(EXTRA_GROUP_CHAT, false)
        val replyText = RemoteInput.getResultsFromIntent(intent)
            ?.getCharSequence(KEY_REPLY_TEXT)
            ?.toString()
            ?.trim()

        if (replyText.isNullOrEmpty()) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val prefs = KoinPlatform.getKoin().get<Preferencemanager>()
                val appRepository = KoinPlatform.getKoin().get<AppRepository>()

                //The app process may have been started just for this broadcast
                if (SessionCache.authState.value !is SessionCache.AuthState.LoggedIn) {
                    SessionCache.loginIfValid(tokens = prefs.getTokens(), developer = false)
                }
                val ownId = (SessionCache.authState.value as? SessionCache.AuthState.LoggedIn)
                    ?.userId ?: return@runCatching

                appRepository.sendMessage(
                    ownId = ownId,
                    messageId = null,
                    empfaenger = chatId,
                    gruppe = groupChat,
                    content = AppRepository.MessageContent.TextContent(replyText),
                    answerid = null,
                )

                appRepository.setAllChatMessagesRead(
                    ownId = ownId,
                    chatid = chatId,
                    gruppe = groupChat,
                    timestamp = getCurrentTimeMillisString()
                )

                val notifId = NotificationManager.NotiId.HexString(chatId).asInt
                KoinPlatform.getKoin().get<Notifier>().appendSentReply(notifId, replyText)
            }.onFailure { e ->
                println("[ReplyReceiver] Error sending reply: ${e.message}")
            }
            pendingResult.finish()
        }
    }
}
