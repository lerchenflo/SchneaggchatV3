@file:OptIn(FlowPreview::class)

package org.lerchenflo.schneaggchatv3mp.chat.presentation.newchat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.IO
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform
import org.lerchenflo.schneaggchatv3mp.app.GlobalViewModel
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.chat.domain.SelectedChat
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager

class NewChatViewModel (
    private val appRepository: AppRepository,
    private val navigator: Navigator
): ViewModel() {

    private val _searchTerm = MutableStateFlow("")
    val searchterm: StateFlow<String> = _searchTerm.asStateFlow()

    fun updateSearchterm(newValue: String) {
        _searchTerm.value = newValue
    }

    private val _availableChats = MutableStateFlow<List<NetworkUtils.NewFriendsUserResponse>>(emptyList())
    val availableChats: StateFlow<List<NetworkUtils.NewFriendsUserResponse>> = _availableChats.asStateFlow()

    init {
        // Kombiniere searchTerm mit einem Flow der die Daten lädt
        viewModelScope.launch {
            searchterm
                .debounce(300) // Warte 300ms nach letzter Eingabe
                .distinctUntilChanged() // Nur wenn sich der Wert ändert
                .collectLatest { term ->
                    loadAvailableUsers(term)
                }
        }
    }

    private suspend fun loadAvailableUsers(searchTerm: String) {
        try {
            val response = appRepository.getAvailableUsers(searchTerm)
            println("reloading users: $response")
            _availableChats.value = response
                .sortedByDescending { it.commonFriendCount }
        } catch (e: Exception) {
            // Handle error
            e.printStackTrace()
            _availableChats.value = emptyList()
        }
    }

    fun addFriend(friendId: String) {
        CoroutineScope(Dispatchers.IO).launch { //Launch in coroutinescope to not access the db on main thread
            val success = appRepository.sendFriendRequest(friendId)
            SnackbarManager.showMessage(
                if (success) "✔" else "Error" //TODO: Fabi bitte fixen i hobs ned im griff
            )
        }
    }


    fun onBackClick() {
        viewModelScope.launch {
            navigator.navigateBack()
        }
    }

    fun onGroupCreatorClick() {
        viewModelScope.launch {
            navigator.navigate(Route.GroupCreator)
        }
    }


}

