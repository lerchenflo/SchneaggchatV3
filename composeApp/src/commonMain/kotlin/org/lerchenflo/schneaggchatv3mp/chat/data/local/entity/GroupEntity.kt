package org.lerchenflo.schneaggchatv3mp.chat.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "groups")
data class GroupEntity(

    @PrimaryKey(autoGenerate = false)
    val id: String,
    val name: String,

    var profilePictureUrl: String,

    val description: String,

    val createDate: Long,

    val updatedAt: Long,
    val profilePicUpdatedAt: Long,

    val expiresAt: Long? = null,

    val notisMuted: Boolean = false
)