package org.lerchenflo.schneaggchatv3mp.login.presentation

import SignUpViewModel
import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.theme.SchneaggchatTheme
import org.lerchenflo.schneaggchatv3mp.utilities.DeviceSizeConfiguration

@Preview()
@Composable
fun SignUpScreen(
    onSignUpSuccess: () -> Unit = {}, // when login has finished successful
    modifier: Modifier = Modifier
        .fillMaxSize()
        .safeContentPadding()
){
    val viewModel = koinViewModel<SignUpViewModel>()

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
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .padding(
                    horizontal = 16.dp,
                    vertical = 24.dp
                )
                .consumeWindowInsets(WindowInsets.navigationBars)


            val windowSizeclass = currentWindowAdaptiveInfo().windowSizeClass
            val deviceConfiguration = DeviceSizeConfiguration.fromWindowSizeClass(windowSizeclass)
            when (deviceConfiguration){
                DeviceSizeConfiguration.MOBILE_PORTRAIT -> {
                    Column(
                        modifier = rootmodifier
                            .verticalScroll(rememberScrollState())
                            .padding(top = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ){
                        SignUpForm1(
                            usernameText = viewModel.username,
                            onusernameTextChange = { viewModel.updateUsername(it) },
                            usernameerrorText = viewModel.usernameerrorMessage,
                            emailText = viewModel.email,
                            onemailTextChange = { viewModel.updateEmail(it) },
                            emailerrorText = viewModel.emailerrorMessage,
                            ongebidateselected = {viewModel.updategebidate(it)},
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        SignUpForm2(
                            passwordText = viewModel.password,
                            onpasswordTextChange = { viewModel.updatePassword(it) },
                            passworderrorText = viewModel.passworderrorMessage,
                            password2Text = viewModel.password2,
                            onpassword2TextChange = { viewModel.updatePassword2(it) },
                            password2errorText = viewModel.password2errorMessage,
                            onSignupButtonClick = { viewModel.signup(onSignUpSuccess) },
                            signupbuttondisabled = viewModel.signupButtonDisabled,
                            signupbuttonloading = viewModel.isLoading,
                            genderslidertext = viewModel.gender,
                            genderslidervalue = viewModel.genderslidervalue,
                            ongendersliderValueChange = {viewModel.updategenderslidervalue(it)},
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                }
                DeviceSizeConfiguration.MOBILE_LANDSCAPE -> {
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
                            usernameText = viewModel.username,
                            onusernameTextChange = { viewModel.updateUsername(it) },
                            usernameerrorText = viewModel.usernameerrorMessage,
                            emailText = viewModel.email,
                            onemailTextChange = { viewModel.updateEmail(it) },
                            emailerrorText = viewModel.emailerrorMessage,
                            modifier = Modifier.weight(1f),
                            ongebidateselected = { viewModel.updategebidate(it) }
                        )

                        SignUpForm2(
                            passwordText = viewModel.password,
                            onpasswordTextChange = { viewModel.updatePassword(it) },
                            passworderrorText = viewModel.passworderrorMessage,
                            password2Text = viewModel.password2,
                            onpassword2TextChange = { viewModel.updatePassword2(it) },
                            password2errorText = viewModel.password2errorMessage,
                            onSignupButtonClick = { viewModel.signup(onSignUpSuccess) },
                            signupbuttondisabled = viewModel.signupButtonDisabled,
                            signupbuttonloading = viewModel.isLoading,
                            genderslidertext = viewModel.gender,
                            genderslidervalue = viewModel.genderslidervalue,
                            ongendersliderValueChange = { viewModel.updategenderslidervalue(it) },
                            modifier = Modifier.weight(1f)

                            )
                    }
                }
                DeviceSizeConfiguration.TABLET_PORTRAIT,
                DeviceSizeConfiguration.TABLET_LANDSCAPE,
                DeviceSizeConfiguration.DESKTOP -> {
                    Column(
                        modifier = rootmodifier
                            .verticalScroll(rememberScrollState())
                            .padding(top = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ){
                        SignUpForm1(
                            usernameText = viewModel.username,
                            onusernameTextChange = { viewModel.updateUsername(it) },
                            usernameerrorText = viewModel.usernameerrorMessage,
                            emailText = viewModel.email,
                            onemailTextChange = {viewModel.updateEmail(it)},
                            emailerrorText = viewModel.emailerrorMessage,
                            ongebidateselected = { viewModel.updategebidate(it) },
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        SignUpForm2(
                            passwordText = viewModel.password,
                            onpasswordTextChange = {viewModel.updatePassword(it)},
                            passworderrorText = viewModel.passworderrorMessage,
                            password2Text = viewModel.password2,
                            onpassword2TextChange = {viewModel.updatePassword2(it)},
                            password2errorText = viewModel.password2errorMessage,
                            onSignupButtonClick = { viewModel.signup(onSignUpSuccess) },
                            signupbuttondisabled = viewModel.signupButtonDisabled,
                            signupbuttonloading = viewModel.isLoading,
                            genderslidertext = viewModel.gender,
                            genderslidervalue = viewModel.genderslidervalue,
                            ongendersliderValueChange = { viewModel.updategenderslidervalue(it) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

}