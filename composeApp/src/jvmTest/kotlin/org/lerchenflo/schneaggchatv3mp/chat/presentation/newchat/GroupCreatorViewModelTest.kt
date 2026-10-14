package org.lerchenflo.schneaggchatv3mp.chat.presentation.newchat

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import assertk.assertions.single
import io.github.ismoy.imagepickerkmp.picker.GalleryPhotoResult
import org.lerchenflo.schneaggchatv3mp.app.navigation.NavigationAction
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chatselector.ChatFilter
import org.lerchenflo.schneaggchatv3mp.fakes.FakeChatListRepository
import org.lerchenflo.schneaggchatv3mp.fakes.FakeChatListRepository.SelectorRequest
import org.lerchenflo.schneaggchatv3mp.fakes.FakeDataSyncCoordinator
import org.lerchenflo.schneaggchatv3mp.fakes.FakeGroupRepository
import org.lerchenflo.schneaggchatv3mp.fakes.FakePictureManager
import org.lerchenflo.schneaggchatv3mp.login.presentation.signup.InputfieldState
import org.lerchenflo.schneaggchatv3mp.testutil.MainDispatcherRule
import org.lerchenflo.schneaggchatv3mp.testutil.TestViewModelStore
import org.lerchenflo.schneaggchatv3mp.testutil.loginTestUser
import org.lerchenflo.schneaggchatv3mp.testutil.resetGlobalState
import org.lerchenflo.schneaggchatv3mp.testutil.runViewModelTest
import org.lerchenflo.schneaggchatv3mp.testutil.testChatListItem
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

private const val OWN_ID = "me"

/**
 * Member list, validation and group creation of the group creator.
 *
 * In `jvmTest`, not `commonTest`: picking the group picture goes through the image picker's
 * platform `GalleryPhotoResult.loadBytes()`. On the JVM it returns an empty array for a file that
 * does not exist, which is enough here because [FakePictureManager] hands back its own bytes.
 */
class GroupCreatorViewModelTest {

    private val mainDispatcher = MainDispatcherRule()
    private val viewModels = TestViewModelStore()

    private val chatListRepository = FakeChatListRepository()
    private val groupRepository = FakeGroupRepository()
    private val dataSyncCoordinator = FakeDataSyncCoordinator()
    private val pictureManager = FakePictureManager().apply { downscaledImage = byteArrayOf(1, 2, 3) }
    private val navigator = Navigator()

    private val anna = testChatListItem("anna", name = "Anna")
    private val ben = testChatListItem("ben", name = "Ben")
    private val existingGroup = testChatListItem("hiking", isGroup = true, name = "Hiking")

    @BeforeTest
    fun setUp() {
        resetGlobalState()
        mainDispatcher.setUp()
        loginTestUser(OWN_ID)
        chatListRepository.chats.value = listOf(existingGroup, anna, ben)
    }

    @AfterTest
    fun tearDown() {
        mainDispatcher.tearDown()
        resetGlobalState()
    }

    private fun createViewModel() = viewModels.add(
        GroupCreatorViewModel(
            dataSyncCoordinator = dataSyncCoordinator,
            chatListRepository = chatListRepository,
            groupRepository = groupRepository,
            pictureManager = pictureManager,
            navigator = navigator,
        )
    )

    /** Two members, a valid name and a picture: everything [GroupCreatorState.valid] asks for. */
    private fun GroupCreatorViewModel.fillValidGroup() {
        onAction(GroupCreatorAction.updateGroupname("Climbing"))
        onAction(GroupCreatorAction.updateGroupdescription("Tuesdays"))
        onAction(GroupCreatorAction.addGroupMember(anna))
        onAction(GroupCreatorAction.addGroupMember(ben))
        onAction(GroupCreatorAction.updateProfilePic(GalleryPhotoResult(uri = "file:///no/such/picture.jpg")))
    }

    @Test
    fun `offers only users, never groups, and passes the search term on`() = runViewModelTest(viewModels) {
        val viewModel = createViewModel()

        assertThat(viewModel.state.availableUsers).containsExactly(anna, ben)

        viewModel.onAction(GroupCreatorAction.updateSearchterm("an"))

        assertThat(viewModel.state.availableUsers).containsExactly(anna)
        assertThat(chatListRepository.selectorRequests.calls.last())
            .isEqualTo(SelectorRequest(searchTerm = "an", userId = OWN_ID, filter = ChatFilter.NONE))
    }

