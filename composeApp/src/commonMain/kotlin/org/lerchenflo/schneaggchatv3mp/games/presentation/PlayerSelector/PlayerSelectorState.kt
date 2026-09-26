package org.lerchenflo.schneaggchatv3mp.games.presentation.PlayerSelector

import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.games.data.PlayerEntity
import org.lerchenflo.schneaggchatv3mp.games.domain.GamePlayer

/**
 * An entry of the player picker. A [Local] player only exists on this device, a [Friend] is a
 * platform user whose results can be uploaded to the leaderboard.
 */
sealed interface SelectablePlayer {
    val key: String
    val name: String

    data class Local(val entity: PlayerEntity) : SelectablePlayer {
        override val key: String get() = "local:${entity.id}"
        override val name: String get() = entity.name
    }

    data class Friend(val user: User) : SelectablePlayer {
        override val key: String get() = "user:${user.id}"
        override val name: String get() = user.name
    }
}

fun SelectablePlayer.toGamePlayer() = GamePlayer(
    name = name,
    userId = (this as? SelectablePlayer.Friend)?.user?.id,
)

data class PlayerSelectorState(
    val localPlayers: List<SelectablePlayer.Local> = emptyList(),
    val friends: List<SelectablePlayer.Friend> = emptyList(),
    /** Selection order matters - the players start the game in the order they were picked. */
    val selectedKeys: List<String> = emptyList(),
    val newPlayerName: String = "",
) {
    private val byKey: Map<String, SelectablePlayer>
        get() = (localPlayers + friends).associateBy { it.key }

    val selectedPlayers: List<SelectablePlayer> get() = selectedKeys.mapNotNull { byKey[it] }

    val selectedGamePlayers: List<GamePlayer> get() = selectedPlayers.map { it.toGamePlayer() }

    val selectedCount: Int get() = selectedKeys.size

    fun isSelected(player: SelectablePlayer): Boolean = player.key in selectedKeys
}

sealed interface PlayerSelectorAction {
    data class OnNewPlayerNameChange(val name: String) : PlayerSelectorAction
    data object OnAddLocalPlayer : PlayerSelectorAction
    data class OnDeleteLocalPlayer(val player: SelectablePlayer.Local) : PlayerSelectorAction
    data class OnToggleSelection(val player: SelectablePlayer) : PlayerSelectorAction
    data object OnClearSelection : PlayerSelectorAction
}
