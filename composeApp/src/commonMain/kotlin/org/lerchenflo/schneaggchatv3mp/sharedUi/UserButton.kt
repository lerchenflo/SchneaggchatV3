package org.lerchenflo.schneaggchatv3mp.sharedUi

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import io.ktor.http.ContentDisposition.Companion.File
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import org.lerchenflo.schneaggchatv3mp.GROUPPROFILEPICTURE
import org.lerchenflo.schneaggchatv3mp.GROUPPROFILEPICTURE_FILE_NAME
import org.lerchenflo.schneaggchatv3mp.USERPROFILEPICTURE_FILE_NAME
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageWithReaders
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chatselector.ChatEntity
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chatselector.ChatSelectorItem
import org.lerchenflo.schneaggchatv3mp.utilities.Base64.decodeFromBase64
import org.lerchenflo.schneaggchatv3mp.utilities.PictureManager
import org.lerchenflo.schneaggchatv3mp.utilities.millisToTimeDateOrYesterday
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

@Composable
@Preview
fun UserButton(
    chatSelectorItem: ChatSelectorItem?,
    showProfilePicture: Boolean = true,
    lastMessage: MessageWithReaders? = null,
    bottomTextOverride: String? = "",
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


            //val filepath = "${chatSelectorItem?.id}${if (chatSelectorItem?.gruppe == true) GROUPPROFILEPICTURE_FILE_NAME else USERPROFILEPICTURE_FILE_NAME}"
            //println(filepath)
            //val pictureManager = koinInject<PictureManager>()

            val imageModel = when (chatSelectorItem?.entity) {
                is ChatEntity.GroupEntity -> {
                    chatSelectorItem.entity.groupWithMembers.group.profilePicture
                }
                is ChatEntity.UserEntity -> {
                    chatSelectorItem.entity.user.profilePicture
                }
                null -> ""
            }

            AsyncImage(
                model = imageModel, // Coil supports local file paths, Uris, URLs, etc.
                contentDescription = "Profile picture",
                modifier = modifierImage,
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
                    text = chatSelectorItem?.getName() ?: stringResource(Res.string.unknown_user),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                //Noti wird zoagt wenn i s ned gleasa hob (Es isch a neue nachricht vo jemandem andra)
                //Oder wenn se no ned gsendet worra isch (Es git no kuan reader entry weils denn würgt mit da ids)
                if(lastMessage != null && !lastMessage.isReadbyMe() && lastMessage.message.sent){
                    Image(
                        painter = painterResource(Res.drawable.noti_bell),
                        contentDescription = stringResource(Res.string.notification_bell),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier
                            .size(20.dp)
                    )
                }

            }

            Spacer(modifier = Modifier.height(2.dp))

            if (lastMessage != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Last message preview
                    Text(
                        text = lastMessage.message.content ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    //println("milis to date ${lastMessage.sendDate} result ${millisToTimeDateOrYesterday(lastMessage.sendDate?.toLong() ?: 0L)}")
                    // Time indicator
                    Text(
                        text = millisToTimeDateOrYesterday(lastMessage.message.sendDate?.toLong() ?: 0L),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            // Bottom Text (status ...)
            if(bottomTextOverride == null || !bottomTextOverride.isEmpty()){

                Text(
                    text = bottomTextOverride ?: (chatSelectorItem?.getStatus() ?: // override if not null
                        stringResource(Res.string.no_status)),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                )
            }
        }
    }
}
