package org.lerchenflo.schneaggchatv3mp.games.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Entity(tableName = "players")
@Serializable
data class PlayerEntity(
    @PrimaryKey
    val id: String = Uuid.random().toString(),
    val name: String
)
