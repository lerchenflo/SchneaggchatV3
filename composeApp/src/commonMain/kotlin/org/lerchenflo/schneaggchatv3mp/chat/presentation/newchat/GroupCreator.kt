package org.lerchenflo.schneaggchatv3mp.chat.presentation.newchat

import SwipeableCardView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ismoy.imagepickerkmp.domain.config.CameraCaptureConfig
import io.github.ismoy.imagepickerkmp.domain.config.CropConfig
import io.github.ismoy.imagepickerkmp.domain.config.GalleryConfig
import io.github.ismoy.imagepickerkmp.domain.config.ImagePickerConfig
import io.github.ismoy.imagepickerkmp.domain.models.CapturePhotoPreference
import io.github.ismoy.imagepickerkmp.domain.models.CompressionLevel
import io.github.ismoy.imagepickerkmp.presentation.ui.components.GalleryPickerLauncher
import io.github.ismoy.imagepickerkmp.presentation.ui.components.ImagePickerLauncher
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.login.presentation.login.InputTextField
import org.lerchenflo.schneaggchatv3mp.login.presentation.signup.SignupAction
import org.lerchenflo.schneaggchatv3mp.sharedUi.ActivityTitle
import org.lerchenflo.schneaggchatv3mp.sharedUi.ProfilePictureView
import org.lerchenflo.schneaggchatv3mp.sharedUi.UserButton
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.group_description
import schneaggchatv3mp.composeapp.generated.resources.group_name
import schneaggchatv3mp.composeapp.generated.resources.group_picture
import schneaggchatv3mp.composeapp.generated.resources.icon_nutzer
import schneaggchatv3mp.composeapp.generated.resources.name_too_long
import schneaggchatv3mp.composeapp.generated.resources.new_group
import schneaggchatv3mp.composeapp.generated.resources.profile_picture
import schneaggchatv3mp.composeapp.generated.resources.search_user
import schneaggchatv3mp.composeapp.generated.resources.tooltip_group_description
import schneaggchatv3mp.composeapp.generated.resources.tooltip_group_name


@Composable
fun GroupCreator() {

    val viewModel = koinViewModel<GroupCreatorViewModel>()

    val searchTerm by viewModel.searchterm.collectAsStateWithLifecycle()
    val groupName by viewModel.groupName.collectAsStateWithLifecycle()
    val groupDescription by viewModel.groupDescription.collectAsStateWithLifecycle()
    val users by viewModel.groupCreatorState.collectAsStateWithLifecycle(emptyList())
    val profilePic by viewModel.profilePic.collectAsStateWithLifecycle()

    var showImagePickerDialog by remember { mutableStateOf(false) }


    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        ActivityTitle(
            title = stringResource(Res.string.new_group),
            onBackClick = {
                viewModel.onBackClick()
            }
        )

        SwipeableCardView(
            onFinished = {
                viewModel.onCreateGroup()
            },
            contentAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize()
        ){
            //User selection card
            CardItem(
                modifier = Modifier.fillMaxSize()
            ) {
                Column {
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
                                    contentDescription = "search"
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
                }
            }

            //Gruppenname
            CardItem {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)

                ) {

                    //Login inputtextfields
                    InputTextField(
                        text = groupName,
                        onValueChange = {viewModel.updateGroupName(it)},
                        label = stringResource(Res.string.group_name),
                        hint = stringResource(Res.string.group_name),
                        errortext = if (groupName.length > 25) stringResource(Res.string.name_too_long) else null,
                        tooltip = stringResource(Res.string.tooltip_group_name),
                        imeAction = ImeAction.Next,
                        maxLines = 1
                    )

                    InputTextField(
                        text = groupDescription,
                        onValueChange = { viewModel.updateGroupDescription(it) },
                        label = stringResource(Res.string.group_description),
                        hint = stringResource(Res.string.group_description),
                        tooltip = stringResource(Res.string.tooltip_group_description),
                    )

                }
            }

            //Profilbild
            CardItem{
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ){

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Text(
                            text = stringResource(Res.string.profile_picture),
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Box(modifier = Modifier.size(200.dp)) {
                            Image(
                                painter = if (profilePic != null) BitmapPainter(profilePic!!.decodeToImageBitmap()) else painterResource(Res.drawable.icon_nutzer),
                                contentDescription = stringResource(Res.string.profile_picture),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .clickable{
                                        showImagePickerDialog = true
                                    }
                            )

                            if (profilePic == null) {
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

                        }
                    }


                }
            }
        }

    }


    Box(modifier = Modifier.fillMaxSize()) {
        if (showImagePickerDialog) {
            GalleryPickerLauncher(
                onPhotosSelected = {
                    viewModel.updateProfilepic(it.first())
                    showImagePickerDialog = false
                },
                onError = {
                    showImagePickerDialog = false
                },
                onDismiss = {
                    showImagePickerDialog = false
                },
                selectionLimit = 1,
                enableCrop = true,
                cameraCaptureConfig = CameraCaptureConfig(
                    compressionLevel = CompressionLevel.HIGH,
                    preference = CapturePhotoPreference.FAST, //No flash
                    cropConfig = CropConfig(
                        enabled = true,
                        aspectRatioLocked = true,
                        circularCrop = true,
                        squareCrop = false,
                        freeformCrop = false
                    ),
                    galleryConfig = GalleryConfig(
                        allowMultiple = false,
                        selectionLimit = 1,
                    )
                )
            )
        }
    }
}