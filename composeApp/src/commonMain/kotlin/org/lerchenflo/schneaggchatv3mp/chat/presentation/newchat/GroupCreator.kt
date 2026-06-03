@file:OptIn(ExperimentalMaterial3Api::class)

package org.lerchenflo.schneaggchatv3mp.chat.presentation.newchat

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.github.ismoy.imagepickerkmp.domain.config.CropConfig
import io.github.ismoy.imagepickerkmp.domain.config.GalleryConfig
import io.github.ismoy.imagepickerkmp.domain.models.MimeType
import io.github.ismoy.imagepickerkmp.features.imagepicker.config.ImagePickerKMPConfig
import io.github.ismoy.imagepickerkmp.features.imagepicker.model.ImagePickerResult
import io.github.ismoy.imagepickerkmp.features.imagepicker.ui.rememberImagePickerKMP
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.MAX_GROUPNAME_LENGTH
import org.lerchenflo.schneaggchatv3mp.MIN_GROUPNAME_LENGTH
import org.lerchenflo.schneaggchatv3mp.login.presentation.login.InputTextField
import org.lerchenflo.schneaggchatv3mp.sharedUi.SwipeableCardView
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import org.lerchenflo.schneaggchatv3mp.sharedUi.popups.MemberSelector
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.camera
import schneaggchatv3mp.composeapp.generated.resources.choose_image_source
import schneaggchatv3mp.composeapp.generated.resources.gallery
import schneaggchatv3mp.composeapp.generated.resources.group_description
import schneaggchatv3mp.composeapp.generated.resources.group_name
import schneaggchatv3mp.composeapp.generated.resources.icon_nutzer
import schneaggchatv3mp.composeapp.generated.resources.new_group
import schneaggchatv3mp.composeapp.generated.resources.profile_picture
import schneaggchatv3mp.composeapp.generated.resources.tooltip_group_description
import schneaggchatv3mp.composeapp.generated.resources.tooltip_group_name

@Composable
fun GroupCreatorScreenRoot(

){
    val viewModel = koinViewModel<GroupCreatorViewModel>()
    GroupCreatorScreen(
        onAction = viewModel::onAction,
        state = viewModel.state,
    )
}


@Composable
private fun GroupCreatorScreen(
    onAction: (GroupCreatorAction) -> Unit,
    state: GroupCreatorState
) {

    var showImagePickerDialog by remember { mutableStateOf(false) }


    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        ActivityTitle(
            title = stringResource(Res.string.new_group),
            onBackClick = {
                onAction(GroupCreatorAction.navigateBack)
            }
        )

        SwipeableCardView(
            onFinished = {
                onAction(GroupCreatorAction.createGroup)
            },
            contentAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize(),
            onBack = { onAction(GroupCreatorAction.navigateBack) },
            finishEnabled = state.creationPermitted,
            backEnabled = true,
            canContinue = { pageIndex ->
                when (pageIndex) {
                    0 -> state.selectedUsers.size >= 2
                    1 -> state.groupname.text.length in MIN_GROUPNAME_LENGTH..< MAX_GROUPNAME_LENGTH
                    2 -> state.profilepic != null
                    else -> true
                }
            }
        ){
            //User selection card
            CardItem {
                MemberSelector(
                    availableUsers = state.availableUsers,
                    selectedUsers = state.selectedUsers,
                    searchTerm = state.searchterm,
                    onSearchTermChange = {onAction(GroupCreatorAction.updateSearchterm(it))},
                    onUserSelected = { onAction(GroupCreatorAction.addGroupMember(it)) },
                    onUserDeselected = { onAction(GroupCreatorAction.removeGroupMember(it)) },
                    modifier = Modifier.fillMaxSize()
                )
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
                        text = state.groupname.text,
                        onValueChange = {onAction(GroupCreatorAction.updateGroupname(it))},
                        label = stringResource(Res.string.group_name),
                        hint = stringResource(Res.string.group_name),
                        errortext = state.groupname.errorMessage,
                        tooltip = stringResource(Res.string.tooltip_group_name),
                        imeAction = ImeAction.Next,
                        maxLines = 1
                    )

                    InputTextField(
                        text = state.groupdescription.text,
                        onValueChange = { onAction(GroupCreatorAction.updateGroupdescription(it)) },
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
                                painter = if (state.profilepic != null) BitmapPainter(state.profilepic.decodeToImageBitmap()) else painterResource(Res.drawable.icon_nutzer),
                                contentDescription = stringResource(Res.string.profile_picture),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .clickable{
                                        showImagePickerDialog = true
                                    }
                            )

                            if (state.profilepic == null) {
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


    val picker = rememberImagePickerKMP(
        config = ImagePickerKMPConfig(
            cropConfig = CropConfig(
                enabled = true,
                aspectRatioLocked = true,
                circularCrop = true,
                squareCrop = false,
                freeformCrop = false
            ),
            galleryConfig = GalleryConfig(
                allowMultiple = false,
                mimeTypes = listOf(MimeType.IMAGE_ALL),
                includeExif = false
            )
        )
    )
    val result = picker.result


    if (showImagePickerDialog) {

        // Handle side effects safely
        LaunchedEffect(result) {
            when (result) {
                is ImagePickerResult.Success -> {
                    onAction(GroupCreatorAction.updateProfilePic(result.photos.first()))

                    picker.reset()
                    showImagePickerDialog = false
                }

                is ImagePickerResult.Dismissed -> {
                    picker.reset()
                    showImagePickerDialog = false
                }

                else -> Unit
            }
        }

        BasicAlertDialog(
            onDismissRequest = {
                showImagePickerDialog = false
            }
        ) {

            Surface(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {

                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .width(IntrinsicSize.Min),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    when (result) {

                        is ImagePickerResult.Loading -> {
                            CircularProgressIndicator()
                        }

                        is ImagePickerResult.Error -> {
                            Text(
                                text = "Error: ${result.exception.message}",
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        is ImagePickerResult.Idle -> {

                            Text(
                                stringResource(Res.string.choose_image_source)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {

                                Button(
                                    onClick = {
                                        picker.launchCamera()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(stringResource(Res.string.camera))
                                }

                                Button(
                                    onClick = {
                                        picker.launchGallery()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(stringResource(Res.string.gallery))
                                }
                            }
                        }

                        is ImagePickerResult.Success,
                        is ImagePickerResult.Dismissed -> {
                            // handled in LaunchedEffect
                        }
                    }
                }
            }
        }
    }
}