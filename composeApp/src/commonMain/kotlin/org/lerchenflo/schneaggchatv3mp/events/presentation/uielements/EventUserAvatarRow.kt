package org.lerchenflo.schneaggchatv3mp.events.presentation.uielements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.lerchenflo.schneaggchatv3mp.chat.domain.User

/** Labelled horizontal strip of user avatars - the invited users, and each participation bucket. */
@Composable
fun EventUserAvatarRow(
    label: String?,
    userIds: List<String>,
    friendsById: Map<String, User>,
    modifier: Modifier = Modifier,
    avatarSize: Dp = 40.dp,
    contentDescriptionFor: @Composable (String) -> String? = { null },
) {
    label?.let {
        Text(
            text = it,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
    }
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        items(userIds, key = { it }) { userId ->
            EventUserAvatar(
                userId = userId,
                friendsById = friendsById,
                size = avatarSize,
                contentDescription = contentDescriptionFor(userId)
            )
        }
    }
}
