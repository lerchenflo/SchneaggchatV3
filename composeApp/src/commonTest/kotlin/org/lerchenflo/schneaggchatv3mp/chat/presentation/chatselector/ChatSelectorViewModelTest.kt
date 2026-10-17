package org.lerchenflo.schneaggchatv3mp.chat.presentation.chatselector

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import kotlinx.coroutines.CompletableDeferred
import org.lerchenflo.schneaggchatv3mp.app.navigation.NavigationAction
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageSearchResult
import org.lerchenflo.schneaggchatv3mp.fakes.FakeAnnouncementRepository
import org.lerchenflo.schneaggchatv3mp.fakes.FakeAppInfoRepository
import org.lerchenflo.schneaggchatv3mp.fakes.FakeAppVersion
import org.lerchenflo.schneaggchatv3mp.fakes.FakeAuthRepository
import org.lerchenflo.schneaggchatv3mp.fakes.FakeChatListRepository
import org.lerchenflo.schneaggchatv3mp.fakes.FakeDataSyncCoordinator
import org.lerchenflo.schneaggchatv3mp.fakes.FakeLogger
import org.lerchenflo.schneaggchatv3mp.fakes.FakeMessageRepository
import org.lerchenflo.schneaggchatv3mp.fakes.FakeNotifier
import org.lerchenflo.schneaggchatv3mp.fakes.FakePermissionManager
import org.lerchenflo.schneaggchatv3mp.fakes.FakeSettingsRepository
import org.lerchenflo.schneaggchatv3mp.testutil.MainDispatcherRule
import org.lerchenflo.schneaggchatv3mp.testutil.TestViewModelStore
import org.lerchenflo.schneaggchatv3mp.testutil.awaitMatching
import org.lerchenflo.schneaggchatv3mp.testutil.loginTestUser
import org.lerchenflo.schneaggchatv3mp.testutil.resetGlobalState
import org.lerchenflo.schneaggchatv3mp.testutil.runViewModelTest
import org.lerchenflo.schneaggchatv3mp.testutil.startTestKoin
import org.lerchenflo.schneaggchatv3mp.testutil.stopTestKoin
import org.lerchenflo.schneaggchatv3mp.testutil.testChatListItem
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

private const val OWN_ID = "me"

/** Chat list, search, pins, the pending-request badge and pull-to-refresh of the chat selector. */
class ChatSelectorViewModelTest {

    private val mainDispatcher = MainDispatcherRule()
    private val viewModels = TestViewModelStore()

    private val chatListRepository = FakeChatListRepository()
    private val messageRepository = FakeMessageRepository()
    private val dataSyncCoordinator = FakeDataSyncCoordinator()
    private val authRepository = FakeAuthRepository()
    // Same version as last start: no changelog popup
    private val appInfoRepository = FakeAppInfoRepository(installedVersion = "1.0.0")
    private val settingsRepository = FakeSettingsRepository().apply { lastStartedVersion.value = "1.0.0" }
    private val notifier = FakeNotifier(token = "push-token")
    private val navigator = Navigator()

