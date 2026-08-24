package org.lerchenflo.schneaggchatv3mp.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.app.logging.LoggingRepository
import org.lerchenflo.schneaggchatv3mp.app.navigation.BottomAppBarSwipable
import org.lerchenflo.schneaggchatv3mp.app.navigation.NavigationAction
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.ObserveAsEvents
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.app.navigation.TOP_LEVEL_DESTINATIONS
import org.lerchenflo.schneaggchatv3mp.app.navigation.decoratedEntriesMap
import org.lerchenflo.schneaggchatv3mp.app.navigation.getVisibleTopLevelDestinations
import org.lerchenflo.schneaggchatv3mp.app.navigation.rememberNavigationState
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
import org.lerchenflo.schneaggchatv3mp.datasource.network.RefreshResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.TokenManager
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
import org.lerchenflo.schneaggchatv3mp.games.presentation.stanislaus.StanislausScreenRoot
import org.lerchenflo.schneaggchatv3mp.games.presentation.undercover.Undercover
import org.lerchenflo.schneaggchatv3mp.games.presentation.yatzi.YatziScreenRoot
import org.lerchenflo.schneaggchatv3mp.login.presentation.autologincredchecker.AutoLoginCredCheckerRoot
import org.lerchenflo.schneaggchatv3mp.login.presentation.emailverifiedcheck.EmailVerifiedCheckScreenRoot
import org.lerchenflo.schneaggchatv3mp.login.presentation.login.LoginScreen
import org.lerchenflo.schneaggchatv3mp.login.presentation.signup.SignUpScreenRoot
import org.lerchenflo.schneaggchatv3mp.events.presentation.EventsRoot
import org.lerchenflo.schneaggchatv3mp.roadmap.presentation.RoadmapScreen
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.SchneaggmapScreenRoot
import org.lerchenflo.schneaggchatv3mp.settings.presentation.SettingsScreen
import org.lerchenflo.schneaggchatv3mp.settings.presentation.appearancesettings.AppearanceSettings
import org.lerchenflo.schneaggchatv3mp.settings.presentation.devsettings.DeveloperSettings
import org.lerchenflo.schneaggchatv3mp.settings.presentation.miscSettings.MiscSettings
import org.lerchenflo.schneaggchatv3mp.settings.presentation.notificationsettings.NotificationSettings
import org.lerchenflo.schneaggchatv3mp.settings.presentation.privacyandsecurity.PrivacyAndSecuritySettings
import org.lerchenflo.schneaggchatv3mp.settings.presentation.schneaggmapsettings.SchneaggmapSettings
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
import schneaggchatv3mp.composeapp.generated.resources.press_back_again_to_exit
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

    val isDeveloper by preferenceManager.getDevSettingsFlow().collectAsStateWithLifecycle(false)

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
            homeRoute = Route.ChatSelector,
            topLevelRoutes = TOP_LEVEL_DESTINATIONS.keys
        )

        val isMobile = remember { appRepository.appVersion.isMobile() }
        val visibleTopLevelRoutes = remember(isMobile, isDeveloper, navigationState.topLevelRoute) {
            getVisibleTopLevelDestinations(
                selectedKey = navigationState.topLevelRoute,
                mobile = isMobile,
                developer = isDeveloper
            )
        }

        val initialPageIndex = remember {
            val idx = visibleTopLevelRoutes.indexOfFirst { it == navigationState.topLevelRoute || it::class == navigationState.topLevelRoute::class }
            if (idx >= 0) {
                idx
            } else {
                // Selected route isn't in the visible list (e.g. a restored route the current
                // filter excludes) - fall back to the home tab's index, never a bare 0. Page 0 on
                // mobile is Schneaggmap, so a naive 0 fallback would silently land on the map.
                val homeIdx = visibleTopLevelRoutes.indexOfFirst { it == navigationState.homeRoute || it::class == navigationState.homeRoute::class }
                if (homeIdx >= 0) homeIdx else 0
            }
        }

        val pagerState = rememberPagerState(
            initialPage = initialPageIndex,
            pageCount = { visibleTopLevelRoutes.size }
        )

        val currentVisibleRoutes by rememberUpdatedState(visibleTopLevelRoutes)
        var isProgrammaticScroll by remember { mutableStateOf(false) }
        // True only while a topLevelRoute change originates from the pager's own settle.
        // Leaving a showOnlyWhenSelected tab (Events/Settings) shrinks the visible page list and
        // shifts every later index by one, so the page the user is already looking at needs a pure
        // index resync — animating that would flash the wrong page then slide back into place.
        var pendingSwipeReindex by remember { mutableStateOf(false) }

        // Route this effect last resynced the pager against - lets it tell "the selected route
        // itself changed" (animate) apart from "the visible list reshuffled under the same route"
        // (e.g. isDeveloper flipping true adds Events/Settings and shifts every later index -
        // silent resync only, animating would flash the wrong page).
        var lastSyncedRoute by remember { mutableStateOf(navigationState.topLevelRoute) }

        // Keep the pager's current page in sync whenever the selected route OR the set of
        // visible tabs changes - either one can make the current page index point at the wrong
        // route.
        LaunchedEffect(navigationState.topLevelRoute, visibleTopLevelRoutes) {
            val routeChanged = navigationState.topLevelRoute != lastSyncedRoute &&
                navigationState.topLevelRoute::class != lastSyncedRoute::class
            lastSyncedRoute = navigationState.topLevelRoute

            val targetIndex = currentVisibleRoutes.indexOfFirst {
                it == navigationState.topLevelRoute || it::class == navigationState.topLevelRoute::class
            }
            if (targetIndex >= 0 && targetIndex != pagerState.currentPage) {
                isProgrammaticScroll = true
                try {
                    if (pendingSwipeReindex || !routeChanged) {
                        pagerState.scrollToPage(targetIndex) // silent resync, no animation
                    } else {
                        pagerState.animateScrollToPage(targetIndex) // tap / programmatic nav
                    }
                } finally {
                    isProgrammaticScroll = false
                    pendingSwipeReindex = false
                }
            }
        }

        // When pager settles on a new page from user swiping, update topLevelRoute
        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.settledPage }
                .collect { settledPage ->
                    // isScrollInProgress guards against a settle emission caused by the visible
                    // list reshuffling under the current page (not an actual user swipe) being
                    // misread as a tab change.
                    if (!isProgrammaticScroll && !pagerState.isScrollInProgress) {
                        val targetRoute = currentVisibleRoutes.getOrNull(settledPage)
                        if (targetRoute != null && targetRoute != navigationState.topLevelRoute && targetRoute::class != navigationState.topLevelRoute::class) {
                            pendingSwipeReindex = true
                            navigationState.topLevelRoute = targetRoute
                        }
                    }
                }
        }

        // Koin-injected Navigator singleton used by ViewModels — its channel events are
        // translated into NavigationState mutations below
        val navigator = koinInject<Navigator>()

        // A tab-root NavDisplay leaves system back unhandled (its stack has a single entry), so the
        // press would otherwise close the app instead of returning to the chat selector.
        NavigationBackHandler(
            state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
            isBackEnabled = navigationState.backExitsToHome,
            onBackCompleted = { scope.launch { navigator.navigateBack() } }
        )

        // At the chat selector itself, first back press shows a "press again to exit" prompt and
        // disarms this handler; the following back press then has nothing left to intercept it and
        // falls through to the platform's normal back behaviour, which closes the app.
        var awaitingExitConfirmation by remember { mutableStateOf(false) }
        NavigationBackHandler(
            state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
            isBackEnabled = navigationState.atHomeRoot && !awaitingExitConfirmation,
            onBackCompleted = {
                awaitingExitConfirmation = true
                scope.launch {
                    SnackbarManager.showMessage(getString(Res.string.press_back_again_to_exit))
                    delay(2000.milliseconds)
                    awaitingExitConfirmation = false
                }
            }
        )

        //Observe what the navigator sends to change screens etc
        ObserveAsEvents(
            flow = navigator.navigationActions
        ) { action ->
            val navigationOptions = action.navigationOptions

            if (navigationOptions.exitPreviousScreen) {
                navigationState.backStacks[navigationState.topLevelRoute]?.let { stack ->
                    if (stack.size > 1) stack.removeAt(stack.size - 1)
                }
            }

            //Remove all routes of the given types from the backstack (class-based so routes with
            //arguments match regardless of their argument values)
            if (navigationOptions.removeAllScreensByClass.isNotEmpty()) {
                navigationOptions.removeAllScreensByClass.forEach { routeClass ->
                    navigationState.backStacks.values.forEach { stack ->
                        stack.removeAll { navKey -> navKey::class == routeClass }
                    }
                }
            }

            if (navigationOptions.removeAllExceptByRoute != null) {
                navigationState.backStacks[navigationState.topLevelRoute]?.removeAll { navKey ->
                    navKey != navigationOptions.removeAllExceptByRoute
                }
            }

            when (action) {
                is NavigationAction.Navigate -> {

                    //Navigate
                    val destination = action.destination
                    val topLevelMatchKey = navigationState.backStacks.keys.firstOrNull { it == destination || it::class == destination::class }

                    // Top-level or flat route — navigate within current tab or switch tab
                    if (topLevelMatchKey != null) {
                        // It's a top-level tab key: reset that tab's backstack if requested,
                        // then switch to it. Always ensure at least the tab root is present.
                        val stack = navigationState.backStacks[topLevelMatchKey]
                        if (stack != null && (navigationOptions.exitAllPreviousScreens || destination == Route.SettingsScreen || destination::class == Route.SettingsScreen::class)) { //Remove backstack for settings
                            stack.clear()
                            stack.add(destination) // tab root must always be present
                        } else if (stack != null && destination != topLevelMatchKey) {
                            stack.clear()
                            stack.add(destination)
                        }
                        navigationState.topLevelRoute = topLevelMatchKey
                    } else {
                        // Flat sub-route on current tab's backstack
                        val stack = navigationState.backStacks[navigationState.topLevelRoute]
                        if (stack != null) {
                            if (navigationOptions.exitAllPreviousScreens) {
                                val tabRoot = navigationState.topLevelRoute
                                stack.clear()
                                stack.add(tabRoot) // keep tab root so the stack is never empty
                            }
                            val existingIndex = stack.indexOfFirst { it::class == destination::class }
                            if (existingIndex >= 0) {
                                while (stack.size > existingIndex) stack.removeAt(stack.size - 1)
                            }
                            stack.add(destination)
                        }
                    }
                }

                is NavigationAction.NavigateBack -> {
                    val stack = navigationState.backStacks[navigationState.topLevelRoute]
                    if (stack != null && stack.size > 1 && !navigationOptions.exitRootWithSubRoute) {
                        stack.removeAt(stack.size - 1)
                    } else {
                        navigationState.topLevelRoute = navigationState.homeRoute
                    }
                }
            }
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
                        val refreshToken = preferenceManager.getTokens().refreshToken
                        if (refreshToken.isNotBlank()) {
                            when (tokenManager.refreshTokens(refreshToken)) {
                                RefreshResult.Invalidated -> {
                                    AppRepository.ActionChannel.sendActionSuspend(AppRepository.ActionChannel.ActionEvent.AuthInvalidated)
                                }
                                RefreshResult.Success, is RefreshResult.Retryable -> {
                                    // Retryable failures (offline, rate limited, server error) are not
                                    // an invalidated session - do nothing, the next attempt may succeed.
                                }
                            }
                        }
                        // Blank token: no session exists (fresh install, or a logout raced this
                        // action) - "session invalidated" toast + forced navigation would be
                        // wrong here; the logout flow already brings the user to the login screen.
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
                    val activeTab = navigationState.topLevelRoute
                    val activeStack = navigationState.backStacks[activeTab]

                    // If already on the correct tab and the target is that tab's root screen,
                    // just reset the sub-backstack instead of pushing a duplicate.
                    if (targetRoute == activeStack?.firstOrNull()) {
                        while ((activeStack.size) > 1) {
                            activeStack.removeAt(activeStack.size - 1)
                        }
                    } else {
                        navigator.navigate(targetRoute as Route)
                    }
                },
                currentRoute = {
                    // Return the last visible entry in the active tab's backstack —
                    // this is what tour step `route` values are compared against.
                    val activeTab = navigationState.topLevelRoute
                    val activeRoute = navigationState.backStacks[activeTab]?.lastOrNull() as? Route
                        ?: activeTab as? Route
                    println("Onboarding: Currentroute $activeRoute")
                    activeRoute
                },
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

                        if (navigationState.showNavBar) {
                            BottomAppBarSwipable(
                                selectedKey = navigationState.topLevelRoute,
                                onSelectKey = {
                                    scope.launch {
                                        navigator.navigate(it)
                                    }
                                },
                                mobile = appRepository.appVersion.isMobile(),
                                developer = isDeveloper,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

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


                        val entriesMap = navigationState.decoratedEntriesMap(
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
                                    entry<Route.SettingsScreen> {
                                        SettingsScreen(
                                            settingsViewmodel = koinInject(),
                                            sharedSettingsViewmodel = koinInject(), // see note below on scoping
                                            onBackClick = {
                                                 scope.launch { navigator.navigateBack() }
                                            },
                                            navigateUserSettings = { scope.launch { navigator.navigate(Route.UserSettings) } },
                                            navigatePrivacyAndSecurity = { scope.launch { navigator.navigate(Route.PrivacyAndSecuritySettings) } },
                                            navigateNotificationSettings = { scope.launch { navigator.navigate(Route.NotificationSettings) } },
                                            navigateDevSettings = { scope.launch { navigator.navigate(Route.DeveloperSettings) } },
                                            navigateAppearanceSettings = { scope.launch { navigator.navigate(Route.AppearanceSettings) } },
                                            navigateMiscSettings = { scope.launch { navigator.navigate(Route.MiscSettings) } },
                                            navigateSchneaggmapSettings = { scope.launch { navigator.navigate(Route.SchneaggmapSettings) } }
                                        )
                                    }

                                    entry<Route.DeveloperSettings> {
                                        DeveloperSettings(
                                            devSettingsViewModel = koinInject(),
                                            sharedSettingsViewmodel = koinInject(),
                                            onBackClick = { scope.launch { navigator.navigateBack() } }
                                        )
                                    }

                                    entry<Route.UserSettings> {
                                        UserSettings(
                                            userSettingsViewModel = koinInject(),
                                            sharedSettingsViewmodel = koinInject(),
                                            onBackClick = { scope.launch { navigator.navigateBack() } }
                                        )
                                    }

                                    entry<Route.PrivacyAndSecuritySettings> {
                                        PrivacyAndSecuritySettings(
                                            viewModel = koinInject(),
                                            sharedSettingsViewmodel = koinInject(),
                                            onBackClick = { scope.launch { navigator.navigateBack() } }
                                        )
                                    }

                                    entry<Route.NotificationSettings> {
                                        NotificationSettings(
                                            viewModel = koinInject(),
                                            onBackClick = { scope.launch { navigator.navigateBack() } }
                                        )
                                    }

                                    entry<Route.AppearanceSettings> {
                                        AppearanceSettings(
                                            appearanceSettingsViewModel = koinInject(),
                                            sharedSettingsViewmodel = koinInject(),
                                            onBackClick = { scope.launch { navigator.navigateBack() } }
                                        )
                                    }

                                    entry<Route.MiscSettings> {
                                        MiscSettings(
                                            miscSettingsViewModel = koinInject(),
                                            sharedSettingsViewmodel = koinInject(),
                                            onBackClick = { scope.launch { navigator.navigateBack() } },
                                            navigateRoadmap = { scope.launch { navigator.navigate(Route.Roadmap) } }
                                        )
                                    }

                                    entry<Route.SchneaggmapSettings> {
                                        SchneaggmapSettings(
                                            schneaggmapSettingsViewModel = koinInject(),
                                            sharedSettingsViewmodel = koinInject(),
                                            onBackClick = { scope.launch { navigator.navigateBack() } }
                                        )
                                    }

                                    entry<Route.Roadmap> {
                                        RoadmapScreen(
                                            roadmapViewModel = koinInject(),
                                            onBackClick = { scope.launch { navigator.navigateBack() } }
                                        )
                                    }

                                    entry<Route.Events> {
                                        EventsRoot(initialEntryId = it.selectedEvent)
                                    }




                                    entry<Route.GamesSelector> {
                                        val gameSelectorViewModel = koinViewModel<GameSelectorViewModel>()
                                        GameSelectorScreen(
                                            onBackClick = {
                                                scope.launch { navigator.navigateBack() }
                                            },
                                            onGameSelection = {
                                                scope.launch { navigator.navigate(it) }
                                            },
                                            viewModel = gameSelectorViewModel
                                        )
                                    }

                                    entry<Route.DartCounter> {
                                        DartCounter(
                                            onBackClick = { scope.launch { navigator.navigateBack() } }
                                        )
                                    }

                                    entry<Route.Undercover> {
                                        Undercover(
                                            onBackClick = { scope.launch { navigator.navigateBack() } }
                                        )
                                    }

                                    entry<Route.TowerStack> {
                                        TowerStackScreen(
                                            onBackClick = { scope.launch { navigator.navigateBack() } }
                                        )
                                    }

                                    entry<Route.Yatzi> {
                                        YatziScreenRoot(
                                            onBackClick = { scope.launch { navigator.navigateBack() } }
                                        )
                                    }

                                    entry<Route.Tetris> {
                                        val tetrisViewModel: TetrisViewModel = koinViewModel<TetrisViewModel>()
                                        TetrisScreen(
                                            onBackClick = { scope.launch { navigator.navigateBack() } },
                                            viewModel = tetrisViewModel
                                        )
                                    }

                                    entry<Route.Morse> {
                                        val morseViewModel: MorseViewModel = koinViewModel()
                                        MorseScreen(
                                            onBackClick = { scope.launch { navigator.navigateBack() } },
                                            viewModel = morseViewModel
                                        )
                                    }

                                    entry<Route.SchneaggaHus> {
                                        SchneaggaHusScreenRoot(
                                            onBackClick = { scope.launch { navigator.navigateBack() } }
                                        )
                                    }

                                    entry<Route.GridRush> {
                                        GridRushScreenRoot(
                                            onBackClick = { scope.launch { navigator.navigateBack() } }
                                        )
                                    }

                                    entry<Route.OddOneOut> {
                                        OddOneOutScreenRoot(
                                            onBackClick = { scope.launch { navigator.navigateBack() } }
                                        )
                                    }

                                    entry<Route.Recap> {
                                        RecapScreenRoot(
                                            onBackClick = { scope.launch { navigator.navigateBack() } }
                                        )
                                    }

                                    entry<Route.CoinFlip> {
                                        CoinFlipScreen(
                                            onBackClick = { scope.launch { navigator.navigateBack() } }
                                        )
                                    }

                                    entry<Route.FingerPicker> {
                                        FingerPickerScreen(
                                            onBackClick = { scope.launch { navigator.navigateBack() } }
                                        )
                                    }

                                    entry<Route.Game2048> {
                                        Game2048ScreenRoot(
                                            onBackClick = { scope.launch { navigator.navigateBack() } }
                                        )
                                    }

                                    entry<Route.Stanislaus> {
                                        StanislausScreenRoot(
                                            onBackClick = { scope.launch { navigator.navigateBack() } }
                                        )
                                    }
                                }
                            )

                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            userScrollEnabled = navigationState.enableSwipeNavigation,
                            key = { pageIndex ->
                                visibleTopLevelRoutes.getOrNull(pageIndex)?.let { it::class.simpleName ?: it.toString() } ?: pageIndex.toString()
                            }
                        ) { pageIndex ->
                            val tabKey = visibleTopLevelRoutes.getOrNull(pageIndex)
                            val entries = entriesMap[tabKey]
                            if (!entries.isNullOrEmpty()) {
                                NavDisplay(
                                    entries = entries,
                                    onBack = { scope.launch { navigator.navigateBack() } },
                                    // HorizontalPager centers pages vertically by default; without an explicit
                                    // fillMaxSize, screens shorter than the viewport wrap-size and get centered,
                                    // showing a top gap instead of just filling down to the bottom nav bar.
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }


                    }
                }

                TapTargetOverlay(tourController)

            }
        }




    }
}