package org.lerchenflo.schneaggchatv3mp.events.domain

import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.group_delete_delay_never
import schneaggchatv3mp.composeapp.generated.resources.group_delete_delay_one_day
import schneaggchatv3mp.composeapp.generated.resources.group_delete_delay_one_hour
import schneaggchatv3mp.composeapp.generated.resources.group_delete_delay_one_week
import schneaggchatv3mp.composeapp.generated.resources.group_delete_delay_three_days

// How long after the event's close date (or start date if there's no close date) the
// connected group chat gets auto-deleted. Chosen by the event's creator.
@Serializable
enum class GroupDeleteDelay {
    NEVER,
    ONE_HOUR,
    ONE_DAY,
    THREE_DAYS,
    ONE_WEEK,
}

fun GroupDeleteDelay.labelRes(): StringResource = when (this) {
    GroupDeleteDelay.NEVER -> Res.string.group_delete_delay_never
    GroupDeleteDelay.ONE_HOUR -> Res.string.group_delete_delay_one_hour
    GroupDeleteDelay.ONE_DAY -> Res.string.group_delete_delay_one_day
    GroupDeleteDelay.THREE_DAYS -> Res.string.group_delete_delay_three_days
    GroupDeleteDelay.ONE_WEEK -> Res.string.group_delete_delay_one_week
}
