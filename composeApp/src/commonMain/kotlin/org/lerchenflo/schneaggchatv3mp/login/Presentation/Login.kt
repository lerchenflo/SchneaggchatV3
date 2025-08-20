package org.lerchenflo.schneaggchatv3mp.login.Presentation

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.lerchenflo.schneaggchatv3mp.theme.SchneaggchatTheme
import org.lerchenflo.schneaggchatv3mp.utilities.DeviceConfiguration

@Preview()
@Composable
fun LoginScreen(
    viewModel: LoginViewModel = viewModel(
        factory = LoginViewModel.Factory
    ),
    onLoginSuccess: () -> Unit = {}, // when login has finished successful
    onSignUp: () -> Unit = {},
    modifier: Modifier = Modifier
        .fillMaxSize()
        .safeContentPadding()
){
    SchneaggchatTheme {
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
            val deviceConfiguration = DeviceConfiguration.fromWindowSizeClass(windowSizeclass)
            when (deviceConfiguration){
                DeviceConfiguration.MOBILE_PORTRAIT -> {
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
                            loginbuttonloading = viewModel.isLoading
                        )
                    }

                }
                DeviceConfiguration.MOBILE_LANDSCAPE -> {
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
                            loginbuttonloading = viewModel.isLoading
                        )
                    }
                }
                DeviceConfiguration.TABLET_PORTRAIT,
                DeviceConfiguration.TABLET_LANDSCAPE,
                DeviceConfiguration.DESKTOP -> {
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
                            loginbuttonloading = viewModel.isLoading
                        )
                    }
                }
            }
        }
    }


    
    /*
    SchneaggchatTheme {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ){

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp), // Space between elements
                modifier = Modifier
                    .fillMaxWidth(0.9f)  // 90% width for better desktop sizing
                    .padding(16.dp),     // Outer padding

            ){
                // Schneaggchat text oba
                BasicText(
                    text = stringResource(Res.string.app_name),
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = 20.sp,
                        maxFontSize = 30.sp
                    ),
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentSize(Alignment.Center)
                        .padding(bottom = 8.dp)
                )
                // login Text oder so
                Text(
                    text = stringResource(Res.string.login),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentSize(Alignment.Center)
                )

                // Error message
                viewModel.errorMessage?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentSize(Alignment.Center)

                ) {
                    // username field
                    OutlinedTextField(
                        value = viewModel.username,
                        onValueChange = viewModel::updateUsername,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrectEnabled = true, // enables suggestions
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        placeholder = { Text(stringResource(Res.string.username))},
                        modifier = Modifier
                    )


                    // password field
                    OutlinedTextField(
                        value = viewModel.password,
                        onValueChange = viewModel::updatePassword,
                        placeholder = { Text(stringResource(Res.string.password))},
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Unspecified,
                            autoCorrectEnabled = false, // no suggestions for password
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                    )
                }


                // login button
                Button(
                    onClick = {
                        viewModel.login(onLoginSuccess)
                    },
                    enabled = !viewModel.isLoading,
                    modifier = Modifier.wrapContentSize(Alignment.Center)
                ) {
                    if (viewModel.isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(stringResource(Res.string.login))
                    }
                }

                // sign up button
                Button(
                    onClick = {onSignUp()},
                    modifier = Modifier
                        .wrapContentSize(Alignment.Center)
                ){
                    Text(
                        text = stringResource(Res.string.sign_up)
                    )
                }


            }
        }
    }

     */
}