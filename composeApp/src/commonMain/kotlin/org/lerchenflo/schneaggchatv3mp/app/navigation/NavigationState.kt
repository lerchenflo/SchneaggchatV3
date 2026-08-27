package org.lerchenflo.schneaggchatv3mp.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.savedstate.compose.serialization.serializers.MutableStateSerializer
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlin.reflect.KClass

class NavigationState(
    val homeRoute: NavKey,
    topLevelRoute: MutableState<NavKey>,
    val backStacks: Map<NavKey, NavBackStack<NavKey>>
) {
    var topLevelRoute by topLevelRoute //Make mutablestate observable

    // The pager gives every tab its own NavDisplay, and a NavDisplay only handles system back
    // while its own stack has more than one entry. At a non-home tab root nothing handles back,
    // so App.kt installs a BackHandler on this condition to return to the home tab.
    val backExitsToHome: Boolean
        get() = topLevelRoute != homeRoute && (backStacks[topLevelRoute]?.size ?: 1) <= 1

    // True at the chat selector itself (home tab, nothing pushed on top) - no NavDisplay
    // handles back here either, so App.kt uses this to arm a "press back again to exit" prompt
    // instead of closing immediately.
    val atHomeRoot: Boolean
        get() = topLevelRoute == homeRoute && (backStacks[homeRoute]?.size ?: 1) <= 1

    val currentRoute: NavKey
        get() = backStacks[topLevelRoute]?.lastOrNull() ?: topLevelRoute

    val showNavBar: Boolean
        get() = currentRoute::class !in hiddenNavBarRoutes

    val enableSwipeNavigation: Boolean
        get() {
            return showNavBar && (backStacks[topLevelRoute]?.size ?: 1) <= 1 && currentRoute::class != Route.Schneaggmap::class
        }
}


@Composable
fun rememberNavigationState(
    startRoute: NavKey,
    homeRoute: NavKey,
    topLevelRoutes: Set<NavKey>
): NavigationState {
    val topLevelRoute = rememberSerializable(
        homeRoute, topLevelRoutes,
        configuration = backStackConfiguration,
        serializer = MutableStateSerializer(PolymorphicSerializer(NavKey::class))
    ) {
        mutableStateOf(homeRoute)
    }

    val backStacks = topLevelRoutes.associateWith { key ->
        // The home tab's backstack starts with startRoute (e.g. AutoLoginCredChecker)
        // so it is shown first. All other tab backstacks start with their own key as usual.
        val initialEntry = if (key == homeRoute) startRoute else key
        rememberNavBackStack(
            configuration = backStackConfiguration,
            initialEntry
        )
    }

    return remember(startRoute, homeRoute, topLevelRoute) {
        NavigationState(
            homeRoute = homeRoute,
            topLevelRoute = topLevelRoute,
            backStacks = backStacks
        )
    }

}

/**
 * Closes a top-level tab: drops its backstack and re-adds the tab route with all-default
 * arguments. Used when `exitPreviousScreen` leaves a tab whose root carried flow-specific args
 * (the map's `currentlyEditedEvent`, the events tab's `selectedEvent`, ...) - the fresh NavKey
 * gives the tab a new NavEntry, so the screen and its ViewModel are recreated on the next visit
 * instead of resurrecting the old state.
 *
 * Skipped when the root is a different route class than the tab key - the home tab legitimately
 * starts on the auth flow (`AutoLoginCredChecker`), which must not be rewritten.
 */
fun NavigationState.resetTabRoot(tabKey: NavKey) {
    val stack = backStacks[tabKey] ?: return
    val root = stack.firstOrNull() ?: return
    if (root::class != tabKey::class) return
    // Already at the bare tab root - removing and re-adding an equal key would destroy and rebuild
    // the NavEntry (new ViewModelStore, and any ModalBottomSheet it hosts torn down mid-flight)
    // for no state change at all.
    if (stack.size == 1 && root == tabKey) return
    while (stack.size > 1) stack.removeAt(stack.size - 1)
    // Replace in place rather than remove-then-re-add, so the entry is swapped in a single step.
    // Tab keys are the all-default instances: Route.Schneaggmap(), Route.Events().
    if (root != tabKey) stack[0] = tabKey
}

