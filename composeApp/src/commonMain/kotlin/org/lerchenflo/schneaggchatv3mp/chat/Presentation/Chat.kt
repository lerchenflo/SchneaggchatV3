package org.lerchenflo.schneaggchatv3mp.chat.Presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.lerchenflo.schneaggchatv3mp.sharedUi.UserButton
import org.lerchenflo.schneaggchatv3mp.theme.SchneaggchatTheme
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.new_chat

@Preview
@Composable
fun ChatScreen(
    sharedViewModel: SharedViewModel,
    modifier: Modifier = Modifier
        .fillMaxSize()
        .safeContentPadding()
){
    SchneaggchatTheme{ // theme wida setza
        Column(
            modifier = modifier
        ){
            // Obere Zeile (Backbutton, profilbild, name, ...)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp, 0.dp, 10.dp, 16.dp)
            ){
                // Backbutton
                Image(
                    painterResource(Res.drawable.new_chat), //todo backbutton icon gschid
                    contentDescription = "backbutton", // todo strings
                    modifier = Modifier
                        .size(40.dp)
                        .clickable {
                            // todo joo iwie zruck
                            SnackbarManager.showMessage("kof da a gschid handy mit am zruckbutton")
                                   },
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                )


                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserButton(
                        user = sharedViewModel.selectedChat.value,
                        showBottomText = true,
                        onClickGes = {
                            // todo open chatdetails
                            SnackbarManager.showMessage("Bald chatdetails")
                        }
                    )
                }
            }
        }

    }
}