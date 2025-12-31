package org.lerchenflo.schneaggchatv3mp.chat.presentation.chatdetails

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.chat.domain.toGroup
import org.lerchenflo.schneaggchatv3mp.chat.domain.toUser
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


@Composable
fun ChatDetails(
    modifier: Modifier = Modifier
        .fillMaxWidth()
) {

    val chatdetailsViewmodel = koinViewModel<ChatDetailsViewmodel>()

    val chatDetails by chatdetailsViewmodel.chatDetails.collectAsStateWithLifecycle()
    
    // Early return if chat not selected - don't render anything
    /*
    if (chatDetails.isNotSelected()){
        println("Chat not selected, navigating back")
        chatdetailsViewmodel.onBackClick()
        return
    }

     */

    val group = chatDetails.isGroup

    var profilePictureDialogShown by remember { mutableStateOf(false) }

    // Profilbild größer azoaga
    if(profilePictureDialogShown){
        ProfilePictureBigDialog(
            onDismiss = {profilePictureDialogShown = false},
            filepath = chatDetails.profilePictureUrl
        )
    }

    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
    ){

        ActivityTitle(
            title = chatDetails.name,
            onBackClick = {
                chatdetailsViewmodel.onBackClick()
            }
        )

        HorizontalDivider()

        Column(
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            // Profile picture
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentWidth(Alignment.CenterHorizontally)
            ){

                ProfilePictureView(
                    filepath = chatDetails.profilePictureUrl,
                    modifier = Modifier
                        .size(200.dp) // Use square aspect ratio
                        .padding(bottom = 10.dp)
                        .clickable {
                            profilePictureDialogShown = true
                        }
                )

            }

            // Status only for user
            if(!group){
                HorizontalDivider()
                // Status
                val statusInfoString = stringResource(Res.string.status_info)

                DescriptionStatusRow(
                    onClick = {
                        SnackbarManager.showMessage(statusInfoString)
                    },
                    titleText = stringResource(Res.string.status),
                    bodyText = chatDetails.status
                        .takeIf { !it.isNullOrBlank() }
                        ?.replace("\\n", "\n")
                        ?: stringResource(Res.string.no_status)
                )

            }

            HorizontalDivider()


            // description for group and user
            var showDescriptionChangeDialog by retain { mutableStateOf(false) } // retain dass ma es handy dräha kann (neue compose ding)

            if(showDescriptionChangeDialog){
                ChangeDescription(
                    onDismiss = {showDescriptionChangeDialog = false},
                    viewModel = chatdetailsViewmodel,
                    selectedChat = chatDetails
                )
            }

            DescriptionStatusRow(
                onClick = { showDescriptionChangeDialog = true },
                titleText = stringResource(Res.string.others_say_about, chatDetails.name),
                bodyText = chatDetails.description
                    .takeIf { !it.isNullOrBlank() }
                    ?.replace("\\n", "\n")
                    ?: stringResource(Res.string.no_description)
            )

            HorizontalDivider()

            //Common groups / Common friends
            if(group){
                chatDetails.toGroup()?.let { groupChat ->
                    GroupMembersView(
                        members = groupChat.groupMembersWithUsers,
                        viewmodel = chatdetailsViewmodel,
                    )
                }
            }else{
                chatDetails.toUser()?.let { userChat ->
                    CommonGroupsView(
                        groups = userChat.commonGroups,
                        viewmodel = chatdetailsViewmodel
                    )
                }
            }

            HorizontalDivider()

            // remove friend / todo Leave group

            //TODO: Andra button vlt?
            NormalButton(
                text = stringResource(Res.string.remove_friend),
                onClick = {
                    //TODO: Popup & Group leave
                    chatdetailsViewmodel.removeFriend()
                },
                modifier = Modifier
                    .fillMaxWidth()
            )
        }


    }


}
