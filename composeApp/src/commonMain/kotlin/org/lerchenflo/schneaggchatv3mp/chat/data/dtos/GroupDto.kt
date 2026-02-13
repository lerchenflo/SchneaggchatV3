package org.lerchenflo.schneaggchatv3mp.chat.data.dtos

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "groups")
data class GroupDto(

    @PrimaryKey(autoGenerate = false)
    val id: String,
    val name: String,

    var profilePictureUrl: String,

    val description: String,

    val createDate: Long,

    val updatedAt: Long,
    val profilePicUpdatedAt: Long,

    val notisMuted: Boolean = false
)