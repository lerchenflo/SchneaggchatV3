package org.lerchenflo.schneaggchatv3mp.login.presentation.signup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiTransportation
import androidx.compose.material.icons.filled.TextRotationNone
import androidx.compose.material3.Scaffold
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.ismoy.imagepickerkmp.domain.config.CameraCaptureConfig
import io.github.ismoy.imagepickerkmp.domain.config.CropConfig
import io.github.ismoy.imagepickerkmp.domain.config.GalleryConfig
import io.github.ismoy.imagepickerkmp.domain.config.ImagePickerConfig
import io.github.ismoy.imagepickerkmp.domain.config.UiConfig
import io.github.ismoy.imagepickerkmp.domain.extensions.loadBytes
import io.github.ismoy.imagepickerkmp.domain.models.CapturePhotoPreference
import io.github.ismoy.imagepickerkmp.domain.models.CompressionLevel
import io.github.ismoy.imagepickerkmp.presentation.ui.components.GalleryPickerLauncher
import io.github.ismoy.imagepickerkmp.presentation.ui.components.ImagePickerLauncher
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.theme.SchneaggchatTheme
import org.lerchenflo.schneaggchatv3mp.utilities.DeviceSizeConfiguration
import org.lerchenflo.schneaggchatv3mp.utilities.clearFocusOnTap


@Composable
fun SignUpScreenRoot(){
    val viewModel = koinViewModel<SignUpViewModel>()

    SignUpScreen(
        onAction = viewModel::onAction,
        state = viewModel.state
    )

}

