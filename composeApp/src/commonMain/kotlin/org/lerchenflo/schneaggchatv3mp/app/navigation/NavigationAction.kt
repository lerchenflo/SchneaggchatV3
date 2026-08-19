package org.lerchenflo.schneaggchatv3mp.app.navigation

import androidx.navigation3.runtime.NavKey

sealed interface NavigationAction{

    val navigationOptions: Navigator.NavigationOptions

    data class NavigateBack(
        override val navigationOptions: Navigator.NavigationOptions = Navigator.NavigationOptions()
    ): NavigationAction

    data class Navigate(
        val destination: NavKey,
        override val navigationOptions: Navigator.NavigationOptions = Navigator.NavigationOptions()
    ) : NavigationAction
}
