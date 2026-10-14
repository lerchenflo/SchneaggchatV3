package org.lerchenflo.schneaggchatv3mp.chat.presentation.newchat

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.single
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.lerchenflo.schneaggchatv3mp.app.navigation.NavigationAction
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.chat.domain.AvailableUser
import org.lerchenflo.schneaggchatv3mp.chat.domain.FriendshipStatus
import org.lerchenflo.schneaggchatv3mp.fakes.FakeAppVersion
import org.lerchenflo.schneaggchatv3mp.fakes.FakeChatListRepository
import org.lerchenflo.schneaggchatv3mp.fakes.FakeLogger
import org.lerchenflo.schneaggchatv3mp.fakes.FakeNotifier
import org.lerchenflo.schneaggchatv3mp.fakes.FakeSettingsRepository
import org.lerchenflo.schneaggchatv3mp.fakes.FakeShareUtils
import org.lerchenflo.schneaggchatv3mp.fakes.FakeUserRepository
import org.lerchenflo.schneaggchatv3mp.testutil.MainDispatcherRule
import org.lerchenflo.schneaggchatv3mp.testutil.TestViewModelStore
import org.lerchenflo.schneaggchatv3mp.testutil.loginTestUser
import org.lerchenflo.schneaggchatv3mp.testutil.resetGlobalState
import org.lerchenflo.schneaggchatv3mp.testutil.runViewModelTest
import org.lerchenflo.schneaggchatv3mp.testutil.startTestKoin
import org.lerchenflo.schneaggchatv3mp.testutil.stopTestKoin
import org.lerchenflo.schneaggchatv3mp.testutil.testChatListItem
import org.lerchenflo.schneaggchatv3mp.utilities.NotificationManager
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

private const val OWN_ID = "me"

/** User search, friend requests and pending requests of the new chat screen. */
@OptIn(ExperimentalCoroutinesApi::class)
class NewChatViewModelTest {

    private val mainDispatcher = MainDispatcherRule()
    private val viewModels = TestViewModelStore()

    private val userRepository = FakeUserRepository()
    private val chatListRepository = FakeChatListRepository()
    private val settingsRepository = FakeSettingsRepository()
    private val shareUtils = FakeShareUtils()
    private val notifier = FakeNotifier()
    private val navigator = Navigator()

    private val incomingRequest = testChatListItem("carla", name = "Carla", friendshipStatus = FriendshipStatus.PENDING, requesterId = "carla")
    private val acceptedFriend = testChatListItem("anna", name = "Anna", friendshipStatus = FriendshipStatus.ACCEPTED)

    @BeforeTest
    fun setUp() {
        resetGlobalState()
        mainDispatcher.setUp()
        startTestKoin(notifier)
        loginTestUser(OWN_ID)
    }

    @AfterTest
    fun tearDown() {
        stopTestKoin()
        mainDispatcher.tearDown()
        resetGlobalState()
    }

    private fun createViewModel() = viewModels.add(
        NewChatViewModel(
            appVersion = FakeAppVersion(),
            chatListRepository = chatListRepository,
            userRepository = userRepository,
            navigator = navigator,
            loggingRepository = FakeLogger(),
            shareUtils = shareUtils,
            settingsRepository = settingsRepository,
        )
    )

    @Test
    fun `opening the screen clears the friend request notifications`() = runViewModelTest(viewModels) {
        createViewModel()

        assertThat(notifier.cancelledIds.calls).containsExactly(NotificationManager.NotiIdType.FRIEND_REQUEST.baseId)
    }

    @Test
    fun `user search waits for typing to stop and lists most common friends first`() = runViewModelTest(viewModels) {
        userRepository.availableUsers = listOf(
            AvailableUser(id = "ben", username = "Ben", commonFriendCount = 1),
            AvailableUser(id = "bernd", username = "Bernd", commonFriendCount = 4),
        )
        val viewModel = createViewModel()

        viewModel.updateSearchterm("b")
        viewModel.updateSearchterm("be")
        advanceUntilIdle()

        assertThat(userRepository.availableUserSearches.calls).containsExactly("be")
        assertThat(viewModel.availableChats.value.map { it.id }).containsExactly("bernd", "ben")
    }

    @Test
    fun `pending requests follow the search term`() = runViewModelTest(viewModels) {
        chatListRepository.pendingFriends.value = listOf(incomingRequest, testChatListItem("dani", friendshipStatus = FriendshipStatus.PENDING))
        val viewModel = createViewModel()

        assertThat(viewModel.pendingFriends.value.map { it.id }).containsExactly("carla", "dani")

        viewModel.updateSearchterm("car")

        assertThat(viewModel.pendingFriends.value.map { it.id }).containsExactly("carla")
    }

    @Test
    fun `tapping a pending request opens the dialog and accepting sends the request back`() = runViewModelTest(viewModels) {
        val viewModel = createViewModel()

        viewModel.onPendingFriendRequestClick(incomingRequest)
        assertThat(viewModel.pendingFriendPopup.value).isEqualTo(incomingRequest)

        viewModel.acceptFriend("carla")

        assertThat(userRepository.friendRequestsSent.calls).containsExactly("carla")
        assertThat(viewModel.pendingFriendPopup.value).isNull()
    }

    @Test
    fun `denying a pending request denies it and closes the dialog`() = runViewModelTest(viewModels) {
        val viewModel = createViewModel()
        viewModel.onPendingFriendRequestClick(incomingRequest)

        viewModel.denyFriend("carla")

        assertThat(userRepository.friendRequestsDenied.calls).containsExactly("carla")
        assertThat(userRepository.friendRequestsSent.calls).isEmpty()
        assertThat(viewModel.pendingFriendPopup.value).isNull()
    }

    @Test
    fun `tapping an accepted friend opens the chat instead of the dialog`() = runViewModelTest(viewModels) {
        val viewModel = createViewModel()

        navigator.navigationActions.test {
            viewModel.onPendingFriendRequestClick(acceptedFriend)

            assertThat(awaitItem()).isEqualTo(
                NavigationAction.Navigate(
                    Route.Chat(chatId = "anna", isGroup = false),
                    Navigator.NavigationOptions(removeAllScreensByClass = listOf(Route.NewChat::class)),
                )
            )
        }
        assertThat(viewModel.pendingFriendPopup.value).isNull()
    }

    @Test
    fun `adding a friend sends the request and reloads the search`() = runViewModelTest(viewModels) {
        val viewModel = createViewModel()
        viewModel.updateSearchterm("be")
        advanceUntilIdle()

        viewModel.addFriend("ben")

        // Runs on Dispatchers.IO, not in viewModelScope
        assertThat(userRepository.friendRequestsSent.awaitCount(1)).containsExactly("ben")
        assertThat(userRepository.availableUserSearches.awaitCount(2)).containsExactly("be", "be")
    }

    @Test
    fun `invite shares the invitation with the server url`() = runViewModelTest(viewModels) {
        settingsRepository.serverUrl.value = "https://chat.example.org"
        val viewModel = createViewModel()

        viewModel.onInviteFriendClick()

        assertThat(shareUtils.sharedStrings.awaitCount(1)).single().contains("https://chat.example.org")
    }
}
