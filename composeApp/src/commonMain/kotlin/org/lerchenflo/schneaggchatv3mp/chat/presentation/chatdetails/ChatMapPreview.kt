package org.lerchenflo.schneaggchatv3mp.chat.presentation.chatdetails

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.events.domain.Event
import org.lerchenflo.schneaggchatv3mp.events.domain.icon
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.EventMarkerLayer
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.StaticMapPreview
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.UserMarkerLayer
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.rememberUserMarkers
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.toPosition
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.chat_details_show_on_map

/** What the map preview of the chat details shows. */
data class ChatMapPreviewState(
    /** Friends with a valid shared location: the user of a user chat, the members of a group. */
    val users: List<User>,
    val onlineUserIds: Set<String>,
    /** The event the group was created for, only when it has a location. */
    val event: Event?,
    /** The own answer to [event] is "going"; decides the pin color, same as on the Schneaggmap. */
    val isEventJoined: Boolean,
    val styleUrl: String,
) {
    /** Every shown position, for the camera. */
    val focus: List<LatLong>
        get() = users.mapNotNull { user -> user.location?.let { LatLong(lat = it.lat, long = it.long) } } +
            listOfNotNull(event?.location)
}

/**
 * Small map in the chat details with the same user and event markers as the Schneaggmap. A tap
 * opens the Schneaggmap (on the user, or with every pin in view for a group).
 */
@Composable
fun ChatMapPreview(
    preview: ChatMapPreviewState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val userMarkers = rememberUserMarkers(preview.users) { it.id in preview.onlineUserIds }

    StaticMapPreview(
        focus = preview.focus,
        styleUrl = preview.styleUrl,
        onClick = onClick,
        contentDescription = stringResource(Res.string.chat_details_show_on_map),
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        height = 180.dp,
    ) {
        preview.event?.let { event ->
            val location = event.location ?: return@let
            EventMarkerLayer(
                eventId = event.id,
                position = location.toPosition(),
                icon = event.type.icon(),
                isJoined = preview.isEventJoined,
            )
        }
        preview.users.forEach { user ->
            val location = user.location ?: return@forEach
            UserMarkerLayer(
                userId = user.id,
                position = LatLong(lat = location.lat, long = location.long).toPosition(),
                marker = userMarkers[user.id]?.marker,
            )
        }
    }
}
