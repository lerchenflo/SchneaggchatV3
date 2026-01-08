@file:OptIn(ExperimentalCoroutinesApi::class)

package org.lerchenflo.schneaggchatv3mp.chat.presentation.chatdetails

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.app.GlobalViewModel
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.chat.data.GroupRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.UserRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.GroupMember
import org.lerchenflo.schneaggchatv3mp.chat.domain.NotSelected
import org.lerchenflo.schneaggchatv3mp.chat.domain.SelectedChatBase
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.chat.domain.isNotSelected
import org.lerchenflo.schneaggchatv3mp.chat.domain.toGroup
import org.lerchenflo.schneaggchatv3mp.chat.domain.toSelectedChat
import org.lerchenflo.schneaggchatv3mp.chat.domain.toUser
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils


data class GroupMemberWithUser(
    val groupMember: GroupMember,
    val user: User?
)

class ChatDetailsViewmodel(
    private val groupRepository: GroupRepository,
    private val globalViewModel: GlobalViewModel,
    private val navigator: Navigator,
    private val userRepository: UserRepository,
    private val appRepository: AppRepository
) : ViewModel() {

    fun onBackClick(){
        viewModelScope.launch {
            navigator.navigateBack()
        }
    }

    var descriptionText by mutableStateOf(TextFieldValue(""))
        private set
    fun updateDescriptionText(newValue: TextFieldValue) {
        descriptionText = newValue
    }

    /**
     * Enriched chat details with group members or common groups populated.
     * This flow automatically transforms the selected chat by fetching and populating:
     * - For groups: groupMembers list
     * - For users: commonGroups list
     */
    val chatDetails: StateFlow<SelectedChatBase> = globalViewModel.selectedChat
        .flatMapLatest { chat ->
            when {
                chat.isGroup -> {
                    groupRepository.getGroupFlow(chat.id).map { updatedGroup ->
                        val membersWithUsers = getGroupMembersWithUsers(chat.id)
                        updatedGroup?.toSelectedChat(
                            unreadCount = chat.unreadMessageCount,
                            unsentCount = chat.unsentMessageCount,
                            lastMessage = chat.lastmessage
                        )?.toGroup()?.copy(
                            groupMembersWithUsers = membersWithUsers
                        ) ?: chat
                    }
                }
                !chat.isNotSelected() -> {
                    userRepository.getUserFlow(chat.id).map { updatedUser ->
                        val commonGroups = groupRepository.getCommonGroups(chat.id)
                        updatedUser?.toSelectedChat(
                            unreadCount = chat.unreadMessageCount,
                            unsentCount = chat.unsentMessageCount,
                            lastMessage = chat.lastmessage
                        )?.toUser()?.copy(commonGroups = commonGroups) ?: chat
                    }
                }
                else -> flow { emit(chat) } // NotSelected - pass through
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = NotSelected()
        )


    private suspend fun getGroupMembersWithUsers(groupId: String): List<GroupMemberWithUser> {
        val members = groupRepository.getGroupMembers(groupId)
        return members.mapNotNull { member ->
            val user = userRepository.getUserById(member.userId)
            if (user != null) {
                GroupMemberWithUser(member, user)
            } else {
                // todo was tuat ma mit lüt in ana gruppe wo ned in da userdatenbank sind?
                GroupMemberWithUser(member, null)
            }
        }
    }

    fun navigateToChat(selectedChat: SelectedChatBase){
        viewModelScope.launch {
            globalViewModel.onSelectChat(selectedChat)
            // Exit all previous screen weil ma jo da selectedgegner im globalviewmodel gändert hot und denn ind chatdetails vo deam typ kummt
            navigator.navigate(Route.Chat, exitPreviousScreen = true) // todo ma kummt nur ind chatauswahl mit 2 mol zruck
        }
    }

    fun updateDescription(selectedChat: SelectedChatBase){
        viewModelScope.launch {
            if (selectedChat.isGroup) {
                appRepository.changeGroupDescription(selectedChat.id, descriptionText.text)
            }else {
                appRepository.changeUserStatusDescription(null, descriptionText.text, selectedChat.id)

            }
        }
    }

    fun removeFriend() {
        if (!chatDetails.value.isGroup) {
            viewModelScope.launch {
                if(appRepository.removeFriend(chatDetails.value.id)){
                    navigator.navigate(Route.ChatSelector, exitAllPreviousScreens = true)
                }
            }
        }
    }

    fun changeAdminStatus(member: GroupMember){
        viewModelScope.launch{
            appRepository.changeGroupMembers(
                action = if(member.admin) NetworkUtils.GroupMemberAction.REMOVE_ADMIN else NetworkUtils.GroupMemberAction.MAKE_ADMIN,
                memberId = member.userId,
                groupId = member.groupId
            )
        }
    }

    fun addMember(userId: String){
        viewModelScope.launch {
            appRepository.changeGroupMembers(
                action = NetworkUtils.GroupMemberAction.ADD_USER,
                memberId = userId,
                groupId = chatDetails.value.id
            )
        }
    }

    fun removeMember(memberId: String){
        viewModelScope.launch {
            appRepository.changeGroupMembers(
                action = NetworkUtils.GroupMemberAction.REMOVE_USER,
                memberId = memberId,
                groupId = chatDetails.value.id
            )
        }
    }

    fun navigateChatSelExitAllPrevious(){
        viewModelScope.launch {
            navigator.navigate(Route.ChatSelector, exitAllPreviousScreens = true)
        }
    }


}