package org.lerchenflo.schneaggchatv3mp.chat.Presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.mp.KoinPlatform.getKoin
import org.lerchenflo.schneaggchatv3mp.OWNID
import org.lerchenflo.schneaggchatv3mp.database.User
import kotlin.reflect.KClass

class ChatSelectorViewModel: ViewModel() {
    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
                return ChatSelectorViewModel() as T
            }
        }
    }


    val sharedViewModel: SharedViewModel = getKoin().get()

    init {
        sharedViewModel.executeuserandmsgidsync()
    }



    private val _searchTerm = MutableStateFlow("")
    val searchterm: StateFlow<String> = _searchTerm.asStateFlow()

    fun updateSearchterm(newValue: String) {
        _searchTerm.value = newValue
    }



    @OptIn(ExperimentalCoroutinesApi::class)
    val usersFlow: Flow<List<User>> = _searchTerm

        .flatMapLatest { term ->
            // getallusers should return Flow<List<User>>
            sharedViewModel.getusersWithLastMessage(searchterm.value)
        }
        .map { list ->

            list.filter { it.id != OWNID } //Sich sealb us da liste ussa
            //list.sortedBy { it.displayName?.lowercase() ?: "" }
        }
        .flowOn(Dispatchers.Default) //Im default despatcher für mehr cores -> Mehr hoaza

    // Expose as StateFlow so UI can collect easily and get a current value
    val usersState: StateFlow<List<User>> = usersFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

}