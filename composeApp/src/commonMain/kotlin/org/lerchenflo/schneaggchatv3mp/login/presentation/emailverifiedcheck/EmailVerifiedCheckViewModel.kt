@file:OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)

package org.lerchenflo.schneaggchatv3mp.login.presentation.emailverifiedcheck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.lerchenflo.schneaggchatv3mp.SUPPORT_EMAIL
import org.lerchenflo.schneaggchatv3mp.app.AppLifecycleManager
import org.lerchenflo.schneaggchatv3mp.app.ApplicationScope
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.utilities.ShareUtils
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.support_email_verification_body
import schneaggchatv3mp.composeapp.generated.resources.support_email_verification_subject
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class EmailVerifiedCheckViewModel(
    private val appRepository: AppRepository,
    private val navigator: Navigator,
    private val shareUtils: ShareUtils,
    private val applicationScope: ApplicationScope
) : ViewModel() {

    private val _state = MutableStateFlow(EmailVerifiedCheckState())
    val state = _state.asStateFlow()

    private var lastEmailVerificationTime: Instant = Instant.DISTANT_PAST

    fun onAction(action: EmailVerifiedCheckAction) {
        when (action) {
            EmailVerifiedCheckAction.OnResendEmailClick -> resendEmail()
            EmailVerifiedCheckAction.OnLogoutClick -> logout()
            EmailVerifiedCheckAction.OnCheckVerificationClick -> checkVerification()
            EmailVerifiedCheckAction.OnChangeEmailDismiss -> showEmailDialog(false)
            is EmailVerifiedCheckAction.OnChangeEmailText -> changeEmail(newEmail = action.text)
            EmailVerifiedCheckAction.OnChangeEmailStart -> showEmailDialog(true)
            EmailVerifiedCheckAction.OnRequestSupportClick -> sendSupportEmail()
        }
    }


    private fun sendSupportEmail() {
        viewModelScope.launch {
            val subject = getString(Res.string.support_email_verification_subject)
            val body = getString(Res.string.support_email_verification_body)
            shareUtils.openMailClient(
                recipient = SUPPORT_EMAIL,
                subject = subject,
                body = body
            )
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
                appRepository.changeUserDetails(newEmail = newEmail, userId = user.id)
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
                appRepository.sendEmailVerify()
            } finally {
                _state.update { it.copy(isResendingEmail = false) }
            }
        }
    }

    private fun logout() {
        viewModelScope.launch {
            appRepository.logout()
            navigator.navigate(Route.Login, navigationOptions = Navigator.NavigationOptions(exitAllPreviousScreens = true))
        }
    }

    private fun checkVerification() {
        println("authstate : ${SessionCache.authState.value}")
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true
                )
            }
            val syncJob = applicationScope.launch {
                appRepository.dataSync(reason = "emailVerifiedCheck") //Launch in application scope to not cancel when logging in suddenly
            }
            syncJob.join() //await data sync finish

            _state.update {
                it.copy(
                    isLoading = false
                )
            }
        }
    }

    var reroutingToChatselector = false

    init {

        applicationScope.launch {
            appRepository.dataSync(reason = "emailVerifiedCheckInit")
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
                        appRepository.getUserByIdFlow(userId = authState.userId)
                    } else {
                        flowOf(null) // emit null and stop when logged out
                    }
                }
                .collect { user ->

                    if (reroutingToChatselector) return@collect

                    if (user != null && user.emailVerifiedAt != null) {
                        println("Email verified in verify screen, rerouting to chatselector")
                        runBlocking {
                            reroutingToChatselector = true
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

                    _state.update { cstate ->
                        cstate.copy(
                            userData = user,
                            currentEmail = user?.email ?: cstate.currentEmail
                        )
                    }
                }
        }
    }


}
