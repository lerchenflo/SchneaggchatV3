package org.lerchenflo.schneaggchatv3mp.games.data

import kotlinx.coroutines.flow.Flow
import org.lerchenflo.schneaggchatv3mp.datasource.database.AppDatabase

class PlayerRepository(
    private val database: AppDatabase,
) {

    fun getAllPlayersFlow(): Flow<List<PlayerEntity>> {
        return database.playerDao().getAllPlayersFlow()
    }

    suspend fun upsertPlayer(player: PlayerEntity) {
        database.playerDao().upsert(player)
    }

    suspend fun deletePlayer(playerId: String) {
        database.playerDao().delete(playerId)
    }
}
