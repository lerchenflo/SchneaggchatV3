package org.lerchenflo.schneaggchatv3mp.utilities.notifications

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import org.jetbrains.compose.resources.getString
import org.koin.mp.KoinPlatform
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.chat.data.UserRepository
import org.lerchenflo.schneaggchatv3mp.utilities.PermissionManager
import org.lerchenflo.schneaggchatv3mp.utilities.PermissionState
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.mark_as_read
import schneaggchatv3mp.composeapp.generated.resources.reply
import kotlin.coroutines.resume

private const val CHANNEL_ID = "schneaggchat_messages"
private const val CHANNEL_NAME = "Messages"
const val EXTRA_FROM_NOTIFICATION = "from_notification"

//How many past messages are kept per chat in the MessagingStyle history shown on the notification
//and read aloud by Android Auto - bounded so a chat that's been unread for a while doesn't build
//an ever-growing in-memory list.
private const val MAX_HISTORY_PER_CHAT = 6

actual class Notifier(private val context: Context, private val permissionManager: PermissionManager) {

    private data class StyledMessage(
        val text: String,
        val timestamp: Long,
        //null means this message was sent by us (matches NotificationCompat.MessagingStyle's
        //convention: a message with no Person is attributed to the style's own "user").
        val senderName: String?,
    )

    private data class ActiveConversation(
        val chatId: String,
        val groupChat: Boolean,
        val groupName: String?,
        val messages: MutableList<StyledMessage> = mutableListOf(),
    )

    //Keyed by notification id (derived from chatId, see Message.toNotificationContent) so a chat
    //always updates the same notification instead of stacking a new one per message.
    private val activeConversations = mutableMapOf<Int, ActiveConversation>()

    private var cachedOwnDisplayName: String? = null

    actual suspend fun getToken(): String? = suspendCancellableCoroutine { cont ->
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token -> cont.resume(token) }
            .addOnFailureListener { cont.resume(null) }
    }

    actual suspend fun removeToken(): Unit = suspendCancellableCoroutine { cont ->
        FirebaseMessaging.getInstance().deleteToken()
            .addOnCompleteListener { cont.resume(Unit) }
    }

    actual suspend fun hasPermission(): Boolean {
        return permissionManager.checkNotificationPermission() == PermissionState.GRANTED
    }

    actual fun showLocalNotification(content: NotificationContent) {
        createChannelIfNeeded()

        if (content.chatId != null) {
            showMessageNotification(content)
        } else {
            showPlainNotification(content)
        }
    }

    private fun showPlainNotification(content: NotificationContent) {
        val pendingIntent = PendingIntent.getActivity(
            context,
            content.id,
            launchIntent(chatId = null, groupChat = false),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(NotificationConfig.iconResId)
            .setContentTitle(content.title)
            .setContentText(content.body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notification = builder.build()
        if (runBlocking { hasPermission() }) {
            @SuppressLint("MissingPermission")
            NotificationManagerCompat.from(context).notify(content.id, notification)
        }
    }

    /**
     * Builds/updates a [NotificationCompat.MessagingStyle] notification for the chat - the shape
     * Android Auto reads aloud and offers voice-reply/mark-as-read on. One notification per chat,
     * not per message: repeated calls for the same chat append to [activeConversations] and
     * re-post rather than stacking a new notification.
     */
    private fun showMessageNotification(content: NotificationContent) {
        val chatId = content.chatId ?: return

        val record = activeConversations.getOrPut(content.id) {
            ActiveConversation(chatId = chatId, groupChat = content.groupChat, groupName = content.groupName)
        }
        record.messages.add(
            StyledMessage(
                text = content.body,
                timestamp = content.timestampMillis.takeIf { it > 0 } ?: System.currentTimeMillis(),
                senderName = content.senderName,
            )
        )
        while (record.messages.size > MAX_HISTORY_PER_CHAT) record.messages.removeAt(0)

        postMessageNotification(content.id, record)
    }

    /** Appends the reply we just sent to the conversation's history and re-posts, confirming to the user (and Android Auto) that it went out. */
    fun appendSentReply(notifId: Int, replyText: String) {
        val record = activeConversations[notifId] ?: return
        record.messages.add(StyledMessage(text = replyText, timestamp = System.currentTimeMillis(), senderName = null))
        while (record.messages.size > MAX_HISTORY_PER_CHAT) record.messages.removeAt(0)
        postMessageNotification(notifId, record)
    }

    private fun postMessageNotification(notifId: Int, record: ActiveConversation) {
        val mePerson = Person.Builder().setName(ownDisplayName()).build()
        val style = NotificationCompat.MessagingStyle(mePerson)
            .setGroupConversation(record.groupChat)
        if (record.groupChat) {
            record.groupName?.let { style.setConversationTitle(it) }
        }
        record.messages.forEach { message ->
            val sender = message.senderName?.let { Person.Builder().setName(it).build() }
            style.addMessage(message.text, message.timestamp, sender)
        }

        val pendingContentIntent = PendingIntent.getActivity(
            context,
            notifId,
            launchIntent(chatId = record.chatId, groupChat = record.groupChat),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(NotificationConfig.iconResId)
            .setStyle(style)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingContentIntent)
            .addAction(replyAction(record.chatId, record.groupChat, notifId))
            .addAction(markAsReadAction(record.chatId, record.groupChat, notifId))

        val notification = builder.build()
        if (runBlocking { hasPermission() }) {
            @SuppressLint("MissingPermission")
            NotificationManagerCompat.from(context).notify(notifId, notification)
        }
    }

    private fun replyAction(chatId: String, groupChat: Boolean, notifId: Int): NotificationCompat.Action {
        val label = runBlocking { getString(Res.string.reply) }

        val remoteInput = RemoteInput.Builder(KEY_REPLY_TEXT)
            .setLabel(label)
            .build()

        val intent = Intent(context, ReplyReceiver::class.java).apply {
            action = ACTION_REPLY
            putExtra(EXTRA_CHAT_ID, chatId)
            putExtra(EXTRA_GROUP_CHAT, groupChat)
        }
        //FLAG_MUTABLE (not IMMUTABLE) is required so the system can attach the RemoteInput result
        //to this PendingIntent - the target (ReplyReceiver) is an explicit component of our own
        //app, so mutability here doesn't open the "mutable implicit intent" hijack risk.
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notifId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        return NotificationCompat.Action.Builder(NotificationConfig.markAsReadIconResId, label, pendingIntent)
            .addRemoteInput(remoteInput)
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
            .setShowsUserInterface(false)
            .build()
    }

    private fun markAsReadAction(chatId: String, groupChat: Boolean, notifId: Int): NotificationCompat.Action {
        val intent = Intent(context, MarkAsReadReceiver::class.java).apply {
            action = ACTION_MARK_AS_READ
            putExtra(EXTRA_CHAT_ID, chatId)
            putExtra(EXTRA_GROUP_CHAT, groupChat)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notifId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Action.Builder(
            NotificationConfig.markAsReadIconResId,
            runBlocking { getString(Res.string.mark_as_read) },
            pendingIntent
        )
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ)
            .setShowsUserInterface(false)
            .build()
    }

    private fun launchIntent(chatId: String?, groupChat: Boolean): Intent {
        //NEW_TASK to launch from a service context, CLEAR_TOP|SINGLE_TOP (with MainActivity's
        //launchMode="singleTop") to resume an already-running app via onNewIntent instead of
        //tearing it down and restarting it (CLEAR_TASK did that on every tap).
        val intentFlags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        return context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = intentFlags
            putExtra(EXTRA_FROM_NOTIFICATION, true)
            chatId?.let { putExtra(EXTRA_CHAT_ID, it) }
            putExtra(EXTRA_GROUP_CHAT, groupChat)
        } ?: Intent().apply {
            setClassName(context, "org.lerchenflo.androidApp.MainActivity")
            flags = intentFlags
            putExtra(EXTRA_FROM_NOTIFICATION, true)
            chatId?.let { putExtra(EXTRA_CHAT_ID, it) }
            putExtra(EXTRA_GROUP_CHAT, groupChat)
        }
    }

    private fun ownDisplayName(): String {
        cachedOwnDisplayName?.let { return it }
        val resolved = runCatching {
            runBlocking {
                val ownId = SessionCache.requireLoggedIn()?.userId ?: return@runBlocking null
                KoinPlatform.getKoin().get<UserRepository>().getUserFlow(ownId).first()?.displayName
            }
        }.getOrNull() ?: ""
        cachedOwnDisplayName = resolved
        return resolved
    }

    actual fun cancelNotification(id: Int) {
        activeConversations.remove(id)
        NotificationManagerCompat.from(context).cancel(id)
    }

    actual fun cancelNotifications(ids: List<Int>) {
        ids.forEach { cancelNotification(it) }
    }

    actual fun cancelAllNotifications() {
        activeConversations.clear()
        NotificationManagerCompat.from(context).cancelAll()
    }

    actual fun cancelMessageNotifications(ids: List<Int>) {
        ids.forEach { cancelNotification(it) }
    }

    private fun createChannelIfNeeded() {
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
