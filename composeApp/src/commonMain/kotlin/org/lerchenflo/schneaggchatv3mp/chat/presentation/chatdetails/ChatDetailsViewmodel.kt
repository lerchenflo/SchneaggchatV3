@file:OptIn(ExperimentalCoroutinesApi::class)

package org.lerchenflo.schneaggchatv3mp.chat.presentation.chatdetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
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


data class GroupMemberWithUser(
    val groupMember: GroupMember,
    val user: User?
)

class ChatDetailsViewmodel(
    private val groupRepository: GroupRepository,
    private val globalViewModel: GlobalViewModel,
    private val navigator: Navigator,
    private val userRepository: UserRepository
) : ViewModel() {

    fun onBackClick(){
        viewModelScope.launch {
            navigator.navigateBack()
        }
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
                    flow {
                        val membersWithUsers = getGroupMembersWithUsers(chat.id)
                        emit(chat.toGroup()!!.copy(
                            groupMembersWithUsers = membersWithUsers
                        ))
                    }
                }
                !chat.isNotSelected() -> {
                    // Fetch common groups and create enriched UserChat
                    flow {
                        val commonGroups = groupRepository.getCommonGroups(chat.id)
                        emit(chat.toUser()!!.copy(commonGroups = commonGroups))
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

    fun navigateToChat(user: User){
        viewModelScope.launch {
            globalViewModel.onSelectChat(user.toSelectedChat(
                unreadCount = 0,
                unsentCount = 0,
                lastMessage = null
            ))
            // Exit all previous screen weil ma jo da selectedgegner im globalviewmodel gändert hot und denn ind chatdetails vo deam typ kummt
            navigator.navigate(Route.Chat, exitPreviousScreen = true) // todo ma kummt nur ind chatauswahl mit 2 mol zruck
        }
    }


}