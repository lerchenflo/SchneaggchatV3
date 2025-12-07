package org.lerchenflo.schneaggchatv3mp.login.presentation.login

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.settings.presentation.UrlChangeDialog
import org.lerchenflo.schneaggchatv3mp.theme.SchneaggchatTheme
import org.lerchenflo.schneaggchatv3mp.utilities.DeviceSizeConfiguration

@Preview()
@Composable
fun LoginScreen(
    modifier: Modifier = Modifier
        .fillMaxSize()
){
    val viewModel = koinViewModel<LoginViewModel>()

    SchneaggchatTheme {

        var showUrlChangeDialog by remember {mutableStateOf(false)}

        //Responsive UI mit scaffold
        Scaffold(
            modifier = Modifier
                .fillMaxSize(),
            floatingActionButton = {
                IconButton(
                    onClick = {
                        showUrlChangeDialog = true
                    },
                ){
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = "change serverurl",
                        modifier = Modifier
                            .size(48.dp)
                    )
                }
            }
        ){

            if(showUrlChangeDialog){
                UrlChangeDialog(
                    onDismiss = {showUrlChangeDialog = false},
                    onConfirm = {showUrlChangeDialog = false}
                )
            }

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

            val windowSizeclass = currentWindowAdaptiveInfo().windowSizeClass
            val deviceConfiguration = DeviceSizeConfiguration.fromWindowSizeClass(windowSizeclass)

            // for continuing with tab
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
                            onLoginButtonClick = { viewModel.login() },
                            onSignupButtonClick = { viewModel.navigateSignUp() },
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
                            onLoginButtonClick = { viewModel.login() },
                            onSignupButtonClick = { viewModel.navigateSignUp() },
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
                            onLoginButtonClick = { viewModel.login() },
                            onSignupButtonClick = { viewModel.navigateSignUp() },
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