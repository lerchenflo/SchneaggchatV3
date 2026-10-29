package org.lerchenflo.schneaggchatv3mp.groupextensions.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.ExtensionOwnerType
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.ExtensionSettings
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.GroupExtensionItem
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.GroupExtensionType

/** One extension with its items, stored like a map entry with its location data (JSON columns). */
@Entity(
    tableName = "group_extensions",
    indices = [Index(value = ["ownerId"])]
)
data class GroupExtensionEntity(
    @PrimaryKey val id: String,

    val ownerType: ExtensionOwnerType,
    val ownerId: String,
    val type: GroupExtensionType,
    val title: String,

    val settings: ExtensionSettings,
    val items: List<GroupExtensionItem>,

    val createdBy: String,
    val createdAt: Long,
    val updatedBy: String,
    val updatedAt: Long,
)
