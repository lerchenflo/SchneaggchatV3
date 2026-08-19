package org.lerchenflo.schneaggchatv3mp.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.app.logging.LoggingRepository
import org.lerchenflo.schneaggchatv3mp.app.navigation.BottomAppBarSwipable
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.ObserveAsEvents
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.app.navigation.TOP_LEVEL_DESTINATIONS
import org.lerchenflo.schneaggchatv3mp.app.navigation.rememberNavigationState
import org.lerchenflo.schneaggchatv3mp.app.navigation.toEntries
import org.lerchenflo.schneaggchatv3mp.app.onboarding.LocalTapTargetController
import org.lerchenflo.schneaggchatv3mp.app.onboarding.TapTargetController
import org.lerchenflo.schneaggchatv3mp.app.onboarding.TapTargetOverlay
import org.lerchenflo.schneaggchatv3mp.app.onboarding.TourSettings
import org.lerchenflo.schneaggchatv3mp.app.onboarding.rememberOnboardingTour
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
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameSelectorScreen
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameSelectorViewModel
import org.lerchenflo.schneaggchatv3mp.games.presentation.coinflip.CoinFlipScreen
import org.lerchenflo.schneaggchatv3mp.games.presentation.dartcounter.DartCounter
import org.lerchenflo.schneaggchatv3mp.games.presentation.fingerpicker.FingerPickerScreen
import org.lerchenflo.schneaggchatv3mp.games.presentation.game2048.Game2048ScreenRoot
import org.lerchenflo.schneaggchatv3mp.games.presentation.gridrush.GridRushScreenRoot
import org.lerchenflo.schneaggchatv3mp.games.presentation.morse.MorseScreen
import org.lerchenflo.schneaggchatv3mp.games.presentation.morse.MorseViewModel
import org.lerchenflo.schneaggchatv3mp.games.presentation.oddoneout.OddOneOutScreenRoot
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.RecapScreenRoot
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggahus.SchneaggaHusScreenRoot
import org.lerchenflo.schneaggchatv3mp.games.presentation.tetris.TetrisScreen
import org.lerchenflo.schneaggchatv3mp.games.presentation.tetris.TetrisViewModel
import org.lerchenflo.schneaggchatv3mp.games.presentation.towerstack.TowerStackScreen
import org.lerchenflo.schneaggchatv3mp.games.presentation.undercover.Undercover
import org.lerchenflo.schneaggchatv3mp.games.presentation.yatzi.YatziScreenRoot
import org.lerchenflo.schneaggchatv3mp.login.presentation.autologincredchecker.AutoLoginCredCheckerRoot
import org.lerchenflo.schneaggchatv3mp.login.presentation.emailverifiedcheck.EmailVerifiedCheckScreenRoot
import org.lerchenflo.schneaggchatv3mp.login.presentation.login.LoginScreen
import org.lerchenflo.schneaggchatv3mp.login.presentation.signup.SignUpScreenRoot
import org.lerchenflo.schneaggchatv3mp.roadmap.presentation.RoadmapScreen
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.SchneaggmapScreenRoot
import org.lerchenflo.schneaggchatv3mp.settings.presentation.SettingsScreen
import org.lerchenflo.schneaggchatv3mp.settings.presentation.appearancesettings.AppearanceSettings
import org.lerchenflo.schneaggchatv3mp.settings.presentation.devsettings.DeveloperSettings
import org.lerchenflo.schneaggchatv3mp.settings.presentation.miscSettings.MiscSettings
import org.lerchenflo.schneaggchatv3mp.settings.presentation.usersettings.UserSettings
import org.lerchenflo.schneaggchatv3mp.sharedUi.clearFocusOnTap
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.AutoFadePopup
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.OfflineBar
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.SnackbarPopup
import org.lerchenflo.schneaggchatv3mp.utilities.LanguageService
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import org.lerchenflo.schneaggchatv3mp.utilities.UiText
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.error_access_not_permitted
import kotlin.time.Duration.Companion.milliseconds


