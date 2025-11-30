package org.lerchenflo.schneaggchatv3mp.app

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.lerchenflo.hallenmanager.sharedUi.UnderConstruction
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.app.navigation.NavigationAction
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.ObserveAsEvents
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.ChatScreen
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chatdetails.ChatDetails
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chatselector.Chatauswahlscreen
import org.lerchenflo.schneaggchatv3mp.chat.presentation.newchat.GroupCreator
import org.lerchenflo.schneaggchatv3mp.chat.presentation.newchat.NewChat
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkError
import org.lerchenflo.schneaggchatv3mp.login.presentation.login.LoginScreen
import org.lerchenflo.schneaggchatv3mp.login.presentation.signup.SignUpScreenRoot
import org.lerchenflo.schneaggchatv3mp.settings.presentation.DeveloperSettings
import org.lerchenflo.schneaggchatv3mp.settings.presentation.SettingsScreen
import org.lerchenflo.schneaggchatv3mp.sharedUi.AutoFadePopup
import org.lerchenflo.schneaggchatv3mp.theme.SchneaggchatTheme
import org.lerchenflo.schneaggchatv3mp.todolist.presentation.TodolistScreen
import org.lerchenflo.schneaggchatv3mp.utilities.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import org.lerchenflo.schneaggchatv3mp.utilities.ThemeSetting
import org.lerchenflo.schneaggchatv3mp.utilities.UiText
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.error_access_not_permitted


