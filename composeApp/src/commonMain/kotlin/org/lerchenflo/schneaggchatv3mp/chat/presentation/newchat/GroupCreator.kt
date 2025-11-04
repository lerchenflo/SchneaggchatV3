package org.lerchenflo.schneaggchatv3mp.chat.presentation.newchat

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.sharedUi.ActivityTitle
import org.lerchenflo.schneaggchatv3mp.sharedUi.ProfilePictureView
import org.lerchenflo.schneaggchatv3mp.sharedUi.UserButton
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.create_group
import schneaggchatv3mp.composeapp.generated.resources.group_description
import schneaggchatv3mp.composeapp.generated.resources.group_name
import schneaggchatv3mp.composeapp.generated.resources.group_picture
import schneaggchatv3mp.composeapp.generated.resources.icon_nutzer
import schneaggchatv3mp.composeapp.generated.resources.info
import schneaggchatv3mp.composeapp.generated.resources.members
import schneaggchatv3mp.composeapp.generated.resources.new_group
import schneaggchatv3mp.composeapp.generated.resources.search_user

@Preview
@Composable
fun GroupCreator(
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .safeContentPadding()
){
    val viewModel = koinViewModel<GroupCreatorViewModel>()
    val searchTerm by viewModel.searchterm.collectAsStateWithLifecycle()
    val groupName by viewModel.groupName.collectAsStateWithLifecycle()
    val groupDescription by viewModel.groupDescription.collectAsStateWithLifecycle()
    val users by viewModel.groupCreatorState.collectAsStateWithLifecycle(emptyList())

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            if (viewModel.groupCreatorStage == GroupCreatorStage.MEMBERSEL) {
                FloatingActionButton(
                    onClick = {
                        viewModel.groupCreatorStage = GroupCreatorStage.GROUPDETAILS
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = stringResource(Res.string.info),
                    )
                }
            } else if (viewModel.groupCreatorStage == GroupCreatorStage.GROUPDETAILS) {
                ExtendedFloatingActionButton(
                    onClick = {
                        SnackbarManager.showMessage("Create group backend missing! Todo")
                    },
                    icon = { Icon(Icons.Outlined.GroupAdd, contentDescription = null) },
                    text = { Text(stringResource(Res.string.create_group)) },
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) {
        Column(
            modifier = modifier
        ) {
            ActivityTitle(
                title = stringResource(Res.string.new_group),
                onBackClick = onBackClick
            )


            MultiChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 10.dp,
                        end = 2.dp,
                        bottom = 10.dp
                    )
            ) {
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(
                        index = 0,
                        count = 2
                    ),
                    onCheckedChange = { viewModel.groupCreatorStage = GroupCreatorStage.MEMBERSEL },
                    checked = viewModel.selectedUsers.count() >= 2, // Mindestens 2 Partypeople in da Partygruppe
                    label = {
                        Row() {
                            Text(stringResource(Res.string.members))
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = stringResource(Res.string.members)
                            )
                        }
                    }

                )
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(
                        index = 1,
                        count = 2
                    ),
                    onCheckedChange = {
                        viewModel.groupCreatorStage = GroupCreatorStage.GROUPDETAILS
                    },
                    checked = viewModel.groupCreatorStage == GroupCreatorStage.GROUPDETAILS,
                    label = {
                        Row {
                            Text(stringResource(Res.string.info))
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = stringResource(Res.string.info)
                            )
                        }
                    }

                )
            }

            // stage 1 : lüt uswähla
            if (viewModel.groupCreatorStage == GroupCreatorStage.MEMBERSEL) {

                // der Teil oba wo die Profilbilder azoagt wörrend
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(
                            start = 10.dp,
                            end = 2.dp,
                            bottom = 10.dp
                        )

                ) {
                    // durch die selected users durchgo und azoaga
                    viewModel.selectedUsers.forEach { user ->
                        Column(
                            modifier = Modifier
                                .padding(end = 5.dp)
                                .clickable {
                                    viewModel.selectedUsers.remove(user)
                                }

                        ) {
                            var size = 70.dp
                            Box(
                                modifier = Modifier.size(size)
                            ) {
                                // Profile picture
                                ProfilePictureView(
                                    filepath = user.profilePicture,
                                    modifier = Modifier
                                        .size(size)
                                        .padding(8.dp)
                                        .clip(CircleShape)
                                )

                                // Trash icon overlay
                                Icon(
                                    imageVector = Icons.Default.Delete, // or Icons.Outlined.Delete
                                    contentDescription = "Remove user",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(30.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
                                            shape = CircleShape
                                        )
                                        .padding(2.dp)

                                )
                            }
                            Text(
                                text = user.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(size)
                            )
                        }
                    }
                }

                // A suchfeld
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 5.dp,
                            bottom = 5.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchTerm,
                        maxLines = 1,
                        onValueChange = { viewModel.updateSearchterm(it) },
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
                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .aspectRatio(1f)
                            .clickable { viewModel.updateSearchterm("") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Reset Search Text",
                            modifier = Modifier
                                .size(30.dp)

                        )
                    }

                }

                // die freunde azoaga zum uswähla
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 16.dp
                    ),
                ) {
                    items(users) { user ->
                        val selected = viewModel.selectedUsers.contains(user)

                        UserButton(
                            selectedChat = user,
                            useOnClickGes = true,
                            lastMessage = null,
                            bottomTextOverride = "",
                            selected = selected,
                            onClickGes = {
                                if (selected) {
                                    viewModel.selectedUsers.remove(user)
                                } else {
                                    viewModel.selectedUsers.add(user)
                                }

                            }
                        )
                        HorizontalDivider(
                            thickness = 0.5.dp
                        )
                    }
                }
            } else if (viewModel.groupCreatorStage == GroupCreatorStage.GROUPDETAILS) {
                // 2. Stage: Name, Profilbild, Beschreibung, etc.

                Column(){
                    Image( // todo image uswähla und azoaga
                        painter = painterResource(Res.drawable.icon_nutzer),
                        contentDescription = stringResource(Res.string.group_picture),
                        modifier = Modifier
                            .size(70.dp)
                            .clickable{
                                SnackbarManager.showMessage("Profile picture backend missing! Todo")
                            }
                    )

                    // Gruppenname
                    OutlinedTextField(
                        value = groupName,
                        maxLines = 1,
                        onValueChange = {viewModel.updateGroupName(it)},
                        modifier = Modifier
                        //    .weight(1f)
                        ,
                        placeholder = { Text(stringResource(Res.string.group_name)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = stringResource(Res.string.group_name)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent
                        )
                    )

                    // Gruppenbeschreibung
                    OutlinedTextField(
                        value = groupDescription,
                        maxLines = 1,
                        onValueChange = {viewModel.updateGroupDescription(it) },
                        modifier = Modifier
                        //    .weight(1f)
                        ,
                        placeholder = { Text(stringResource(Res.string.group_description)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = stringResource(Res.string.group_description)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent
                        )
                    )
                }

                // Mehr Einstellungen für gruppen falls ma lustig isch

                //Easter Egg: i chill gad mit am StWm Thum in Klagenfurt weil ma do irend an blödsinn holand

            }

        }
    }

}