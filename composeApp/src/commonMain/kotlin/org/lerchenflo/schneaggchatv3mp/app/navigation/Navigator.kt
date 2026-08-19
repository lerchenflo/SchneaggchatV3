package org.lerchenflo.schneaggchatv3mp.app.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlin.reflect.KClass

class Navigator {
    private val _navigationActions = Channel<NavigationAction>()
    val navigationActions = _navigationActions.receiveAsFlow()

    suspend fun navigate(
        destination: NavKey,
        navigationOptions: NavigationOptions = NavigationOptions()
    ) {
        _navigationActions.send(
            NavigationAction.Navigate(destination, navigationOptions)
        )
    }

    suspend fun navigateBack(navigationOptions: NavigationOptions = NavigationOptions()) {
        _navigationActions.send(NavigationAction.NavigateBack(navigationOptions))
    }


    data class NavigationOptions(
        val exitPreviousScreen: Boolean = false,
        val exitAllPreviousScreens: Boolean = false,
        val removeAllScreensByClass: List<KClass<out NavKey>> = emptyList(), //Remove all screens of these route types
        val removeAllExceptByRoute: NavKey? = null, //Remove all but this route
        val exitRootWithSubRoute: Boolean = false // when going back from a subroute, also pop the root
    )
}