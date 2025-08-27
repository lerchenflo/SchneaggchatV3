package org.lerchenflo.schneaggchatv3mp.login.presentation

import LoginViewModel
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.theme.SchneaggchatTheme
import org.lerchenflo.schneaggchatv3mp.utilities.DeviceSizeConfiguration

@Preview()
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit = {}, // when login has finished successful
    onSignUp: () -> Unit = {},
    modifier: Modifier = Modifier
        .fillMaxSize()
        .safeContentPadding()
){
    val viewModel = koinViewModel<LoginViewModel>()

    SchneaggchatTheme {

        // bruchts numma und da logout button müsst ma sunsch apassa dass ma ned direkt wida igloggt isch
        //viewModel.trysavedcredslogin(onLoginSuccess)

        //Responsive UI mit scaffold
        Scaffold(
            modifier = Modifier
                .fillMaxSize(),
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

            // for contiuning with tab
            val usernameFocusRequester = remember { FocusRequester() }
            val passwordFocusRequester = remember { FocusRequester() }
            val loginFocusRequester = remember { FocusRequester() }

            when (deviceConfiguration){
                DeviceSizeConfiguration.MOBILE_PORTRAIT -> {
                    Column(
                        modifier = rootmodifier,
                        verticalArrangement = Arrangement.spacedBy(32.dp)
                    ){
                        LoginHeaderText(
                            modifier = Modifier
                                .fillMaxWidth()
                        )

                        Spacer(Modifier.height(6.dp))

                        LoginFormSection(
                            usernameText = viewModel.username,
                            onusernameTextChange = { viewModel.updateUsername(it) },
                            passwordText = viewModel.password,
                            onPasswordTextChange = { viewModel.updatePassword(it) },
                            passwordTextError = viewModel.errorMessage,
                            loginbuttondisabled = viewModel.loginButtonDisabled,
                            modifier = Modifier
                                .fillMaxWidth(),
                            onLoginButtonClick = { viewModel.login(onLoginSuccess) },
                            onSignupButtonClick = { onSignUp() },
                            loginbuttonloading = viewModel.isLoading,
                            usernameFocusRequester = usernameFocusRequester,
                            passwordFocusRequester = passwordFocusRequester,
                            loginFocusRequester = loginFocusRequester
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
                        LoginHeaderText(
                            modifier = Modifier
                                .weight(1f)
                        )

                        LoginFormSection(
                            usernameText = viewModel.username,
                            onusernameTextChange = { viewModel.updateUsername(it) },
                            passwordText = viewModel.password,
                            onPasswordTextChange = { viewModel.updatePassword(it) },
                            passwordTextError = viewModel.errorMessage,
                            loginbuttondisabled = viewModel.loginButtonDisabled,
                            modifier = Modifier
                                .weight(1f),
                            onLoginButtonClick = { viewModel.login(onLoginSuccess) },
                            onSignupButtonClick = { onSignUp() },
                            loginbuttonloading = viewModel.isLoading,
                            usernameFocusRequester = usernameFocusRequester,
                            passwordFocusRequester = passwordFocusRequester,
                            loginFocusRequester = loginFocusRequester
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
                        LoginHeaderText(
                            modifier = Modifier
                                .widthIn(max = 540.dp),
                            alignment = Alignment.CenterHorizontally

                        )
                        Spacer(Modifier.height(6.dp))

                        LoginFormSection(
                            usernameText = viewModel.username,
                            onusernameTextChange = { viewModel.updateUsername(it) },
                            passwordText = viewModel.password,
                            onPasswordTextChange = { viewModel.updatePassword(it) },
                            passwordTextError = viewModel.errorMessage,
                            loginbuttondisabled = viewModel.loginButtonDisabled,
                            modifier = Modifier
                                .weight(1f),
                            onLoginButtonClick = { viewModel.login(onLoginSuccess) },
                            onSignupButtonClick = { onSignUp() },
                            loginbuttonloading = viewModel.isLoading,
                            usernameFocusRequester = usernameFocusRequester,
                            passwordFocusRequester = passwordFocusRequester,
                            loginFocusRequester = loginFocusRequester
                        )
                    }
                }
            }
        }
    }

}