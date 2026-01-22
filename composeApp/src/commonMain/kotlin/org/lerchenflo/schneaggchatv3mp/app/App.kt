package org.lerchenflo.schneaggchatv3mp.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.Blind
import androidx.compose.material.icons.filled.Menu
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
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.app.navigation.NavigationAction
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.ObserveAsEvents
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.ChatScreen
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chatdetails.ChatDetails
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chatselector.Chatauswahlscreen
import org.lerchenflo.schneaggchatv3mp.chat.presentation.newchat.GroupCreatorScreenRoot
import org.lerchenflo.schneaggchatv3mp.chat.presentation.newchat.NewChat
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkError
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.isConnectionError
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameScreenElement
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameSelectorScreen
import org.lerchenflo.schneaggchatv3mp.games.presentation.dartcounter.DartCounter
import org.lerchenflo.schneaggchatv3mp.games.presentation.towerstack.TowerStackScreen
import org.lerchenflo.schneaggchatv3mp.games.presentation.undercover.Undercover
import org.lerchenflo.schneaggchatv3mp.login.presentation.login.LoginScreen
import org.lerchenflo.schneaggchatv3mp.login.presentation.signup.SignUpScreenRoot
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.SchneaggmapScreenRoot
import org.lerchenflo.schneaggchatv3mp.settings.presentation.SettingsScreen
import org.lerchenflo.schneaggchatv3mp.settings.presentation.SharedSettingsViewmodel
import org.lerchenflo.schneaggchatv3mp.settings.presentation.appearancesettings.AppearanceSettings
import org.lerchenflo.schneaggchatv3mp.settings.presentation.devsettings.DeveloperSettings
import org.lerchenflo.schneaggchatv3mp.settings.presentation.miscSettings.MiscSettings
import org.lerchenflo.schneaggchatv3mp.settings.presentation.usersettings.UserSettings
import org.lerchenflo.schneaggchatv3mp.sharedUi.clearFocusOnTap
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.AutoFadePopup
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.OfflineBar
import org.lerchenflo.schneaggchatv3mp.theme.SchneaggchatTheme
import org.lerchenflo.schneaggchatv3mp.todolist.presentation.TodolistScreen
import org.lerchenflo.schneaggchatv3mp.utilities.LanguageService
import org.lerchenflo.schneaggchatv3mp.utilities.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import org.lerchenflo.schneaggchatv3mp.utilities.ThemeSetting
import org.lerchenflo.schneaggchatv3mp.utilities.UiText
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.error_access_not_permitted
import schneaggchatv3mp.composeapp.generated.resources.games_dartcounter_title
import schneaggchatv3mp.composeapp.generated.resources.games_stack_tower
import schneaggchatv3mp.composeapp.generated.resources.games_undercover_title


