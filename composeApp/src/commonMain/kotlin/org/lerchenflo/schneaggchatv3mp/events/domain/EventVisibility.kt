package org.lerchenflo.schneaggchatv3mp.events.domain

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Public
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.event_friends_only
import schneaggchatv3mp.composeapp.generated.resources.event_invited_friends_only
import schneaggchatv3mp.composeapp.generated.resources.event_public

@Serializable
enum class EventVisibility {
    PUBLIC,               // Everyone (all of the creator's friends, even if not invited) can see and join
    FRIENDS_ONLY,         // Only the creator's friends can see and join, invited or not
    INVITED_FRIENDS_ONLY, // Only explicitly invited users can see and join
}

fun EventVisibility.icon(): ImageVector = when (this) {
    EventVisibility.PUBLIC -> Icons.Default.Public
    EventVisibility.FRIENDS_ONLY -> Icons.Default.Groups
    EventVisibility.INVITED_FRIENDS_ONLY -> Icons.Default.PersonAdd
}

fun EventVisibility.labelRes(): StringResource = when (this) {
    EventVisibility.PUBLIC -> Res.string.event_public
    EventVisibility.FRIENDS_ONLY -> Res.string.event_friends_only
    EventVisibility.INVITED_FRIENDS_ONLY -> Res.string.event_invited_friends_only
}
