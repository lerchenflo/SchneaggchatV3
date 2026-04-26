package org.lerchenflo.schneaggchatv3mp.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.Blind
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Star
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
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
import org.lerchenflo.schneaggchatv3mp.app.logging.LoggingRepository
import org.lerchenflo.schneaggchatv3mp.app.navigation.NavigationAction
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.ObserveAsEvents
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.app.theme.SchneaggchatTheme
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.ChatScreen
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chatdetails.ChatDetails
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chatselector.Chatauswahlscreen
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chatselector.MessageChatSelector
import org.lerchenflo.schneaggchatv3mp.chat.presentation.newchat.GroupCreatorScreenRoot
import org.lerchenflo.schneaggchatv3mp.chat.presentation.newchat.NewChat
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.datasource.network.TokenManager
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.isConnectionError
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.ThemeSetting
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameScreenElement
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameSelectorScreen
import org.lerchenflo.schneaggchatv3mp.games.presentation.dartcounter.DartCounter
import org.lerchenflo.schneaggchatv3mp.games.presentation.tetris.TetrisScreen
import org.lerchenflo.schneaggchatv3mp.games.presentation.tetris.TetrisViewModel
import org.lerchenflo.schneaggchatv3mp.games.presentation.towerstack.TowerStackScreen
import org.lerchenflo.schneaggchatv3mp.games.presentation.undercover.Undercover
import org.lerchenflo.schneaggchatv3mp.games.presentation.yatzi.YatziGameScreen
import org.lerchenflo.schneaggchatv3mp.games.presentation.yatzi.YatziSetupScreen
import org.lerchenflo.schneaggchatv3mp.games.presentation.yatzi.YatziViewModel
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
import org.lerchenflo.schneaggchatv3mp.todolist.presentation.TodolistScreen
import org.lerchenflo.schneaggchatv3mp.utilities.IncomingDataManager
import org.lerchenflo.schneaggchatv3mp.utilities.LanguageService
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.ThemeSetting
import org.lerchenflo.schneaggchatv3mp.games.presentation.morse.MorseScreen
import org.lerchenflo.schneaggchatv3mp.games.presentation.morse.MorseViewModel
import org.lerchenflo.schneaggchatv3mp.games.presentation.yatzi.YatziViewModel
import org.lerchenflo.schneaggchatv3mp.utilities.UiText
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.error_access_not_permitted
import schneaggchatv3mp.composeapp.generated.resources.games_dartcounter_title
import schneaggchatv3mp.composeapp.generated.resources.games_morse_title
import schneaggchatv3mp.composeapp.generated.resources.games_stack_tower
import schneaggchatv3mp.composeapp.generated.resources.games_undercover_title


