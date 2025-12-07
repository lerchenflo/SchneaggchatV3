package org.lerchenflo.schneaggchatv3mp.app.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface Route : NavKey {

    // Chat - Feature
    @Serializable
    data object ChatSelector: Route, NavKey
    @Serializable
    data object Chat: Route, NavKey
    @Serializable
    data object NewChat: Route, NavKey
    @Serializable
    data object GroupCreator: Route, NavKey
    @Serializable
    data object AutoLoginCredChecker: Route, NavKey

    @Serializable
    data object Login: Route, NavKey
    @Serializable
    data object SignUp: Route, NavKey


    @Serializable
    data object ChatDetails: Route, NavKey

    @Serializable
    data object Todolist: Route, NavKey

    @Serializable
    data object UnderConstruction: Route, NavKey


    @Serializable
    data object Settings: Route, NavKey {
        @Serializable
        data object SettingsScreen: Route, NavKey

        @Serializable
        data object DeveloperSettings: Route, NavKey

        @Serializable
        data object UserSettings: Route, NavKey
    }
}

