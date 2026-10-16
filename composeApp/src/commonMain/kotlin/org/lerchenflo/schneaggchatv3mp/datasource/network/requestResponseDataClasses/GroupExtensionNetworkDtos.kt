package org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/*
 * Group extensions ride along with /groups/sync (see NetworkUtils.GroupSyncResponse).
 * Enum values stay plain strings and the polymorphic payloads stay raw JSON here, so an extension
 * type this app version does not know yet still deserializes and gets stored - decoding into the
 * domain types happens on read (GroupExtensionResponseMapper).
 */

@Serializable
data class GroupExtensionResponse(
    val id: String,

    /** ExtensionOwnerType name */
    val ownerType: String,
    val ownerId: String,
    /** GroupExtensionType name */
    val type: String,
    val title: String,

    /** ExtensionSettings as wire JSON */
    val settings: JsonElement? = null,

    val createdBy: String,
    val createdAt: Long,
    val updatedBy: String,
    val updatedAt: Long,

    val version: Long,
)

@Serializable
data class GroupExtensionItemResponse(
    val id: String,
    val extensionId: String,

    /** ExtensionOwnerType name */
    val ownerType: String,
    val ownerId: String,
    /** GroupExtensionType name */
    val type: String,

    /** ExtensionItemData as wire JSON */
    val data: JsonElement,

    val createdBy: String,
    val createdAt: Long,
    val updatedBy: String,
    val updatedAt: Long,

    val version: Long,
)
