package org.lerchenflo.schneaggchatv3mp.chat.Presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.chat.domain.GetAllUserUseCase
import org.lerchenflo.schneaggchatv3mp.chat.domain.UpsertUserUseCase
import org.lerchenflo.schneaggchatv3mp.database.User

class SharedViewModel(
    private val upsertUserUseCase: UpsertUserUseCase,
    private val getAllUserUseCase: GetAllUserUseCase
):ViewModel() {

    val upsertUser = fun(user: User){
        viewModelScope.launch { upsertUserUseCase(user) }
    }

    fun getAllUsers(): Flow<List<User>> {
        return getAllUserUseCase()
    }
}