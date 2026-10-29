package org.lerchenflo.schneaggchatv3mp.groupextensions.domain

import kotlinx.serialization.Serializable

/**
 * One entry inside an extension, e.g. one quote or one expense. Embedded in [GroupExtension.items]
 * and stored with it, so it is serializable like the map's location data.
 */
@Serializable
data class GroupExtensionItem(
    val id: String,
    val data: ExtensionItemData,
    val createdBy: String,
    val createdAt: Long,
    val updatedBy: String,
    val updatedAt: Long,
)
