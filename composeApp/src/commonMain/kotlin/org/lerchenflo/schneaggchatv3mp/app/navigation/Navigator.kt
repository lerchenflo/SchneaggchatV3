package org.lerchenflo.schneaggchatv3mp.app.navigation

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

class Navigator {
    private val _navigationActions = Channel<NavigationAction>()
    val navigationActions = _navigationActions.receiveAsFlow()

    suspend fun navigate(
        destination: Route,
        navigationOptions: NavigationOptions = NavigationOptions()
    ){
        _navigationActions.send(NavigationAction.Navigate(destination, navigationOptions))
    }

    suspend fun navigateBack(navigationOptions: NavigationOptions = NavigationOptions()){
        _navigationActions.send(NavigationAction.NavigateBack(navigationOptions))
    }

    suspend fun navigateToSubRoute(
        rootRoute: Route,
        destination: Route,
        navigationOptions: NavigationOptions = NavigationOptions()
    ){
        _navigationActions.send(NavigationAction.NavigateSubRoute(
            rootRoute = rootRoute,
            destination = destination,
            navigationOptions = navigationOptions
        ))
    }


    data class NavigationOptions(
        val exitPreviousScreen: Boolean = false,
        val exitAllPreviousScreens: Boolean = false,
        val removeAllScreensByRoute: List<Route> = emptyList(), //Remove all of these types
        val removeAllExceptByRoute: Route? = null //Remove all but this route
    )
}