package org.lerchenflo.schneaggchatv3mp.groupextensions.domain

/**
 * One entry inside an extension container, e.g. one quote or one expense.
 * [ownerType], [ownerId] and [type] are copies of the container's values, set by the server.
 */
data class GroupExtensionItem(
    val id: String,
    val extensionId: String,
    val ownerType: ExtensionOwnerType,
    val ownerId: String,
    val type: GroupExtensionType,
    val data: ExtensionItemData,
    val createdBy: String,
    val createdAt: Long,
    val updatedBy: String,
    val updatedAt: Long,
)