    @Test
    fun `creation is permitted only with two members, a valid name and a picture`() = runViewModelTest(viewModels) {
        val viewModel = createViewModel()

        viewModel.onAction(GroupCreatorAction.updateGroupname("Climbing"))
        viewModel.onAction(GroupCreatorAction.addGroupMember(anna))
        viewModel.onAction(GroupCreatorAction.addGroupMember(ben))
        assertThat(viewModel.state.creationPermitted).isFalse() // no picture yet

        viewModel.onAction(GroupCreatorAction.updateProfilePic(GalleryPhotoResult(uri = "file:///no/such/picture.jpg")))
        assertThat(viewModel.state.creationPermitted).isTrue()

        viewModel.onAction(GroupCreatorAction.removeGroupMember(ben))
        assertThat(viewModel.state.creationPermitted).isFalse() // one member left
    }

    /**
     * The name rule, on the state directly: an invalid name's error text is loaded with a
     * suspending `getString`, so through the ViewModel the check would race the resource read.
     * Pins that 25 characters are not allowed although the error text only appears above
     * `MAX_GROUPNAME_LENGTH` (25), see `plans/Todo.md`.
     */
    @Test
    fun `a valid state needs a name of 3 to 24 characters`() {
        fun stateWithName(name: String) = GroupCreatorState(
            searchterm = "",
            groupname = InputfieldState(text = name),
            profilepic = byteArrayOf(1),
            availableUsers = emptyList(),
            selectedUsers = listOf(anna, ben),
            creationPermitted = false,
        )

        assertThat(stateWithName("Cl").valid()).isFalse()
        assertThat(stateWithName("Cli").valid()).isTrue()
        assertThat(stateWithName("x".repeat(24)).valid()).isTrue()
        assertThat(stateWithName("x".repeat(25)).valid()).isFalse()
    }

    @Test
    fun `creating sends the group, syncs and opens the new group chat`() = runViewModelTest(viewModels) {
        groupRepository.createdGroupId = "new-group"
        val viewModel = createViewModel()
        viewModel.fillValidGroup()

        navigator.navigationActions.test {
            viewModel.onAction(GroupCreatorAction.createGroup)

            assertThat(awaitItem()).isEqualTo(
                NavigationAction.Navigate(
                    Route.Chat(chatId = "new-group", isGroup = true),
                    Navigator.NavigationOptions(removeAllExceptByRoute = Route.ChatSelector),
                )
            )
        }

        val created = groupRepository.createCalls.calls.single()
        assertThat(created.name).isEqualTo("Climbing")
        assertThat(created.description).isEqualTo("Tuesdays")
        assertThat(created.memberIds).containsExactly("anna", "ben")
        assertThat(created.profilePic.toList()).containsExactly(1.toByte(), 2.toByte(), 3.toByte())
        assertThat(dataSyncCoordinator.syncReasons.calls).containsExactly("groupCreated")
        assertThat(viewModel.state.isLoading).isFalse()
    }

    @Test
    fun `a failed creation neither syncs nor navigates`() = runViewModelTest(viewModels) {
        groupRepository.createdGroupId = null
        val viewModel = createViewModel()
        viewModel.fillValidGroup()

        navigator.navigationActions.test {
            viewModel.onAction(GroupCreatorAction.createGroup)

            expectNoEvents()
        }

        assertThat(groupRepository.createCalls.calls).single()
        assertThat(dataSyncCoordinator.syncReasons.calls).isEmpty()
        assertThat(viewModel.state.isLoading).isFalse()
    }

    @Test
    fun `an invalid group is not sent`() = runViewModelTest(viewModels) {
        val viewModel = createViewModel()
        viewModel.onAction(GroupCreatorAction.updateGroupname("Climbing"))

        viewModel.onAction(GroupCreatorAction.createGroup)

        assertThat(groupRepository.createCalls.calls).isEmpty()
    }
}
