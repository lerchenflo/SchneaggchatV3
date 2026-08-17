package org.lerchenflo.schneaggchatv3mp.games.presentation.PlayerSelector

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.games.data.PlayerEntity
import org.lerchenflo.schneaggchatv3mp.games.data.PlayerRepository

class PlayerSelectorViewModel(
    private val playerRepository: PlayerRepository,
    private val appRepository: AppRepository
) : ViewModel() {

    private val _localPlayers = mutableStateListOf<PlayerEntity>()
    val localPlayers: List<PlayerEntity> get() = _localPlayers

    private val _friends = mutableStateListOf<User>()
    val friends: List<User> get() = _friends

    // Combined list of both local players and friends
    val allPlayers: List<Any> get() = _localPlayers + _friends

    private val _selectedPlayers = mutableStateListOf<Any>()
    val selectedPlayers: List<Any> get() = _selectedPlayers

    init {
        viewModelScope.launch {
            // Load local players
            playerRepository.getAllPlayersFlow().collectLatest { playerList ->
                _localPlayers.clear()
                _localPlayers.addAll(playerList)
            }
        }

        viewModelScope.launch {
            // Load friends and current user
            val ownId = SessionCache.requireLoggedIn()?.userId
            val ownUserFlow = if (ownId != null) appRepository.getUserByIdFlow(ownId) else flowOf(null)
            val friendsFlow = appRepository.getFriendsFlow("")

            ownUserFlow.combine(friendsFlow) { ownUser, friendsList ->
                val list = mutableListOf<User>()
                ownUser?.let { list.add(it) }
                list.addAll(friendsList)
                list
            }.collectLatest { combinedList ->
                _friends.clear()
                _friends.addAll(combinedList)
            }
        }
    }

    fun addPlayer(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            playerRepository.upsertPlayer(PlayerEntity(name = name.trim()))
        }
    }

    fun deletePlayer(player: PlayerEntity) {
        viewModelScope.launch {
            playerRepository.deletePlayer(player.id)
            _selectedPlayers.remove(player)
        }
    }

    fun toggleSelection(player: Any) {
        if (_selectedPlayers.contains(player)) {
            _selectedPlayers.remove(player)
        } else {
            _selectedPlayers.add(player)
        }
    }
    
    fun clearSelection() {
        _selectedPlayers.clear()
    }

    fun getSelectedPlayerNames(): List<String> {
        return _selectedPlayers.map { player ->
            when (player) {
                is PlayerEntity -> player.name
                is User -> player.name
                else -> ""
            }
        }
    }
}
