package org.lerchenflo.schneaggchatv3mp.chatauswahl.Presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.chatauswahl.domain.GetAllUserUseCase
import org.lerchenflo.schneaggchatv3mp.chatauswahl.domain.UpsertUserUseCase
import org.lerchenflo.schneaggchatv3mp.database.User

class SharedViewModel(
    private val upsertUserUseCase: UpsertUserUseCase,
    private val getAllUserUseCase: GetAllUserUseCase,
    private val networkUtils: NetworkUtils
):ViewModel() {

    val login = fun(username: String, password: String){
        viewModelScope.launch {
            networkUtils.login(username, password)
                .onSuccess{value ->
                    println("Login:$value")
                }
        }
    }



    val upsertUser = fun(user: User){
        viewModelScope.launch { upsertUserUseCase(user) }
    }

    fun getAllUsers(): Flow<List<User>> {
        return getAllUserUseCase()
    }
}