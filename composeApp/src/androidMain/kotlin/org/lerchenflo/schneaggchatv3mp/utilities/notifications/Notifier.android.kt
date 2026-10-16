package org.lerchenflo.schneaggchatv3mp.utilities.notifications

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.graphics.drawable.IconCompat
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.getString
import org.koin.mp.KoinPlatform
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.logging.LoggingRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.UserRepository
import org.lerchenflo.schneaggchatv3mp.utilities.PermissionManager
import org.lerchenflo.schneaggchatv3mp.utilities.PermissionState
import java.io.File
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.mark_as_read
import schneaggchatv3mp.composeapp.generated.resources.reply
import schneaggchatv3mp.composeapp.generated.resources.you_sender
import kotlin.coroutines.resume

private const val CHANNEL_ID = "schneaggchat_messages"
private const val CHANNEL_NAME = "Messages"
const val EXTRA_FROM_NOTIFICATION = "from_notification"

//How many past messages are kept per chat in the MessagingStyle history shown on the notification
//and read aloud by Android Auto - bounded so a chat that's been unread for a while doesn't build
//an ever-growing in-memory list.
private const val MAX_HISTORY_PER_CHAT = 6

//Upper bound for resolving our own display name from the database while building a notification -
//a slow or stuck query must never keep a message notification from being posted.
private const val OWN_NAME_LOOKUP_TIMEOUT_MS = 1_000L

//Same idea for resolving a sender's profile picture path - on timeout the default icon is used.
private const val AVATAR_LOOKUP_TIMEOUT_MS = 1_000L

//Edge length avatars are decoded to. Every message's Person carries its own copy of the bitmap
//through the binder, so this stays small enough that a full history never nears the 1MB limit.
private const val AVATAR_SIZE_PX = 128

//Decoded avatars kept across notifications, so re-posting a chat doesn't decode the file again.
private const val MAX_CACHED_AVATARS = 32