@Composable
@Preview(showBackground = true)
fun App() {
    val preferenceManager = koinInject<Preferencemanager>()
    val languageService = koinInject<LanguageService>()
    val themeSetting by preferenceManager.getThemeFlow().collectAsState(initial = ThemeSetting.SYSTEM)

    // Apply saved language on app startup
    LaunchedEffect(Unit) {
        val savedLanguage = languageService.getCurrentLanguage()
        languageService.applyLanguage(savedLanguage)
    }

    // Track app lifecycle for notification handling
    AppLifecycleTracker()

    SchneaggchatTheme(
        themeSetting = themeSetting
    ) {

        val scope = rememberCoroutineScope()


        //Setup of backstack(All available routes)
        val rootBackStack = rememberNavBackStack(
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
                        subclass(Route.ChatDetails::class, Route.ChatDetails.serializer())
                        subclass(Route.Todolist::class, Route.Todolist.serializer())
                        subclass(Route.Schneaggmap::class, Route.Schneaggmap.serializer())

                        //Subgraph for settings
                        subclass(Route.Settings::class, Route.Settings.serializer())

                        //sugraph for games
                        subclass(Route.Games::class, Route.Games.serializer())

                    }
                }
            },
            Route.AutoLoginCredChecker //Initial activity: Autologinchecker
        )

        val settingsBackStack = rememberNavBackStack(
            configuration = SavedStateConfiguration{
                serializersModule = SerializersModule {
                    polymorphic(NavKey::class) {
                        subclass(Route.Settings.SettingsScreen::class, Route.Settings.SettingsScreen.serializer())
                        subclass(Route.Settings.DeveloperSettings::class, Route.Settings.DeveloperSettings.serializer())
                        subclass(Route.Settings.UserSettings::class, Route.Settings.UserSettings.serializer())
                        subclass(Route.Settings.AppearanceSettings::class, Route.Settings.AppearanceSettings.serializer())
                        subclass(Route.Settings.MiscSettings::class, Route.Settings.MiscSettings.serializer())
                    }
                }
            },
            Route.Settings.SettingsScreen
        )


        val gamesBackStack = rememberNavBackStack(
            configuration = SavedStateConfiguration{
                serializersModule = SerializersModule {
                    polymorphic(NavKey::class) {
                        subclass(Route.Games.GamesSelector::class, Route.Games.GamesSelector.serializer())
                        subclass(Route.Games.DartCounter::class, Route.Games.DartCounter.serializer())

                        subclass(Route.Games.Undercover::class, Route.Games.Undercover.serializer())
                        subclass(Route.Games.TowerStack::class, Route.Games.TowerStack.serializer())


                    }
                }
            },
            Route.Games.GamesSelector
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
                        rootBackStack.clear()
                    }
                    if (action.exitPreviousScreen){
                        if (rootBackStack.size > 1){
                            rootBackStack.removeAt(rootBackStack.size - 1) //Removelast not working on older android
                        }
                    }
                    rootBackStack.add(action.destination)
                }
                NavigationAction.NavigateBack -> {

                    //Duplicate check for exiting chat after navigating from chatdetails
                    if (rootBackStack.size > 1){
                        val currentScreen = rootBackStack[rootBackStack.size - 1]
                        val previousScreen = rootBackStack[rootBackStack.size - 2]

                        // Check if the last two items are of the same type
                        if (currentScreen::class == previousScreen::class) {
                            // Remove both - current and previous
                            rootBackStack.removeAt(rootBackStack.size - 1) // Remove current
                            rootBackStack.removeAt(rootBackStack.size - 1) // Remove previous (now last)
                        } else {
                            // Normal back navigation - just remove current
                            rootBackStack.removeAt(rootBackStack.size - 1)
                        }
                    }

                }
            }
        }


        //Error popup handling
        var currentError by remember { mutableStateOf<AppRepository.ErrorChannel.ErrorEvent?>(null) }
        LaunchedEffect(Unit) {
            AppRepository.errors.collect { error ->
                //println("Error popup thrown: $error")
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
            snackbarHost = { SnackbarHost(snackbarHostState) },
            modifier = Modifier
                .clearFocusOnTap()
        ) { innerpadding ->

            Column(
                modifier = Modifier
                    .padding(innerpadding),
            ) {

                //Show when offline
                if (!SessionCache.online) {
                    OfflineBar(
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                }



                //Main content
                NavDisplay(
                    backStack = rootBackStack,
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator()
                    ),
                    entryProvider = entryProvider {

                        //Authentication
                        entry<Route.AutoLoginCredChecker> {
                            //Content of this screen

                            val globalViewModel = koinInject<GlobalViewModel>()

                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

                            //Load initial credentials
                            LaunchedEffect(Unit) {
                                //Load saved credentials (Tokens, Userid)
                                val savedCreds = appRepository.loadSavedLoginConfig() //Returns boolean

                                if (!savedCreds){
                                    //No tokens are saved, we navigate to the login
                                    navigator.navigate(Route.Login, exitAllPreviousScreens = true)
                                }else {

                                    //At this point there were credentials loaded from storage
                                    //Navigate to the chat
                                    navigator.navigate(Route.ChatSelector, exitAllPreviousScreens = true)

                                    //Launch a token refresh async
                                    globalViewModel.viewModelScope.launch {

                                        //Refresh the tokens (If there is an error which is not a network error (Access denied, tokens invalidated) we need to log out again)
                                        val error = appRepository.refreshTokens()

                                        //If no error happenend, sync all data with the server and return
                                        if (error == null) {
                                            appRepository.dataSync()
                                            return@launch
                                        }

                                        //At this point an error happened during token refresh
                                        println("Autologin token refresh error: $error")

                                        //Is the error a connection error (Connection to the server could not be established)
                                        if (error.isConnectionError()){
                                            println("Autologin connection error")

                                            //No sense to sync data, return
                                            return@launch
                                        }


                                        //Now there should only be real server errors left
                                        if (error is NetworkError.Unauthorized){
                                            println("Autologin not permitted by server, rerouting to login")

                                            //Throwing an error message for the user
                                            AppRepository.trySendError(
                                                event = AppRepository.ErrorChannel.ErrorEvent(
                                                    401,
                                                    errorMessageUiText = UiText.StringResourceText(Res.string.error_access_not_permitted),
                                                    duration = 5000L,
                                                )
                                            )

                                            //deleting all saved credentials, they will be invalid on next app start too
                                            appRepository.logout()

                                            //Navigate back to the loginscreen
                                            navigator.navigate(Route.Login, exitAllPreviousScreens = true)
                                        }else {
                                            //Log any other error which might occur
                                            AppRepository.trySendError(
                                                event = AppRepository.ErrorChannel.ErrorEvent(
                                                    error = error,
                                                    duration = 15000
                                                )
                                            )
                                        }
                                    }
                                }


                            }
                        }

                        entry<Route.Login> {
                            LoginScreen()
                        }

                        entry<Route.SignUp> {
                            SignUpScreenRoot()
                        }



                        //Chat
                        entry<Route.ChatSelector> {
                            Chatauswahlscreen()
                        }

                        entry<Route.Chat> {
                            ChatScreen()
                        }
                        entry<Route.ChatDetails> {
                            ChatDetails()
                        }


                        //New chat
                        entry<Route.NewChat> {
                            NewChat()
                        }

                        entry<Route.GroupCreator> {
                            GroupCreatorScreenRoot()
                        }



                        //Settings
                        entry<Route.Settings> {

                            //Initialize global settingsviewmodel which will survive as long as the settings are open
                            val sharedSettingsViewmodel = koinViewModel<SharedSettingsViewmodel>()

                            NavDisplay(
                                backStack = settingsBackStack,
                                entryProvider = entryProvider {
                                    entry<Route.Settings.SettingsScreen> {
                                        SettingsScreen(
                                            settingsViewmodel = koinInject(),
                                            sharedSettingsViewmodel = sharedSettingsViewmodel,
                                            onBackClick = {
                                                scope.launch {
                                                    navigator.navigateBack() //Settings backstack gets cleared automatically
                                                }
                                            },
                                            navigateUserSettings = {settingsBackStack.add(Route.Settings.UserSettings)},
                                            navigateDevSettings = {settingsBackStack.add(Route.Settings.DeveloperSettings)},
                                            navigateAppearanceSettings = {settingsBackStack.add(Route.Settings.AppearanceSettings)},
                                            navigateMiscSettings = {settingsBackStack.add(Route.Settings.MiscSettings)}
                                        )
                                    }

                                    entry<Route.Settings.DeveloperSettings> {
                                        DeveloperSettings(
                                            devSettingsViewModel = koinInject(),
                                            sharedSettingsViewmodel = sharedSettingsViewmodel,
                                            onBackClick = {
                                                if (settingsBackStack.size > 1){
                                                    settingsBackStack.removeAt(settingsBackStack.size - 1)
                                                }
                                            }
                                        )
                                    }

                                    entry<Route.Settings.UserSettings> {
                                        UserSettings(
                                            userSettingsViewModel = koinInject(),
                                            sharedSettingsViewmodel = sharedSettingsViewmodel,
                                            onBackClick = {
                                                if (settingsBackStack.size > 1){
                                                    settingsBackStack.removeAt(settingsBackStack.size - 1)
                                                }
                                            }
                                        )
                                    }

                                    entry<Route.Settings.AppearanceSettings> {
                                        AppearanceSettings(
                                            appearanceSettingsViewModel = koinInject(),
                                            sharedSettingsViewmodel = sharedSettingsViewmodel,
                                            onBackClick = {
                                                if (settingsBackStack.size > 1){
                                                    settingsBackStack.removeAt(settingsBackStack.size - 1)
                                                }
                                            }
                                        )
                                    }

                                    entry<Route.Settings.MiscSettings> {
                                        MiscSettings(
                                            miscSettingsViewModel = koinInject(),
                                            sharedSettingsViewmodel = sharedSettingsViewmodel,
                                            onBackClick = {
                                                if (settingsBackStack.size > 1){
                                                    settingsBackStack.removeAt(settingsBackStack.size - 1)
                                                }
                                            }
                                        )
                                    }
                                }
                            )
                        }


                        entry<Route.Todolist> {
                            TodolistScreen()

                        }


                        entry<Route.Schneaggmap> {
                            SchneaggmapScreenRoot()
                        }

                        entry<Route.Games> {
                            //TODO: Shared games viewmodel for game selection

                            val gamesList = listOf<GameScreenElement>(
                                GameScreenElement(
                                    title = stringResource(Res.string.games_dartcounter_title),
                                    icon = Icons.Default.AdsClick, // ma darf sich gern was besseres usdenka
                                    route = Route.Games.DartCounter
                                ),
                                GameScreenElement(
                                    title = stringResource(Res.string.games_undercover_title),
                                    icon = Icons.Default.Blind, // todo i hab noch ned verstanda um was es in deam spiel goht
                                    route = Route.Games.Undercover
                                ),
                                GameScreenElement(
                                    title = stringResource(Res.string.games_stack_tower),
                                    icon = Icons.Default.Menu, //
                                    route = Route.Games.TowerStack
                                ),

                            )
                            NavDisplay(
                                backStack = gamesBackStack,
                                entryProvider = entryProvider {
                                    entry <Route.Games.GamesSelector> {
                                        GameSelectorScreen(
                                            onBackClick = {
                                                scope.launch {
                                                    navigator.navigateBack() //Settings backstack gets cleared automatically
                                                }
                                            },
                                            onGameSelection = {
                                                scope.launch {
                                                    gamesBackStack.add(it)
                                                }
                                            },
                                            gamesList = gamesList
                                        )

                                    }

                                    entry <Route.Games.DartCounter> {
                                        DartCounter(
                                            onBackClick = {
                                                if (gamesBackStack.size > 1){
                                                    gamesBackStack.removeAt(gamesBackStack.size - 1)
                                                }
                                            }
                                        )
                                    }

                                    entry <Route.Games.Undercover> {
                                        Undercover(
                                            onBackClick = {
                                                if (gamesBackStack.size > 1){
                                                    gamesBackStack.removeAt(gamesBackStack.size - 1)
                                                }
                                            }
                                        )
                                    }

                                    entry <Route.Games.TowerStack> {
                                        TowerStackScreen(
                                            onBackClick = {
                                                if (gamesBackStack.size > 1){
                                                    gamesBackStack.removeAt(gamesBackStack.size - 1)
                                                }
                                            }
                                        )
                                    }
                                }
                            )

                        }
                    }

                )

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