package org.lerchenflo.schneaggchatv3mp.utilities.notifications

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.lerchenflo.schneaggchatv3mp.utilities.PermissionManager
import org.lerchenflo.schneaggchatv3mp.utilities.PermissionState
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.mark_as_read
import kotlin.coroutines.resume

private const val CHANNEL_ID = "schneaggchat_messages"
private const val CHANNEL_NAME = "Messages"
const val EXTRA_FROM_NOTIFICATION = "from_notification"

actual class Notifier(private val context: Context, private val permissionManager: PermissionManager) {

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
        
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_FROM_NOTIFICATION, true)
        } ?: Intent().apply {
            setClassName(context, "org.lerchenflo.androidApp.MainActivity")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_FROM_NOTIFICATION, true)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            content.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(NotificationConfig.iconResId)
            .setContentTitle(content.title)
            .setContentText(content.body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        //Only message notifications carry a chat, only those can be marked as read
        content.chatId?.let { chatId ->
            //Tag the notification so every notification of this chat can be dismissed at once
            builder.addExtras(Bundle().apply { putString(EXTRA_CHAT_ID, chatId) })
            builder.addAction(
                NotificationConfig.markAsReadIconResId,
                runBlocking { getString(Res.string.mark_as_read) },
                markAsReadIntent(chatId, content.groupChat, content.id)
            )
        }

        val notification = builder.build()
        if (runBlocking { hasPermission() }) {
            @SuppressLint("MissingPermission")
            NotificationManagerCompat.from(context).notify(content.id, notification)
        }
    }

    actual fun cancelNotification(id: Int) {
        NotificationManagerCompat.from(context).cancel(id)
    }

    actual fun cancelNotifications(ids: List<Int>) {
        ids.forEach { NotificationManagerCompat.from(context).cancel(it) }
    }

    actual fun cancelAllNotifications() {
        NotificationManagerCompat.from(context).cancelAll()
    }

    actual fun cancelMessageNotifications(ids: List<Int>) {
        ids.forEach { NotificationManagerCompat.from(context).cancel(it) }
    }

    private fun markAsReadIntent(chatId: String, groupChat: Boolean, notificationId: Int): PendingIntent {
        val intent = Intent(context, MarkAsReadReceiver::class.java).apply {
            action = ACTION_MARK_AS_READ
            putExtra(EXTRA_CHAT_ID, chatId)
            putExtra(EXTRA_GROUP_CHAT, groupChat)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        return PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createChannelIfNeeded() {
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
