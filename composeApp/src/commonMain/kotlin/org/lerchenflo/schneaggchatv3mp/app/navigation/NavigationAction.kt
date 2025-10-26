package org.lerchenflo.schneaggchatv3mp.app.navigation

import androidx.navigation.NavOptionsBuilder

sealed interface NavigationAction{
    data class Navigate(
        val destination: Route,
        val navOptions: NavOptionsBuilder.() -> Unit = {}
    ) : NavigationAction

    data object NavigateBack: NavigationAction
}