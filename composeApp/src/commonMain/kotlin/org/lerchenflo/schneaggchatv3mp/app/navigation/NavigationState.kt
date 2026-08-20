package org.lerchenflo.schneaggchatv3mp.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
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

    val stacksInUse: List<NavKey>
        //When navigating back always go to home destination
        get() = if (topLevelRoute == homeRoute) {
            listOf(homeRoute)
        } else {
            listOf(homeRoute, topLevelRoute)
        }

    val currentRoute: NavKey
        get() = backStacks[topLevelRoute]?.lastOrNull() ?: topLevelRoute

    val showNavBar: Boolean
        get() = currentRoute::class !in hiddenNavBarRoutes
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

@Composable
fun NavigationState.toEntries(
    entryProvider: (NavKey) -> NavEntry<NavKey>
): SnapshotStateList<NavEntry<NavKey>> {
    val decoratedEntries = backStacks.mapValues { (_, stack) ->
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

    return stacksInUse
        .flatMap { decoratedEntries[it] ?: emptyList() }
        .toMutableStateList()

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
    Route.Game2048::class
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


            //Settings
            subclass(Route.SettingsScreen::class, Route.SettingsScreen.serializer())
            subclass(Route.DeveloperSettings::class, Route.DeveloperSettings.serializer())
            subclass(Route.UserSettings::class, Route.UserSettings.serializer())
            subclass(Route.PrivacyAndSecuritySettings::class, Route.PrivacyAndSecuritySettings.serializer())
            subclass(Route.AppearanceSettings::class, Route.AppearanceSettings.serializer())
            subclass(Route.MiscSettings::class, Route.MiscSettings.serializer())
            subclass(Route.SchneaggmapSettings::class, Route.SchneaggmapSettings.serializer())
            subclass(Route.Roadmap::class, Route.Roadmap.serializer())
        }
    }
}