@Composable
@Preview(showBackground = true)
fun App() {
    val preferenceManager = koinInject<Preferencemanager>()
    val languageService = koinInject<LanguageService>()
    val themeSetting by preferenceManager.getThemeFlow().collectAsState(initial = ThemeSetting.SYSTEM)

    val tokenManager = koinInject<TokenManager>()
    val loggingRepository = koinInject<LoggingRepository>()

    val globalViewModel = koinInject<GlobalViewModel>() //Init instantly for flow collections to start correctly


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


        //Initialize global repository
        val appRepository = koinInject<AppRepository>()

        val navigationState = rememberNavigationState(
            startRoute = Route.AutoLoginCredChecker,
            topLevelRoutes = TOP_LEVEL_DESTINATIONS.keys
        )
        val navigator = remember {
            Navigator(navigationState)
        }


        //Init snackbarmanager
        LaunchedEffect(Unit) {
            SnackbarManager.init(scope)
        }


        ObserveAsEvents(
            flow = AppRepository.ActionChannel.actions,
        ) { action ->
            scope.launch {
                when (action) {
                    AppRepository.ActionChannel.ActionEvent.Login -> {
                        // Login action handled automatically by HTTP client refresh
                        //if (rootBackStack.contains(Route.ChatSelector)){

                        //}
                        val error = tokenManager.refreshTokens(preferenceManager.getTokens().refreshToken)

                        if (error != null && !error.isConnectionError()){
                            AppRepository.ActionChannel.sendActionSuspend(AppRepository.ActionChannel.ActionEvent.AuthInvalidated)
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
                delay(error.duration.milliseconds)
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




        //Snackbar popup handling
        var currentSnackbarEvent by remember { mutableStateOf<SnackbarManager.SnackbarEvent?>(null) }
        LaunchedEffect(Unit) {
            SnackbarManager.snackbars.collect {
                currentSnackbarEvent = it
            }
        }
        currentSnackbarEvent?.let { snackbar ->
            SnackbarPopup(
                snackbarEvent = snackbar,
                onDismiss = { currentSnackbarEvent = null }
            )
        }




        val tour = rememberOnboardingTour(appRepository.appVersion.isAndroid(), appRepository.appVersion.isDesktop())

        val tourController = remember {
            TapTargetController(
                tour = tour,
                onNavigateToRoute = { targetRoute ->
                    println("Onboarding: Navigating $targetRoute")
                    navigator.navigate(targetRoute)
                },
                currentRoute = { navigationState.topLevelRoute },
                onFinished = {
                    scope.launch {
                        preferenceManager.setOnboardingSeen(true)
                    }
                },
                tourSettings = TourSettings(
                    iconPadding = 12.dp
                )
            )
        }

        CompositionLocalProvider(LocalTapTargetController provides tourController) {

            Box() {
                Scaffold(
                    modifier = Modifier
                        .clearFocusOnTap()
                        .imePadding(),
                    bottomBar = {

                        BottomAppBarSwipable(
                            selectedKey = navigationState.topLevelRoute,
                            onSelectKey = {
                                navigator.navigate(
                                    route = it,
                                )
                            },
                            mobile = appRepository.appVersion.isMobile(),
                            modifier = Modifier.fillMaxWidth()
                        )

                    }

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


                        NavDisplay(
                            entries = navigationState.toEntries(
                                entryProvider = entryProvider {

                                    //Authentication
                                    entry<Route.AutoLoginCredChecker> {
                                        AutoLoginCredCheckerRoot()
                                    }

                                    entry<Route.Login> {
                                        LoginScreen()
                                    }

                                    entry<Route.SignUp> {
                                        SignUpScreenRoot()
                                    }

                                    entry<Route.EmailVerifiedCheck> {
                                        EmailVerifiedCheckScreenRoot()
                                    }



                                    //Chat
                                    entry<Route.ChatSelector> {
                                        Chatauswahlscreen()
                                    }

                                    entry<Route.Chat> { route ->
                                        ChatScreen(
                                            chatId = route.chatId,
                                            isGroup = route.isGroup,
                                            highlightMessageId = route.highlightMessageId
                                        )
                                    }
                                    entry<Route.ChatDetails> { route ->
                                        ChatDetails(
                                            chatId = route.chatId,
                                            isGroup = route.isGroup
                                        )
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

                                    entry<Route.Schneaggmap> { route ->
                                        SchneaggmapScreenRoot(
                                            initialEntryId = route.initialEntryId
                                        )
                                    }



                                    //Settings
                                    entry<Route.Settings.SettingsScreen> {
                                        SettingsScreen(
                                            settingsViewmodel = koinInject(),
                                            sharedSettingsViewmodel = koinInject(), // see note below on scoping
                                            onBackClick = {
                                                scope.launch { navigator.navigateBack() }
                                            },
                                            navigateUserSettings = { navigator.navigate(Route.Settings.UserSettings) },
                                            navigateDevSettings = { navigator.navigate(Route.Settings.DeveloperSettings) },
                                            navigateAppearanceSettings = { navigator.navigate(Route.Settings.AppearanceSettings) },
                                            navigateMiscSettings = { navigator.navigate(Route.Settings.MiscSettings) },
                                            navigateSchneaggmapSettings = { navigator.navigate(Route.Settings.SchneaggmapSettings) }
                                        )
                                    }

                                    entry<Route.Settings.DeveloperSettings> {
                                        DeveloperSettings(
                                            devSettingsViewModel = koinInject(),
                                            sharedSettingsViewmodel = koinInject(),
                                            onBackClick = { scope.launch { navigator.navigateBack() } }
                                        )
                                    }

                                    entry<Route.Settings.UserSettings> {
                                        UserSettings(
                                            userSettingsViewModel = koinInject(),
                                            sharedSettingsViewmodel = koinInject(),
                                            onBackClick = { scope.launch { navigator.navigateBack() } }
                                        )
                                    }

                                    entry<Route.Settings.AppearanceSettings> {
                                        AppearanceSettings(
                                            appearanceSettingsViewModel = koinInject(),
                                            sharedSettingsViewmodel = koinInject(),
                                            onBackClick = { scope.launch { navigator.navigateBack() } }
                                        )
                                    }

                                    entry<Route.Settings.MiscSettings> {
                                        MiscSettings(
                                            miscSettingsViewModel = koinInject(),
                                            sharedSettingsViewmodel = koinInject(),
                                            onBackClick = { scope.launch { navigator.navigateBack() } },
                                            navigateRoadmap = { navigator.navigate(Route.Settings.Roadmap) }
                                        )
                                    }

                                    entry<Route.Settings.Roadmap> {
                                        RoadmapScreen(
                                            roadmapViewModel = koinInject(),
                                            onBackClick = { scope.launch { navigator.navigateBack() } }
                                        )
                                    }




                                    entry<Route.Games.GamesSelector> {
                                        val gameSelectorViewModel = koinViewModel<GameSelectorViewModel>()
                                        GameSelectorScreen(
                                            onBackClick = {
                                                scope.launch { navigator.navigateBack() }
                                            },
                                            onGameSelection = {
                                                navigator.navigate(it)
                                            },
                                            viewModel = gameSelectorViewModel
                                        )
                                    }

                                    entry<Route.Games.DartCounter> {
                                        DartCounter(
                                            onBackClick = { scope.launch { navigator.navigateBack() } }
                                        )
                                    }

                                    entry<Route.Games.Undercover> {
                                        Undercover(
                                            onBackClick = { scope.launch { navigator.navigateBack() } }
                                        )
                                    }

                                    entry<Route.Games.TowerStack> {
                                        TowerStackScreen(
                                            onBackClick = { scope.launch { navigator.navigateBack() } }
                                        )
                                    }

                                    entry<Route.Games.Yatzi> {
                                        YatziScreenRoot(
                                            onBackClick = { scope.launch { navigator.navigateBack() } }
                                        )
                                    }

                                    entry<Route.Games.Tetris> {
                                        val tetrisViewModel: TetrisViewModel = koinViewModel<TetrisViewModel>()
                                        TetrisScreen(
                                            onBackClick = { scope.launch { navigator.navigateBack() } },
                                            viewModel = tetrisViewModel
                                        )
                                    }

                                    entry<Route.Games.Morse> {
                                        val morseViewModel: MorseViewModel = koinViewModel()
                                        MorseScreen(
                                            onBackClick = { scope.launch { navigator.navigateBack() } },
                                            viewModel = morseViewModel
                                        )
                                    }

                                    entry<Route.Games.SchneaggaHus> {
                                        SchneaggaHusScreenRoot(
                                            onBackClick = { scope.launch { navigator.navigateBack() } }
                                        )
                                    }

                                    entry<Route.Games.GridRush> {
                                        GridRushScreenRoot(
                                            onBackClick = { scope.launch { navigator.navigateBack() } }
                                        )
                                    }

                                    entry<Route.Games.OddOneOut> {
                                        OddOneOutScreenRoot(
                                            onBackClick = { scope.launch { navigator.navigateBack() } }
                                        )
                                    }

                                    entry<Route.Games.Recap> {
                                        RecapScreenRoot(
                                            onBackClick = { scope.launch { navigator.navigateBack() } }
                                        )
                                    }

                                    entry<Route.Games.CoinFlip> {
                                        CoinFlipScreen(
                                            onBackClick = { scope.launch { navigator.navigateBack() } }
                                        )
                                    }

                                    entry<Route.Games.FingerPicker> {
                                        FingerPickerScreen(
                                            onBackClick = { scope.launch { navigator.navigateBack() } }
                                        )
                                    }

                                    entry<Route.Games.Game2048> {
                                        Game2048ScreenRoot(
                                            onBackClick = { scope.launch { navigator.navigateBack() } }
                                        )
                                    }
                                }
                            ),
                            onBack = navigator::navigateBack
                        )


                    }
                }

                TapTargetOverlay(tourController)

            }
        }




    }
}