package org.lerchenflo.schneaggchatv3mp.login.presentation.autologincredchecker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.app.AppLifecycleManager
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.utilities.IncomingDataManager

class AutoLoginCredCheckerViewModel(
    private val appRepository: AppRepository,
    private val navigator: Navigator
) : ViewModel() {

    private val _state = MutableStateFlow(AutoLoginCredCheckerState())
    val state = _state.asStateFlow()

    init {
        checkCredentials()
    }

    fun onAction(action: AutoLoginCredCheckerAction) {
        when (action) {
            AutoLoginCredCheckerAction.CheckCredentials -> checkCredentials()
        }
    }

    private fun checkCredentials() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val savedCreds = appRepository.loadSavedLoginConfig()

            if (SessionCache.isLoggedIn()) {
                // User got logged in when loading the saved config
                if (savedCreds.emailVerified) {
                    // Email is verified, rerouting
                    println("AUTOLOGINCHECKER: Logged in and Email verified, routing to chatselector")

                    if (IncomingDataManager.isNewDataAvailable()) {
                        println("AUTOLOGINCHECKER: New data available")
                        navigator.navigate(
                            Route.ChatSelector,
                            navigationOptions = Navigator.NavigationOptions(exitAllPreviousScreens = true)
                        )
                        navigator.navigate(
                            Route.MessageChatSelector
                        )
                    } else {
                        println("AUTOLOGINCHECKER: no new data available")
                        navigator.navigate(
                            Route.ChatSelector,
                            navigationOptions = Navigator.NavigationOptions(exitAllPreviousScreens = true)
                        )
                    }
                    // ChatSelector reached - no more backstack-clearing navigation will follow,
                    // safe from here on for a pending notification tap to navigate to its chat.
                    AppLifecycleManager.notifyStartupRoutingDone()
                } else {
                    // Email not verified, navigate to email verify checker. Startup routing is
                    // NOT done yet - EmailVerifiedCheckViewModel still has to navigate to
                    // ChatSelector itself (clearing the backstack again) once verified.
                    navigator.navigate(
                        Route.EmailVerifiedCheck,
                        navigationOptions = Navigator.NavigationOptions(exitAllPreviousScreens = true)
                    )
                }
            } else {
                // User not logged in, reroute to login. Not marking routing as done: nothing to
                // navigate to yet anyway since a pending chat requires a logged-in user.
                navigator.navigate(
                    Route.Login,
                    navigationOptions = Navigator.NavigationOptions(exitAllPreviousScreens = true)
                )
            }

            _state.update { it.copy(isLoading = false) }
        }
    }
}
