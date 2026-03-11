package org.lerchenflo.schneaggchatv3mp.login.presentation.signup

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.runBlocking
import io.github.ismoy.imagepickerkmp.domain.config.CameraCaptureConfig
import io.github.ismoy.imagepickerkmp.domain.config.CropConfig
import io.github.ismoy.imagepickerkmp.domain.config.GalleryConfig
import io.github.ismoy.imagepickerkmp.domain.models.CapturePhotoPreference
import io.github.ismoy.imagepickerkmp.domain.models.CompressionLevel
import io.github.ismoy.imagepickerkmp.presentation.ui.components.GalleryPickerLauncher
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.app.theme.SchneaggchatTheme
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.login.presentation.login.InputTextField
import org.lerchenflo.schneaggchatv3mp.login.presentation.login.TooltipIconButton
import org.lerchenflo.schneaggchatv3mp.sharedUi.SwipeableCardView
import org.lerchenflo.schneaggchatv3mp.sharedUi.buttons.NormalButton
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import org.lerchenflo.schneaggchatv3mp.getPrivacyPolicyUrl
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.accept_agb_pt1
import schneaggchatv3mp.composeapp.generated.resources.accept_agb_pt2
import schneaggchatv3mp.composeapp.generated.resources.create_account
import schneaggchatv3mp.composeapp.generated.resources.email
import schneaggchatv3mp.composeapp.generated.resources.password
import schneaggchatv3mp.composeapp.generated.resources.password_again
import schneaggchatv3mp.composeapp.generated.resources.profile_picture
import schneaggchatv3mp.composeapp.generated.resources.select_gebi_date
import schneaggchatv3mp.composeapp.generated.resources.select_profile_pic
import schneaggchatv3mp.composeapp.generated.resources.tooltip_birthdate
import schneaggchatv3mp.composeapp.generated.resources.tooltip_email
import schneaggchatv3mp.composeapp.generated.resources.tooltip_password
import schneaggchatv3mp.composeapp.generated.resources.tooltip_password_repeat
import schneaggchatv3mp.composeapp.generated.resources.tooltip_profile_picture
import schneaggchatv3mp.composeapp.generated.resources.tooltip_terms
import schneaggchatv3mp.composeapp.generated.resources.username
import dev.darkokoa.datetimewheelpicker.WheelDatePicker
import dev.darkokoa.datetimewheelpicker.core.WheelPickerDefaults
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import schneaggchatv3mp.composeapp.generated.resources.icon_nutzer
import schneaggchatv3mp.composeapp.generated.resources.tooltip_username
import kotlin.time.Clock
import kotlin.time.ExperimentalTime


@Composable
fun SignUpScreenRoot(){
    val viewModel = koinViewModel<SignUpViewModel>()

    SignUpScreen(
        onAction = viewModel::onAction,
        state = viewModel.state
    )
}

