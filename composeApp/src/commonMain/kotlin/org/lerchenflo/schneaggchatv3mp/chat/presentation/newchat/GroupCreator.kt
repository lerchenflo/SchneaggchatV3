package org.lerchenflo.schneaggchatv3mp.chat.presentation.newchat

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material3.Button
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ismoy.imagepickerkmp.domain.config.ImagePickerConfig
import io.github.ismoy.imagepickerkmp.presentation.ui.components.ImagePickerLauncher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.sharedUi.ActivityTitle
import org.lerchenflo.schneaggchatv3mp.sharedUi.ProfilePictureView
import org.lerchenflo.schneaggchatv3mp.sharedUi.UserButton
import org.lerchenflo.schneaggchatv3mp.utilities.PlatformBackHandler
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.at_least_members
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.create_group
import schneaggchatv3mp.composeapp.generated.resources.group_description
import schneaggchatv3mp.composeapp.generated.resources.group_name
import schneaggchatv3mp.composeapp.generated.resources.group_name_missing
import schneaggchatv3mp.composeapp.generated.resources.group_picture
import schneaggchatv3mp.composeapp.generated.resources.icon_nutzer
import schneaggchatv3mp.composeapp.generated.resources.info
import schneaggchatv3mp.composeapp.generated.resources.members
import schneaggchatv3mp.composeapp.generated.resources.new_group
import schneaggchatv3mp.composeapp.generated.resources.search_user

@Preview
@Composable
fun GroupCreator(
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .safeContentPadding()
){
    val minMembers = 2 // Mindestens 2 Partypeople in da Partygruppe. do ändera wenn ma mehr oder weniger will


    val viewModel = koinViewModel<GroupCreatorViewModel>()
    val searchTerm by viewModel.searchterm.collectAsStateWithLifecycle()
    val groupName by viewModel.groupName.collectAsStateWithLifecycle()
    val groupDescription by viewModel.groupDescription.collectAsStateWithLifecycle()
    val users by viewModel.groupCreatorState.collectAsStateWithLifecycle(emptyList())
    val membersSelected = viewModel.selectedUsers.count() >= minMembers
    val groupInfoComplete = groupName.isNotBlank()


    PlatformBackHandler(
        enabled = viewModel.groupCreatorStage == GroupCreatorStage.GROUPDETAILS
    ) {
        viewModel.groupCreatorStage = GroupCreatorStage.MEMBERSEL
    }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            if (viewModel.groupCreatorStage == GroupCreatorStage.MEMBERSEL) {
                FloatingActionButton(
                    onClick = {
                        if (membersSelected) { // nur witta go wenn ma genug members usgwählt hot
                            viewModel.groupCreatorStage = GroupCreatorStage.GROUPDETAILS
                        }else{
                            CoroutineScope(Dispatchers.IO).launch {
                                SnackbarManager.showMessage(
                                    getString( Res.string.at_least_members,minMembers)
                                )
                            }
                        }
                    },
                    containerColor = if (membersSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (membersSelected)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant

                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = stringResource(Res.string.info),
                    )
                }
            } else if (viewModel.groupCreatorStage == GroupCreatorStage.GROUPDETAILS) {
                ExtendedFloatingActionButton(
                    onClick = {
                        if (groupInfoComplete) {
                            SnackbarManager.showMessage("Create group backend missing! Todo")
                        }else{
                            CoroutineScope(Dispatchers.IO).launch {
                                SnackbarManager.showMessage(
                                    getString(Res.string.group_name_missing)
                                )
                            }
                        }

                    },
                    containerColor = if (groupInfoComplete)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (groupInfoComplete)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    icon = { Icon(Icons.Outlined.GroupAdd, contentDescription = null) },
                    text = { Text(stringResource(Res.string.create_group)) },
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) {

        var showImagePickerDialog by remember { mutableStateOf(false) }
        Box(modifier = Modifier.fillMaxSize()) {
            if (showImagePickerDialog) {
                //TODO: Copy image selection from Create screen
                ImagePickerLauncher(
                    config = ImagePickerConfig(//TODO: Fix strings
                        onPhotoCaptured = { /* TODO: handle image */ },
                        onDismiss = { showImagePickerDialog = false },
                        onError = {},

                    )
                )
            }
        }



        Column(
            modifier = modifier
        ) {
            ActivityTitle(
                title = stringResource(Res.string.new_group),
                onBackClick = {
                    viewModel.onBackClick()
                }
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
                    checked = membersSelected,
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
                    checked = groupInfoComplete,
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
                                    filepath = user.profilePictureUrl,
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

                 Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)

                ) {

                    Image( // todo image uswähla und azoaga
                        painter = painterResource(Res.drawable.icon_nutzer),
                        contentDescription = stringResource(Res.string.group_picture),
                        modifier = Modifier
                            .size(70.dp)
                            .clickable{
                                showImagePickerDialog = true
                            }
                    )
                
               

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Avatar with edit overlay
                        Box(modifier = Modifier.size(72.dp)) {
                            Image(
                                painter = painterResource(Res.drawable.icon_nutzer),
                                contentDescription = stringResource(Res.string.group_picture),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    .clickable {
                                        SnackbarManager.showMessage("Profile picture backend missing! Todo")
                                        /*todo*/
                                    }
                            )

                            // small edit icon overlay
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit picture",
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(x = 4.dp, y = 4.dp) // slight overlap
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))

                        OutlinedTextField(
                            value = groupName,
                            onValueChange = { viewModel.updateGroupName(it) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(stringResource(Res.string.group_name)) },
                            /*leadingIcon = {
                                Icon(imageVector = Icons.Default.Badge, contentDescription = null)
                            },

                             */
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }



                    // Gruppenbeschreibung - multiline
                    OutlinedTextField(
                        value = groupDescription,
                        onValueChange = { viewModel.updateGroupDescription(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 96.dp), // encourages multiline
                        placeholder = { Text(stringResource(Res.string.group_description)) },
                        trailingIcon = {
                            Icon(imageVector = Icons.Default.Description, contentDescription = null)
                        },
                        singleLine = false,
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface
                        )
                    )

                // Mehr Einstellungen für gruppen falls ma lustig isch

                //Easter Egg: i chill gad mit am StWm Thum in Klagenfurt weil ma do irend an blödsinn holand


                }
            }

        }
    }

}