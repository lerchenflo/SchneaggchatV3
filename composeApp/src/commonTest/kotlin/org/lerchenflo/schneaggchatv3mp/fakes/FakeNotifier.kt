package org.lerchenflo.schneaggchatv3mp.fakes

import org.lerchenflo.schneaggchatv3mp.utilities.notifications.NotificationContent
import org.lerchenflo.schneaggchatv3mp.utilities.notifications.Notifier

/** [Notifier] that shows nothing and records what it was asked to show or cancel. */
class FakeNotifier(
    var token: String? = null,
    var permission: Boolean = true,
) : Notifier {

    val shown = CallLog<NotificationContent>()
    val cancelledIds = CallLog<Int>()
    val cancelledMessageIds = CallLog<List<Int>>()
    val removedTokens = CallLog<Unit>()

    override suspend fun getToken(): String? = token

    override suspend fun removeToken() = removedTokens.record(Unit)

    override suspend fun hasPermission(): Boolean = permission

    override fun showLocalNotification(content: NotificationContent) = shown.record(content)

    override fun cancelNotification(id: Int) = cancelledIds.record(id)

    override fun cancelNotifications(ids: List<Int>) = ids.forEach { cancelledIds.record(it) }

    override fun cancelAllNotifications() = Unit

    override fun cancelMessageNotifications(ids: List<Int>) = cancelledMessageIds.record(ids)
}
