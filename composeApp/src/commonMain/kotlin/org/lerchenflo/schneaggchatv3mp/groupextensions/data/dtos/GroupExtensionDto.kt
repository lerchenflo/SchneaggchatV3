package org.lerchenflo.schneaggchatv3mp.groupextensions.data.dtos

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One extension container. [ownerType], [type] and [settings] are kept exactly as the server sent
 * them (enum names / wire JSON), so a container of a type this app version does not know yet is
 * still stored and shows up after an app update without a re-sync. Decoding happens in
 * toGroupExtension(), which skips what it can't read.
 */
@Entity(
    tableName = "group_extensions",
    indices = [
        Index(value = ["ownerId"]),
        // MAX(version) is the sync watermark, an index seek instead of a table scan.
        Index(value = ["version"]),
    ]
)
data class GroupExtensionDto(
    @PrimaryKey val id: String,

    val ownerType: String,
    val ownerId: String,
    val type: String,
    val title: String,

    /** Wire JSON of ExtensionSettings, null when the server sent none */
    val settings: String?,

    val createdBy: String,
    val createdAt: Long,
    val updatedBy: String,
    val updatedAt: Long,

    /** Server sync version, shared counter with the items */
    val version: Long,
)
