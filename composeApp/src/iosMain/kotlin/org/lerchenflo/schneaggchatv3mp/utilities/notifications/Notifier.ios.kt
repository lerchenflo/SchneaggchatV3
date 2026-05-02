package org.lerchenflo.schneaggchatv3mp.utilities.notifications

import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter
import platform.UIKit.UIApplication

actual class Notifier {

    actual fun show(content: NotificationContent) {
        val notifContent = UNMutableNotificationContent().apply {
            setTitle(content.title)
            setBody(content.body)
            setSound(UNNotificationSound.defaultSound())
        }
        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = content.id.toString(),
            content = notifContent,
            trigger = null,
        )
        UNUserNotificationCenter.currentNotificationCenter()
            .addNotificationRequest(request, withCompletionHandler = null)
    }

    actual suspend fun getToken(): String {
        return IosPushTokenStore.token.filterNotNull().first()
    }

    actual suspend fun deleteToken() {
        IosPushTokenStore.clear()
    }

    actual fun initialize() {
        UNUserNotificationCenter.currentNotificationCenter()
            .requestAuthorizationWithOptions(
                UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge,
            ) { granted, _ ->
                if (granted) {
                    UIApplication.sharedApplication.registerForRemoteNotifications()
                }
            }
    }

    actual fun cancel(id: Int) {
        UNUserNotificationCenter.currentNotificationCenter()
            .removeDeliveredNotificationsWithIdentifiers(listOf(id.toString()))
    }

    actual fun cancelAll() {
        UNUserNotificationCenter.currentNotificationCenter()
            .removeAllDeliveredNotifications()
    }
}