@Composable
fun NavigationState.decoratedEntriesMap(
    entryProvider: (NavKey) -> NavEntry<NavKey>
): Map<NavKey, List<NavEntry<NavKey>>> {
    return backStacks.mapValues { (_, stack) ->
        val decorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
            rememberViewModelStoreNavEntryDecorator()
        )
        rememberDecoratedNavEntries(
            backStack = stack,
            entryDecorators = decorators,
            entryProvider = entryProvider
        )
    }
}


private val authFlowRoutes: Set<KClass<out NavKey>> = setOf(
    Route.AutoLoginCredChecker::class,
    Route.Login::class,
    Route.SignUp::class,
    Route.EmailVerifiedCheck::class
)

private val gameRoutes: Set<KClass<out NavKey>> = setOf(
    Route.DartCounter::class,
    Route.Undercover::class,
    Route.TowerStack::class,
    Route.Yatzi::class,
    Route.Tetris::class,
    Route.Morse::class,
    Route.SchneaggaHus::class,
    Route.GridRush::class,
    Route.OddOneOut::class,
    Route.Recap::class,
    Route.CoinFlip::class,
    Route.FingerPicker::class,
    Route.Game2048::class,
    Route.Stanislaus::class
)

private val chatRoutes: Set<KClass<out NavKey>> = setOf(
    Route.Chat::class,
    Route.ChatDetails::class,
    Route.NewChat::class,
    Route.MessageChatSelector::class,
    Route.GroupCreator::class
)

private val hiddenNavBarRoutes: Set<KClass<out NavKey>> = authFlowRoutes + gameRoutes + chatRoutes

val backStackConfiguration = SavedStateConfiguration {
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
            subclass(Route.EmailVerifiedCheck::class, Route.EmailVerifiedCheck.serializer())
            subclass(Route.ChatDetails::class, Route.ChatDetails.serializer())
            subclass(Route.Schneaggmap::class, Route.Schneaggmap.serializer())
            subclass(Route.Events::class, Route.Events.serializer())


            //Games
            subclass(Route.GamesSelector::class, Route.GamesSelector.serializer())
            subclass(Route.DartCounter::class, Route.DartCounter.serializer())
            subclass(Route.Undercover::class, Route.Undercover.serializer())
            subclass(Route.TowerStack::class, Route.TowerStack.serializer())
            subclass(Route.Yatzi::class, Route.Yatzi.serializer())
            subclass(Route.Tetris::class, Route.Tetris.serializer())
            subclass(Route.Morse::class, Route.Morse.serializer())
            subclass(Route.SchneaggaHus::class, Route.SchneaggaHus.serializer())
            subclass(Route.GridRush::class, Route.GridRush.serializer())
            subclass(Route.OddOneOut::class, Route.OddOneOut.serializer())
            subclass(Route.Recap::class, Route.Recap.serializer())
            subclass(Route.CoinFlip::class, Route.CoinFlip.serializer())
            subclass(Route.FingerPicker::class, Route.FingerPicker.serializer())
            subclass(Route.Game2048::class, Route.Game2048.serializer())
            subclass(Route.Stanislaus::class, Route.Stanislaus.serializer())


            //Settings
            subclass(Route.SettingsScreen::class, Route.SettingsScreen.serializer())
            subclass(Route.DeveloperSettings::class, Route.DeveloperSettings.serializer())
            subclass(Route.UserSettings::class, Route.UserSettings.serializer())
            subclass(Route.PrivacyAndSecuritySettings::class, Route.PrivacyAndSecuritySettings.serializer())
            subclass(Route.NotificationSettings::class, Route.NotificationSettings.serializer())
            subclass(Route.AppearanceSettings::class, Route.AppearanceSettings.serializer())
            subclass(Route.MiscSettings::class, Route.MiscSettings.serializer())
            subclass(Route.SchneaggmapSettings::class, Route.SchneaggmapSettings.serializer())
            subclass(Route.Roadmap::class, Route.Roadmap.serializer())
        }
    }
}