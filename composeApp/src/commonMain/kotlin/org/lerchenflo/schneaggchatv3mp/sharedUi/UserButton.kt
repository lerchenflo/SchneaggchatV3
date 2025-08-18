package org.lerchenflo.schneaggchatv3mp.sharedUi

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.lerchenflo.schneaggchatv3mp.database.User
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.icon_nutzer
import schneaggchatv3mp.composeapp.generated.resources.no_status
import schneaggchatv3mp.composeapp.generated.resources.noti_bell
import schneaggchatv3mp.composeapp.generated.resources.notification_bell
import schneaggchatv3mp.composeapp.generated.resources.profile_picture
import schneaggchatv3mp.composeapp.generated.resources.unknown_user

/**
 * Can be used everywhere where the Username as well as additional information needs to be shown
 * @param user pass User object
 * @param showProfilePicture if true shows the profile picture of the user on the left
 * @param unreadMessages shows an Unread Messages Icon next to the Username
 * @param lastMessage pass lastMessage (Message) Object (not done yet) to show the message as well as the time
 * @param bottomTextOverride if null -> bottom Text displays status, if empty -> Text disappeares, if not empty -> overrides text. Default: Empty String
 * @param useOnClickGes select one OnClickListener for everything or separate listeners for text and Image
 * @param onClickGes OnClickListener for everything
 * @param onClickText OnClickListener only for the Text
 * @param onClickImage OnClickListener only for the Image (profile picture)
 */

@Preview
@Composable
fun UserButton(
    user: User?,
    showProfilePicture: Boolean = true,
    unreadMessages: Boolean = false,
    lastMessage: String? = null, // todo wenn die messageklasse exisitier a Message übergeaba
    bottomTextOverride: String? = "",
    useOnClickGes: Boolean = true,
    onClickGes: () -> Unit = {},  // Add click for everything
    onClickText: () -> Unit = {},  // Add click for name ...
    onClickImage: () -> Unit = {}  // Add click for image (profilepicture)
) {
    var modifierGes = Modifier
        .fillMaxWidth()
        .height(IntrinsicSize.Min) // Use minimal intrinsic height
    if(useOnClickGes){
        modifierGes = modifierGes.clickable{onClickGes()}
    }
    Row(
        modifier = modifierGes,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if(showProfilePicture){
            // Profile picture
            var modifierImage = Modifier
                .size(48.dp) // Use square aspect ratio
                .padding(end = 8.dp) // Right padding only
                .clip(CircleShape) // Circular image
            if(!useOnClickGes){
                modifierImage = modifierImage.clickable{onClickImage()}
            }
            Image(
                painter = painterResource(Res.drawable.icon_nutzer),
                contentDescription = stringResource(Res.string.profile_picture),
                modifier = modifierImage
            )
        }

        // User info column
        var modifierInfo = Modifier
            .weight(1f)
        if(!useOnClickGes){
            modifierInfo = modifierInfo.clickable {onClickText()} // onclick to open chat
        }
        Column(
            modifier = modifierInfo
        ) {
            // Username + unread Message symbol
            Row(

            ){
                Text(
                    text = user?.name ?: stringResource(Res.string.unknown_user),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if(unreadMessages){
                    Image(
                        painter = painterResource(Res.drawable.noti_bell),
                        contentDescription = stringResource(Res.string.notification_bell),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier
                            .size(20.dp)
                    )
                }

            }

            // todo unreadMessages

            Spacer(modifier = Modifier.height(4.dp))

            if (!lastMessage.isNullOrEmpty()) { // todo: check if lastmessage isch existent
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Last message preview
                    Text(
                        text = "Placeholder message text",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Time indicator
                    Text(
                        text = "16:15",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            // Bottom Text (status ...)
            if(bottomTextOverride == null || !bottomTextOverride.isEmpty()){

                Text(
                    text = bottomTextOverride ?: (user?.status ?: // override if not null
                        stringResource(Res.string.no_status)),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                )
            }
        }
    }
}
