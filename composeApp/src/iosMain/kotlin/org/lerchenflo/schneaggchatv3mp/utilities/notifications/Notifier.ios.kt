package org.lerchenflo.schneaggchatv3mp.utilities.notifications

import platform.UIKit.UIApplication
import platform.UIKit.unregisterForRemoteNotifications
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter
import org.lerchenflo.schneaggchatv3mp.utilities.PermissionManager
import org.lerchenflo.schneaggchatv3mp.utilities.PermissionState

actual class Notifier(private val permissionManager: PermissionManager) {

    actual suspend fun getToken(): String? = IosPushTokenStore.getToken()

    actual suspend fun removeToken() {
        IosPushTokenStore.clearToken()
        UIApplication.sharedApplication.unregisterForRemoteNotifications()
    }

    actual suspend fun hasPermission(): Boolean =
        permissionManager.checkNotificationPermission() == PermissionState.GRANTED

    actual fun showLocalNotification(content: NotificationContent) {
        val unc = UNUserNotificationCenter.currentNotificationCenter()
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

    actual fun cancelAllNotifications() {
        UNUserNotificationCenter.currentNotificationCenter().removeAllDeliveredNotifications()
        UNUserNotificationCenter.currentNotificationCenter().removeAllPendingNotificationRequests()
    }

    actual fun cancelMessageNotifications(ids: List<Int>) {
        // APNs delivers notifications with server-assigned identifiers unknown to the client.
        // Per-id cancellation cannot match them, so wipe all delivered notifications.
        UNUserNotificationCenter.currentNotificationCenter().removeAllDeliveredNotifications()
        UNUserNotificationCenter.currentNotificationCenter().removeAllPendingNotificationRequests()
    }
}