@Composable
@Preview(showBackground = true)
fun App() {
    val preferenceManager = koinInject<Preferencemanager>()
    val themeSetting by preferenceManager.getThemeFlow().collectAsState(initial = ThemeSetting.SYSTEM)

    SchneaggchatTheme(
        themeSetting = themeSetting
    ) {

        val navigator = koinInject<Navigator>()
        val navController = rememberNavController()

        val snackbarHostState = remember { SnackbarHostState() } // for snackbar
        val scope = rememberCoroutineScope()

        val appRepository = koinInject<AppRepository>()

        // initialize manager
        LaunchedEffect(Unit) {
            SnackbarManager.init(snackbarHostState, scope)
        }

        ObserveAsEvents(
            flow = navigator.navigationActions
        ){  action ->
            when(action){
                is NavigationAction.Navigate -> navController.navigate(action.destination){action.navOptions(this)}
                NavigationAction.NavigateBack -> navController.navigateUp()
            }
        }


        var currentError by remember { mutableStateOf<AppRepository.ErrorChannel.ErrorEvent?>(null) }

        LaunchedEffect(Unit) {
            AppRepository.errors.collect { error ->
                println("Error popup thrown: $error")
                currentError = error
                kotlinx.coroutines.delay(error.duration)
                currentError = null
            }
        }

        currentError?.let { error ->
            AutoFadePopup(
                message = error.toStringComposable(), // Called in composable context
                showDuration = error.duration,
                onDismiss = { currentError = null }
            )
        }



        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->


            NavHost(
                navController = navController,
                startDestination = Route.ChatGraph
            ) {

                navigation<Route.ChatGraph>(
                    startDestination = Route.AutoLoginCredChecker
                ) {
                    composable<Route.AutoLoginCredChecker> {

                        val globalViewModel = it.sharedKoinViewModel<GlobalViewModel>(navController)


                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }


                        LaunchedEffect(Unit) {
                            val savedCreds = appRepository.loadSavedLoginConfig()

                            if (savedCreds) {
                                navController.navigate(Route.ChatSelector) {
                                    popUpTo(Route.AutoLoginCredChecker) { inclusive = true } // remove login from backstack
                                }

                                globalViewModel.viewModelScope.launch {
                                    //Autologin

                                    val error = appRepository.refreshTokens()
                                    if (error == NetworkError.UNAUTHORIZED){
                                        println("token refresh failed, rerouting to login")
                                        AppRepository.trySendError(
                                            event = AppRepository.ErrorChannel.ErrorEvent(
                                                401,
                                                errorMessageUiText = UiText.StringResourceText(Res.string.error_access_not_permitted),
                                                duration = 5000L,
                                            )
                                        )
                                        navController.navigate(Route.Login) {
                                            popUpTo(Route.Login) { inclusive = true }
                                        }
                                    }

                                }
                            } else {
                                navController.navigate(Route.Login) {
                                    popUpTo(Route.AutoLoginCredChecker) { inclusive = true }
                                }

                            }
                        }
                    }



                    // chat selector (gegnerauswahl)
                    composable<Route.ChatSelector>(
                        enterTransition = { slideInHorizontally { fullWidth -> fullWidth } },
                        exitTransition = { slideOutHorizontally { fullWidth -> -fullWidth } },
                        popEnterTransition = { slideInHorizontally { fullWidth -> -fullWidth } },
                        popExitTransition = { slideOutHorizontally { fullWidth -> fullWidth } }
                    ) {
                        val globalViewModel = it.sharedKoinViewModel<GlobalViewModel>(navController)


                        LaunchedEffect(true) {
                            globalViewModel.onLeaveChat() // am afang wenn ma noch nix selected hot an null
                        }

                        Chatauswahlscreen()
                    }

                    // chat
                    composable<Route.Chat> {
                        ChatScreen(
                            onBackClick = {
                                navController.navigateUp()
                            },
                            onChatDetailsClick = {
                                navController.navigate(Route.ChatDetails)
                            }
                        )

                    }

                    // newChat (neuegegnergruppen)
                    composable<Route.NewChat> {
                        NewChat(
                            onBackClick = {
                                navController.navigateUp()
                            },
                            onGroupCreator = {
                                navController.navigate(Route.GroupCreator)
                            }
                        )
                    }
                    // group creator
                    composable<Route.GroupCreator> {
                        GroupCreator(
                            onBackClick = {
                                navController.navigateUp()
                            }
                        )
                    }

                    composable<Route.ChatDetails>{
                        ChatDetails(
                            onBackClick = {
                                navController.navigateUp()
                            }
                        )
                    }

                    // Login screen
                    composable<Route.Login>{

                        LoginScreen(

                            onLoginSuccess = {
                                println("Login success, Chatselector")
                                navController.navigate(Route.ChatSelector) {
                                    popUpTo(Route.ChatGraph) { inclusive = true }
                                }

                            },
                            onSignUp = {
                                println("Create acc")
                                navController.navigate(Route.SignUp)
                            }
                        )
                    }

                    // Sign up page
                    composable<Route.SignUp>{
                        SignUpScreenRoot()
                    }

                    // Settings page
                    composable<Route.Settings>{
                        SettingsScreen(
                            onBackClick = {
                                navController.navigateUp()
                            },
                            toLoginNavigator = {
                                navController.navigate(Route.Login)
                            },
                            toDevSettingsNavigator = {
                                navController.navigate(Route.DeveloperSettings)
                            }
                        )
                    }

                    //Todoliste
                    composable<Route.Todolist>{
                        TodolistScreen(
                            onBackClick = {
                                navController.navigateUp()
                            }
                        )
                    }

                    // Developer Settings
                    composable<Route.DeveloperSettings>{
                        DeveloperSettings(
                            onBackClick = {
                                navController.navigateUp()
                            }
                        )
                    }

                    // Under Construction
                    composable<Route.UnderConstruction>{
                        UnderConstruction(

                        )
                    }

                }
            }

        }

    }
}

@Composable
private inline fun <reified T: ViewModel> NavBackStackEntry.sharedKoinViewModel(
    navController: NavController
): T {
    val navGraphRoute = destination.parent?.route ?: return koinViewModel<T>()
    val parentEntry = remember(this) {
        navController.getBackStackEntry(navGraphRoute)
    }
    return koinViewModel(
        viewModelStoreOwner = parentEntry
    )
}