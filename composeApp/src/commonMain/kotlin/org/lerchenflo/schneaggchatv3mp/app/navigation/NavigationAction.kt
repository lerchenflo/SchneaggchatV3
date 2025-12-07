package org.lerchenflo.schneaggchatv3mp.app.navigation

import androidx.navigation.NavOptionsBuilder

sealed interface NavigationAction{

    data object NavigateBack: NavigationAction

    data class Navigate(
        val destination: Route,
        val exitPreviousScreen: Boolean = false,
        val exitAllPreviousScreens: Boolean = false,
    ) : NavigationAction

    data class NavigateSettings(
        val destination: Route,
    ) : NavigationAction
}