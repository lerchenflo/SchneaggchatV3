package org.lerchenflo.schneaggchatv3mp.app.navigation

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlin.reflect.KClass

class Navigator {
    private val _navigationActions = Channel<NavigationAction>()
    val navigationActions = _navigationActions.receiveAsFlow()

    suspend fun navigate(
        destination: Route,
        navigationOptions: NavigationOptions = NavigationOptions()
    ){
        val root = destination.parentRootOrNull()
        _navigationActions.send(
            if (root != null) {
                NavigationAction.NavigateSubRoute(
                    rootRoute = root,
                    destination = destination,
                    navigationOptions = navigationOptions
                )
            } else {
                NavigationAction.Navigate(destination, navigationOptions)
            }
        )
    }

    suspend fun navigateBack(navigationOptions: NavigationOptions = NavigationOptions()){
        _navigationActions.send(NavigationAction.NavigateBack(navigationOptions))
    }


    data class NavigationOptions(
        val exitPreviousScreen: Boolean = false,
        val exitAllPreviousScreens: Boolean = false,
        val removeAllScreensByClass: List<KClass<out Route>> = emptyList(), //Remove all screens of these route types
        val removeAllExceptByRoute: Route? = null, //Remove all but this route
        val exitRootWithSubRoute: Boolean = false // when going back from a subroute, also pop the root
    )
}