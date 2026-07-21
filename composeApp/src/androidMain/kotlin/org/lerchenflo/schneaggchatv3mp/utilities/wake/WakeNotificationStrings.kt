package org.lerchenflo.schneaggchatv3mp.utilities.wake

import org.jetbrains.compose.resources.getString
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.wake_notification_title
import schneaggchatv3mp.composeapp.generated.resources.wake_notification_title_group
import schneaggchatv3mp.composeapp.generated.resources.wake_stop
import schneaggchatv3mp.composeapp.generated.resources.wake_woken_alongside

/**
 * Localized texts for the wake alarm notification. Resolved off the critical path because
 * Compose Resources lookups are suspending and startForeground cannot wait for them.
 */
data class WakeNotificationStrings(
    val title: String,
    val body: String,
    val stopLabel: String,
) {
    companion object {
        suspend fun resolve(
            senderName: String,
            groupName: String,
            reason: String,
            wokenUserCount: Int,
            wokenDeviceCount: Int,
        ): WakeNotificationStrings {
            val title = if (groupName.isNotEmpty()) {
                getString(Res.string.wake_notification_title_group, senderName, groupName)
            } else {
                getString(Res.string.wake_notification_title, senderName)
            }

            val body = buildString {
                append(reason)
                //Only worth mentioning when somebody else is being woken too.
                if (wokenUserCount > 1) {
                    if (isNotEmpty()) append(" · ")
                    append(getString(Res.string.wake_woken_alongside, wokenUserCount, wokenDeviceCount))
                }
            }

            return WakeNotificationStrings(
                title = title,
                body = body,
                stopLabel = getString(Res.string.wake_stop),
            )
        }
    }
}
