package org.lerchenflo.schneaggchatv3mp.chat.presentation.chatselector

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.sharedUi.buttons.UserButton
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.filter
import schneaggchatv3mp.composeapp.generated.resources.search_friend
import schneaggchatv3mp.composeapp.generated.resources.send_to


// This is a stripped down version of ChatSelector when sharing over Schneaggchat to select a chat

@Composable
fun MessageChatSelector(
    modifier: Modifier = Modifier
) {
    val chatSelectorViewModel = koinViewModel<ChatSelectorViewModel>()

    val availablegegners by chatSelectorViewModel.chatSelectorState.collectAsStateWithLifecycle(emptyList())
    val searchterm by chatSelectorViewModel.searchTerm.collectAsStateWithLifecycle()

    val ownId = SessionCache.requireLoggedIn()?.userId ?: return


    Column(
        modifier = modifier
    ) {
        ActivityTitle(
            title = stringResource(Res.string.send_to),
            onBackClick = {
                // todo navigate to actual chatSelector
            }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            val focusmanager = LocalFocusManager.current


            BasicTextField(
                value = searchterm,
                onValueChange = { chatSelectorViewModel.updateSearchterm(it) },
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(20.dp)
                    ),
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = true,
                decorationBox = { innerTextField ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (searchterm.isEmpty()) {
                                Text(
                                    text = stringResource(Res.string.search_friend),
                                    fontSize = 14.sp,
                                    color = LocalContentColor.current.copy(alpha = 0.4f)
                                )
                            }
                            innerTextField()
                        }

                        if (searchterm.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel search",
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable {
                                        focusmanager.clearFocus()
                                        chatSelectorViewModel.updateSearchterm("")
                                    }
                            )
                        }
                    }
                },
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            )

            Spacer(Modifier.width(10.dp))

            var filterDropdownExpanded by remember { mutableStateOf(false) }

            Box {
                Icon(
                    imageVector = Icons.Default.FilterAlt,
                    contentDescription = stringResource(Res.string.filter),
                    modifier = Modifier
                        .size(30.dp)
                        .clickable { filterDropdownExpanded = true },
                    tint = MaterialTheme.colorScheme.onSurface

                )
                DropdownMenu(
                    expanded = filterDropdownExpanded,
                    onDismissRequest = { filterDropdownExpanded = false }
                ) {
                    ChatFilter.entries.forEach { filter ->
                        DropdownMenuItem(
                            text = {
                                Row{
                                    Icon(
                                        imageVector = filter.getIcon(),
                                        contentDescription = filter.toUiText().asString(),
                                        tint = if(chatSelectorViewModel.filter.value == filter)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text(
                                        text = filter.toUiText().asString()
                                    )
                                }
                            },
                            onClick = {
                                filterDropdownExpanded = false
                                chatSelectorViewModel.updateFilter(filter)
                            }
                        )
                    }
                }
            }
        }

        val liststate = rememberLazyListState()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = 16.dp
            ),
            state = liststate
        ) {
            items(
                items = availablegegners,
                key = {"${it.id}_${it.isGroup}"}
            ) { gegner ->

                UserButton(
                    selectedChat = gegner,
                    useOnClickGes = true,
                    lastMessage = gegner.lastmessage,
                    onClickGes = { chatSelectorViewModel.onChatSelectedWithMessage(gegner) },
                    showPin = true,
                    ownId = ownId

                )
                HorizontalDivider(
                    thickness = 0.5.dp
                )

            }
        }

    }

}