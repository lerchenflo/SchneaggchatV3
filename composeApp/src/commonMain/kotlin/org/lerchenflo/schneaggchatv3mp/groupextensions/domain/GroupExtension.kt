package org.lerchenflo.schneaggchatv3mp.groupextensions.domain

/**
 * One extension instance owned by a group, e.g. the split "Italien 2026". A group can hold any
 * number of extensions, also several of the same type. Its entries live inside it in [items], the
 * same way a map entry carries its location data.
 */
data class GroupExtension(
    val id: String,
    val ownerType: ExtensionOwnerType,
    val ownerId: String,
    val type: GroupExtensionType,
    val title: String,
    val settings: ExtensionSettings = ExtensionSettings.None,
    val items: List<GroupExtensionItem> = emptyList(),
    val createdBy: String,
    val createdAt: Long,
    val updatedBy: String,
    val updatedAt: Long,
)