actual class Notifier(private val context: Context, private val permissionManager: PermissionManager) {

    private data class StyledMessage(
        val text: String,
        val timestamp: Long,
        //null means this message was sent by us (matches NotificationCompat.MessagingStyle's
        //convention: a message with no Person is attributed to the style's own "user").
        val senderName: String?,
        val senderId: String?,
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

    private data class CachedAvatar(val path: String, val lastModified: Long, val length: Long, val icon: IconCompat)

    //Keyed by user id, access-ordered so the least recently used avatar is evicted first. The file
    //stamp in CachedAvatar invalidates an entry once the user's picture is replaced on disk.
    private val avatarCache = object : LinkedHashMap<String, CachedAvatar>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CachedAvatar>): Boolean =
            size > MAX_CACHED_AVATARS
    }

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

    //Synchronized: pushes (one coroutine each), the socket handler and the notification action
    //receivers all reach this from different threads, and activeConversations is a plain map.
    @Synchronized
    actual fun showLocalNotification(content: NotificationContent) {
        try {
            createChannelIfNeeded()
        } catch (e: Throwable) {
            logError("Creating the notification channel failed: ${e.describe()}")
        }

        if (content.chatId != null) {
            try {
                showMessageNotification(content)
            } catch (e: Throwable) {
                //Never drop a message because the MessagingStyle notification could not be built -
                //fall back to a plain one and record why, so the failure shows up in the logs.
                logError("Message notification failed, showing plain fallback: ${e.describe()}")
                showPlainNotificationSafely(content.id, content.title, content.body, content.chatId, content.groupChat)
            }
        } else {
            showPlainNotificationSafely(content.id, content.title, content.body, content.chatId, content.groupChat)
        }
    }

    /**
     * Plain notification, then - if even that throws - a bare one without resources or a content
     * intent. Each failure is logged; this never throws.
     */
    private fun showPlainNotificationSafely(id: Int, title: String, body: String, chatId: String?, groupChat: Boolean) {
        try {
            showPlainNotification(id, title, body, chatId, groupChat)
        } catch (e: Throwable) {
            logError("Plain notification failed, showing bare fallback: ${e.describe()}")
            try {
                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_email)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .build()
                @SuppressLint("MissingPermission")
                NotificationManagerCompat.from(context).notify(id, notification)
            } catch (e: Throwable) {
                logError("Bare fallback notification failed: ${e.describe()}")
            }
        }
    }

    private fun showPlainNotification(id: Int, title: String, body: String, chatId: String?, groupChat: Boolean) {
        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            launchIntent(chatId = chatId, groupChat = groupChat),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(NotificationConfig.iconResId)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notification = builder.build()
        if (runBlocking { hasPermission() }) {
            @SuppressLint("MissingPermission")
            NotificationManagerCompat.from(context).notify(id, notification)
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
                senderId = content.senderId,
            )
        )
        while (record.messages.size > MAX_HISTORY_PER_CHAT) record.messages.removeAt(0)

        postConversationSafely(content.id, record, fallbackTitle = content.title, fallbackBody = content.body)
    }

    /** Appends the reply we just sent to the conversation's history and re-posts, confirming to the user (and Android Auto) that it went out. */
    @Synchronized
    fun appendSentReply(notifId: Int, replyText: String) {
        val record = activeConversations[notifId] ?: return
        record.messages.add(StyledMessage(text = replyText, timestamp = System.currentTimeMillis(), senderName = null, senderId = null))
        while (record.messages.size > MAX_HISTORY_PER_CHAT) record.messages.removeAt(0)
        postConversationSafely(notifId, record, fallbackTitle = record.groupName ?: ownDisplayName(), fallbackBody = replyText)
    }

    /**
     * Posts the MessagingStyle notification with profile pictures; if that throws (a broken
     * picture, a too-large binder transaction, ...) retries with the default icon only, then falls
     * back to a plain notification. Every failure is logged; this never throws.
     */
    private fun postConversationSafely(notifId: Int, record: ActiveConversation, fallbackTitle: String, fallbackBody: String) {
        try {
            postMessageNotification(notifId, record, withProfilePictures = true)
            return
        } catch (e: Throwable) {
            logError("Message notification with profile pictures failed, retrying with default icon: ${e.describe()}")
        }
        try {
            postMessageNotification(notifId, record, withProfilePictures = false)
            return
        } catch (e: Throwable) {
            logError("Message notification with default icon failed, showing plain fallback: ${e.describe()}")
        }
        showPlainNotificationSafely(notifId, fallbackTitle, fallbackBody, record.chatId, record.groupChat)
    }

    private fun postMessageNotification(notifId: Int, record: ActiveConversation, withProfilePictures: Boolean) {
        //Every Person carries an icon - without one, MessagingStyle draws a letter avatar (sender
        //initial in a coloured circle). The app icon is the default whenever no picture resolves.
        val appIcon = IconCompat.createWithResource(context, context.applicationInfo.icon)
        val resolvedAvatars = mutableMapOf<String, IconCompat>()
        fun avatarFor(userId: String?): IconCompat {
            if (!withProfilePictures || userId == null) return appIcon
            return resolvedAvatars.getOrPut(userId) { profilePictureIcon(userId) ?: appIcon }
        }

        val ownId = runCatching { SessionCache.requireLoggedIn()?.userId }
            .onFailure { logError("Resolving own user id for the notification avatar failed: ${it.describe()}") }
            .getOrNull()
        val mePerson = Person.Builder().setName(ownDisplayName()).setIcon(avatarFor(ownId)).build()
        val style = NotificationCompat.MessagingStyle(mePerson)
            .setGroupConversation(record.groupChat)
        if (record.groupChat) {
            record.groupName?.let { style.setConversationTitle(it) }
        }
        record.messages.forEach { message ->
            val sender = message.senderName?.let { Person.Builder().setName(it).setIcon(avatarFor(message.senderId)).build() }
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

    /**
     * Name MessagingStyle attributes our own messages to. Must never be blank: MessagingStyle
     * rejects a [Person] with an empty name, and a push arriving in a fresh process (app killed)
     * runs before the session is restored, so the real name isn't resolvable yet. Only a real
     * name is cached - a fallback stays uncached so the next notification retries.
     */
    private fun ownDisplayName(): String {
        cachedOwnDisplayName?.let { return it }
        val resolved = runCatching {
            runBlocking {
                val ownId = SessionCache.requireLoggedIn()?.userId ?: return@runBlocking null
                withTimeoutOrNull(OWN_NAME_LOOKUP_TIMEOUT_MS) {
                    KoinPlatform.getKoin().get<UserRepository>().getUserFlow(ownId).first()?.displayName
                }
            }
        }.getOrNull()?.takeIf { it.isNotBlank() }

        if (resolved != null) {
            cachedOwnDisplayName = resolved
            return resolved
        }
        return runCatching { runBlocking { getString(Res.string.you_sender) } }.getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: "You"
    }

    /**
     * The user's stored profile picture as a small square icon, or null when there is none (no
     * user row, no picture downloaded yet, file missing) or it can't be read - the caller then
     * uses the default icon. Failures are logged; this never throws.
     */
    private fun profilePictureIcon(userId: String): IconCompat? {
        return try {
            val path = runBlocking {
                withTimeoutOrNull(AVATAR_LOOKUP_TIMEOUT_MS) {
                    KoinPlatform.getKoin().get<UserRepository>().getUserById(userId)?.profilePictureUrl
                }
            }?.takeIf { it.isNotBlank() } ?: return null

            val file = File(path)
            if (!file.isFile || !file.canRead() || file.length() <= 0L) return null
            val lastModified = file.lastModified()
            val length = file.length()

            avatarCache[userId]
                ?.takeIf { it.path == path && it.lastModified == lastModified && it.length == length }
                ?.let { return it.icon }

            val bitmap = decodeAvatarBitmap(file)
            if (bitmap == null) {
                logError("Profile picture of $userId could not be decoded, using default icon")
                return null
            }
            IconCompat.createWithBitmap(bitmap).also { icon ->
                avatarCache[userId] = CachedAvatar(path, lastModified, length, icon)
            }
        } catch (e: Throwable) {
            logError("Loading profile picture of $userId failed, using default icon: ${e.describe()}")
            null
        }
    }

    /** Decodes [file] subsampled, then centre-crops and scales it to an [AVATAR_SIZE_PX] square. */
    private fun decodeAvatarBitmap(file: File): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sampleSize = 1
        while (minOf(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= AVATAR_SIZE_PX) sampleSize *= 2
        val decoded = BitmapFactory.decodeFile(
            file.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = sampleSize }
        ) ?: return null

        val side = minOf(decoded.width, decoded.height)
        val square = Bitmap.createBitmap(decoded, (decoded.width - side) / 2, (decoded.height - side) / 2, side, side)
        val scaled = Bitmap.createScaledBitmap(square, AVATAR_SIZE_PX, AVATAR_SIZE_PX, true)
        if (square !== scaled && square !== decoded) square.recycle()
        if (decoded !== scaled) decoded.recycle()
        return scaled
    }

    private fun Throwable.describe(): String = "${this::class.simpleName}: $message"

    private fun logError(message: String) {
        println("[Notifier] $message")
        runCatching { runBlocking { KoinPlatform.getKoin().get<LoggingRepository>().logError("[Notifier] $message") } }
    }

    @Synchronized
    actual fun cancelNotification(id: Int) {
        activeConversations.remove(id)
        NotificationManagerCompat.from(context).cancel(id)
    }

    actual fun cancelNotifications(ids: List<Int>) {
        ids.forEach { cancelNotification(it) }
    }

    @Synchronized
    actual fun cancelAllNotifications() {
        activeConversations.clear()
        avatarCache.clear()
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
