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

// Friend's picture when known, else app's generic default avatar (never guess a URL for a non-friend)
@Composable
fun EventUserAvatar(
    userId: String,
    friendsById: Map<String, User>,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val pictureUrl = friendsById[userId]?.profilePictureUrl

    if (!pictureUrl.isNullOrBlank()) {
        ProfilePictureView(
            filepath = pictureUrl,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
        )
    } else {
        Image(
            painter = painterResource(Res.drawable.icon_nutzer),
            contentDescription = null,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
    }
}
