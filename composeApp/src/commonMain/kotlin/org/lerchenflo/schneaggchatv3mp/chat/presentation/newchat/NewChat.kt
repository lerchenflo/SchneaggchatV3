package org.lerchenflo.schneaggchatv3mp.chat.presentation.newchat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContactMail
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.sharedUi.ActivityTitle
import org.lerchenflo.schneaggchatv3mp.sharedUi.UserButton
import org.lerchenflo.schneaggchatv3mp.theme.SchneaggchatTheme
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.common_friends
import schneaggchatv3mp.composeapp.generated.resources.invite_friend
import schneaggchatv3mp.composeapp.generated.resources.new_chat
import schneaggchatv3mp.composeapp.generated.resources.new_group
import schneaggchatv3mp.composeapp.generated.resources.search_friend
import schneaggchatv3mp.composeapp.generated.resources.search_user
import schneaggchatv3mp.composeapp.generated.resources.settings

@OptIn(InternalResourceApi::class)
@Composable
fun NewChat(
    onBackClick: () -> Unit = {},
    onGroupCreator: () -> Unit = {},
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .safeContentPadding()
){
    val viewModel = koinViewModel<NewChatViewModel>()
    val searchterm by viewModel.searchterm.collectAsStateWithLifecycle()
    val newChats by viewModel.newChatState.collectAsStateWithLifecycle(emptyList())

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
                onGroupCreator()
            }
        )

        // invite Friend
        Option(
            text = stringResource(Res.string.invite_friend),
            icon = Icons.Default.ContactMail,
            onClick = {
                SnackbarManager.showMessage("Freund einladen noch nicht implementiert")
            }
        )

        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 5.dp,
                    bottom = 5.dp
                )
        ){
            OutlinedTextField(
                value = searchterm,
                maxLines = 1,
                onValueChange = {viewModel.updateSearchterm(it) },
                modifier = Modifier
                    .weight(1f),
                placeholder = { Text(stringResource(Res.string.search_user)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search"
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent
                )
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = 16.dp
            ),
        ) {
            items(newChats) { user ->
                UserButton(
                    selectedChat = user,
                    useOnClickGes = false,
                    lastMessage = null,
                    bottomTextOverride = stringResource(Res.string.common_friends,"3"),
                    onClickText = {
                        /*todo*/
                        SnackbarManager.showMessage("TODO: send friend request")
                    },
                    onClickImage = {
                        /*todo*/
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
            top = 7.dp,
            bottom = 7.dp
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