    private val anna = testChatListItem("anna", name = "Anna")
    private val ben = testChatListItem("ben", name = "Ben", unreadMessageCount = 2)
    private val hikingGroup = testChatListItem("hiking", isGroup = true, name = "Hiking", unreadMessageCount = 5)

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
        ChatSelectorViewModel(
            appVersion = FakeAppVersion(),
            dataSyncCoordinator = dataSyncCoordinator,
            chatListRepository = chatListRepository,
            messageRepository = messageRepository,
            authRepository = authRepository,
            appInfoRepository = appInfoRepository,
            navigator = navigator,
            loggingRepository = FakeLogger(),
            permissionManager = FakePermissionManager(),
            settingsRepository = settingsRepository,
            announcementRepository = FakeAnnouncementRepository(),
        )
    )

    @Test
    fun `shows the chat list of the logged-in user in repository order`() = runViewModelTest(viewModels) {
        chatListRepository.chats.value = listOf(hikingGroup, anna, ben)

        val viewModel = createViewModel()

        assertThat(viewModel.chatSelectorState.awaitMatching { it.isNotEmpty() })
            .containsExactly(hikingGroup, anna, ben)
        assertThat(chatListRepository.chatListRequests.calls).containsExactly(OWN_ID)
    }

    @Test
    fun `loads the chat list once the user logs in`() = runViewModelTest(viewModels) {
        resetGlobalState() // logged out
        chatListRepository.chats.value = listOf(anna)
        val viewModel = createViewModel()

        loginTestUser(OWN_ID)

        assertThat(viewModel.chatSelectorState.awaitMatching { it.isNotEmpty() }).containsExactly(anna)
        assertThat(chatListRepository.chatListRequests.calls).containsExactly(OWN_ID)
    }

    @Test
    fun `search filters the loaded list by name without querying the chat list again`() = runViewModelTest(viewModels) {
        chatListRepository.chats.value = listOf(hikingGroup, anna, ben)
        val viewModel = createViewModel()
        viewModel.chatSelectorState.awaitMatching { it.size == 3 }

        viewModel.updateSearchterm("AN")

        assertThat(viewModel.chatSelectorState.awaitMatching { it.size == 1 }).containsExactly(anna)
        assertThat(chatListRepository.chatListRequests.calls).containsExactly(OWN_ID)
    }

    @Test
    fun `unread filter keeps only chats with unread messages`() = runViewModelTest(viewModels) {
        chatListRepository.chats.value = listOf(hikingGroup, anna, ben)
        val viewModel = createViewModel()
        viewModel.chatSelectorState.awaitMatching { it.size == 3 }

        viewModel.updateFilter(ChatFilter.UNREAD)

        assertThat(viewModel.chatSelectorState.awaitMatching { it.size == 2 }).containsExactly(hikingGroup, ben)
    }

    @Test
    fun `message search asks the repository for the debounced term and shows its results`() = runViewModelTest(viewModels) {
        val result = MessageSearchResult(
            messageId = "m1",
            chatId = "anna",
            isGroup = false,
            chatName = "Anna",
            chatProfilePictureUrl = "",
            senderName = "Anna",
            preview = "see you at the lake",
            sendDate = 1L,
        )
        chatListRepository.messageSearchResults.value = listOf(result)
        val viewModel = createViewModel()

        viewModel.updateSearchterm("lake")

        assertThat(viewModel.messageSearchResults.awaitMatching { it.isNotEmpty() }).containsExactly(result)
        assertThat(chatListRepository.messageSearchRequests.calls)
            .containsExactly(FakeChatListRepository.SearchRequest(searchTerm = "lake", userId = OWN_ID))
    }

    @Test
    fun `selecting a message search result clears the search and opens the chat at that message`() = runViewModelTest(viewModels) {
        val viewModel = createViewModel()
        viewModel.updateSearchterm("lake")
        val result = MessageSearchResult("m1", "hiking", true, "Hiking", "", "Ben", "lake", 1L)

        navigator.navigationActions.test {
            viewModel.onMessageSearchResultSelected(result)

            assertThat(awaitItem()).isEqualTo(
                NavigationAction.Navigate(Route.Chat(chatId = "hiking", isGroup = true, highlightMessageId = "m1"))
            )
        }
        assertThat(viewModel.searchTerm.value).isEqualTo("")
    }

    @Test
    fun `pinUnpinChat pins an unpinned chat and unpins a pinned one`() = runViewModelTest(viewModels) {
        val pinnedAnna = anna.copy(pinned = 1_700_000_000_000L)
        val viewModel = createViewModel()

        viewModel.pinUnpinChat(hikingGroup)
        viewModel.pinUnpinChat(pinnedAnna)

        assertThat(chatListRepository.pinCalls.calls).containsExactly(
            FakeChatListRepository.PinCall(chatId = "hiking", group = true, pinned = true),
            FakeChatListRepository.PinCall(chatId = "anna", group = false, pinned = false),
        )
    }

    @Test
    fun `pending friend badge counts only requests from others`() = runViewModelTest(viewModels) {
        chatListRepository.pendingFriends.value = listOf(
            testChatListItem("carla", requesterId = "carla"),
            testChatListItem("dani", requesterId = "dani"),
            testChatListItem("emil", requesterId = OWN_ID), // our own outgoing request
        )

        val viewModel = createViewModel()

        assertThat(viewModel.pendingFriendCount.awaitMatching { it > 0 }).isEqualTo(2)
    }

    @Test
    fun `refresh sends the offline queue then syncs and ignores a second refresh while running`() = runViewModelTest(viewModels) {
        val order = mutableListOf<String>()
        val syncGate = CompletableDeferred<Unit>()
        messageRepository.onSendOfflineMessages = { order += "sendOffline" }
        dataSyncCoordinator.onDataSync = { order += "dataSync"; syncGate.await() }
        val viewModel = createViewModel()

        viewModel.refresh()
        viewModel.refresh() // still syncing

        assertThat(order).containsExactly("sendOffline", "dataSync")
        assertThat(messageRepository.sendOfflineCalls.calls).containsExactly(OWN_ID)
        assertThat(dataSyncCoordinator.syncReasons.calls).containsExactly("chatSelectorRefresh")

        syncGate.complete(Unit)
        viewModel.refresh()

        assertThat(dataSyncCoordinator.syncReasons.count).isEqualTo(2)
    }

    @Test
    fun `registers the notifier's push token at start`() = runViewModelTest(viewModels) {
        createViewModel()

        assertThat(authRepository.notificationTokens.calls).containsExactly("push-token")
    }
}
