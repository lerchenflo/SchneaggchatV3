package org.lerchenflo.schneaggchatv3mp.chat.presentation.unknownuser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.lerchenflo.schneaggchatv3mp.app.ApplicationScope
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.domain.ErrorChannel
import org.lerchenflo.schneaggchatv3mp.app.domain.onSuccess
import org.lerchenflo.schneaggchatv3mp.app.domain.reportErrorTo
import org.lerchenflo.schneaggchatv3mp.app.launchOnMain
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.chat.domain.FriendRequestState
import org.lerchenflo.schneaggchatv3mp.chat.domain.FriendshipStatus
import org.lerchenflo.schneaggchatv3mp.chat.domain.UserIdentityName
import org.lerchenflo.schneaggchatv3mp.chat.domain.UserIdentityRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.UserRepository
import org.lerchenflo.schneaggchatv3mp.utilities.device.ShareUtils
import org.lerchenflo.schneaggchatv3mp.utilities.ui.SnackbarManager
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.friend_request_sent

/**
 * The popup for one user ([Route.UnknownUser]), like WhatsApp's: a friend gets Message, Call (with
 * a shared phone number) and Info; anyone else sends or accepts a friend request. A blocked user
 * or the own user closes it again. Opened from anywhere by navigating to the route.
 */
class UnknownUserViewModel(
    private val route: Route.UnknownUser,
    private val userRepository: UserRepository,
    private val userIdentityRepository: UserIdentityRepository,
    private val navigator: Navigator,
    private val errorChannel: ErrorChannel,
    private val applicationScope: ApplicationScope,
    private val shareUtils: ShareUtils,
) : ViewModel() {

    private val _state = MutableStateFlow(
        UnknownUserState(
            // The caller's name (e.g. from the search) shows until the one path knows the user
            name = route.name?.let { UserIdentityName.Known(it) } ?: UserIdentityName.Unknown,
            commonFriendCount = route.commonFriendCount,
        )
    )
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch { load() }
        // Name and picture through the one path; a user this device does not know is looked up
        viewModelScope.launch {
            userIdentityRepository.observeUser(route.userId).collect { identity ->
                _state.update {
                    it.copy(
                        name = identity.name.takeIf { name -> name != UserIdentityName.Unknown } ?: it.name,
                        profilePictureFilePath = identity.profilePictureUrl,
                    )
                }
            }
        }
    }

    fun onAction(action: UnknownUserAction) {
        when (action) {
            UnknownUserAction.OnSendRequestClick -> sendRequest()
            UnknownUserAction.OnMessageClick -> openInsteadOfPopup(Route.Chat(chatId = route.userId, isGroup = false))
            UnknownUserAction.OnInfoClick -> openInsteadOfPopup(Route.ChatDetails(chatId = route.userId, isGroup = false))
            UnknownUserAction.OnCallClick -> state.value.phoneNumber?.let { shareUtils.openPhoneDialer(it) }
            UnknownUserAction.OnDismissClick -> viewModelScope.launch { navigator.navigateBack() }
        }
    }

    private suspend fun load() {
        val ownId = SessionCache.requireLoggedIn()?.userId
        if (ownId == null || route.userId == ownId) {
            navigator.navigateBack()
            return
        }

        val user = userRepository.getUserById(route.userId)
        val requestState = FriendRequestState.of(user, ownId)
        when {
            requestState != null -> _state.update { it.copy(requestState = requestState) }

            user?.friendshipStatus == FriendshipStatus.ACCEPTED -> _state.update {
                it.copy(isFriend = true, phoneNumber = user.phoneNumber?.takeIf { number -> number.isNotBlank() })
            }

            // Blocked: nothing to offer
            else -> navigator.navigateBack()
        }
    }

    /** Replaces the popup with [destination], so back goes to where the popup was opened. */
    private fun openInsteadOfPopup(destination: Route) {
        viewModelScope.launch {
            navigator.navigate(
                destination,
                Navigator.NavigationOptions(removeAllScreensByClass = listOf(Route.UnknownUser::class))
            )
        }
    }

    private fun sendRequest() {
        // In the app scope: the request must go out even though the popup closes right away
        applicationScope.launchOnMain {
            userRepository.sendFriendRequest(route.userId).reportErrorTo(errorChannel).onSuccess {
                SnackbarManager.showMessage(getString(Res.string.friend_request_sent))
            }
        }
        viewModelScope.launch { navigator.navigateBack() }
    }
}
