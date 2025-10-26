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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chatdetails.ChatDetails
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.ChatScreen
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chatselector.Chatauswahlscreen
import org.lerchenflo.schneaggchatv3mp.chat.presentation.newchat.NewChat
import org.lerchenflo.schneaggchatv3mp.database.AppRepository
import org.lerchenflo.schneaggchatv3mp.login.presentation.login.LoginScreen
import org.lerchenflo.schneaggchatv3mp.login.presentation.signup.SignUpScreen
import org.lerchenflo.schneaggchatv3mp.settings.presentation.SettingsScreen
import org.lerchenflo.schneaggchatv3mp.theme.SchneaggchatTheme
import org.lerchenflo.schneaggchatv3mp.todolist.presentation.TodolistScreen
import org.lerchenflo.schneaggchatv3mp.utilities.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import org.lerchenflo.schneaggchatv3mp.utilities.ThemeSetting


@Composable
@Preview(showBackground = true)
fun App() {
    val preferenceManager = koinInject<Preferencemanager>()
    val themeSetting by preferenceManager.getThemeFlow().collectAsState(initial = ThemeSetting.SYSTEM)

    SchneaggchatTheme(themeSetting = themeSetting) {
        val navController = rememberNavController()
        val snackbarHostState = remember { SnackbarHostState() } // for snackbar
        val scope = rememberCoroutineScope()

        val appRepository = koinInject<AppRepository>()

        // initialize manager
        LaunchedEffect(Unit) {
            SnackbarManager.init(snackbarHostState, scope)
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
                            val savedCreds = appRepository.areLoginCredentialsSaved()

                            if (savedCreds) {
                                navController.navigate(Route.ChatSelector) {
                                    popUpTo(Route.AutoLoginCredChecker) { inclusive = true } // remove loading from backstack
                                }

                                globalViewModel.viewModelScope.launch {
                                    //Autologin
                                    appRepository.login(SessionCache.username,
                                        SessionCache.passwordDonotprint, onResult = { success, body ->
                                        println("Login abgeschlossen mit success: $success")
                                    })
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

                        Chatauswahlscreen(
                            onChatSelected = { user ->
                                globalViewModel.onSelectChat(user)

                                navController.navigate(Route.Chat)
                            },
                            onNewChatClick = {
                                navController.navigate(Route.NewChat)
                            },
                            onSettingsClick = {
                                navController.navigate(Route.Settings)
                            },
                            onToolsAndGamesClick = {
                                navController.navigate(Route.Todolist)
                            }
                        )
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
                        SignUpScreen(
                            onSignUpSuccess = {
                                println("Signup success, chatselector")
                                navController.navigate(Route.ChatSelector){
                                    popUpTo(Route.ChatGraph) { inclusive = true }
                                }
                            }
                        )
                    }

                    // Settings page
                    composable<Route.Settings>{
                        SettingsScreen(
                            onBackClick = {
                                navController.navigateUp()
                            },
                            toLoginNavigator = {
                                navController.navigate(Route.Login)
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