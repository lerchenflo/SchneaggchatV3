package org.lerchenflo.schneaggchatv3mp.chat.Presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.database.User
import org.lerchenflo.schneaggchatv3mp.sharedUi.UserButton
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.*

@Composable
fun Chatauswahlscreen(
    onChatSelected: (User) -> Unit,  // navigation callback
    onNewChatClick: () -> Unit,
    modifier: Modifier = Modifier
        .safeContentPadding()
) {

    val sharedViewModel = koinViewModel<SharedViewModel>()

    val users by sharedViewModel.getAllUsers().collectAsState(initial = emptyList())

    //Hauptlayout
    Column(
        modifier = modifier

    ) {
        //Obere Zeile für Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically

        ) {
            BasicText(
                text = stringResource(Res.string.app_name),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp),
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                ),
                autoSize = TextAutoSize.StepBased(
                    minFontSize = 20.sp,
                    maxFontSize = 30.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                val size = 40.dp
                Image(
                    painterResource(Res.drawable.tools_and_games),
                    contentDescription = stringResource(Res.string.tools_and_games),
                    modifier = Modifier
                        .size(size)
                        .clickable { SnackbarManager.showMessage("Es gibt noch koa spiele und o koa tools") },
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                )
                Spacer(Modifier.width(4.dp))
                Image(
                    painterResource(Res.drawable.schneaggmap),
                    contentDescription = stringResource(Res.string.schneaggmap),
                    modifier = Modifier
                        .size(size)
                        .clickable { SnackbarManager.showMessage("Es gibt noch koa schneaggmap") },
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                )
                Spacer(Modifier.width(4.dp))
                Image(
                    painterResource(Res.drawable.settings_gear),
                    contentDescription = stringResource(Res.string.settings),
                    modifier = Modifier
                        .size(size)
                        .clickable { SnackbarManager.showMessage("Es gibt noch koa settings") },
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                )
            }
        }

        //Zweite Zeile für Freund suchen
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = "",
                onValueChange = { /* TODO: Suchen */ }, //In da datenbank gits a suchfeature
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(Res.string.search_friend)) }
            )

            Spacer(Modifier.width(8.dp))

            Image(
                painterResource(Res.drawable.filter),
                contentDescription = stringResource(Res.string.filter),
                modifier = Modifier
                    .size(48.dp)
                    .clickable { SnackbarManager.showMessage("muasch noch selber suacha") },
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
            )

            Spacer(Modifier.width(8.dp))

            Button(
                onClick = { onNewChatClick() },
                modifier = Modifier.height(48.dp)
            ) {
                Icon(
                    painterResource(Res.drawable.new_chat),
                    null,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        //Gegneranzeige
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
        ) {
            items(users) { user ->
                UserButton(
                    user = user,
                    useOnClickGes = false,
                    unreadMessages = false,
                    lastMessage = null,
                    onClickText = { onChatSelected(user)},
                    onClickImage = {
                        SnackbarManager.showMessage("Imagepreview incoming")
                    }
                )
                HorizontalDivider(
                    thickness = 2.dp
                )
            }
        }
    }
}


