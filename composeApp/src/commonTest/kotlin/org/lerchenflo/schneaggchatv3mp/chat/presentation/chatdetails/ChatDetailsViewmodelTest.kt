package org.lerchenflo.schneaggchatv3mp.chat.presentation.chatdetails

import androidx.compose.ui.text.input.TextFieldValue
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isEmpty
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import assertk.assertions.prop
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.lerchenflo.schneaggchatv3mp.app.navigation.NavigationAction
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.chat.domain.toChatListItem
import org.lerchenflo.schneaggchatv3mp.chat.domain.FriendshipStatus
import org.lerchenflo.schneaggchatv3mp.chat.domain.GroupMemberAction
import org.lerchenflo.schneaggchatv3mp.fakes.FakeEventRepository
import org.lerchenflo.schneaggchatv3mp.fakes.FakeGroupRepository
import org.lerchenflo.schneaggchatv3mp.fakes.FakeGroupRepository.MemberCall
import org.lerchenflo.schneaggchatv3mp.fakes.FakeMessageRepository
import org.lerchenflo.schneaggchatv3mp.fakes.FakePictureManager
import org.lerchenflo.schneaggchatv3mp.fakes.FakeUserRepository
import org.lerchenflo.schneaggchatv3mp.fakes.FakeUserRepository.DetailsCall
import org.lerchenflo.schneaggchatv3mp.fakes.FakeUserRepository.WakeCall
import org.lerchenflo.schneaggchatv3mp.testutil.MainDispatcherRule
import org.lerchenflo.schneaggchatv3mp.testutil.TestViewModelStore
import org.lerchenflo.schneaggchatv3mp.testutil.awaitMatching
import org.lerchenflo.schneaggchatv3mp.testutil.loginTestUser
import org.lerchenflo.schneaggchatv3mp.testutil.resetGlobalState
import org.lerchenflo.schneaggchatv3mp.testutil.runViewModelTest
import org.lerchenflo.schneaggchatv3mp.testutil.testGroup
import org.lerchenflo.schneaggchatv3mp.testutil.testUser
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

private const val OWN_ID = "me"
private const val GROUP_ID = "hiking"
private const val FRIEND_ID = "anna"

