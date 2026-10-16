package org.lerchenflo.schneaggchatv3mp.groupextensions.data.dtos

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One entry inside an extension container, holding the actual content in [data]. Stored the same
 * raw way as [GroupExtensionDto], so items of an unknown type survive until an app update can read them.
 * No foreign key to the container on purpose: an item arriving before or without its container
 * must not fail the whole sync batch.
 */
@Entity(
    tableName = "group_extension_items",
    indices = [
        Index(value = ["extensionId"]),
        Index(value = ["ownerId"]),
        // MAX(version) is the sync watermark, an index seek instead of a table scan.
        Index(value = ["version"]),
    ]
)
data class GroupExtensionItemDto(
    @PrimaryKey val id: String,

    val extensionId: String,
    val ownerType: String,
    val ownerId: String,
    val type: String,

    /** Wire JSON of ExtensionItemData */
    val data: String,

    val createdBy: String,
    val createdAt: Long,
    val updatedBy: String,
    val updatedAt: Long,

    /** Server sync version, shared counter with the containers */
    val version: Long,
)
