package org.lerchenflo.schneaggchatv3mp.datasource.network.dto

import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.ExtensionItemData
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.ExtensionOwnerType
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.ExtensionSettings
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.GroupExtensionType

/** One extension with its embedded items, as every group extension endpoint and the socket send it. */
@Serializable
data class GroupExtensionResponse(
    val id: String,
    val ownerType: ExtensionOwnerType,
    val ownerId: String,
    val type: GroupExtensionType,
    val title: String,
    val settings: ExtensionSettings = ExtensionSettings.None,
    val items: List<GroupExtensionItemResponse> = emptyList(),
    val createdBy: String,
    val createdAt: Long,
    val updatedBy: String,
    val updatedAt: Long,
)

@Serializable
data class GroupExtensionItemResponse(
    val id: String,
    val data: ExtensionItemData,
    val createdBy: String,
    val createdAt: Long,
    val updatedBy: String,
    val updatedAt: Long,
)