/** Group edits (name, description, members, expiry) and friend actions of the chat details screen. */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatDetailsViewmodelTest {

    private val mainDispatcher = MainDispatcherRule()
    private val viewModels = TestViewModelStore()

    private val userRepository = FakeUserRepository()
    private val groupRepository = FakeGroupRepository()
    private val navigator = Navigator()

    private val me = testUser(OWN_ID, friendshipStatus = null)
    private val anna = testUser(FRIEND_ID, name = "Anna")
    private val ben = testUser("ben", name = "Ben")
    private val carla = testUser("carla", name = "Carla")
    private val pendingDave = testUser("dave", name = "Dave", friendshipStatus = FriendshipStatus.PENDING)

    private val hiking = testGroup(GROUP_ID, name = "Hiking", memberIds = listOf(OWN_ID, FRIEND_ID, "left-user"), adminIds = listOf(OWN_ID))
    private val climbing = testGroup("climbing", name = "Climbing", memberIds = listOf(OWN_ID, "ben", "carla"))

    @BeforeTest
    fun setUp() {
        resetGlobalState()
        mainDispatcher.setUp()
        loginTestUser(OWN_ID)
        userRepository.users.value = listOf(me, anna, ben, carla, pendingDave)
        groupRepository.groups.value = listOf(hiking, climbing)
    }

    @AfterTest
    fun tearDown() {
        mainDispatcher.tearDown()
        resetGlobalState()
    }

    private fun createViewModel(chatId: String, isGroup: Boolean) = viewModels.add(
        ChatDetailsViewmodel(
            groupRepository = groupRepository,
            navigator = navigator,
            userRepository = userRepository,
            pictureManager = FakePictureManager(),
            eventRepository = FakeEventRepository(),
            messageRepository = FakeMessageRepository(),
            chatId = chatId,
            isGroup = isGroup,
        )
    )

    // ─── Groups ───────────────────────────────────────────────────────────────

    @Test
    fun `group details resolve each member to its stored user`() = runViewModelTest(viewModels) {
        val viewModel = createViewModel(GROUP_ID, isGroup = true)

        val details = viewModel.chatDetails.awaitMatching { it is ChatDetailsState.GroupDetails }

        assertThat(details).isInstanceOf<ChatDetailsState.GroupDetails>()
            .prop(ChatDetailsState.GroupDetails::members)
            .transform { members -> members.map { it.groupMember.userId to it.user?.id } }
            .containsExactly(OWN_ID to OWN_ID, FRIEND_ID to FRIEND_ID, "left-user" to null)
    }

    @Test
    fun `name and description and expiry changes go to the group`() = runViewModelTest(viewModels) {
        val viewModel = createViewModel(GROUP_ID, isGroup = true)

        viewModel.updateGroupName("Hiking club")
        viewModel.updateDescriptionText(TextFieldValue("Every Sunday"))
        viewModel.updateDescription()
        viewModel.setGroupExpiry(1_800_000_000_000L)
        viewModel.setGroupExpiry(null)

        assertThat(groupRepository.nameChanges.calls).containsExactly(GROUP_ID to "Hiking club")
        assertThat(groupRepository.descriptionChanges.calls).containsExactly(GROUP_ID to "Every Sunday")
        assertThat(groupRepository.expiryChanges.calls).containsExactly(GROUP_ID to 1_800_000_000_000L, GROUP_ID to null)
        assertThat(userRepository.detailsChanges.calls).isEmpty()
    }

    @Test
    fun `member actions add and remove members and toggle the admin flag`() = runViewModelTest(viewModels) {
        val viewModel = createViewModel(GROUP_ID, isGroup = true)
        val (ownMember, annaMember) = hiking.members

        viewModel.addMember("ben")
        viewModel.removeMember(FRIEND_ID)
        viewModel.changeAdminStatus(annaMember) // not admin yet
        viewModel.changeAdminStatus(ownMember) // admin

        assertThat(groupRepository.memberChanges.calls).containsExactly(
            MemberCall(GroupMemberAction.ADD_USER, "ben", GROUP_ID),
            MemberCall(GroupMemberAction.REMOVE_USER, FRIEND_ID, GROUP_ID),
            MemberCall(GroupMemberAction.MAKE_ADMIN, FRIEND_ID, GROUP_ID),
            MemberCall(GroupMemberAction.REMOVE_ADMIN, OWN_ID, GROUP_ID),
        )
    }

    @Test
    fun `member picker offers friends outside the group and other groups and a group adds its friends`() =
        runViewModelTest(viewModels) {
            val viewModel = createViewModel(GROUP_ID, isGroup = true)
            // The screen collects the picker list while it is open
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.availableNewMembers.collect {} }
            val climbingItem = climbing.toChatListItem()

            val available = viewModel.availableNewMembers.awaitMatching { it.isNotEmpty() }
            assertThat(available.map { it.id }).containsExactly("climbing", "ben", "carla")

            viewModel.onUserSelected(climbingItem)

            assertThat(viewModel.selectedNewMembers.map { it.id }).containsExactly("ben", "carla")
            assertThat(viewModel.isItemSelected(climbingItem)).isTrue()

            viewModel.onUserDeSelected(climbingItem)

            assertThat(viewModel.selectedNewMembers).isEmpty()
            assertThat(viewModel.isItemSelected(climbingItem)).isFalse()
        }

    // ─── Friends ──────────────────────────────────────────────────────────────

    @Test
    fun `user details show the groups shared with that friend`() = runViewModelTest(viewModels) {
        groupRepository.groups.value = listOf(hiking, climbing)
        val viewModel = createViewModel(FRIEND_ID, isGroup = false)

        val details = viewModel.chatDetails.awaitMatching { it is ChatDetailsState.UserDetails }

        assertThat(details).isInstanceOf<ChatDetailsState.UserDetails>()
            .transform { it.commonGroups.map { group -> group.id } }
            .containsExactly(GROUP_ID)
    }

    @Test
    fun `removing a friend returns to the chat selector`() = runViewModelTest(viewModels) {
        val viewModel = createViewModel(FRIEND_ID, isGroup = false)

        navigator.navigationActions.test {
            viewModel.removeFriend()

            assertThat(awaitItem()).isEqualTo(
                NavigationAction.Navigate(Route.ChatSelector, Navigator.NavigationOptions(exitAllPreviousScreens = true))
            )
        }
        assertThat(userRepository.friendsRemoved.calls).containsExactly(FRIEND_ID)
    }

    @Test
    fun `a failed friend removal stays on the screen`() = runViewModelTest(viewModels) {
        userRepository.writeResult = false
        val viewModel = createViewModel(FRIEND_ID, isGroup = false)

        navigator.navigationActions.test {
            viewModel.removeFriend()

            expectNoEvents()
        }
        assertThat(userRepository.friendsRemoved.calls).containsExactly(FRIEND_ID)
    }

    @Test
    fun `nickname and description of a friend are saved as that friend's details`() = runViewModelTest(viewModels) {
        val viewModel = createViewModel(FRIEND_ID, isGroup = false)

        viewModel.updateNickname("Annie")
        viewModel.updateDescriptionText(TextFieldValue("Met at the lake"))
        viewModel.updateDescription()

        assertThat(userRepository.detailsChanges.calls).containsExactly(
            DetailsCall(FRIEND_ID, null, null, null, null, newNickName = "Annie", newPhoneNumber = null),
            DetailsCall(FRIEND_ID, null, newDescription = "Met at the lake", null, null, null, null),
        )
        assertThat(groupRepository.descriptionChanges.calls).isEmpty()
    }

    @Test
    fun `sending a friend request from a group member reaches the repository`() = runViewModelTest(viewModels) {
        val viewModel = createViewModel(GROUP_ID, isGroup = true)

        viewModel.sendFriendRequest("left-user")

        // Runs on Dispatchers.IO, not in viewModelScope
        assertThat(userRepository.friendRequestsSent.awaitCount(1)).containsExactly("left-user")
    }

    @Test
    fun `wake targets this chat with the given reason`() = runViewModelTest(viewModels) {
        val viewModel = createViewModel(GROUP_ID, isGroup = true)

        viewModel.sendWake("Lunch!")

        assertThat(userRepository.wakeCalls.calls).containsExactly(WakeCall(GROUP_ID, isGroup = true, reason = "Lunch!"))
    }
}
