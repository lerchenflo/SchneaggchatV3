package org.lerchenflo.schneaggchatv3mp.sharedUi

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.MessageWithReadersDto
import org.lerchenflo.schneaggchatv3mp.chat.domain.SelectedChat
import org.lerchenflo.schneaggchatv3mp.utilities.millisToTimeDateOrYesterday
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.no_status
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

@Composable
@Preview
fun UserButton(
    selectedChat: SelectedChat,
    showProfilePicture: Boolean = true,
    lastMessage: MessageWithReadersDto? = null,
    bottomTextOverride: String? = "",
    unreadmessageBubbleCount: Int = 0,
    unsentmessageBubbleCount: Int = 0,
    useOnClickGes: Boolean = true,
    onClickGes: () -> Unit = {},  // Add click for everything
    onClickText: () -> Unit = {},  // Add click for name ...
    onClickImage: () -> Unit = {}  // Add click for image (profilepicture)
) {
    var modifierGes = Modifier
        .fillMaxWidth()
        .padding(6.dp)
        .height(50.dp)
        //.height(IntrinsicSize.Min) // Use minimal intrinsic height
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
                .size(40.dp) // Use square aspect ratio
                .padding(end = 8.dp) // Right padding only
                .clip(CircleShape) // Circular image
            if(!useOnClickGes){
                modifierImage = modifierImage.clickable{onClickImage()}
            }


            ProfilePictureView(
                filepath = selectedChat.profilePicture,
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
                    text = selectedChat.name
                        .takeIf { it.isNotBlank() }
                        ?: stringResource(Res.string.unknown_user),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )


                if (unsentmessageBubbleCount != 0){
                    Badge(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ){
                        Text(
                            text = "$unsentmessageBubbleCount",
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                if (unreadmessageBubbleCount != 0){
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ){
                        Text(
                            text = "${unreadmessageBubbleCount - unsentmessageBubbleCount}", // ungesendete messages sind no ned gleasa vo mir
                        )
                    }
                }

                /*
                //Noti wird zoagt wenn i s ned gleasa hob (Es isch a neue nachricht vo jemandem andra)
                //Oder wenn se no ned gsendet worra isch (Es git no kuan reader entry weils denn würgt mit da ids)
                if(lastMessage != null && !lastMessage.isReadbyMe() && lastMessage.message.sent){
                    Image(
                        painter = painterResource(Res.drawable.noti_bell),
                        contentDescription = stringResource(Res.string.notification_bell),
                        modifier = Modifier
                            .size(20.dp)
                    )
                }

                 */

            }

            Spacer(modifier = Modifier.height(2.dp))

            if (lastMessage != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Last message preview
                    Text(
                        text = lastMessage.messageDto.content ?: "",
                        style = MaterialTheme.typography.bodyMedium,

                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    //println("milis to date ${lastMessage.sendDate} result ${millisToTimeDateOrYesterday(lastMessage.sendDate?.toLong() ?: 0L)}")
                    // Time indicator
                    Text(
                        text = millisToTimeDateOrYesterday(lastMessage.messageDto.sendDate?.toLong() ?: 0L),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            // Bottom Text (status ...)
            if(bottomTextOverride == null || !bottomTextOverride.isEmpty()){

                Text(
                    text = bottomTextOverride
                        ?: selectedChat.status
                            .takeIf { it.isNotBlank() }
                        ?: stringResource(Res.string.no_status),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth()
                )

            }
        }
    }
}
