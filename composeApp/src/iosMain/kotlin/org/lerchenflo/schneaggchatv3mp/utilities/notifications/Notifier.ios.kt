package org.lerchenflo.schneaggchatv3mp.utilities.notifications

import platform.UIKit.UIApplication
import platform.UIKit.unregisterForRemoteNotifications
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

actual class Notifier {

    actual suspend fun getToken(): String? = IosPushTokenStore.getToken()

    actual suspend fun removeToken() {
        IosPushTokenStore.clearToken()
        UIApplication.sharedApplication.unregisterForRemoteNotifications()
    }

    actual suspend fun hasPermission(): Boolean = suspendCoroutine { cont ->
        UNUserNotificationCenter.currentNotificationCenter()
            .getNotificationSettingsWithCompletionHandler { settings ->
                cont.resume(settings?.authorizationStatus == UNAuthorizationStatusAuthorized)
            }
    }

    actual fun showLocalNotification(content: NotificationContent) {
        val unc = UNUserNotificationCenter.currentNotificationCenter()
        val notifContent = UNMutableNotificationContent()
        notifContent.title = content.title
        notifContent.body = content.body
        notifContent.sound = UNNotificationSound.defaultSound()
        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = content.id.toString(),
            content = notifContent,
            trigger = null,
        )
        unc.addNotificationRequest(request) { error ->
            if (error != null) println("[Notifier.ios] addNotificationRequest error: ${error.localizedDescription}")
        }
    }

    actual fun cancelNotification(id: Int) {
        val ids = listOf(id.toString())
        UNUserNotificationCenter.currentNotificationCenter()
            .removeDeliveredNotificationsWithIdentifiers(ids)
        UNUserNotificationCenter.currentNotificationCenter()
            .removePendingNotificationRequestsWithIdentifiers(ids)
    }

    actual fun cancelNotifications(ids: List<Int>) {
        val strIds = ids.map { it.toString() }
        UNUserNotificationCenter.currentNotificationCenter()
            .removeDeliveredNotificationsWithIdentifiers(strIds)
        UNUserNotificationCenter.currentNotificationCenter()
            .removePendingNotificationRequestsWithIdentifiers(strIds)
    }
}