@Preview(
    showBackground = true,
)
@Composable
fun SignUpScreen(
    modifier: Modifier = Modifier
        .fillMaxSize(),
    onAction : (SignupAction) -> Unit = {},
    state: SignupState = SignupState()
){

    SchneaggchatTheme {
        //Responsive UI mit scaffold
        Scaffold(
            modifier = modifier,
        ){

            val rootmodifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(
                    topStart = 15.dp,
                    topEnd = 15.dp
                ))
                .padding(
                    horizontal = 16.dp,
                    vertical = 24.dp
                )

            var showImagePickerDialog by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxSize()) {
                if (showImagePickerDialog ) {

                    //TODO: Fix all strings
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

            val focus = SignupFocusRequesters(
                profilePic = remember { FocusRequester() },
                username = remember { FocusRequester() },
                email = remember { FocusRequester() },
                date = remember { FocusRequester() },
                password = remember { FocusRequester() },
                password2 = remember { FocusRequester() },
                terms = remember { FocusRequester() },
                signup = remember { FocusRequester() },
            )


            //Show other ui only if you are not currently selecting an image
            if (!showImagePickerDialog){
                val windowSizeclass = currentWindowAdaptiveInfo().windowSizeClass
                val deviceConfiguration = DeviceSizeConfiguration.fromWindowSizeClass(windowSizeclass)
                when (deviceConfiguration){
                    DeviceSizeConfiguration.MOBILE_PORTRAIT -> {
                        println("Mobile Portrait")
                        Column(
                            modifier = rootmodifier
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ){
                            SignUpForm1(
                                usernameText = state.usernameState.text,
                                onusernameTextChange = { onAction(SignupAction.OnUsernameTextChange(it)) },
                                usernameerrorText = state.usernameState.errorMessage,
                                emailText = state.emailState.text,
                                onemailTextChange = { onAction(SignupAction.OnEmailTextChange(it)) },
                                emailerrorText = state.emailState.errorMessage,
                                ongebidateselected = { onAction(SignupAction.OnGebiDateChange(it!!)) },
                                selectedgebidate = state.gebiDate,
                                onBackClick = { onAction(SignupAction.OnBackClicked) },
                                selectedProfilePic = state.profilePic,
                                onProfilePicClick = {showImagePickerDialog = true},
                                focus = focus,
                                modifier = Modifier.fillMaxWidth(),

                                )

                            Spacer(modifier = Modifier.height(16.dp))

                            SignUpForm2(
                                passwordText = state.passwordState.text,
                                onpasswordTextChange = {
                                    onAction(
                                        SignupAction.OnPasswordTextChange(
                                            it,
                                            false
                                        )
                                    )
                                },
                                passworderrorText = state.passwordState.errorMessage,
                                password2Text = state.passwordRetypeState.text,
                                onpassword2TextChange = {
                                    onAction(
                                        SignupAction.OnPasswordTextChange(
                                            it,
                                            true
                                        )
                                    )
                                },
                                password2errorText = state.passwordRetypeState.errorMessage,
                                onSignupButtonClick = { onAction(SignupAction.OnSignUpButtonPress) },
                                signupbuttondisabled = state.createButtonDisabled,
                                signupbuttonloading = state.isLoading,
                                onCheckBoxCheckedChange = { onAction(SignupAction.OnAgbChecked(it)) },
                                checkboxChecked = state.agbsAccepted,
                                modifier = Modifier.fillMaxWidth(),
                                focus = focus

                                )
                        }

                    }
                    DeviceSizeConfiguration.MOBILE_LANDSCAPE -> {
                        println("Mobile Landscape")

                        Row(
                            modifier = rootmodifier
                                .verticalScroll(rememberScrollState()),

                            horizontalArrangement = Arrangement.spacedBy(32.dp)
                        ){


                            SignUpForm1(
                                usernameText = state.usernameState.text,
                                onusernameTextChange = { onAction(SignupAction.OnUsernameTextChange(it)) },
                                usernameerrorText = state.usernameState.errorMessage,
                                emailText = state.emailState.text,
                                onemailTextChange = { onAction(SignupAction.OnEmailTextChange(it)) },
                                emailerrorText = state.emailState.errorMessage,
                                ongebidateselected = { onAction(SignupAction.OnGebiDateChange(it!!)) },
                                selectedgebidate = state.gebiDate,
                                onBackClick = {onAction(SignupAction.OnBackClicked)},
                                selectedProfilePic = state.profilePic,
                                onProfilePicClick = {showImagePickerDialog = true},
                                focus = focus,
                                modifier = Modifier.weight(1f),
                            )

                            SignUpForm2(
                                passwordText = state.passwordState.text,
                                onpasswordTextChange = { onAction(SignupAction.OnPasswordTextChange(it, false)) },
                                passworderrorText = state.passwordState.errorMessage,
                                password2Text = state.passwordRetypeState.text,
                                onpassword2TextChange = { onAction(SignupAction.OnPasswordTextChange(it, true)) },
                                password2errorText = state.passwordRetypeState.errorMessage,
                                onSignupButtonClick = { onAction(SignupAction.OnSignUpButtonPress)},
                                signupbuttondisabled = state.createButtonDisabled,
                                signupbuttonloading = state.isLoading,
                                onCheckBoxCheckedChange = { onAction(SignupAction.OnAgbChecked(it)) },
                                checkboxChecked = state.agbsAccepted,
                                modifier = Modifier.weight(1f),
                                focus = focus

                            )
                        }
                    }
                    DeviceSizeConfiguration.TABLET_PORTRAIT,
                    DeviceSizeConfiguration.TABLET_LANDSCAPE,
                    DeviceSizeConfiguration.DESKTOP -> {
                        println("Desktop")

                        Column(
                            modifier = rootmodifier
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ){
                            SignUpForm1(
                                usernameText = state.usernameState.text,
                                onusernameTextChange = { onAction(SignupAction.OnUsernameTextChange(it)) },
                                usernameerrorText = state.usernameState.errorMessage,
                                emailText = state.emailState.text,
                                onemailTextChange = { onAction(SignupAction.OnEmailTextChange(it)) },
                                emailerrorText = state.emailState.errorMessage,
                                ongebidateselected = { onAction(SignupAction.OnGebiDateChange(it!!)) },
                                onBackClick = {onAction(SignupAction.OnBackClicked)},
                                selectedProfilePic = state.profilePic,
                                onProfilePicClick = {showImagePickerDialog = true},
                                selectedgebidate = state.gebiDate,
                                focus = focus,
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            SignUpForm2(
                                passwordText = state.passwordState.text,
                                onpasswordTextChange = { onAction(SignupAction.OnPasswordTextChange(it, false)) },
                                passworderrorText = state.passwordState.errorMessage,
                                password2Text = state.passwordRetypeState.text,
                                onpassword2TextChange = { onAction(SignupAction.OnPasswordTextChange(it, true)) },
                                password2errorText = state.passwordRetypeState.errorMessage,
                                onSignupButtonClick = { onAction(SignupAction.OnSignUpButtonPress)},
                                signupbuttondisabled = state.createButtonDisabled,
                                onCheckBoxCheckedChange = { onAction(SignupAction.OnAgbChecked(it)) },
                                checkboxChecked = state.agbsAccepted,
                                signupbuttonloading = state.isLoading,
                                focus = focus
                            )
                        }
                    }
                }
            }
        }
    }

}