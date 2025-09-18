package org.lerchenflo.schneaggchatv3mp.chat.presentation.chatselector

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.chat.domain.SelectedChat
import org.lerchenflo.schneaggchatv3mp.database.AppRepository
import org.lerchenflo.schneaggchatv3mp.sharedUi.RoundLoadingIndicator
import org.lerchenflo.schneaggchatv3mp.sharedUi.UserButton
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.add
import schneaggchatv3mp.composeapp.generated.resources.app_name
import schneaggchatv3mp.composeapp.generated.resources.filter
import schneaggchatv3mp.composeapp.generated.resources.loadinginfo_messages
import schneaggchatv3mp.composeapp.generated.resources.loadinginfo_offline
import schneaggchatv3mp.composeapp.generated.resources.schneaggmap
import schneaggchatv3mp.composeapp.generated.resources.search_friend
import schneaggchatv3mp.composeapp.generated.resources.settings
import schneaggchatv3mp.composeapp.generated.resources.settings_gear
import schneaggchatv3mp.composeapp.generated.resources.tools_and_games

@OptIn(ExperimentalMaterial3Api::class) // PullToRefreshBox is experimental
@Preview
@Composable
fun Chatauswahlscreen(
    onChatSelected: (SelectedChat) -> Unit,  // navigation callback
    onNewChatClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onToolsAndGamesClick: () -> Unit,
    modifier: Modifier = Modifier
        .safeContentPadding()
) {

    val appRepository = koinInject<AppRepository>()
    val viewModel = koinViewModel<ChatSelectorViewModel>()
    val availablegegners by viewModel.chatSelectorState.collectAsStateWithLifecycle(emptyList())
    val searchterm by viewModel.searchterm.collectAsStateWithLifecycle()


    //Noti permission abfrage für ios (Machts auto on start´)
    //val permissionUtil = NotifierManager.getPermissionUtil()
    //permissionUtil.askNotificationPermission()


    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNewChatClick() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Chat,
                    contentDescription = stringResource(Res.string.add),

                )
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ){
        //Hauptlayout
        Column{
            //Obere Zeile für Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically

            ) {
                Text(
                    text = stringResource(Res.string.app_name),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 5.dp),

                    autoSize = TextAutoSize.StepBased(
                        minFontSize = 10.sp,
                        maxFontSize = 25.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val size = 30.dp //Größe vo a ui elemente oba rechts
                val distance = 10.dp //Abstand zwüschat da buttons oba rechts

                RoundLoadingIndicator(
                    visible = viewModel.isLoadingMessages || !SessionCache.loggedIn,
                    onClick = {
                        if (viewModel.isLoadingMessages) {
                            CoroutineScope(Dispatchers.IO).launch {
                                SnackbarManager.showMessage(getString(Res.string.loadinginfo_messages))
                            }
                        } else {
                            CoroutineScope(Dispatchers.IO).launch {
                                SnackbarManager.showMessage(getString(Res.string.loadinginfo_offline))
                            }
                        }

                    },
                    strokeWidth = 2.dp,
                    size = 18.dp

                )

                Spacer(Modifier.width(distance))

                val touchSize = 40.dp
                val iconSize = 28.dp
                val gap = 0.dp
                Box(
                    modifier = Modifier
                        .padding(2.dp)                      // smaller outer padding so icons sit closer together
                        .size(touchSize)
                        .clip(CircleShape)                  // optional: nicer ripple shape
                        .clickable { onToolsAndGamesClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Checklist,
                        contentDescription = stringResource(Res.string.tools_and_games),
                        modifier = Modifier.size(iconSize), // bigger visible icon
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(Modifier.width(gap))

                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .size(touchSize)
                        .clip(CircleShape)
                        .clickable { SnackbarManager.showMessage("Es gibt noch koa schneaggmap") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = stringResource(Res.string.schneaggmap),
                        modifier = Modifier.size(iconSize),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(Modifier.width(gap))

                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .size(touchSize)
                        .clip(CircleShape)
                        .clickable { onSettingsClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(Res.string.settings),
                        modifier = Modifier.size(iconSize),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }


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
                    placeholder = { Text(stringResource(Res.string.search_friend)) },
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


                Spacer(Modifier.width(10.dp))

                Image(
                    painterResource(Res.drawable.filter),
                    contentDescription = stringResource(Res.string.filter),
                    modifier = Modifier
                        .size(30.dp)
                        .clickable { SnackbarManager.showMessage("muasch noch selber suacha") },
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)

                )

                /*
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

                 */
            }

            //Gegneranzeige
            //val state = rememberPullToRefreshState()
            PullToRefreshBox( // needs experimental opt in
                isRefreshing = false,
                onRefresh = { viewModel.refresh() }, // Trigger refresh
                indicator = {
                    null //Überschrieba dassa garned kut
                    /*
                    Indicator(// optional custom indicator
                        modifier = Modifier.align(Alignment.TopCenter),
                        isRefreshing = viewModel._isRefreshing.value,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        state = state
                    )

                     */
                },

                ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 16.dp
                    ),
                ) {
                    items(availablegegners) { gegner ->
                        UserButton(
                            selectedChat = gegner,
                            useOnClickGes = false,
                            lastMessage = gegner.lastmessage,
                            onClickText = { onChatSelected(gegner) },
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

    }



}


