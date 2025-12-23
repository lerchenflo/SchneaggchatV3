package org.lerchenflo.schneaggchatv3mp.chat.presentation.newchat

import androidx.compose.runtime.mutableStateListOf
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
import org.koin.viewmodel.emptyState
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.chat.data.UserRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.SelectedChat
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils
import kotlin.emptyArray

class GroupCreatorViewModel (
    private val appRepository: AppRepository,
    private val navigator: Navigator
): ViewModel() {

    private val _searchTerm = MutableStateFlow("")
    val searchterm: StateFlow<String> = _searchTerm.asStateFlow()

    fun updateSearchterm(newValue: String) {
        _searchTerm.value = newValue
    }

    private val _groupName = MutableStateFlow("")
    val groupName: StateFlow<String> = _groupName.asStateFlow()

    fun updateGroupName(newValue: String) {
        _groupName.value = newValue
    }
    private val _groupDescription = MutableStateFlow("")
    val groupDescription: StateFlow<String> = _groupDescription.asStateFlow()

    fun updateGroupDescription(newValue: String) {
        _groupDescription.value = newValue
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val usersFlow: Flow<List<SelectedChat>> = _searchTerm
        .flatMapLatest { term ->
            appRepository.getChatSelectorFlow(term)
        }
        .map { list ->
            list.filter {
                //Only show friends
                it.friendshipStatus == NetworkUtils.FriendshipStatus.ACCEPTED
            }
        }
        .flowOn(Dispatchers.Default)

    val groupCreatorState: StateFlow<List<SelectedChat>> = usersFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val selectedUsers = mutableStateListOf<SelectedChat>()

    private val _profilePic = MutableStateFlow<ByteArray?>(null)
    val profilePic: StateFlow<ByteArray?> = _profilePic.asStateFlow()

    fun updateProfilepic(profilePicUri: GalleryPhotoResult) {
        CoroutineScope(Dispatchers.Default).launch{ //Use one core
            val bytearray = profilePicUri
                .loadBytes()

            _profilePic.value = bytearray
        }
    }


    fun onBackClick(){
        viewModelScope.launch {
            navigator.navigateBack()
        }
    }

    fun onCreateGroup() {

        //TODO: Check nulls

        viewModelScope.launch {
            val groupId = appRepository.createGroup(
                name = groupName.value,
                description = groupDescription.value,
                memberIds = selectedUsers.map { member ->
                    member.id
                },
                profilePic = profilePic.value!!
            )

            println("Group created: groupid: $groupId")

            //Launch sync
            CoroutineScope(Dispatchers.IO).launch {
                appRepository.dataSync()
            }

            //TODO: Navigate to chat (When new selectedchat implemented)
        }
    }

}
