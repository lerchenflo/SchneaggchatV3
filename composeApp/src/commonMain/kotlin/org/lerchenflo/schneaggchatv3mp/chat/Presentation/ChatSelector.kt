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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.lerchenflo.schneaggchatv3mp.database.tables.User
import org.lerchenflo.schneaggchatv3mp.login.Presentation.InputTextField
import org.lerchenflo.schneaggchatv3mp.sharedUi.RoundLoadingIndicator
import org.lerchenflo.schneaggchatv3mp.sharedUi.UserButton
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.app_name
import schneaggchatv3mp.composeapp.generated.resources.filter
import schneaggchatv3mp.composeapp.generated.resources.loadinginfo
import schneaggchatv3mp.composeapp.generated.resources.new_chat
import schneaggchatv3mp.composeapp.generated.resources.schneaggmap
import schneaggchatv3mp.composeapp.generated.resources.search_friend
import schneaggchatv3mp.composeapp.generated.resources.settings
import schneaggchatv3mp.composeapp.generated.resources.settings_gear
import schneaggchatv3mp.composeapp.generated.resources.tools_and_games

@Preview
@Composable
fun Chatauswahlscreen(
    viewModel: ChatSelectorViewModel = viewModel(
        factory = ChatSelectorViewModel .Factory
    ),
    onChatSelected: (User) -> Unit,  // navigation callback
    onNewChatClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
        .safeContentPadding()
) {

    val users by viewModel.usersState.collectAsStateWithLifecycle() // androidx.lifecycle.compose
    val searchterm by viewModel.searchterm.collectAsState() // read-only display of current search term



    //Hauptlayout
    Column(
        modifier = modifier

    ) {
        //Obere Zeile für Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically

        ) {
            BasicText(
                text = stringResource(Res.string.app_name),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 5.dp),
                style = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                autoSize = TextAutoSize.StepBased(
                    minFontSize = 10.sp,
                    maxFontSize = 25.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val size = 30.dp //Größe vo a ui elemente oba rechts
            val distance = 10.dp //Abstand zwüschat da buttons oba rechts

            //Loadingbar für messages
            RoundLoadingIndicator(
                visible = viewModel.isLoadingMessages,
                onClick = {
                    CoroutineScope(Dispatchers.IO).launch {
                        SnackbarManager.showMessage(getString(Res.string.loadinginfo))
                    }
                },
                strokeWidth = 2.dp,
                size = 18.dp


            )

            Spacer(Modifier.width(distance))


            Image(
                painterResource(Res.drawable.tools_and_games),
                contentDescription = stringResource(Res.string.tools_and_games),
                modifier = Modifier
                    .size(size)
                    .clickable { SnackbarManager.showMessage("Es gibt noch koa spiele und o koa tools") },
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
            )
            Spacer(Modifier.width(distance))
            Image(
                painterResource(Res.drawable.schneaggmap),
                contentDescription = stringResource(Res.string.schneaggmap),
                modifier = Modifier
                    .size(size)
                    .clickable { SnackbarManager.showMessage("Es gibt noch koa schneaggmap") },
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
            )
            Spacer(Modifier.width(distance))
            Image(
                painterResource(Res.drawable.settings_gear),
                contentDescription = stringResource(Res.string.settings),
                modifier = Modifier
                    .size(size)
                    .clickable {onSettingsClick()},
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
            )

        }

        //Zweite Zeile für Freund suchen
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            OutlinedTextField(
                value = searchterm,
                maxLines = 1,
                onValueChange = { viewModel.updateSearchterm(it) }, //In da datenbank gits a suchfeature
                modifier = Modifier
                    .weight(1f),
                placeholder = { Text(stringResource(Res.string.search_friend)) }
            )


            Spacer(Modifier.width(10.dp))

            Image(
                painterResource(Res.drawable.filter),
                contentDescription = stringResource(Res.string.filter),
                modifier = Modifier
                    .size(30.dp)
                    .clickable { SnackbarManager.showMessage("muasch noch selber suacha") },
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
            )

            Spacer(Modifier.width(10.dp))

            Button(
                onClick = { onNewChatClick() },
                modifier = Modifier
                    .size(48.dp)
                    .weight(0.4f)
            ) {
                Icon(
                    painterResource(Res.drawable.new_chat),
                    "Add friend",
                    modifier = Modifier.size(20.dp)
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
                    lastMessage = user.lastmessage,
                    onClickText = { onChatSelected(user)},
                    onClickImage = {
                        SnackbarManager.showMessage("TODO")
                    }
                )
                HorizontalDivider(
                    thickness = 0.5.dp
                )
            }
        }
    }
}


