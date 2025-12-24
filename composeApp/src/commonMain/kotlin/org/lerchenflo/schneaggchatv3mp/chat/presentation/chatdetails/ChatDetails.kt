package org.lerchenflo.schneaggchatv3mp.chat.presentation.chatdetails

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.app.GlobalViewModel
import org.lerchenflo.schneaggchatv3mp.chat.domain.NotSelected
import org.lerchenflo.schneaggchatv3mp.chat.domain.SelectedChat
import org.lerchenflo.schneaggchatv3mp.chat.domain.SelectedChatBase
import org.lerchenflo.schneaggchatv3mp.chat.domain.isNotSelected
import org.lerchenflo.schneaggchatv3mp.sharedUi.ActivityTitle
import org.lerchenflo.schneaggchatv3mp.sharedUi.NormalButton
import org.lerchenflo.schneaggchatv3mp.sharedUi.ProfilePictureBigDialog
import org.lerchenflo.schneaggchatv3mp.sharedUi.ProfilePictureView
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.no_description
import schneaggchatv3mp.composeapp.generated.resources.no_status
import schneaggchatv3mp.composeapp.generated.resources.others_say_about
import schneaggchatv3mp.composeapp.generated.resources.remove_friend
import schneaggchatv3mp.composeapp.generated.resources.status
import schneaggchatv3mp.composeapp.generated.resources.status_info
import schneaggchatv3mp.composeapp.generated.resources.unknown_user

@Composable
@Preview
fun ChatDetails(
    modifier: Modifier = Modifier
        .fillMaxWidth()
) {

    val chatdetailsViewmodel = koinViewModel<ChatDetailsViewmodel>()

    val selectedChat by chatdetailsViewmodel.selectedChat.collectAsStateWithLifecycle()
    val group = selectedChat.isGroup


    if (selectedChat.isNotSelected()){
        println("Chat not selected, navigating back")
        chatdetailsViewmodel.onBackClick()
    }


    var profilePictureDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
    ){
        ActivityTitle(
            title = selectedChat.name,
            onBackClick = {
                chatdetailsViewmodel.onBackClick()
            }
        )
        HorizontalDivider()
        // Profile picture
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
        ){

            ProfilePictureView(
                filepath = selectedChat.profilePictureUrl,
                modifier = Modifier
                    .size(200.dp) // Use square aspect ratio
                    .padding(bottom = 10.dp)
                    .clickable {
                        profilePictureDialog = true
                    }
            )

        }

        //TODO: Augenkrebs die activity buggla buggla

        // Id azoaga
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
        ){
            Text(
                text = "id: ${selectedChat.id }"
            )
        }

        // nur für user Status azoaga
        if(!group){
            HorizontalDivider()
            // Status
            val statusInfoString = stringResource(Res.string.status_info)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 10.dp,
                        end = 10.dp,
                        bottom = 10.dp
                    )
                    .clickable{
                        SnackbarManager.showMessage(statusInfoString)
                    }
            ){
                Column(){
                    Text(
                        text = stringResource(Res.string.status),
                        modifier = Modifier

                    )
                    Text(
                        text = selectedChat.status
                            .takeIf { !it.isNullOrBlank() }
                            ?.replace("\\n", "\n")
                            ?: stringResource(Res.string.no_status),
                        softWrap = true,
                        maxLines = 20
                    )
                }

            }
        }
        HorizontalDivider()
        // description
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 10.dp,
                    end = 10.dp,
                )
                .clickable{
                    // todo popup to change text
                }
        ){
            Column(){
                Text(
                    text = stringResource(Res.string.others_say_about, selectedChat.name),
                    modifier = Modifier

                )
                Text(
                    text = selectedChat.description
                        .takeIf { !it.isNullOrBlank() }
                        ?.replace("\\n", "\n")
                        ?: stringResource(Res.string.no_description),
                    softWrap = true,
                    maxLines = 20
                )

            }

        }
        HorizontalDivider()

        if(group){
            // todo show users from group

        }else{
            // todo show gemeinsame gruppen

        }

        HorizontalDivider()

        // remove friend
        NormalButton(
            text = stringResource(Res.string.remove_friend),
            onClick = {
                // todo
            },
            modifier = Modifier
                .fillMaxWidth()
        )

        // Profilbild größer azoaga
        if(profilePictureDialog){
            ProfilePictureBigDialog(
                onDismiss = {profilePictureDialog = false},
                filepath = selectedChat.profilePictureUrl
            )
        }

    }
}