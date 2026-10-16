package org.lerchenflo.schneaggchatv3mp.groupextensions.domain

/**
 * One extension instance (container) owned by a group, e.g. the split "Italien 2026".
 * A group can hold any number of extensions, also several of the same type.
 */
data class GroupExtension(
    val id: String,
    val ownerType: ExtensionOwnerType,
    val ownerId: String,
    val type: GroupExtensionType,
    val title: String,
    val settings: ExtensionSettings = ExtensionSettings.None,
    val createdBy: String,
    val createdAt: Long,
    val updatedBy: String,
    val updatedAt: Long,
)
