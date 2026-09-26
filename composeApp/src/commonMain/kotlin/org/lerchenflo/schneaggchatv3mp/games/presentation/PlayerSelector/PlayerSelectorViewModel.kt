package org.lerchenflo.schneaggchatv3mp.games.presentation.PlayerSelector

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.games.data.PlayerEntity
import org.lerchenflo.schneaggchatv3mp.games.data.PlayerRepository

class PlayerSelectorViewModel(
    private val playerRepository: PlayerRepository,
    private val appRepository: AppRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PlayerSelectorState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            playerRepository.getAllPlayersFlow().collectLatest { playerList ->
                _state.update { current ->
                    current.copy(localPlayers = playerList.map { SelectablePlayer.Local(it) })
                }
            }
        }

        viewModelScope.launch {
            // Yourself first, then the friends you can pick from
            val ownId = SessionCache.requireLoggedIn()?.userId
            val ownUserFlow = if (ownId != null) appRepository.getUserByIdFlow(ownId) else flowOf(null)
            val friendsFlow = appRepository.getFriendsFlow("")

            ownUserFlow.combine(friendsFlow) { ownUser, friendsList ->
                buildList {
                    ownUser?.let { add(it) }
                    addAll(friendsList)
                }
            }.collectLatest { combinedList ->
                _state.update { current ->
                    current.copy(friends = combinedList.map { SelectablePlayer.Friend(it) })
                }
            }
        }
    }

    fun onAction(action: PlayerSelectorAction) {
        when (action) {
            is PlayerSelectorAction.OnNewPlayerNameChange ->
                _state.update { it.copy(newPlayerName = action.name) }
            PlayerSelectorAction.OnAddLocalPlayer -> addLocalPlayer()
            is PlayerSelectorAction.OnDeleteLocalPlayer -> deleteLocalPlayer(action.player)
            is PlayerSelectorAction.OnToggleSelection -> toggleSelection(action.player)
            PlayerSelectorAction.OnClearSelection -> _state.update { it.copy(selectedKeys = emptyList()) }
        }
    }

    private fun addLocalPlayer() {
        val name = _state.value.newPlayerName.trim()
        if (name.isBlank()) return
        _state.update { it.copy(newPlayerName = "") }
        viewModelScope.launch {
            playerRepository.upsertPlayer(PlayerEntity(name = name))
        }
    }

    private fun deleteLocalPlayer(player: SelectablePlayer.Local) {
        viewModelScope.launch {
            playerRepository.deletePlayer(player.entity.id)
            _state.update { it.copy(selectedKeys = it.selectedKeys - player.key) }
        }
    }

    private fun toggleSelection(player: SelectablePlayer) {
        _state.update { current ->
            val keys = if (player.key in current.selectedKeys) {
                current.selectedKeys - player.key
            } else {
                current.selectedKeys + player.key
            }
            current.copy(selectedKeys = keys)
        }
    }
}
