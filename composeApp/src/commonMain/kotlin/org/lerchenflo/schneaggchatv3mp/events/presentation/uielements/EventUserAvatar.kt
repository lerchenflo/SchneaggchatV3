package org.lerchenflo.schneaggchatv3mp.events.presentation.uielements

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import org.jetbrains.compose.resources.painterResource
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.sharedUi.picture.ProfilePictureView
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.icon_nutzer

// Known user's picture (a friend, or the logged-in user themselves), else the app's generic
// default avatar - never guess a URL for a user we have no record of.
@Composable
fun EventUserAvatar(
    userId: String,
    usersById: Map<String, User>,
    size: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val pictureUrl = usersById[userId]?.profilePictureUrl

    if (!pictureUrl.isNullOrBlank()) {
        ProfilePictureView(
            filepath = pictureUrl,
            contentDescription = contentDescription,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
        )
    } else {
        Image(
            painter = painterResource(Res.drawable.icon_nutzer),
            contentDescription = contentDescription,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
    }
}
