package org.lerchenflo.schneaggchatv3mp.login.presentation.signup

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.theme.SchneaggchatTheme
import org.lerchenflo.schneaggchatv3mp.utilities.DeviceSizeConfiguration

// todo Datenschutz und Agbs
// todo back button
// todo error messages only when typed

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
    heightDp = 400,
    widthDp = 600
)
@Composable
fun SignUpScreen(
    modifier: Modifier = Modifier
        .fillMaxSize()
        .safeContentPadding(),
    onAction : (SignupAction) -> Unit = {},
    state: SignupState = SignupState()
){

    SchneaggchatTheme {
        //Responsive UI mit scaffold
        Scaffold(
            modifier = modifier,
            contentWindowInsets = WindowInsets.statusBars
        ){innerpadding ->

            val rootmodifier = Modifier
                .fillMaxSize()
                .padding(innerpadding)
                .clip(RoundedCornerShape(
                    topStart = 15.dp,
                    topEnd = 15.dp
                ))
                .padding(
                    horizontal = 16.dp,
                    vertical = 24.dp
                )
                .consumeWindowInsets(WindowInsets.navigationBars)


            val windowSizeclass = currentWindowAdaptiveInfo().windowSizeClass
            val deviceConfiguration = DeviceSizeConfiguration.fromWindowSizeClass(windowSizeclass)
            when (deviceConfiguration){
                DeviceSizeConfiguration.MOBILE_PORTRAIT -> {
                    println("Mobile Portrait")
                    Column(
                        modifier = rootmodifier
                            .verticalScroll(rememberScrollState())
                            .padding(top = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ){
                        SignUpForm1(
                            usernameText = state.usernameState.text,
                            onusernameTextChange = { onAction(SignupAction.OnUsernameTextChange(it)) },
                            usernameerrorText = state.usernameState.errorMessage,
                            emailText = state.emailState.text,
                            onemailTextChange = { onAction(SignupAction.OnEmailTextChange(it)) },
                            emailerrorText = state.emailState.errorMessage,
                            ongebidateselected = { /*TODO*/},
                            selectedgebidate = state.gebiDate,
                            modifier = Modifier.fillMaxWidth()
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

                        )
                    }

                }
                DeviceSizeConfiguration.MOBILE_LANDSCAPE -> {
                    println("Mobile Landscape")

                    Row(
                        modifier = rootmodifier
                            .windowInsetsPadding(WindowInsets.displayCutout)
                            .padding(
                                horizontal = 32.dp
                            )
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
                            modifier = Modifier.weight(1f)

                            )
                    }
                }
                DeviceSizeConfiguration.TABLET_PORTRAIT,
                DeviceSizeConfiguration.TABLET_LANDSCAPE,
                DeviceSizeConfiguration.DESKTOP -> {
                    println("Desktop")

                    Column(
                        modifier = rootmodifier
                            .verticalScroll(rememberScrollState())
                            .padding(top = 32.dp),
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
                            selectedgebidate = state.gebiDate,
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
                        )
                    }
                }
            }
        }
    }

}