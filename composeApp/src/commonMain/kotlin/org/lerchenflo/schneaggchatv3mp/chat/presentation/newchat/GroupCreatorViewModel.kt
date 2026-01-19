@file:OptIn(ExperimentalCoroutinesApi::class)

package org.lerchenflo.schneaggchatv3mp.chat.presentation.newchat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.ismoy.imagepickerkmp.domain.extensions.loadBytes
import io.github.ismoy.imagepickerkmp.domain.models.GalleryPhotoResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.viewmodel.emptyState
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.chat.data.UserRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.Group
import org.lerchenflo.schneaggchatv3mp.chat.domain.SelectedChat
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils
import org.lerchenflo.schneaggchatv3mp.login.presentation.signup.InputfieldState
import org.lerchenflo.schneaggchatv3mp.login.presentation.signup.SignupState
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.name_too_long
import schneaggchatv3mp.composeapp.generated.resources.name_too_short
import kotlin.emptyArray

data class GroupCreatorState(
    val searchterm: String,

    val groupname: InputfieldState = InputfieldState(),
    val groupdescription: InputfieldState = InputfieldState(),

    val profilepic : ByteArray?,

    val availableUsers: List<SelectedChat>,
    val selectedUsers: List<SelectedChat>,

    val creationPermitted : Boolean

) {
    fun valid() : Boolean {
        return (selectedUsers.size >= 2 &&
            groupname.text.length > 2 &&
            groupname.text.length < 25 &&
                profilepic != null)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as GroupCreatorState

        if (creationPermitted != other.creationPermitted) return false
        if (searchterm != other.searchterm) return false
        if (groupname != other.groupname) return false
        if (groupdescription != other.groupdescription) return false
        if (!profilepic.contentEquals(other.profilepic)) return false
        if (availableUsers != other.availableUsers) return false
        if (selectedUsers != other.selectedUsers) return false

        return true
    }

    override fun hashCode(): Int {
        var result = creationPermitted.hashCode()
        result = 31 * result + searchterm.hashCode()
        result = 31 * result + groupname.hashCode()
        result = 31 * result + groupdescription.hashCode()
        result = 31 * result + (profilepic?.contentHashCode() ?: 0)
        result = 31 * result + availableUsers.hashCode()
        result = 31 * result + selectedUsers.hashCode()
        return result
    }
}


sealed interface GroupCreatorAction {
    data class updateSearchterm(val newValue: String) : GroupCreatorAction
    data class updateGroupname(val newValue: String) : GroupCreatorAction
    data class updateGroupdescription(val newValue: String) : GroupCreatorAction
    data class updateProfilePic(val profilePicUri: GalleryPhotoResult) : GroupCreatorAction

    data class addGroupMember(val member: SelectedChat) : GroupCreatorAction
    data class removeGroupMember(val member: SelectedChat) : GroupCreatorAction

    data object createGroup : GroupCreatorAction
    data object navigateBack: GroupCreatorAction


}

class GroupCreatorViewModel (
    private val appRepository: AppRepository,
    private val navigator: Navigator
): ViewModel() {

    var state by mutableStateOf(GroupCreatorState(
        searchterm = "",
        groupname = InputfieldState(),
        groupdescription = InputfieldState(),
        profilepic = null,
        availableUsers = emptyList(),
        selectedUsers = emptyList(),
        creationPermitted = false
    ))
        private set


    private val _searchTermFlow = MutableStateFlow("")


    init {
        viewModelScope.launch {
            _searchTermFlow
                .flatMapLatest { searchTerm ->
                    // This will cancel the previous flow and start a new one
                    // whenever searchTerm changes
                    appRepository.getChatSelectorFlow(searchTerm)
                }
                .map { chats ->
                    chats.filter { chat ->
                                !chat.isGroup
                    }
                }
                .collect { filteredUsers ->
                    state = state.copy(
                        availableUsers = filteredUsers
                    )
                }
        }
    }



    fun onAction(action: GroupCreatorAction) {
        viewModelScope.launch {
            when (action) {
                is GroupCreatorAction.updateGroupdescription -> {
                    state = state.copy(
                        groupdescription = state.groupdescription.copy(
                            text = action.newValue,
                        )
                    )
                }
                is GroupCreatorAction.updateGroupname -> {
                    val newtext = action.newValue
                    state = state.copy(
                        groupname = state.groupname.copy(
                            text = newtext,
                            errorMessage = if (newtext.length > 25) getString(Res.string.name_too_long) else if (newtext.length < 3) getString(Res.string.name_too_short) else null
                        )
                    )
                }
                is GroupCreatorAction.updateSearchterm -> {
                    state = state.copy(
                        searchterm = action.newValue
                    )
                    _searchTermFlow.value = action.newValue
                }
                is GroupCreatorAction.updateProfilePic -> {
                    val bytearray = action.profilePicUri
                        .loadBytes()

                    state = state.copy(
                        profilepic = bytearray
                    )
                }

                is GroupCreatorAction.addGroupMember -> {
                    state = state.copy(
                        selectedUsers = state.selectedUsers + action.member
                    )
                }
                is GroupCreatorAction.removeGroupMember -> {
                    state = state.copy(
                        selectedUsers = state.selectedUsers - action.member
                    )
                }
                GroupCreatorAction.createGroup -> {
                    onCreateGroup()
                }

                GroupCreatorAction.navigateBack -> {
                    onBackClick()
                }
            }


            //Validation
            state = state.copy(
                creationPermitted = state.valid()
            )

        }
    }


    fun onCreateGroup() {

        if (!state.valid()) return

        viewModelScope.launch {
            val groupId = appRepository.createGroup(
                name = state.groupname.text,
                description = state.groupdescription.text,
                memberIds = state.selectedUsers.map { member ->
                    member.id
                },
                profilePic = state.profilepic!!
            )

            //Group creation not failed
            if (groupId != null) {
                println("Group created: groupid: $groupId")

                //Launch sync
                CoroutineScope(Dispatchers.IO).launch {
                    appRepository.dataSync()
                }

                //TODO: Navigate directly to group chat?
                navigator.navigate(Route.ChatSelector, exitPreviousScreen = true)
            }


        }
    }


    fun onBackClick(){
        viewModelScope.launch {
            navigator.navigateBack()
        }
    }

}
