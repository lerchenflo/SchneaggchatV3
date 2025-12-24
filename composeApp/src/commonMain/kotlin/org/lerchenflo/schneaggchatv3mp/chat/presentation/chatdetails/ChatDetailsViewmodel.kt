package org.lerchenflo.schneaggchatv3mp.chat.presentation.chatdetails

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.app.GlobalViewModel
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.chat.data.GroupRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.NotSelected
import org.lerchenflo.schneaggchatv3mp.chat.domain.SelectedChat
import org.lerchenflo.schneaggchatv3mp.chat.domain.SelectedChatBase
import org.lerchenflo.schneaggchatv3mp.chat.domain.toGroup
import org.lerchenflo.schneaggchatv3mp.chat.domain.toUser
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository

class ChatDetailsViewmodel(
    private val groupRepository: GroupRepository,
    private val globalViewModel: GlobalViewModel,
    private val navigator: Navigator
) : ViewModel() {

    fun onBackClick(){
        viewModelScope.launch {
            navigator.navigateBack()
        }
    }

    private val _selectedChat = MutableStateFlow<SelectedChatBase>(NotSelected())
    val selectedChat: StateFlow<SelectedChatBase> = _selectedChat.asStateFlow()

    init {
        // Correctly collect from globalViewModel
        viewModelScope.launch {
            globalViewModel.selectedChat.collect { chat ->
                _selectedChat.value = chat

                //TODO Fabi: Wittamacha??
                //I hob o scho in selectedchat die values inegmacht:
                chat.toGroup()!!.groupMembers //Etc aber die groupmembers musch do setza sunsch wird z viel gschaffat in da gegnerauswahl
                if (chat.isGroup) {
                    println("Groupmembers: ${groupRepository.getGroupMembers(chat.id)}")
                }else {
                    println("Common groups: ${groupRepository.getCommonGroups(chat.id)}")
                }



            }
        }


    }


}