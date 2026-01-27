package org.lerchenflo.schneaggchatv3mp.games.presentation.PlayerSelector

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.datasource.database.PlayerDao
import org.lerchenflo.schneaggchatv3mp.games.data.PlayerEntity

class PlayerSelectorViewModel(
    private val playerDao: PlayerDao
) : ViewModel() {

    private val _players = mutableStateListOf<PlayerEntity>()
    val players: List<PlayerEntity> get() = _players

    private val _selectedPlayers = mutableStateListOf<PlayerEntity>()
    val selectedPlayers: List<PlayerEntity> get() = _selectedPlayers

    init {
        viewModelScope.launch {
            playerDao.getAllPlayersFlow().collectLatest { playerList ->
                _players.clear()
                _players.addAll(playerList)
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

    fun toggleSelection(player: PlayerEntity) {
        if (_selectedPlayers.contains(player)) {
            _selectedPlayers.remove(player)
        } else {
            _selectedPlayers.add(player)
        }
    }
    
    fun clearSelection() {
        _selectedPlayers.clear()
    }
}
