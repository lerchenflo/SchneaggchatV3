package org.lerchenflo.schneaggchatv3mp.chat.presentation.chatdetails

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import org.lerchenflo.schneaggchatv3mp.app.GlobalViewModel
import org.lerchenflo.schneaggchatv3mp.sharedUi.ActivityTitle
import org.lerchenflo.schneaggchatv3mp.sharedUi.NormalButton
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.icon_nutzer
import schneaggchatv3mp.composeapp.generated.resources.no_description
import schneaggchatv3mp.composeapp.generated.resources.no_status
import schneaggchatv3mp.composeapp.generated.resources.others_say_about
import schneaggchatv3mp.composeapp.generated.resources.profile_picture
import schneaggchatv3mp.composeapp.generated.resources.remove_friend
import schneaggchatv3mp.composeapp.generated.resources.status
import schneaggchatv3mp.composeapp.generated.resources.status_info
import schneaggchatv3mp.composeapp.generated.resources.unknown_user

@Composable
@Preview
fun ChatDetails(
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .safeContentPadding()
) {
    val globalViewModel = koinInject<GlobalViewModel>()
    val selectedChatName = globalViewModel.selectedChat.value?.getName() ?: stringResource(Res.string.unknown_user)
    val group = globalViewModel.selectedChat.value?.gruppe ?: false

    Column(
        modifier = modifier
    ){
        ActivityTitle(
            title = selectedChatName,
            onBackClick = onBackClick
        )
        HorizontalDivider()
        // Profile picture
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
        ){
            Image(
                painter = painterResource(Res.drawable.icon_nutzer),
                contentDescription = stringResource(Res.string.profile_picture),
                modifier = Modifier
                    .size(200.dp) // Use square aspect ratio
                    .padding(bottom = 10.dp)
                    .clip(CircleShape) // Circular image
                    .clickable {
                        //todo: profilbild groß azoaga
                    }
            )
        }

        // Id azoaga
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
        ){
            Text(
                text = "id: ${globalViewModel.selectedChat.value?.id }"
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
                        text = globalViewModel.selectedChat.value?.getStatus()?.replace("\\n", "\n")
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
                    text = stringResource(Res.string.others_say_about, selectedChatName),
                    modifier = Modifier

                )
                Text(
                    text = globalViewModel.selectedChat.value?.getDescription()?.replace("\\n", "\n")
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


    }
}