@Composable
fun SignUpScreen(
    modifier: Modifier = Modifier.fillMaxSize(),
    onAction : (SignupAction) -> Unit = {},
    state: SignupState = SignupState()
){
    val preferencemanager = koinInject<Preferencemanager>()
    SchneaggchatTheme {
        var showImagePickerDialog by remember { mutableStateOf(false) }

        Column(
            modifier = modifier
        ) {
            ActivityTitle(
                title = stringResource(Res.string.create_account),
                onBackClick = {
                    onAction(SignupAction.OnBackClicked)
                }
            )

            SwipeableCardView(
                onFinished = {
                    onAction(SignupAction.OnSignUpButtonPress)
                },
                onBack = { onAction(SignupAction.OnBackClicked) },
                finishEnabled = state.isInputComplete(),
                backEnabled = true,
                modifier = Modifier.fillMaxSize(),
                canContinue = { pageIndex ->
                    when (pageIndex) {
                        0 -> state.usernameState.errorMessage == null &&
                                state.usernameState.text.isNotEmpty() &&
                                state.emailState.errorMessage == null &&
                                state.emailState.text.isNotEmpty() &&
                                state.gebiDate != null &&
                                state.gebiErrorText == null

                        1 -> state.profilePic != null && state.profilePicErrorText == null

                        2 -> state.passwordState.errorMessage == null &&
                                state.passwordState.text.isNotEmpty() &&
                               state.passwordRetypeState.errorMessage == null &&
                                state.passwordRetypeState.text.isNotEmpty() &&
                               state.agbsAccepted &&
                               state.agbsErrorText == null
                        else -> true
                    }
                }
            ) {

                // User Info Card
                CardItem {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        InputTextField(
                            text = state.usernameState.text,
                            onValueChange = { onAction(SignupAction.OnUsernameTextChange(it)) },
                            label = stringResource(Res.string.username),
                            hint = stringResource(Res.string.username),
                            errortext = state.usernameState.errorMessage,
                            tooltip = stringResource(Res.string.tooltip_username),
                            imeAction = ImeAction.Next,
                            keyboardType = KeyboardType.Text,
                            modifier = Modifier.fillMaxWidth()
                        )

                        InputTextField(
                            text = state.emailState.text,
                            onValueChange = { onAction(SignupAction.OnEmailTextChange(it)) },
                            label = stringResource(Res.string.email),
                            hint = stringResource(Res.string.email),
                            errortext = state.emailState.errorMessage,
                            tooltip = stringResource(Res.string.tooltip_email),
                            imeAction = ImeAction.Next,
                            keyboardType = KeyboardType.Email,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Birth date picker button
                        var showDatePicker by remember { mutableStateOf(false) }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    showDatePicker = true
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cake,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                if (state.gebiDate != null) {
                                    Text(
                                        text = "${state.gebiDate.day.toString().padStart(2, '0')}.${state.gebiDate.month.number.toString().padStart(2, '0')}.${state.gebiDate.year}"
                                    )
                                } else {
                                    Text(
                                        text = stringResource(Res.string.select_gebi_date)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            TooltipIconButton(stringResource(Res.string.tooltip_birthdate))
                        }
                        
                        if (state.gebiErrorText != null) {
                            Text(
                                text = state.gebiErrorText,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        if (showDatePicker) {
                            BirthdatePickerPopup(
                                onDateSelected = { selectedDate ->
                                    onAction(SignupAction.OnGebiDateChange(selectedDate))
                                    showDatePicker = false
                                },
                                onDismiss = { showDatePicker = false }
                            )
                        }
                    }
                }

                // Profile Picture Card
                CardItem {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = stringResource(Res.string.profile_picture),
                                style = MaterialTheme.typography.headlineSmall
                            )

                            Box(modifier = Modifier.size(200.dp)) {
                                Image(
                                    painter = if (state.profilePic != null) BitmapPainter(state.profilePic.decodeToImageBitmap()) else painterResource(Res.drawable.icon_nutzer),
                                    contentDescription = stringResource(Res.string.profile_picture),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .clickable {
                                            showImagePickerDialog = true
                                        }
                                )

                                if (state.profilePic == null) {
                                    Icon(
                                        imageVector = Icons.Default.AddAPhoto,
                                        contentDescription = "Add photo",
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(8.dp)
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer)
                                            .padding(8.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            Text(
                                text = stringResource(Res.string.select_profile_pic),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )

                            if (state.profilePicErrorText != null) {
                                Text(
                                    text = state.profilePicErrorText,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                // Password & Terms Card
                CardItem {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        InputTextField(
                            text = state.passwordState.text,
                            onValueChange = { onAction(SignupAction.OnPasswordTextChange(it, false)) },
                            label = stringResource(Res.string.password),
                            hint = stringResource(Res.string.password),
                            errortext = state.passwordState.errorMessage,
                            tooltip = stringResource(Res.string.tooltip_password),
                            imeAction = ImeAction.Next,
                            keyboardType = KeyboardType.Password,
                            modifier = Modifier.fillMaxWidth()
                        )

                        InputTextField(
                            text = state.passwordRetypeState.text,
                            onValueChange = { onAction(SignupAction.OnPasswordTextChange(it, true)) },
                            label = stringResource(Res.string.password_again),
                            hint = stringResource(Res.string.password),
                            errortext = state.passwordRetypeState.errorMessage,
                            tooltip = stringResource(Res.string.tooltip_password_repeat),
                            imeAction = ImeAction.Done,
                            keyboardType = KeyboardType.Password,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Terms checkbox
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = state.agbsAccepted,
                                onCheckedChange = { onAction(SignupAction.OnAgbChecked(it)) }
                            )

                            Spacer(modifier = Modifier.width(2.dp))

                            val text1 = stringResource(Res.string.accept_agb_pt1)
                            val text2 = stringResource(Res.string.accept_agb_pt2)

                            val annotatedString = buildAnnotatedString {
                                withStyle(
                                    style = SpanStyle(
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                ) {
                                    append(text1)
                                }

                                val startIndex = length
                                append(text2)
                                addLink(
                                    url = LinkAnnotation.Url(
                                        url = getPrivacyPolicyUrl(runBlocking { preferencemanager.getServerUrl() }),
                                        styles = TextLinkStyles(
                                            style = SpanStyle(
                                                color = MaterialTheme.colorScheme.primary,
                                                textDecoration = TextDecoration.Underline
                                            )
                                        )
                                    ),
                                    start = startIndex,
                                    end = length
                                )
                            }

                            Text(text = annotatedString)

                            Spacer(modifier = Modifier.width(8.dp))

                            TooltipIconButton(stringResource(Res.string.tooltip_terms))
                        }

                        if (state.agbsErrorText != null) {
                            Text(
                                text = state.agbsErrorText,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

        // Image picker dialog
        if (showImagePickerDialog) {
            GalleryPickerLauncher(
                onPhotosSelected = {
                    onAction(SignupAction.OnProfilepicSelected(it.first()))
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
                    preference = CapturePhotoPreference.FAST,
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

