package org.lerchenflo.schneaggchatv3mp.app.navigation

import androidx.navigation.NavOptionsBuilder
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.receiveAsFlow

class Navigator(
    val startDestination: Route,
) {
    private val _navigationActions = Channel<NavigationAction>()
    val navigationActions = _navigationActions.receiveAsFlow()

    suspend fun navigate(
        destination: Route,
        exitPreviousScreen: Boolean = false,
        exitAllPreviousScreens: Boolean = false
    ){
        _navigationActions.send(NavigationAction.Navigate(destination, exitPreviousScreen, exitAllPreviousScreens))
    }

    suspend fun navigateBack(){
        _navigationActions.send(NavigationAction.NavigateBack)
    }

    suspend fun navigateSettings(
        destination: Route
    ){
        _navigationActions.send(NavigationAction.NavigateSettings(destination))
    }
}