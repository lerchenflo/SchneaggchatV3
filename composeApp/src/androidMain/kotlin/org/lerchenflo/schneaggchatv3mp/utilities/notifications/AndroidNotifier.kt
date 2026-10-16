package org.lerchenflo.schneaggchatv3mp.utilities.notifications

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Shader
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.content.LocusIdCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.getString
import org.koin.mp.KoinPlatform
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.domain.Logger
import org.lerchenflo.schneaggchatv3mp.chat.data.local.GroupLocalDataSource
import org.lerchenflo.schneaggchatv3mp.chat.data.local.UserLocalDataSource
import org.lerchenflo.schneaggchatv3mp.utilities.PermissionManager
import org.lerchenflo.schneaggchatv3mp.utilities.PictureManager
import org.lerchenflo.schneaggchatv3mp.utilities.PermissionState
import java.io.File
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.mark_as_read
import schneaggchatv3mp.composeapp.generated.resources.reply
import schneaggchatv3mp.composeapp.generated.resources.you_have_new_messages
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

//Prefix of the conversation shortcut published per chat (see publishConversationShortcut)
private const val CHAT_SHORTCUT_PREFIX = "chat_"

//All chat notifications (messages and reactions) are bundled under one summary, like WhatsApp does
private const val CHAT_GROUP_KEY = "schneaggchat_chats"

//Id of that summary - next to the fixed ids 1-5 of NotificationManager.NotiIdType. Chat
//notification ids are chat id hashes, so a collision is practically impossible.
private const val CHAT_SUMMARY_ID = 6

//Lines the summary lists when the bundle is collapsed
private const val MAX_SUMMARY_LINES = 6

