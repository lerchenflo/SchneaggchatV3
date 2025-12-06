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
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.compose.composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.lerchenflo.hallenmanager.sharedUi.UnderConstruction
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
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

        val scope = rememberCoroutineScope()


        //Setup of backstack(All available routes)
        val backStack = rememberNavBackStack(
            configuration = SavedStateConfiguration{
                serializersModule = SerializersModule {
                    polymorphic(NavKey::class) {
                        subclass(Route.Login::class, Route.Login.serializer())
                        subclass(Route.AutoLoginCredChecker::class, Route.AutoLoginCredChecker.serializer())
                        subclass(Route.ChatSelector::class, Route.ChatSelector.serializer())
                        subclass(Route.Chat::class, Route.Chat.serializer())
                        subclass(Route.NewChat::class, Route.NewChat.serializer())
                        subclass(Route.GroupCreator::class, Route.GroupCreator.serializer())
                        subclass(Route.SignUp::class, Route.SignUp.serializer())
                        subclass(Route.Settings::class, Route.Settings.serializer())
                        subclass(Route.ChatDetails::class, Route.ChatDetails.serializer())
                        subclass(Route.Todolist::class, Route.Todolist.serializer())
                        subclass(Route.UnderConstruction::class, Route.UnderConstruction.serializer())
                        subclass(Route.DeveloperSettings::class, Route.DeveloperSettings.serializer())
                    }
                }
            },
            Route.AutoLoginCredChecker //Initial activity: Autologinchecker
        )

        //Initialize navigator (TO navigate from viewmodels)
        val navigator = koinInject<Navigator>()

        //Initialize global repository
        val appRepository = koinInject<AppRepository>()


        val snackbarHostState = remember { SnackbarHostState() } // for snackbar
        LaunchedEffect(Unit) {
            SnackbarManager.init(snackbarHostState, scope)
        }

        //Observe what the navigator sends to change screens etc
        ObserveAsEvents(
            flow = navigator.navigationActions
        ){  action ->
            when(action){
                is NavigationAction.Navigate -> {
                    if (action.exitAllPreviousScreens){
                        backStack.clear() //TODO: DOES THIS WORK??
                    }
                    if (action.exitPreviousScreen){
                        backStack.removeLast()
                    }
                    backStack.add(action.destination)
                }
                NavigationAction.NavigateBack -> {
                    backStack.removeLast()
                }
            }
        }


        //Error popup handling
        var currentError by remember { mutableStateOf<AppRepository.ErrorChannel.ErrorEvent?>(null) }
        LaunchedEffect(Unit) {
            AppRepository.errors.collect { error ->
                println("Error popup thrown: $error")
                currentError = error
                delay(error.duration)
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
        ) { innerpadding ->

            //TODO: Use this padding everywhere to fix layouts?
            NavDisplay(
                backStack = backStack,
                entryProvider = { key ->
                    when(key) {
                        is Route.AutoLoginCredChecker -> {
                            NavEntry(key) {
                                //Content of this screen

                                val globalViewModel = koinInject<GlobalViewModel>()

                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

                                //Load initial credentials
                                LaunchedEffect(Unit) {
                                    val savedCreds = appRepository.loadSavedLoginConfig()

                                    if (savedCreds) {
                                        navigator.navigate(Route.ChatSelector, exitAllPreviousScreens = true) //Clear backstack

                                        globalViewModel.viewModelScope.launch {
                                            //Autologin

                                            val error = appRepository.refreshTokens()
                                            if (error == NetworkError.Unauthorized()){
                                                println("token refresh failed, rerouting to login")
                                                AppRepository.trySendError(
                                                    event = AppRepository.ErrorChannel.ErrorEvent(
                                                        401,
                                                        errorMessageUiText = UiText.StringResourceText(Res.string.error_access_not_permitted),
                                                        duration = 5000L,
                                                    )
                                                )
                                                navigator.navigate(Route.Login, exitAllPreviousScreens = true) //Clear backstack
                                            }
                                            //TODO: Sometimes there are other errors (Not logged in but in chatselector)

                                        }
                                    } else {
                                        navigator.navigate(Route.Login, exitAllPreviousScreens = true) //Clear backstack
                                    }
                                }
                            }
                        }
                        is Route.Login -> {
                            NavEntry(key) {
                                LoginScreen()
                            }
                        }
                        is Route.SignUp -> {
                            NavEntry(key) {
                                SignUpScreenRoot()
                            }
                        }
                        is Route.ChatSelector -> {
                            NavEntry(key) {
                                Chatauswahlscreen()
                            }
                        }
                        is Route.Chat -> {
                            NavEntry(key) {
                                ChatScreen()
                            }
                        }
                        is Route.NewChat -> {
                            NavEntry(key) {
                                NewChat()
                            }
                        }
                        is Route.GroupCreator -> {
                            NavEntry(key) {
                                GroupCreator()
                            }
                        }

                        is Route.ChatDetails -> {
                            NavEntry(key) {
                                ChatDetails()
                            }
                        }
                        is Route.Todolist -> {
                            NavEntry(key) {
                                TodolistScreen()
                            }
                        }


                        is Route.DeveloperSettings -> {
                            NavEntry(key) {
                                DeveloperSettings()
                            }
                        }
                        is Route.Settings -> {
                            NavEntry(key) {
                                SettingsScreen()
                            }
                        }



                        is Route.UnderConstruction -> {
                            NavEntry(key) {
                                UnderConstruction(

                                )
                            }
                        }



                        else -> error("Unknown navkey: $key")
                    }
                }
            )

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