package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat

import androidx.compose.ui.text.input.TextFieldValue
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isSameInstanceAs
import assertk.assertions.prop
import assertk.assertions.single
import org.lerchenflo.schneaggchatv3mp.app.AppLifecycleManager
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageDisplayItem
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.SendMessageContent.TextContent
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.MessageContent
import org.lerchenflo.schneaggchatv3mp.fakes.FakeAppVersion
import org.lerchenflo.schneaggchatv3mp.fakes.FakeAudioManager
import org.lerchenflo.schneaggchatv3mp.fakes.FakeGroupRepository
import org.lerchenflo.schneaggchatv3mp.fakes.FakeLogger
import org.lerchenflo.schneaggchatv3mp.fakes.FakeMessageRepository
import org.lerchenflo.schneaggchatv3mp.fakes.FakeMessageRepository.SendCall
import org.lerchenflo.schneaggchatv3mp.fakes.FakeNotifier
import org.lerchenflo.schneaggchatv3mp.fakes.FakePermissionManager
import org.lerchenflo.schneaggchatv3mp.fakes.FakePictureManager
import org.lerchenflo.schneaggchatv3mp.fakes.FakeSettingsRepository
import org.lerchenflo.schneaggchatv3mp.fakes.FakeUserRepository
import org.lerchenflo.schneaggchatv3mp.testutil.MainDispatcherRule
import org.lerchenflo.schneaggchatv3mp.testutil.TestViewModelStore
import org.lerchenflo.schneaggchatv3mp.testutil.awaitMatching
import org.lerchenflo.schneaggchatv3mp.testutil.loginTestUser
import org.lerchenflo.schneaggchatv3mp.testutil.resetGlobalState
import org.lerchenflo.schneaggchatv3mp.testutil.runViewModelTest
import org.lerchenflo.schneaggchatv3mp.testutil.startTestKoin
import org.lerchenflo.schneaggchatv3mp.testutil.stopTestKoin
import org.lerchenflo.schneaggchatv3mp.testutil.testMessage
import org.lerchenflo.schneaggchatv3mp.testutil.testUser
import org.lerchenflo.schneaggchatv3mp.utilities.NotificationManager
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

private const val OWN_ID = "me"
private const val FRIEND_ID = "anna"

/** Sending, editing, deleting, reacting and mark-read of the chat screen, for a direct chat with [FRIEND_ID]. */
class ChatViewModelTest {

    private val mainDispatcher = MainDispatcherRule()
    private val viewModels = TestViewModelStore()

    private val messageRepository = FakeMessageRepository()
    private val userRepository = FakeUserRepository()
    private val settingsRepository = FakeSettingsRepository()
    private val notifier = FakeNotifier()

