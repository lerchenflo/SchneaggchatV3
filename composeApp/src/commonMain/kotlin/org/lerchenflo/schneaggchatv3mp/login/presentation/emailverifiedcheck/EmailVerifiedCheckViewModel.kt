@file:OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)

package org.lerchenflo.schneaggchatv3mp.login.presentation.emailverifiedcheck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.lerchenflo.schneaggchatv3mp.BASE_SERVER_URL
import org.lerchenflo.schneaggchatv3mp.app.AppLifecycleManager
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.datasource.domain.AuthRepository
import org.lerchenflo.schneaggchatv3mp.datasource.domain.DataSyncCoordinator
import org.lerchenflo.schneaggchatv3mp.datasource.domain.SessionTerminator
import org.lerchenflo.schneaggchatv3mp.datasource.domain.SettingsRepository
import org.lerchenflo.schneaggchatv3mp.datasource.domain.UserRepository
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.email_check_check_emails_snackbar
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class EmailVerifiedCheckViewModel(
    private val dataSyncCoordinator: DataSyncCoordinator,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val sessionTerminator: SessionTerminator,
    private val navigator: Navigator,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(EmailVerifiedCheckState())
    val state = _state.asStateFlow()

    /** The saved server URL; the FAQ link opens this server's page. */
    val serverUrl: StateFlow<String> = settingsRepository.getServerUrlFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, BASE_SERVER_URL)

    private var lastEmailVerificationTime: Instant = Instant.DISTANT_PAST

    fun onAction(action: EmailVerifiedCheckAction) {
        when (action) {
            EmailVerifiedCheckAction.OnResendEmailClick -> resendEmail()
            EmailVerifiedCheckAction.OnLogoutClick -> logout()
            EmailVerifiedCheckAction.OnCheckVerificationClick -> checkVerification()
            EmailVerifiedCheckAction.OnChangeEmailDismiss -> showEmailDialog(false)
            is EmailVerifiedCheckAction.OnChangeEmailText -> changeEmail(newEmail = action.text)
            EmailVerifiedCheckAction.OnChangeEmailStart -> showEmailDialog(true)
        }
    }


    private fun showEmailDialog(show: Boolean) {
        _state.update {
            it.copy(
                showChangeEmailPopup = show
            )
        }
    }

    private fun changeEmail(newEmail: String) {

        state.value.userData?.let { user ->
            viewModelScope.launch {
                userRepository.changeUserDetails(newEmail = newEmail, userId = user.id)
                showEmailDialog(false)
                resendEmail()
            }
        }
    }

    private fun resendEmail() {
        if (lastEmailVerificationTime.plus(2.minutes) > Clock.System.now()) {
            println("Email sending is throttled")
            return //If last email was sent in the last 2 mins
        }

        viewModelScope.launch {
            // Guard set synchronously, before any suspend call, so a second
            // near-simultaneous tap can't slip through while this coroutine is
            // still suspended inside sendEmailVerify() below.
            if (_state.value.isResendingEmail) return@launch
            _state.update { it.copy(isResendingEmail = true) }

            try {
                lastEmailVerificationTime = Clock.System.now()
                authRepository.sendEmailVerify()
            } finally {
                _state.update { it.copy(isResendingEmail = false) }
            }
        }
    }

    private fun logout() {
        viewModelScope.launch {
            sessionTerminator.logout()
            navigator.navigate(Route.Login, navigationOptions = Navigator.NavigationOptions(exitAllPreviousScreens = true))
        }
    }

    private fun checkVerification() {
        println("authstate : ${SessionCache.authState.value}")
        // UNDISPATCHED: the sync is started right away, even if this ViewModel is cleared before the
        // main thread gets to it; it runs in the app-wide scope (plan R2).
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            _state.update {
                it.copy(
                    isLoading = true
                )
            }
            //Runs in the app-wide scope, so it is not cancelled when logging in suddenly
            dataSyncCoordinator.dataSync(reason = "emailVerifiedCheck") //await data sync finish

            _state.update {
                it.copy(
                    isLoading = false
                )
            }
        }
    }

    var reroutingToChatselector = false

    // True once the own user was missing locally while logged in, i.e. it only arrives with the
    // sync after a fresh login - then the "Hello <name>" greeting is held briefly before rerouting.
    // A normal app start already has the user, so it isn't slowed down.
    private var ownUserCameFromSync = false

    init {

        // UNDISPATCHED: the sync is started before the rerouting below can clear this ViewModel;
        // it then runs in the app-wide scope (plan R2).
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            dataSyncCoordinator.dataSync(reason = "emailVerifiedCheckInit")
        }

        viewModelScope.launch {
            delay(8.seconds)
            _state.update { it.copy(canResendEmail = true) }
        }


        viewModelScope.launch {
            SessionCache.authState
                .flatMapLatest { authState ->
                    //println("AUTHSTATE CHANGED: $authState")
                    if (authState is SessionCache.AuthState.LoggedIn) {
                        userRepository.getUserByIdFlow(userId = authState.userId)
                    } else {
                        flowOf(null) // emit null and stop when logged out
                    }
                }
                .collect { user ->

                    if (reroutingToChatselector) return@collect

                    if (user == null && SessionCache.authState.value is SessionCache.AuthState.LoggedIn) {
                        ownUserCameFromSync = true
                    }

                    _state.update { cstate ->
                        cstate.copy(
                            userData = user,
                            currentEmail = user?.email ?: cstate.currentEmail
                        )
                    }

                    if (user != null && user.emailVerifiedAt != null) {
                        println("Email verified in verify screen, rerouting to chatselector")
                        reroutingToChatselector = true
                        if (ownUserCameFromSync) delay(GREETING_DURATION)
                        runBlocking {
                            navigator.navigate(
                                destination = Route.ChatSelector,
                                navigationOptions = Navigator.NavigationOptions(
                                    exitAllPreviousScreens = true
                                )
                            )
                        }
                        // ChatSelector reached - safe from here on for a pending notification
                        // tap to navigate to its chat.
                        AppLifecycleManager.notifyStartupRoutingDone()
                    }


                    if (user != null && user.emailVerifiedAt == null) {
                        SnackbarManager.showMessage(getString(Res.string.email_check_check_emails_snackbar), showTime = 6000)
                    }
                }
        }
    }

    private companion object {
        val GREETING_DURATION = 1500.milliseconds
    }
}
