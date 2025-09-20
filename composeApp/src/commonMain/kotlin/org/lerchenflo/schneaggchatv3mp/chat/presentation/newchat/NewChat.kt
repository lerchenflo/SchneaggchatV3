package org.lerchenflo.schneaggchatv3mp.chat.presentation.newchat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContactMail
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material3.Button
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.sharedUi.ActivityTitle
import org.lerchenflo.schneaggchatv3mp.theme.SchneaggchatTheme
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.invite_friend
import schneaggchatv3mp.composeapp.generated.resources.new_chat
import schneaggchatv3mp.composeapp.generated.resources.new_group
import schneaggchatv3mp.composeapp.generated.resources.settings

@Composable
fun NewChat(
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .safeContentPadding()
){
    SchneaggchatTheme {
        Column(
            modifier = modifier
        ){

            // da text mit am backbutton oba
            ActivityTitle(
                title = stringResource(Res.string.new_chat),
                onBackClick = onBackClick
            )

            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

            // new group
            Option(
                text = stringResource(Res.string.new_group),
                icon = Icons.Default.GroupAdd,
                onClick = {
                    SnackbarManager.showMessage("Gruppe erstellen noch nicht implementiert")
                }
            )

            // invite Friend
            Option(
                text = stringResource(Res.string.invite_friend),
                icon = Icons.Default.ContactMail,
                onClick = {
                    SnackbarManager.showMessage("Gruppe erstellen noch nicht implementiert")
                }
            )



        }

    }

}

// für einheitliche ding wo ma nur an ona stell ändera muss
@Composable
fun Option(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .padding(
            start = 16.dp,
            end = 16.dp,
            top = 20.dp,
            bottom = 2.dp
        )
        .clickable{onClick()}
){
    Box(
        modifier = modifier

    ){
        Row(
            verticalAlignment = Alignment.CenterVertically
        ){
            Icon(
                contentDescription = text,
                modifier = Modifier.size(35.dp),
                tint = MaterialTheme.colorScheme.primary.copy(),
                imageVector = icon
            )
            Spacer(modifier = Modifier.width(24.dp))
            Text(
                text = text,
                maxLines = 1,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}