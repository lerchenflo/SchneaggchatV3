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
import kotlinx.serialization.serializer

class NavigationState(
    val startRoute: NavKey,
    topLevelRoute: MutableState<NavKey>,
    val backStacks: Map<NavKey, NavBackStack<NavKey>>
) {
    var topLevelRoute by topLevelRoute //Make mutablestate observable

    val stacksInUse: List<NavKey>
        //When navigating back always go to home destination
        get() = if (topLevelRoute == startRoute) {
            listOf(startRoute)
        } else {
            listOf(startRoute, topLevelRoute)
        }
}


@Composable
fun rememberNavigationState(
    startRoute: NavKey,
    topLevelRoutes: Set<NavKey>
): NavigationState {
    val topLevelRoute = rememberSerializable(
        startRoute, topLevelRoutes,
        configuration = backStackConfiguration,
        serializer = MutableStateSerializer(PolymorphicSerializer(NavKey::class))
    ) {
        mutableStateOf(startRoute)
    }

    val backStacks = topLevelRoutes.associateWith { key ->
        rememberNavBackStack(
            configuration = backStackConfiguration,
            key
        )
    }

    return remember(startRoute, topLevelRoute) {
        NavigationState(
            startRoute = startRoute,
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


            //Games
            subclass(Route.Games.GamesSelector::class, Route.Games.GamesSelector.serializer())
            subclass(Route.Games.DartCounter::class, Route.Games.DartCounter.serializer())
            subclass(Route.Games.Undercover::class, Route.Games.Undercover.serializer())
            subclass(Route.Games.TowerStack::class, Route.Games.TowerStack.serializer())
            subclass(Route.Games.Yatzi::class, Route.Games.Yatzi.serializer())
            subclass(Route.Games.Tetris::class, Route.Games.Tetris.serializer())
            subclass(Route.Games.Morse::class, Route.Games.Morse.serializer())
            subclass(Route.Games.SchneaggaHus::class, Route.Games.SchneaggaHus.serializer())
            subclass(Route.Games.GridRush::class, Route.Games.GridRush.serializer())
            subclass(Route.Games.OddOneOut::class, Route.Games.OddOneOut.serializer())
            subclass(Route.Games.Recap::class, Route.Games.Recap.serializer())
            subclass(Route.Games.CoinFlip::class, Route.Games.CoinFlip.serializer())
            subclass(Route.Games.FingerPicker::class, Route.Games.FingerPicker.serializer())
            subclass(Route.Games.Game2048::class, Route.Games.Game2048.serializer())


            //Settings
            subclass(Route.Settings.SettingsScreen::class, Route.Settings.SettingsScreen.serializer())
            subclass(
                Route.Settings.DeveloperSettings::class,
                Route.Settings.DeveloperSettings.serializer()
            )
            subclass(Route.Settings.UserSettings::class, Route.Settings.UserSettings.serializer())
            subclass(
                Route.Settings.AppearanceSettings::class,
                Route.Settings.AppearanceSettings.serializer()
            )
            subclass(Route.Settings.MiscSettings::class, Route.Settings.MiscSettings.serializer())
            subclass(
                Route.Settings.SchneaggmapSettings::class,
                Route.Settings.SchneaggmapSettings.serializer()
            )
            subclass(Route.Settings.Roadmap::class, Route.Settings.Roadmap.serializer())
        }
    }
}