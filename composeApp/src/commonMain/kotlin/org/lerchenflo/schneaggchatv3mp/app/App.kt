package org.lerchenflo.schneaggchatv3mp.app

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.ViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.chat.Presentation.ChatScreen
import org.lerchenflo.schneaggchatv3mp.chat.Presentation.Chatauswahlscreen
import org.lerchenflo.schneaggchatv3mp.chat.Presentation.NewChat
import org.lerchenflo.schneaggchatv3mp.chat.Presentation.SharedViewModel
import org.lerchenflo.schneaggchatv3mp.login.Presentation.LoginScreen
import org.lerchenflo.schneaggchatv3mp.login.Presentation.SignUpScreen
import org.lerchenflo.schneaggchatv3mp.theme.SchneaggchatTheme
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager


@Composable
@Preview
fun App() {
    SchneaggchatTheme {
        val navController = rememberNavController()
        val snackbarHostState = remember { SnackbarHostState() } // for snackbar
        val scope = rememberCoroutineScope()
        //println("hello world")
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
                val startDestination = if(false){Route.ChatSelector} else {Route.Login} // todo logik ob ma ind login kummt oder ned
                navigation<Route.ChatGraph>(
                    startDestination = startDestination
                ) {
                    // chat selector (gegnerauswahl)
                    composable<Route.ChatSelector>(
                        enterTransition = { slideInHorizontally { fullWidth -> fullWidth } },
                        exitTransition = { slideOutHorizontally { fullWidth -> -fullWidth } },
                        popEnterTransition = { slideInHorizontally { fullWidth -> -fullWidth } },
                        popExitTransition = { slideOutHorizontally { fullWidth -> fullWidth } }
                    ) {
                        val sharedViewModel = it.sharedKoinViewModel<SharedViewModel>(navController)

                        LaunchedEffect(true) {
                            sharedViewModel.onLeaveChat() // am afang wenn ma noch nix selected hot an null
                        }

                        Chatauswahlscreen(
                            onChatSelected = { user ->
                                sharedViewModel.onSelectChat(user)

                                // Navigate to Chat screen with user ID
                                println("opening chat with $user.id")
                                navController.navigate(Route.Chat)
                            },
                            onNewChatClick = {
                                navController.navigate(Route.NewChat)
                            }

                        )
                    }

                    // chat
                    composable<Route.Chat> {
                        val sharedViewModel = it.sharedKoinViewModel<SharedViewModel>(navController)
                        ChatScreen(
                            sharedViewModel = sharedViewModel,
                            onBackClick = {
                                navController.navigateUp()
                            }
                        )

                    }

                    // newChat (neuegegnergruppen)
                    composable<Route.NewChat> {
                        NewChat()
                    }

                    // Login screen
                    composable<Route.Login>{

                        LoginScreen(

                            onLoginSuccess = {
                                navController.navigate(Route.ChatSelector)
                            },
                            onSignUp = {
                                navController.navigate(Route.SignUp)
                            }
                        )
                    }

                    composable<Route.SignUp>{
                        SignUpScreen()
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