    private val sentMessage = testMessage(id = "m1", senderId = OWN_ID, receiverId = FRIEND_ID, sendDate = 1_000L, myMessage = true)
    private val unreadMessage = testMessage(id = "m2", senderId = FRIEND_ID, receiverId = OWN_ID, sendDate = 2_000L, readByMe = false)

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
        ChatViewModel(
            appVersion = FakeAppVersion(),
            messageRepository = messageRepository,
            userRepository = userRepository,
            groupRepository = FakeGroupRepository(),
            settingsRepository = settingsRepository,
            navigator = Navigator(),
            loggingRepository = FakeLogger(),
            pictureManager = FakePictureManager(),
            permissionsManager = FakePermissionManager(),
            audioManager = FakeAudioManager(),
            chatId = FRIEND_ID,
            isGroup = false,
        )
    )

    private fun ChatViewModel.type(text: String) =
        onAction(ChatAction.OnSendContentChange(TextContent(TextFieldValue(text))))

    private val emptyInput = TextContent(TextFieldValue(""))

    @Test
    fun `send passes the typed text to the repository and clears the input`() = runViewModelTest(viewModels) {
        val viewModel = createViewModel()

        viewModel.type("Hello")
        viewModel.onAction(ChatAction.OnSendClick)

        assertThat(messageRepository.sendCalls.calls).containsExactly(
            SendCall(
                ownId = OWN_ID,
                messageId = null,
                receiverId = FRIEND_ID,
                isGroup = false,
                content = MessageContent.TextContent("Hello"),
                answerId = null,
                localPk = 0L,
                clientMessageId = null,
            )
        )
        assertThat(viewModel.state.value.sendContent).isEqualTo(emptyInput)
    }

    @Test
    fun `a reply sends the replied message's id and ends the reply`() = runViewModelTest(viewModels) {
        val viewModel = createViewModel()

        viewModel.onAction(ChatAction.OnMessageAction(MessageAction.ReplyToMessage(unreadMessage)))
        viewModel.type("Me too")
        viewModel.onAction(ChatAction.OnSendClick)

        assertThat(messageRepository.sendCalls.calls).single().prop(SendCall::answerId).isEqualTo("m2")
        assertThat(viewModel.state.value.replyMessage).isNull()
    }

    @Test
    fun `blank text is not sent`() = runViewModelTest(viewModels) {
        val viewModel = createViewModel()

        viewModel.type("  \n ")
        viewModel.onAction(ChatAction.OnSendClick)

        assertThat(messageRepository.sendCalls.calls).isEmpty()
    }

    @Test
    fun `nothing is sent while logged out`() = runViewModelTest(viewModels) {
        val viewModel = createViewModel()
        resetGlobalState() // logs out

        viewModel.type("Hello")
        viewModel.onAction(ChatAction.OnSendClick)

        assertThat(messageRepository.sendCalls.calls).isEmpty()
    }

    @Test
    fun `edit puts the message into the input and confirming sends the new text`() = runViewModelTest(viewModels) {
        val viewModel = createViewModel()

        viewModel.onAction(ChatAction.OnMessageAction(MessageAction.StartEditMessage(sentMessage)))

        assertThat(viewModel.state.value.editMessage).isEqualTo(sentMessage)
        assertThat(viewModel.state.value.sendContent).isEqualTo(TextContent(TextFieldValue(sentMessage.content)))

        viewModel.type("Edited")
        viewModel.onAction(ChatAction.OnConfirmEditClick)

        assertThat(messageRepository.editCalls.calls)
            .containsExactly(FakeMessageRepository.EditCall(sentMessage, "Edited"))
        assertThat(viewModel.state.value.editMessage).isNull()
        assertThat(viewModel.state.value.sendContent).isEqualTo(emptyInput)
        assertThat(messageRepository.sendCalls.calls).isEmpty()
    }

    @Test
    fun `delete removes a sent message on the server and a queued one only locally`() = runViewModelTest(viewModels) {
        val queuedMessage = testMessage(id = null, senderId = OWN_ID, receiverId = FRIEND_ID, sendDate = 3_000L, myMessage = true, sent = false, localPK = 7L)
        val viewModel = createViewModel()

        viewModel.onAction(ChatAction.OnMessageAction(MessageAction.DeleteMessage(sentMessage)))
        viewModel.onAction(ChatAction.OnMessageAction(MessageAction.DeleteMessage(queuedMessage)))

        assertThat(messageRepository.deleteCalls.calls).containsExactly("m1")
        assertThat(messageRepository.deleteLocalCalls.calls).containsExactly(7L)
    }

    @Test
    fun `toggling a reaction sends it for that message`() = runViewModelTest(viewModels) {
        val viewModel = createViewModel()

        viewModel.onAction(ChatAction.OnMessageAction(MessageAction.ToggleReaction(messageId = "m2", reaction = "🍻")))

        assertThat(messageRepository.reactCalls.calls).containsExactly(FakeMessageRepository.ReactCall("m2", "🍻"))
    }

    @Test
    fun `shows the chat's messages newest first`() = runViewModelTest(viewModels) {
        messageRepository.messages.value = listOf(sentMessage, unreadMessage)
        val viewModel = createViewModel()

        val displayItems = viewModel.state.awaitMatching { it.displayItems.isNotEmpty() }.displayItems

        assertThat(displayItems.filterIsInstance<MessageDisplayItem.MessageItem>().map { it.message.id })
            .containsExactly("m2", "m1")
    }

    @Test
    fun `app resume marks the chat read up to the newest message when something is unread`() = runViewModelTest(viewModels) {
        messageRepository.messages.value = listOf(sentMessage, unreadMessage)
        val viewModel = createViewModel()
        viewModel.state.awaitMatching { it.displayItems.isNotEmpty() }

        AppLifecycleManager.notifyAppResumed()

        val call = messageRepository.markReadInBackgroundCalls.calls.single()
        assertThat(call.chatId).isEqualTo(FRIEND_ID)
        assertThat(call.isGroup).isFalse()
        assertThat(call.newestMessageId).isEqualTo("m2")
        assertThat(notifier.cancelledMessageIds.calls)
            .containsExactly(listOf(NotificationManager.NotiId.HexString(FRIEND_ID).asInt))

        // The same guard is passed every time, so the repository can skip what is already covered
        AppLifecycleManager.notifyAppResumed()
        assertThat(messageRepository.markReadInBackgroundCalls.calls[1].guard).isSameInstanceAs(call.guard)
    }

    @Test
    fun `app resume does not mark read when everything is read`() = runViewModelTest(viewModels) {
        messageRepository.messages.value = listOf(sentMessage, unreadMessage.copy(readByMe = true))
        val viewModel = createViewModel()
        viewModel.state.awaitMatching { it.displayItems.isNotEmpty() }

        AppLifecycleManager.notifyAppResumed()

        assertThat(messageRepository.markReadInBackgroundCalls.calls).isEmpty()
    }

    @Test
    fun `chat partner follows the friend's online state`() = runViewModelTest(viewModels) {
        userRepository.users.value = listOf(testUser(FRIEND_ID, name = "Anna"))
        val viewModel = createViewModel()

        val partner = viewModel.state.awaitMatching { it.chatPartner != null }.chatPartner
        assertThat(partner?.isOnline).isEqualTo(false)

        userRepository.onlineFriendIds.value = setOf(FRIEND_ID)

        assertThat(viewModel.state.awaitMatching { it.chatPartner?.isOnline == true }.chatPartner?.name)
            .isEqualTo("Anna")
    }

    @Test
    fun `a saved draft is put into the empty input`() = runViewModelTest(viewModels) {
        settingsRepository.drafts.value = mapOf((FRIEND_ID to false) to "half written")

        val viewModel = createViewModel()

        assertThat(viewModel.state.value.sendContent).isEqualTo(TextContent(TextFieldValue("half written")))
    }
}
