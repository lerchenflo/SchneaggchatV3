package org.lerchenflo.schneaggchatv3mp.games.presentation.PlayerSelector

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.datasource.database.PlayerDao
import org.lerchenflo.schneaggchatv3mp.games.data.PlayerEntity
import org.lerchenflo.schneaggchatv3mp.chat.domain.User

class PlayerSelectorViewModel(
    private val playerDao: PlayerDao,
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
            playerDao.getAllPlayersFlow().collectLatest { playerList ->
                _localPlayers.clear()
                _localPlayers.addAll(playerList)
            }
        }

        viewModelScope.launch {
            // Load friends
            appRepository.getFriendsFlow("").collectLatest { friendsList ->
                _friends.clear()
                _friends.addAll(friendsList)
            }
        }
    }

    fun addPlayer(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            playerDao.upsert(PlayerEntity(name = name.trim()))
        }
    }

    fun deletePlayer(player: PlayerEntity) {
        viewModelScope.launch {
            playerDao.delete(player.id)
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
