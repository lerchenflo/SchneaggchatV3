package org.lerchenflo.schneaggchatv3mp.utilities.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import org.lerchenflo.schneaggchatv3mp.androidApp.R

actual class Notifier(private val context: Context) {

    actual fun show(content: NotificationContent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) return
        }
        val notification = NotificationCompat.Builder(context, content.channelId)
            .setContentTitle(content.title)
            .setContentText(content.body)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(content.id, notification)
    }

    actual suspend fun getToken(): String = suspendCancellableCoroutine { cont ->
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token -> cont.resume(token) }
            .addOnFailureListener { cont.resume("") }
    }

    actual suspend fun deleteToken(): Unit = suspendCancellableCoroutine { cont ->
        FirebaseMessaging.getInstance().deleteToken()
            .addOnSuccessListener { cont.resume(Unit) }
            .addOnFailureListener { cont.resume(Unit) }
    }

    actual fun initialize() {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(NotificationChannels.MESSAGES, "Messages", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "New chat messages"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(NotificationChannels.FRIEND_REQUESTS, "Friend requests", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Friend request notifications"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(NotificationChannels.SYSTEM, "System", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "System notifications"
            }
        )
    }

    actual fun cancel(id: Int) {
        NotificationManagerCompat.from(context).cancel(id)
    }

    actual fun cancelAll() {
        NotificationManagerCompat.from(context).cancelAll()
    }
}
