package org.lerchenflo.androidApp.notifications

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.runBlocking
import org.koin.mp.KoinPlatform
import org.lerchenflo.androidApp.R
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.utilities.notifications.NotificationChannels
import org.lerchenflo.schneaggchatv3mp.utilities.notifications.NotificationContentBuilder

class AppFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        if (remoteMessage.data.isEmpty()) return
        runBlocking {
            try {
                val koin = KoinPlatform.getKoin()
                val preferencemanager = koin.get<Preferencemanager>()
                val encryptionKey = preferencemanager.getEncryptionKey()

                val content = NotificationContentBuilder.fromMap(remoteMessage.data, encryptionKey) ?: return@runBlocking
                showNotification(content.id, content.title, content.body, content.channelId)

                SessionCache.login(tokens = preferencemanager.getTokens(), developer = false)
                koin.get<AppRepository>().messageIdSync()
            } catch (e: Exception) {
                showNotification(
                    id = 1,
                    title = "Schneaggchat",
                    body = "Failed to process notification",
                    channelId = NotificationChannels.SYSTEM,
                )
            }
        }
    }

    override fun onNewToken(token: String) {
        runBlocking {
            try {
                val koin = KoinPlatform.getKoin()
                val repo = koin.get<AppRepository>()
                repo.setNotificationToken(token)
            } catch (_: Exception) {}
        }
    }

    private fun showNotification(id: Int, title: String, body: String, channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) return
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.schneaggchat_logo_v3_transparent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(this).notify(id, notification)
    }
}
