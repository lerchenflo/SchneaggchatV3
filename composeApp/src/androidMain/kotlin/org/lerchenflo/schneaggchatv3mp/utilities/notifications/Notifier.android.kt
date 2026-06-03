package org.lerchenflo.schneaggchatv3mp.utilities.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private const val CHANNEL_ID = "schneaggchat_messages"
private const val CHANNEL_NAME = "Messages"
const val EXTRA_FROM_NOTIFICATION = "from_notification"

actual class Notifier(private val context: Context) {

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
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
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
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(NotificationConfig.iconResId)
            .setContentTitle(content.title)
            .setContentText(content.body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        if (hasNotificationPermission()) {
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

    private fun createChannelIfNeeded() {
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