class AndroidNotifier(private val context: Context, private val permissionManager: PermissionManager) : Notifier {

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
        //Group name for group chats, the other person's name for 1:1 chats - title of the conversation
        var chatName: String?,
        val messages: MutableList<StyledMessage> = mutableListOf(),
    )

    //Keyed by notification id (derived from chatId, see Message.toNotificationContent) so a chat
    //always updates the same notification instead of stacking a new one per message.
    private val activeConversations = mutableMapOf<Int, ActiveConversation>()

    private var cachedOwnDisplayName: String? = null

    private data class CachedAvatar(val path: String, val lastModified: Long, val length: Long, val bitmap: Bitmap)

    //Keyed by "user:<id>" / "group:<id>", access-ordered so the least recently used avatar is evicted
    //first. The file stamp in CachedAvatar invalidates an entry once the picture is replaced on disk.
    private val avatarCache = object : LinkedHashMap<String, CachedAvatar>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CachedAvatar>): Boolean =
            size > MAX_CACHED_AVATARS
    }

    //Shortcut id -> label and picture it was last published with, so an unchanged chat isn't re-published per message
    private val publishedShortcuts = mutableMapOf<String, String>()

    override suspend fun getToken(): String? {
        val result = suspendCancellableCoroutine<Result<String?>> { cont ->
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token -> cont.resume(Result.success(token)) }
                .addOnFailureListener { e -> cont.resume(Result.failure(e)) }
        }
        return result.onFailure { e ->
            //No token means no pushes at all - must be visible in the logs
            runCatching { KoinPlatform.getKoin().get<Logger>().logError("[Notifier] Getting the notification token failed: ${e.describe()}") }
        }.getOrNull()
    }

    override suspend fun removeToken(): Unit = suspendCancellableCoroutine { cont ->
        FirebaseMessaging.getInstance().deleteToken()
            .addOnCompleteListener { cont.resume(Unit) }
    }

    override suspend fun hasPermission(): Boolean {
        return permissionManager.checkNotificationPermission() == PermissionState.GRANTED
    }

    //Synchronized: pushes (one coroutine each), the socket handler and the notification action
    //receivers all reach this from different threads, and activeConversations is a plain map.
    @Synchronized
    override fun showLocalNotification(content: NotificationContent) {
        try {
            createChannelIfNeeded()
        } catch (e: Throwable) {
            logError("Creating the notification channel failed: ${e.describe()}")
        }

        if (content.conversation && content.chatId != null) {
            try {
                showMessageNotification(content)
            } catch (e: Throwable) {
                //Never drop a message because the MessagingStyle notification could not be built -
                //fall back to a plain one and record why, so the failure shows up in the logs.
                logError("Message notification failed, showing plain fallback: ${e.describe()}")
                showPlainNotificationSafely(content.id, content.title, content.body, content.chatId, content.groupChat, content.senderId)
            }
        } else {
            showPlainNotificationSafely(content.id, content.title, content.body, content.chatId, content.groupChat, content.avatarUserId)
        }
    }

    /**
     * Plain notification, then - if even that throws - a bare one without resources or a content
     * intent. Each failure is logged; this never throws.
     */
    private fun showPlainNotificationSafely(id: Int, title: String, body: String, chatId: String?, groupChat: Boolean, avatarUserId: String? = null) {
        try {
            showPlainNotification(id, title, body, chatId, groupChat, avatarUserId)
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

    /**
     * [avatarUserId]: whose profile picture is shown as the large icon - the default look when there
     * is none. With a [chatId] (reactions, message fallbacks) it joins the chat bundle.
     */
    private fun showPlainNotification(id: Int, title: String, body: String, chatId: String?, groupChat: Boolean, avatarUserId: String?) {
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

        avatarUserId?.let { userAvatar(it) }?.let { builder.setLargeIcon(it) }
        if (chatId != null) builder.setGroup(CHAT_GROUP_KEY)

        notifyChecked(id, builder.build())
        if (chatId != null) updateChatSummary(postedId = id)
    }

    /**
     * Posts [notification], logging every reason it would not show up: missing permission (then it
     * is not posted at all), notifications or the channel turned off in the system settings.
     */
    private fun notifyChecked(id: Int, notification: Notification) {
        if (!runBlocking { hasPermission() }) {
            logError("Notification $id not shown: the notification permission is not granted")
            return
        }
        try {
            val manager = NotificationManagerCompat.from(context)
            if (!manager.areNotificationsEnabled()) {
                logError("Notification $id not shown: notifications are turned off for the app in the system settings")
            } else if (manager.getNotificationChannel(CHANNEL_ID)?.importance == NotificationManager.IMPORTANCE_NONE) {
                logError("Notification $id not shown: the \"$CHANNEL_NAME\" channel is turned off in the system settings")
            }
        } catch (e: Throwable) {
            logError("Checking whether notifications are turned off failed: ${e.describe()}")
        }
        @SuppressLint("MissingPermission")
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    /**
     * Posts the summary that bundles all chat notifications, or removes it once none is left. Built
     * from the notifications actually showing, so it stays right across process restarts.
     * [postedId] / [removedId]: a chat notification just posted or cancelled - the system applies
     * both asynchronously, so the active list may not reflect them yet. Never throws.
     */
    private fun updateChatSummary(postedId: Int? = null, removedId: Int? = null) {
        try {
            val shown = context.getSystemService(NotificationManager::class.java).activeNotifications
                .filter { it.notification.group == CHAT_GROUP_KEY && it.id != CHAT_SUMMARY_ID && it.id != removedId }
            val childIds = shown.map { it.id }.toSet() + listOfNotNull(postedId)

            if (childIds.isEmpty()) {
                NotificationManagerCompat.from(context).cancel(CHAT_SUMMARY_ID)
                return
            }

            val style = NotificationCompat.InboxStyle()
            shown.sortedByDescending { it.postTime }.take(MAX_SUMMARY_LINES).forEach { child ->
                val extras = child.notification.extras
                val title = extras.getCharSequence(Notification.EXTRA_TITLE)
                val text = extras.getCharSequence(Notification.EXTRA_TEXT)
                listOfNotNull(title, text).joinToString(": ").takeIf { it.isNotBlank() }?.let { style.addLine(it) }
            }

            val summary = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(NotificationConfig.iconResId)
                .setContentTitle(appLabel())
                .setContentText(runCatching { runBlocking { getString(Res.string.you_have_new_messages) } }.getOrDefault(""))
                .setStyle(style)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setGroup(CHAT_GROUP_KEY)
                .setGroupSummary(true)
                //Only the chat notifications alert - the summary itself stays silent
                .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
                .setAutoCancel(true)
                .setContentIntent(
                    PendingIntent.getActivity(
                        context,
                        CHAT_SUMMARY_ID,
                        launchIntent(chatId = null, groupChat = false),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
                .build()

            //Missing permission or blocked notifications were already logged for the chat notification itself
            if (runBlocking { hasPermission() }) {
                @SuppressLint("MissingPermission")
                NotificationManagerCompat.from(context).notify(CHAT_SUMMARY_ID, summary)
            }
        } catch (e: Throwable) {
            logError("Updating the chat summary notification failed: ${e.describe()}")
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
            //A push may run in a fresh process: pick up the messages the chat's notification already
            //shows, so it keeps its history instead of being replaced by the newest message alone
            ActiveConversation(chatId = chatId, groupChat = content.groupChat, chatName = null).apply {
                messages += restoreShownMessages(content.id)
            }
        }
        //Kept current, so a renamed group or a changed name shows up on the next message
        val chatName = if (content.groupChat) content.groupName else content.senderName
        if (!chatName.isNullOrBlank()) record.chatName = chatName
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
        postConversationSafely(notifId, record, fallbackTitle = record.chatName ?: ownDisplayName(), fallbackBody = replyText)
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
        showPlainNotificationSafely(notifId, fallbackTitle, fallbackBody, record.chatId, record.groupChat, record.chatId.takeIf { !record.groupChat })
    }

    /**
     * Messages the notification [notifId] currently shows, read back from its MessagingStyle. Empty
     * when it isn't showing or can't be read. Failures are logged; this never throws.
     */
    private fun restoreShownMessages(notifId: Int): List<StyledMessage> {
        return try {
            val shown = context.getSystemService(NotificationManager::class.java).activeNotifications
                .firstOrNull { it.id == notifId }
                ?.notification
                ?: return emptyList()
            val style = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(shown)
                ?: return emptyList()
            style.messages.takeLast(MAX_HISTORY_PER_CHAT).map { message ->
                StyledMessage(
                    text = message.text?.toString().orEmpty(),
                    timestamp = message.timestamp,
                    //No person = one of our own replies, same convention as when it was added
                    senderName = message.person?.name?.toString(),
                    senderId = message.person?.key,
                )
            }
        } catch (e: Throwable) {
            logError("Restoring the messages of notification $notifId failed: ${e.describe()}")
            emptyList()
        }
    }

    private fun postMessageNotification(notifId: Int, record: ActiveConversation, withProfilePictures: Boolean) {
        //Every Person carries an icon - without one, MessagingStyle draws a letter avatar (sender
        //initial in a coloured circle). The app icon is the default whenever no picture resolves.
        val appIcon = IconCompat.createWithResource(context, context.applicationInfo.icon)
        val resolvedAvatars = mutableMapOf<String, IconCompat>()
        fun avatarFor(userId: String?): IconCompat {
            if (!withProfilePictures || userId == null) return appIcon
            return resolvedAvatars.getOrPut(userId) { userAvatar(userId)?.let { IconCompat.createWithBitmap(it) } ?: appIcon }
        }

        val ownId = runCatching { SessionCache.requireLoggedIn()?.userId }
            .onFailure { logError("Resolving own user id for the notification avatar failed: ${it.describe()}") }
            .getOrNull()
        val mePerson = Person.Builder().setName(ownDisplayName()).setIcon(avatarFor(ownId)).build()
        val style = NotificationCompat.MessagingStyle(mePerson)
            .setGroupConversation(record.groupChat)
        if (record.groupChat) {
            record.chatName?.let { style.setConversationTitle(it) }
        }
        record.messages.forEach { message ->
            val sender = message.senderName?.let {
                Person.Builder().setName(it).setKey(message.senderId).setIcon(avatarFor(message.senderId)).build()
            }
            style.addMessage(message.text, message.timestamp, sender)
        }

        //Picture of the whole conversation: the group's for groups, the other person's for 1:1 chats
        val chatBitmap = when {
            !withProfilePictures -> null
            record.groupChat -> groupAvatar(record.chatId)
            else -> userAvatar(record.chatId)
        }
        val chatLabel = record.chatName?.takeIf { it.isNotBlank() } ?: appLabel()
        val chatPerson = if (record.groupChat) null else {
            Person.Builder()
                .setName(chatLabel)
                .setKey(record.chatId)
                .setIcon(chatBitmap?.let { IconCompat.createWithBitmap(it) } ?: appIcon)
                .build()
        }
        val shortcutId = publishConversationShortcut(record, chatLabel, chatBitmap, chatPerson)

        val pendingContentIntent = PendingIntent.getActivity(
            context,
            notifId,
            launchIntent(chatId = record.chatId, groupChat = record.groupChat),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(NotificationConfig.iconResId)
            .setStyle(style)
            //Not drawn next to the MessagingStyle, but read by the chat summary and older launchers
            .setContentTitle(chatLabel)
            .setContentText(record.messages.lastOrNull()?.text)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingContentIntent)
            .setGroup(CHAT_GROUP_KEY)
            .addAction(replyAction(record.chatId, record.groupChat, notifId))
            .addAction(markAsReadAction(record.chatId, record.groupChat, notifId))

        if (shortcutId != null) {
            builder.setShortcutId(shortcutId)
            builder.setLocusId(LocusIdCompat(shortcutId))
        }
        //Android 11+ shows the conversation picture from the shortcut. Older versions (and a failed
        //shortcut) only know the large icon.
        if (shortcutId == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            chatBitmap?.let { builder.setLargeIcon(it) }
        }

        notifyChecked(notifId, builder.build())
        updateChatSummary(postedId = notifId)
    }

    /**
     * Publishes the chat as a long-lived conversation shortcut and returns its id. Since Android 11
     * a MessagingStyle notification only counts as a conversation - shown with the chat's picture,
     * in the conversations section - when it points at such a shortcut. Returns null when it could
     * not be published; the notification then goes out without it. Never throws.
     */
    private fun publishConversationShortcut(record: ActiveConversation, label: String, chatBitmap: Bitmap?, chatPerson: Person?): String? {
        val shortcutId = CHAT_SHORTCUT_PREFIX + record.chatId
        val stamp = "$label|${chatBitmap?.let { System.identityHashCode(it) } ?: 0}"
        if (publishedShortcuts[shortcutId] == stamp) return shortcutId

        return try {
            val icon = chatBitmap?.let { IconCompat.createWithBitmap(it) }
                ?: IconCompat.createWithResource(context, context.applicationInfo.icon)
            val intent = launchIntent(chatId = record.chatId, groupChat = record.groupChat).apply {
                //Shortcut intents need an action
                if (action == null) action = Intent.ACTION_VIEW
            }
            val builder = ShortcutInfoCompat.Builder(context, shortcutId)
                .setShortLabel(label)
                .setLongLived(true)
                .setIcon(icon)
                .setIntent(intent)
                .setLocusId(LocusIdCompat(shortcutId))
            chatPerson?.let { builder.setPerson(it) }

            ShortcutManagerCompat.pushDynamicShortcut(context, builder.build())
            publishedShortcuts[shortcutId] = stamp
            shortcutId
        } catch (e: Throwable) {
            logError("Publishing the conversation shortcut for ${record.chatId} failed, posting without it: ${e.describe()}")
            null
        }
    }

    private fun appLabel(): String =
        runCatching { context.applicationInfo.loadLabel(context.packageManager).toString() }.getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: NOTIFICATION_FALLBACK_TITLE

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
                    KoinPlatform.getKoin().get<UserLocalDataSource>().getUserFlow(ownId).first()?.displayName
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
     * The user's stored profile picture as a small round bitmap, or null when there is none (no
     * picture downloaded yet, file missing) or it can't be read - the caller then uses the default
     * icon. Looks at the path in the user's row first, then at the fixed per-user path, where a
     * picture downloaded for a not yet synced user lands. Failures are logged; this never throws.
     */
    private fun userAvatar(userId: String): Bitmap? = avatarBitmap("user:$userId") {
        listOfNotNull(
            KoinPlatform.getKoin().get<UserLocalDataSource>().getUserById(userId)?.profilePictureUrl,
            KoinPlatform.getKoin().get<PictureManager>().getProfilePicFilePath(userId, gruppe = false),
        )
    }

    /** Same as [userAvatar] for a group's picture. */
    private fun groupAvatar(groupId: String): Bitmap? = avatarBitmap("group:$groupId") {
        listOfNotNull(
            KoinPlatform.getKoin().get<GroupLocalDataSource>().getGroupById(groupId)?.profilePictureUrl,
            KoinPlatform.getKoin().get<PictureManager>().getProfilePicFilePath(groupId, gruppe = true),
        )
    }

    private fun avatarBitmap(cacheKey: String, candidatePaths: suspend () -> List<String>): Bitmap? {
        return try {
            val paths = runBlocking {
                withTimeoutOrNull(AVATAR_LOOKUP_TIMEOUT_MS) { candidatePaths() }
            }
            if (paths == null) {
                logError("Looking up the picture for $cacheKey timed out, using default icon")
                return null
            }

            val file = paths.asSequence()
                .filter { it.isNotBlank() }
                .map { File(it) }
                .firstOrNull { it.isFile && it.canRead() && it.length() > 0L }
                ?: return null
            val path = file.absolutePath
            val lastModified = file.lastModified()
            val length = file.length()

            avatarCache[cacheKey]
                ?.takeIf { it.path == path && it.lastModified == lastModified && it.length == length }
                ?.let { return it.bitmap }

            val bitmap = decodeAvatarBitmap(file)
            if (bitmap == null) {
                logError("Picture for $cacheKey could not be decoded, using default icon")
                return null
            }
            avatarCache[cacheKey] = CachedAvatar(path, lastModified, length, bitmap)
            bitmap
        } catch (e: Throwable) {
            logError("Loading the picture for $cacheKey failed, using default icon: ${e.describe()}")
            null
        }
    }

    /** Decodes [file] subsampled, then centre-crops and scales it to an [AVATAR_SIZE_PX] circle. */
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

        //Round like every profile picture in the app - the large icon and some launchers show it as is
        val round = Bitmap.createBitmap(AVATAR_SIZE_PX, AVATAR_SIZE_PX, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = BitmapShader(scaled, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        Canvas(round).drawCircle(AVATAR_SIZE_PX / 2f, AVATAR_SIZE_PX / 2f, AVATAR_SIZE_PX / 2f, paint)
        scaled.recycle()
        return round
    }

    private fun Throwable.describe(): String = "${this::class.simpleName}: $message"

    private fun logError(message: String) {
        println("[Notifier] $message")
        runCatching { runBlocking { KoinPlatform.getKoin().get<Logger>().logError("[Notifier] $message") } }
    }

    @Synchronized
    override fun cancelNotification(id: Int) {
        activeConversations.remove(id)
        NotificationManagerCompat.from(context).cancel(id)
        updateChatSummary(removedId = id)
    }

    override fun cancelNotifications(ids: List<Int>) {
        ids.forEach { cancelNotification(it) }
    }

    /** Clears everything, conversation shortcuts included - runs when the account's data is deleted (logout). */
    @Synchronized
    override fun cancelAllNotifications() {
        activeConversations.clear()
        avatarCache.clear()
        publishedShortcuts.clear()
        NotificationManagerCompat.from(context).cancelAll()
        try {
            val chatShortcutIds = ShortcutManagerCompat.getShortcuts(
                context,
                ShortcutManagerCompat.FLAG_MATCH_DYNAMIC or ShortcutManagerCompat.FLAG_MATCH_CACHED
            ).map { it.id }.filter { it.startsWith(CHAT_SHORTCUT_PREFIX) }
            ShortcutManagerCompat.removeLongLivedShortcuts(context, chatShortcutIds)
            ShortcutManagerCompat.removeAllDynamicShortcuts(context)
        } catch (e: Throwable) {
            logError("Removing the conversation shortcuts failed: ${e.describe()}")
        }
    }

    override fun cancelMessageNotifications(ids: List<Int>) {
        ids.forEach { cancelNotification(it) }
    }

    private fun createChannelIfNeeded() {
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
