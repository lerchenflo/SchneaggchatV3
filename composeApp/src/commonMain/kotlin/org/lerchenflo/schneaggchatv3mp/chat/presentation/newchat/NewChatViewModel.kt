package org.lerchenflo.schneaggchatv3mp.chat.presentation.newchat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import org.koin.mp.KoinPlatform
import org.lerchenflo.schneaggchatv3mp.app.GlobalViewModel
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.chat.domain.SelectedChat
import org.lerchenflo.schneaggchatv3mp.database.AppRepository

class NewChatViewModel (
    private val appRepository: AppRepository,
): ViewModel() {
    val globalViewModel: GlobalViewModel = KoinPlatform.getKoin().get()

    private val _searchTerm = MutableStateFlow("")
    val searchterm: StateFlow<String> = _searchTerm.asStateFlow()

    fun updateSearchterm(newValue: String) {
        _searchTerm.value = newValue
    }

    // Todo Backend info für neue User vom Server hola und iwie sortiera
    /*
    I will dass nur lüt mit 1 oder mehr gemeinsame Freunde azoagt wörrend solang da searchterm "" isch.
    Wenn ma was suacht wörrend denn halt die ergebnisse azoagt wo am besta zuatreffend
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val newChatsFlow: Flow<List<SelectedChat>> = _searchTerm
        .flatMapLatest { term ->
            appRepository.getChatSelectorFlow(term)
        }
        .map { list ->
            list
                // Sich selber ussa filtern
                .filter { (it.id != SessionCache.getOwnIdValue() && !it.isGroup) }
                // todo nach anzahl gemeinsamer Freunde sortieren
                .sortedByDescending { it.lastmessage?.getSendDateAsLong() }
        }
        .flowOn(Dispatchers.Default)

    val newChatState: StateFlow<List<SelectedChat>> = newChatsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Companion.WhileSubscribed(5_000),
            initialValue = emptyList()
        )




    // GroupCreator
    @OptIn(ExperimentalCoroutinesApi::class)
    val usersFlow: Flow<List<SelectedChat>> = _searchTerm
        .flatMapLatest { term ->
            appRepository.getChatSelectorFlow(term)
        }
        .map { list ->
            list
                // Sich selber ussa filtern
                .filter { (it.id != SessionCache.getOwnIdValue() && !it.isGroup) }
                // todo nach anzahl gemeinsamer Freunde sortieren
                .sortedByDescending { it.lastmessage?.getSendDateAsLong() }
        }
        .flowOn(Dispatchers.Default)

    val groupCreatorState: StateFlow<List<SelectedChat>> = newChatsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Companion.WhileSubscribed(5_000),
            initialValue = emptyList()
        )
    var groupCreatorStage by mutableStateOf(GroupCreatorStage.MEMBERSEL)

    val selectedUsers = mutableStateListOf<SelectedChat>()



}

enum class GroupCreatorStage{
    MEMBERSEL,
    GROUPDETAILS
}