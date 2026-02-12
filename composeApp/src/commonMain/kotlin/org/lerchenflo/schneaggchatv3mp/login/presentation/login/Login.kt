package org.lerchenflo.schneaggchatv3mp.login.presentation.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.UrlChangeDialog
import org.lerchenflo.schneaggchatv3mp.app.theme.SchneaggchatTheme
import org.lerchenflo.schneaggchatv3mp.utilities.DeviceSizeConfiguration
import org.lerchenflo.schneaggchatv3mp.utilities.LanguageService
import org.lerchenflo.schneaggchatv3mp.utilities.preferences.LanguageSetting
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.version

@Preview()
@Composable
fun LoginScreen(
    modifier: Modifier = Modifier
        .fillMaxSize()
){
    val viewModel = koinViewModel<LoginViewModel>()
    val appRepository = koinInject<AppRepository>()

    SchneaggchatTheme {


        val languageService = koinInject<LanguageService>()

        var showVoriLanguageDialog by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            val language = languageService.getCurrentLanguage()

            if (language == LanguageSetting.SYSTEM) {
                val systemlanguage = languageService.getSystemLanguage()
                println("system language: $systemlanguage")

                if (systemlanguage.trim().startsWith("de") || systemlanguage.trim().startsWith("DE")) {
                    showVoriLanguageDialog = true
                }
            }
        }

        val scope = rememberCoroutineScope()

        if (showVoriLanguageDialog) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Deutsche Systemsprache erkannt") },
                text = {
                    Column {
                        Text("Möchtest du zu Vorarlbergerisch wechseln?\nDu kannst dies später in den Einstellungen umstellen.")

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                scope.launch {
                                    languageService.applyLanguage(LanguageSetting.GERMAN)
                                    showVoriLanguageDialog = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Deutsch (DE)")
                        }
                        HorizontalDivider()

                        Button(
                            onClick = {
                                scope.launch {
                                    languageService.applyLanguage(LanguageSetting.VORI)
                                    showVoriLanguageDialog = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Vorarlbergerisch (DE-AT)")
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {},
            )
        }


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
                    onConfirm = {newserverurl ->
                        viewModel.updateServerUrl(newserverurl)
                        showUrlChangeDialog = false
                                },
                    serverUrl = viewModel.serverUrl
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

                        VersionText(appRepository)
                    }

                }
                DeviceSizeConfiguration.MOBILE_LANDSCAPE -> {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Row(
                            modifier = rootmodifier
                                .windowInsetsPadding(WindowInsets.displayCutout)
                                .padding(
                                    horizontal = 32.dp
                                )
                                .verticalScroll(rememberScrollState())
                                .weight(1f),

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
                        VersionText(appRepository)
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
                                .widthIn(max = 540.dp),
                            onLoginButtonClick = { viewModel.login() },
                            onSignupButtonClick = { viewModel.navigateSignUp() },
                            loginbuttonloading = viewModel.isLoading,
                            usernameFocusRequester = usernameFocusRequester,
                            passwordFocusRequester = passwordFocusRequester,
                            loginFocusRequester = loginFocusRequester
                        )

                        VersionText(appRepository)
                    }
                }
            }

        }
    }

}

@Composable
fun VersionText(appRepository: AppRepository) {
    Text(
        text = stringResource(Res.string.version, appRepository.appVersion.getVersionName()) + " Buildnr: " + appRepository.appVersion.getVersionCode(),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    )
}