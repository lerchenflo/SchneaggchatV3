package org.lerchenflo.schneaggchatv3mp.app.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import org.lerchenflo.schneaggchatv3mp.login.presentation.login.InputTextField
import kotlin.reflect.KClass

class Navigator(val navigationState: NavigationState) {

    fun navigate(
        route: NavKey
    ){
        if (route in navigationState.backStacks.keys) {
            navigationState.topLevelRoute = route
        } else {
            //Navigate to sub screens
            navigationState.backStacks[navigationState.topLevelRoute]?.add(route)
        }
    }

    fun navigateBack(){
        val currentStack = navigationState.backStacks[navigationState.topLevelRoute]
            ?: error("Backstack for ${navigationState.topLevelRoute} doesnt exist")
        val currentRoute = currentStack.last()

        if (currentRoute == navigationState.topLevelRoute) {
            navigationState.topLevelRoute = navigationState.startRoute
        } else {
            currentStack.removeLastOrNull()
        }
    }


}