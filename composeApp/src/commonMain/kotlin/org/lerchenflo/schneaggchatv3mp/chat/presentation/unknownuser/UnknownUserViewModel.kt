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
import org.lerchenflo.schneaggchatv3mp.chat.domain.UserRepository
import org.lerchenflo.schneaggchatv3mp.utilities.PictureManager
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.friend_request_sent

/**
 * The popup for one user the own user is not friends with ([Route.UnknownUser]): shows what is
 * known about them and sends or accepts a friend request. A friend gets their chat instead, and a
 * blocked user or the own user closes it again. Opened from anywhere by navigating to the route.
 */
class UnknownUserViewModel(
    private val route: Route.UnknownUser,
    private val userRepository: UserRepository,
    private val pictureManager: PictureManager,
    private val navigator: Navigator,
    private val errorChannel: ErrorChannel,
    private val applicationScope: ApplicationScope,
) : ViewModel() {

    private val _state = MutableStateFlow(
        UnknownUserState(name = route.name, commonFriendCount = route.commonFriendCount)
    )
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch { load() }
    }

    fun onAction(action: UnknownUserAction) {
        when (action) {
            UnknownUserAction.OnSendRequestClick -> sendRequest()
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
            requestState != null -> _state.update {
                it.copy(
                    // The stored name (nickname) is the newest one; the caller's name is the fallback
                    name = user?.displayName ?: it.name,
                    profilePictureFilePath = pictureManager.getProfilePicFilePath(route.userId, false),
                    requestState = requestState,
                )
            }

            user?.friendshipStatus == FriendshipStatus.ACCEPTED -> navigator.navigate(
                Route.Chat(chatId = route.userId, isGroup = false),
                Navigator.NavigationOptions(removeAllScreensByClass = listOf(Route.UnknownUser::class))
            )

            // Blocked: nothing to offer
            else -> navigator.navigateBack()
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
