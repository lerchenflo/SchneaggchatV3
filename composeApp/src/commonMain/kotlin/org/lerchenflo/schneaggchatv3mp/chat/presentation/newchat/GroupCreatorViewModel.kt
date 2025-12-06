package org.lerchenflo.schneaggchatv3mp.chat.presentation.newchat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
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
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform
import org.lerchenflo.schneaggchatv3mp.app.GlobalViewModel
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.chat.domain.SelectedChat
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository

class GroupCreatorViewModel (
    private val appRepository: AppRepository,
    private val navigator: Navigator
): ViewModel() {
    val globalViewModel: GlobalViewModel = KoinPlatform.getKoin().get()

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
            list
                // Sich selber ussa filtern
                .filter { (it.id != SessionCache.getOwnIdValue() && !it.isGroup) }
                .sortedByDescending { it.lastmessage?.getSendDateAsLong() }
        }
        .flowOn(Dispatchers.Default)

    val groupCreatorState: StateFlow<List<SelectedChat>> = usersFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Companion.WhileSubscribed(5_000),
            initialValue = emptyList()
        )
    var groupCreatorStage by mutableStateOf(GroupCreatorStage.MEMBERSEL)

    val selectedUsers = mutableStateListOf<SelectedChat>()

    fun onBackClick(){
        viewModelScope.launch {
            navigator.navigateBack()
        }
    }

}

enum class GroupCreatorStage{
    MEMBERSEL,
    GROUPDETAILS
}