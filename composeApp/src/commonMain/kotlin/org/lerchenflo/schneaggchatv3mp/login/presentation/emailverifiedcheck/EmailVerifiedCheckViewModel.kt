package org.lerchenflo.schneaggchatv3mp.login.presentation.emailverifiedcheck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.SUPPORT_EMAIL
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.utilities.ShareUtils

class EmailVerifiedCheckViewModel(
    private val appRepository: AppRepository,
    private val navigator: Navigator,
    private val shareUtils: ShareUtils
) : ViewModel() {

    private val _state = MutableStateFlow(EmailVerifiedCheckState())
    val state = _state.asStateFlow()

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
        shareUtils.openMailClient(
            recipient = SUPPORT_EMAIL,
            subject = "Email Verification not working",
            body = "Es tuat ned (Bitte mehr infos)"
        )
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
            }
        }
    }

    private fun resendEmail() {
        viewModelScope.launch {
            appRepository.sendEmailVerify()
        }
    }

    private fun logout() {
        viewModelScope.launch {
            appRepository.logout()
            navigator.navigate(Route.Login, navigationOptions = Navigator.NavigationOptions(exitAllPreviousScreens = true))
        }
    }

    private fun checkVerification() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true
                )
            }
            appRepository.dataSync()
            _state.update {
                it.copy(
                    isLoading = false
                )
            }
        }
    }

    init {
        viewModelScope.launch {
            appRepository.getUserByIdFlow(
                userId =SessionCache.requireLoggedIn()?.userId ?: ""
            )
                .collectLatest { user ->

                    if (user != null && user.emailVerifiedAt != null) {
                        println("Email verified in verify screen, rerouting to chatselector")
                        navigator.navigate(
                            destination = Route.ChatSelector,
                            navigationOptions = Navigator.NavigationOptions(
                                exitAllPreviousScreens = true
                            )
                        )
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
