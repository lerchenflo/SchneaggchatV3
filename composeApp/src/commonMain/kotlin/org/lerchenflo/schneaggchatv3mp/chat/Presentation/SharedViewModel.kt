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
import org.lerchenflo.schneaggchatv3mp.network.NetworkUtils
import org.lerchenflo.schneaggchatv3mp.network.util.onError
import org.lerchenflo.schneaggchatv3mp.network.util.onSuccess
import org.lerchenflo.schneaggchatv3mp.network.util.onSuccessWithBody

class SharedViewModel(
    private val upsertUserUseCase: UpsertUserUseCase,
    private val getAllUserUseCase: GetAllUserUseCase,
    private val networkUtils: NetworkUtils

):ViewModel() {

    val login = fun(username: String, password: String){
        viewModelScope.launch {
            networkUtils.login(username, password)
                .onSuccessWithBody { bool, string ->
                    println("Login: $bool mit nachricht $string")
                }
                .onError {
                    println(it.toString())
                }
        }
    }



    // user getten und inserten
    val upsertUser = fun(user: User){
        viewModelScope.launch { upsertUserUseCase(user) }
    }

    fun getAllUsers(searchterm: String = ""): Flow<List<User>> {
        return getAllUserUseCase(searchterm)
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