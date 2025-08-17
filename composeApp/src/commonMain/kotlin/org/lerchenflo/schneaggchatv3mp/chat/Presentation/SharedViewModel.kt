package org.lerchenflo.schneaggchatv3mp.chat.Presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.chat.domain.GetAllUserUseCase
import org.lerchenflo.schneaggchatv3mp.chat.domain.UpsertUserUseCase
import org.lerchenflo.schneaggchatv3mp.database.User

class SharedViewModel(
    private val upsertUserUseCase: UpsertUserUseCase,
    private val getAllUserUseCase: GetAllUserUseCase
):ViewModel() {

    // user getten und inserten
    val upsertUser = fun(user: User){
        viewModelScope.launch { upsertUserUseCase(user) }
    }

    fun getAllUsers(): Flow<List<User>> {
        return getAllUserUseCase()
    }




    //aktuell ausgewählter chat
    private val _selectedChat = MutableStateFlow<User?>(null)
    val selectedChat = _selectedChat.asStateFlow()

    fun onSelectChat(user: User?) {
        _selectedChat.value = user
    }

    fun onLeaveChat(){
        _selectedChat.value = null
    }
}