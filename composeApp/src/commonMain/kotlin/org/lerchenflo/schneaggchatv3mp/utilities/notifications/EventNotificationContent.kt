package org.lerchenflo.schneaggchatv3mp.utilities.notifications

import schneaggchatv3mp.composeapp.generated.resources.system_event_changed
import schneaggchatv3mp.composeapp.generated.resources.notification_fallback_body
import schneaggchatv3mp.composeapp.generated.resources.event_reminder_noti_body_at
import org.jetbrains.compose.resources.getString
import org.lerchenflo.schneaggchatv3mp.events.domain.EventUpdateChange
import org.lerchenflo.schneaggchatv3mp.events.presentation.leadTimeTextOf
import org.lerchenflo.schneaggchatv3mp.utilities.millisToString
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.event_reminder_noti_body
import schneaggchatv3mp.composeapp.generated.resources.event_reminder_noti_title
import schneaggchatv3mp.composeapp.generated.resources.event_update_start_and_location
import schneaggchatv3mp.composeapp.generated.resources.system_event_cancelled
import schneaggchatv3mp.composeapp.generated.resources.system_event_location_changed
import schneaggchatv3mp.composeapp.generated.resources.system_event_start_changed

private const val EVENT_DATE_FORMAT = "dd.MM.yyyy HH:mm"

/**
 * "Reminder: Grillabend" / "Starts in 1 day". Shared by the socket and the push path. Without a
 * lead time it names the start instead, so a reminder is never dropped for a missing field.
 */
suspend fun eventReminderNotificationContent(
    eventId: String,
    eventTitle: String,
    leadMinutes: Int,
    startDate: Long,
): NotificationContent = NotificationContent(
    id = eventNotificationId(eventId),
    title = getString(Res.string.event_reminder_noti_title, eventTitle),
    body = when {
        leadMinutes > 0 -> getString(Res.string.event_reminder_noti_body, leadTimeTextOf(leadMinutes))
        startDate > 0 -> getString(Res.string.event_reminder_noti_body_at, millisToString(startDate, EVENT_DATE_FORMAT))
        else -> getString(Res.string.notification_fallback_body)
    },
    eventId = eventId.ifBlank { null },
    kind = NotificationKind.SOCIAL,
)

/**
 * Event title as the title, what changed as the body. [changes] this client doesn't know get the
 * generic "X updated the event", so the alert is never dropped. Shared by the socket and the push path.
 */
suspend fun eventUpdateNotificationContent(
    eventId: String,
    eventTitle: String,
    changes: Set<EventUpdateChange>,
    startDate: Long,
    actorName: String,
): NotificationContent {
    val start = millisToString(startDate, EVENT_DATE_FORMAT)
    val body = when {
        EventUpdateChange.CANCELLED in changes -> getString(Res.string.system_event_cancelled, actorName)
        EventUpdateChange.START in changes && EventUpdateChange.LOCATION in changes ->
            getString(Res.string.event_update_start_and_location, actorName, start)
        EventUpdateChange.START in changes -> getString(Res.string.system_event_start_changed, actorName, start)
        EventUpdateChange.LOCATION in changes -> getString(Res.string.system_event_location_changed, actorName)
        else -> getString(Res.string.system_event_changed, actorName)
    }
    return NotificationContent(
        id = eventNotificationId(eventId),
        title = eventTitle.ifBlank { NOTIFICATION_FALLBACK_TITLE },
        body = body,
        // A cancelled event is gone, the tap then just opens the events screen
        eventId = eventId.ifBlank { null },
        kind = NotificationKind.SOCIAL,
    )
}

/**
 * Plain alert from the raw fields, for when building the localized one failed - shown instead of
 * nothing. Never throws.
 */
suspend fun eventFallbackNotificationContent(eventId: String, eventTitle: String): NotificationContent =
    NotificationContent(
        id = eventNotificationId(eventId),
        title = eventTitle.ifBlank { NOTIFICATION_FALLBACK_TITLE },
        body = runCatching { getString(Res.string.notification_fallback_body) }.getOrDefault(""),
        eventId = eventId.ifBlank { null },
        kind = NotificationKind.SOCIAL,
    )
