package org.lerchenflo.schneaggchatv3mp.app.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface Route : NavKey {

    // Chat - Feature
    @Serializable
    data object ChatSelector: Route
    @Serializable
    data object MessageChatSelector: Route
    @Serializable
    data class Chat(val chatId: String, val isGroup: Boolean, val highlightMessageId: String? = null): Route
    @Serializable
    data object NewChat: Route
    @Serializable
    data object GroupCreator: Route
    @Serializable
    data object AutoLoginCredChecker: Route

    @Serializable
    data object Login: Route
    @Serializable
    data object SignUp: Route
    @Serializable
    data object EmailVerifiedCheck: Route


    @Serializable
    data class ChatDetails(val chatId: String, val isGroup: Boolean): Route

    @Serializable
    data class Schneaggmap(val initialEntryId: String? = null): Route


    @Serializable
    data object Settings: Route {
        @Serializable
        data object SettingsScreen: Route

        @Serializable
        data object DeveloperSettings: Route

        @Serializable
        data object UserSettings: Route

        @Serializable
        data object AppearanceSettings: Route

        @Serializable
        data object MiscSettings: Route

        @Serializable
        data object SchneaggmapSettings: Route

        @Serializable
        data object Roadmap: Route
    }


    @Serializable
    data object Games: Route {
        @Serializable
        data object GamesSelector: Route

        @Serializable
        data object DartCounter: Route

        @Serializable
        data object Undercover: Route

        @Serializable
        data object TowerStack: Route

        @Serializable
        data object Yatzi: Route

        @Serializable
        data object Tetris: Route

        @Serializable
        data object Morse: Route

        @Serializable
        data object SchneaggaHus: Route

        @Serializable
        data object GridRush: Route

        @Serializable
        data object OddOneOut: Route

        @Serializable
        data object Recap: Route

        @Serializable
        data object CoinFlip: Route

        @Serializable
        data object FingerPicker: Route
    }
}


fun Route.parentRootOrNull(): Route? = when (this) {
    is Route.Settings.SettingsScreen,
    is Route.Settings.DeveloperSettings,
    is Route.Settings.UserSettings,
    is Route.Settings.AppearanceSettings,
    is Route.Settings.MiscSettings,
    is Route.Settings.SchneaggmapSettings,
    is Route.Settings.Roadmap -> Route.Settings

    is Route.Games.GamesSelector,
    is Route.Games.DartCounter,
    is Route.Games.Undercover,
    is Route.Games.TowerStack,
    is Route.Games.Yatzi,
    is Route.Games.Tetris,
    is Route.Games.Morse,
    is Route.Games.SchneaggaHus,
    is Route.Games.GridRush,
    is Route.Games.OddOneOut,
    is Route.Games.Recap,
    is Route.Games.CoinFlip,
    is Route.Games.FingerPicker -> Route.Games

    else -> null
}

