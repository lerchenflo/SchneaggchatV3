package org.lerchenflo.schneaggchatv3mp.events.presentation

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.event_lead_days
import schneaggchatv3mp.composeapp.generated.resources.event_lead_hours
import schneaggchatv3mp.composeapp.generated.resources.event_lead_minutes

private const val MINUTES_PER_DAY = 24 * 60

private data class LeadTimeParts(val days: Int, val hours: Int, val minutes: Int)

private fun splitLeadTime(totalMinutes: Int) = LeadTimeParts(
    days = totalMinutes / MINUTES_PER_DAY,
    hours = (totalMinutes % MINUTES_PER_DAY) / 60,
    minutes = totalMinutes % 60,
)

/** A lead time in minutes as "1 day 2 h 30 min"; zero parts are left out. */
@Composable
fun leadTimeText(totalMinutes: Int): String {
    val parts = splitLeadTime(totalMinutes)
    val texts = mutableListOf<String>()
    if (parts.days > 0) texts += pluralStringResource(Res.plurals.event_lead_days, parts.days, parts.days)
    if (parts.hours > 0) texts += stringResource(Res.string.event_lead_hours, parts.hours)
    if (parts.minutes > 0 || texts.isEmpty()) texts += stringResource(Res.string.event_lead_minutes, parts.minutes)
    return texts.joinToString(" ")
}

/** Same as [leadTimeText], outside of composition (notifications). */
suspend fun leadTimeTextOf(totalMinutes: Int): String {
    val parts = splitLeadTime(totalMinutes)
    val texts = mutableListOf<String>()
    if (parts.days > 0) texts += getPluralString(Res.plurals.event_lead_days, parts.days, parts.days)
    if (parts.hours > 0) texts += getString(Res.string.event_lead_hours, parts.hours)
    if (parts.minutes > 0 || texts.isEmpty()) texts += getString(Res.string.event_lead_minutes, parts.minutes)
    return texts.joinToString(" ")
}