@Composable
@Preview(showBackground = true)
fun App() {
    val preferenceManager = koinInject<Preferencemanager>()
    val languageService = koinInject<LanguageService>()
    val themeSetting by preferenceManager.getThemeFlow().collectAsState(initial = ThemeSetting.SYSTEM)

    val tokenManager = koinInject<TokenManager>()
    val loggingRepository = koinInject<LoggingRepository>()

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
                        subclass(Route.MessageChatSelector::class, Route.MessageChatSelector.serializer())
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

        //Backstack for settings
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

        //Backstack for games
        val gamesBackStack = rememberNavBackStack(
            configuration = SavedStateConfiguration{
                serializersModule = SerializersModule {
                    polymorphic(NavKey::class) {
                        subclass(Route.Games.GamesSelector::class, Route.Games.GamesSelector.serializer())
                        subclass(Route.Games.DartCounter::class, Route.Games.DartCounter.serializer())

                        subclass(Route.Games.Undercover::class, Route.Games.Undercover.serializer())
                        subclass(Route.Games.TowerStack::class, Route.Games.TowerStack.serializer())
                        subclass(Route.Games.YatziSetup::class, Route.Games.YatziSetup.serializer())
                        subclass(Route.Games.YatziGame::class, Route.Games.YatziGame.serializer())
                        subclass(Route.Games.Tetris::class, Route.Games.Tetris.serializer())
                        subclass(Route.Games.Morse::class, Route.Games.Morse.serializer())


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

            /*
            println("NAVIGATION: Navigating ${when (action) {
                is NavigationAction.Navigate -> {
                    "to " + action.destination
                }
                is NavigationAction.NavigateBack -> {
                    "back"
                }
            }
            } \n${rootBackStack.toFormattedString()}")

             */

            val navigationOptions = action.navigationOptions

            if (navigationOptions.exitPreviousScreen){
                if (rootBackStack.size > 1){
                    rootBackStack.removeAt(rootBackStack.size - 1) //Removelast not working on older android
                }
            }

            //Helper function to remove all routes of a specific type from the backstack
            fun removeAllScreens(route: Route) {
                rootBackStack.removeAll { navKey -> navKey == route }
            }

            if (navigationOptions.removeAllScreensByRoute.isNotEmpty()) {
                navigationOptions.removeAllScreensByRoute.forEach {
                    removeAllScreens(it)
                }
            }

            if (navigationOptions.removeAllExceptByRoute != null) {
                rootBackStack.removeAll { navKey ->
                    navKey != navigationOptions.removeAllExceptByRoute
                }
            }


            when(action){
                is NavigationAction.Navigate -> {
                    if (navigationOptions.exitAllPreviousScreens){
                        rootBackStack.clear()
                    }

                    rootBackStack.add(action.destination)
                }
                is NavigationAction.NavigateBack -> {

                    //Ignore exitallpreviousscreens because the app would close

                    if (rootBackStack.size > 1){
                        rootBackStack.removeAt(rootBackStack.size - 1)
                    }
                }
            }
        }


        ObserveAsEvents(
            flow = AppRepository.ActionChannel.actions,
        ) { action ->
            scope.launch {
                when (action) {
                    AppRepository.ActionChannel.ActionEvent.Login -> {
                        // Login action handled automatically by HTTP client refresh
                        if (rootBackStack.contains(Route.ChatSelector)){
                            val error = tokenManager.refreshTokens(preferenceManager.getTokens().refreshToken)

                            if (error != null && !error.isConnectionError()){
                                AppRepository.ActionChannel.sendActionSuspend(AppRepository.ActionChannel.ActionEvent.AuthInvalidated)
                            }
                        }
                    }

                    AppRepository.ActionChannel.ActionEvent.AuthInvalidated -> {
                        //Throwing an error message for the user
                        AppRepository.ErrorChannel.trySendError(
                            event = AppRepository.ErrorChannel.ErrorEvent(
                                401,
                                errorMessageUiText = UiText.StringResourceText(Res.string.error_access_not_permitted),
                                duration = 5000L,
                            )
                        )
                        appRepository.logout()
                        navigator.navigate(
                            Route.Login,
                            navigationOptions = Navigator.NavigationOptions(exitAllPreviousScreens = true)
                        )
                    }
                }
            }
        }


        //Error popup handling
        var currentError by remember { mutableStateOf<AppRepository.ErrorChannel.ErrorEvent?>(null) }
        LaunchedEffect(Unit) {
            AppRepository.ErrorChannel.errors.collect { error ->
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
                .imePadding(),
            //contentWindowInsets = WindowInsets.c

        ) { innerpadding ->

            Column(
                modifier = Modifier
                    .padding(innerpadding),
            ) {

                val online by SessionCache.onlineFlow.collectAsStateWithLifecycle()

                //Show offline bar when offline
                if (!online) {
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
                            val globalViewModel = koinInject<GlobalViewModel>()

                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                            /*
                         (already logged in? [app open])
                             True                       false
                                /                            \
                              /                                       \
                         Incoming Data?                             Tokens saved?
                          /       \                                     /       \
                         true       false                          true      false
                         /                \                           /          \
                        chatselector    sendDataSelector    Incoming Data?      Login Screen
                                                            /        \
                                                           True     false
                                                            /          \
                                                      chatselector     sendDataSelector
                             */

                            LaunchedEffect(Unit) {
                                if (SessionCache.isLoggedIn()) { //If already logged in reroute to chatselector
                                    println("LOGGED IN; rerouting to chatselector")

                                    if(IncomingDataManager.isNewDataAvailable()){
                                        scope.launch {
                                            // manually navigate to messageChatselecotor to add Chatselector to backstack
                                            rootBackStack.clear()
                                            rootBackStack.add(Route.ChatSelector)
                                            rootBackStack.add(Route.MessageChatSelector)
                                        }
                                    }else{
                                        scope.launch {
                                            navigator.navigate(
                                                Route.ChatSelector,
                                                navigationOptions = Navigator.NavigationOptions(exitAllPreviousScreens = true)
                                            )
                                        }
                                    }


                                } else {
                                    val savedCreds = appRepository.loadSavedLoginConfig()
                                    if (!savedCreds) {
                                        navigator.navigate(
                                            Route.Login,
                                            navigationOptions = Navigator.NavigationOptions(exitAllPreviousScreens = true)
                                        )
                                    } else {
                                        println("IncomingDataManager text : ${IncomingDataManager.sharedText.value}")
                                        if(IncomingDataManager.isNewDataAvailable()){
                                            // manually navigate to messageChatselecotor to add Chatselector to backstack
                                            rootBackStack.clear()
                                            rootBackStack.add(Route.ChatSelector)
                                            rootBackStack.add(Route.MessageChatSelector)
                                        }else{
                                            navigator.navigate(
                                                Route.ChatSelector,
                                                navigationOptions = Navigator.NavigationOptions(exitAllPreviousScreens = true)
                                            )
                                        }

                                        globalViewModel.viewModelScope.launch {
                                            try { appRepository.dataSync() }
                                            catch (e: Exception) { println("Data sync error: ${e.message}") }
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
                        entry<Route.MessageChatSelector> {
                            MessageChatSelector()
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
                            //TODO GAMES: Shared games viewmodel for game selection
                            val yatziViewModel: YatziViewModel = koinViewModel<YatziViewModel>()

                            val gamesList = listOf<GameScreenElement>(
                                GameScreenElement(
                                    title = stringResource(Res.string.games_dartcounter_title),
                                    icon = Icons.Default.AdsClick, // ma darf sich gern was besseres usdenka
                                    route = Route.Games.DartCounter,
                                    inDev = false
                                ),
                                GameScreenElement(
                                    title = stringResource(Res.string.games_undercover_title),
                                    icon = Icons.Default.Blind,
                                    route = Route.Games.Undercover,
                                    inDev = false
                                ),
                                GameScreenElement(
                                    title = stringResource(Res.string.games_stack_tower),
                                    icon = Icons.Default.Menu, //
                                    route = Route.Games.TowerStack,
                                    inDev = true
                                ),
                                GameScreenElement(
                                    title = "Yahtzee",
                                    icon = Icons.Default.Star,
                                    route = Route.Games.YatziSetup,
                                    inDev = false
                                ),
                                GameScreenElement(
                                    title = "Tetris",
                                    icon = Icons.Default.Menu, // Placeholder
                                    route = Route.Games.Tetris,
                                    inDev = false
                                ),
                                GameScreenElement(
                                    title = stringResource(Res.string.games_morse_title),
                                    icon = Icons.Default.GraphicEq,
                                    route = Route.Games.Morse,
                                    inDev = false
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

                                    entry <Route.Games.YatziSetup> {
                                        YatziSetupScreen(
                                            onBack = {
                                                if (gamesBackStack.size > 1){
                                                    gamesBackStack.removeAt(gamesBackStack.size - 1)
                                                }
                                            },
                                            onStartGame = {
                                                gamesBackStack.add(Route.Games.YatziGame)
                                            },
                                            viewModel = yatziViewModel
                                        )
                                    }

                                    entry <Route.Games.YatziGame> {
                                        YatziGameScreen(
                                            onBack = {
                                                if (gamesBackStack.size > 1){
                                                    gamesBackStack.removeAt(gamesBackStack.size - 1)
                                                }
                                            },
                                            viewModel = yatziViewModel
                                        )
                                    }

                                    entry <Route.Games.Tetris> {
                                        val tetrisViewModel: TetrisViewModel = koinViewModel<TetrisViewModel>()
                                        TetrisScreen(
                                            onBackClick = {
                                                if (gamesBackStack.size > 1){
                                                    gamesBackStack.removeAt(gamesBackStack.size - 1)
                                                }
                                            },
                                            viewModel = tetrisViewModel
                                        )
                                    }

                                    entry <Route.Games.Morse> {
                                        val morseViewModel: MorseViewModel = koinViewModel()
                                        MorseScreen(
                                            onBackClick = {
                                                if (gamesBackStack.size > 1){
                                                    gamesBackStack.removeAt(gamesBackStack.size - 1)
                                                }
                                            },
                                            viewModel = morseViewModel
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