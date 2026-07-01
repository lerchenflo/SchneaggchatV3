package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.utilities.getCurrentTimeMillisLong
import org.lerchenflo.schneaggchatv3mp.utilities.millisToTimeDateOrYesterday
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.icon_nutzer
import schneaggchatv3mp.composeapp.generated.resources.schneaggmap_user_online

private const val ONLINE_THRESHOLD_MILLIS = 2 * 60 * 1000L

@Composable
fun FriendLocationsPreview(friends: List<User>, onUserClick: (User) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        friends.forEach { user ->
            FriendChip(user, onUserClick)
        }
    }
}

@Composable
private fun FriendChip(user: User, onUserClick: (User) -> Unit) {
    val displayName = user.displayName
    val locationDate = user.location?.date
    val isOnline = locationDate != null && (getCurrentTimeMillisLong() - locationDate) < ONLINE_THRESHOLD_MILLIS
    val statusText = when {
        isOnline -> stringResource(Res.string.schneaggmap_user_online)
        locationDate != null -> millisToTimeDateOrYesterday(locationDate)
        else -> "-"
    }

    var profileBitmap by remember(user.profilePictureUrl) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(user.profilePictureUrl) {
        if (user.profilePictureUrl.isNotBlank()) {
            profileBitmap = runCatching {
                SystemFileSystem.source(Path(user.profilePictureUrl)).buffered().readByteArray()
                    .decodeToImageBitmap()
            }.getOrNull()
        }
    }

    val onlineColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainer

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clickable { onUserClick(user) }
            .background(surfaceColor, RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .border(
                    width = 2.dp,
                    color = if (isOnline) onlineColor else MaterialTheme.colorScheme.outline,
                    shape = CircleShape
                )
        ) {
            val bitmap = profileBitmap
            if (bitmap != null) {
                Image(
                    painter = BitmapPainter(bitmap),
                    contentDescription = displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
            } else {
                Image(
                    painter = painterResource(Res.drawable.icon_nutzer),
                    contentDescription = displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
            }
        }

        Column {
            Text(
                text = displayName,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall,
                color = if (isOnline) onlineColor else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
