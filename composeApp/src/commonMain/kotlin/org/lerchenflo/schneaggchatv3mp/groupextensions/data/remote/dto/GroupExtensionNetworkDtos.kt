package org.lerchenflo.schneaggchatv3mp.groupextensions.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.ExtensionItemData
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.ExtensionOwnerType
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.ExtensionSettings
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.GroupExtensionType

@Serializable
data class GroupExtensionSyncResponse(
    /**
     * GroupExtensionResponse entries, kept as raw JSON and decoded one by one
     * (decodeGroupExtensionResponses), so an extension of a type this app version does not know
     * yet is skipped instead of failing the whole page. The IdTimeStamp sync sends it again after
     * an app update, because the client never stored it.
     */
    val updatedExtensions: List<JsonElement>,
    val deletedExtensions: List<String>,
    val moreEntries: Boolean,
)

/** Create ([extensionId] null) or rename / change the settings of an extension. Never touches its items. */
@Serializable
data class ExtensionRequest(
    val extensionId: String?,
    val ownerType: ExtensionOwnerType,
    val ownerId: String,
    val type: GroupExtensionType,
    val title: String,
    val settings: ExtensionSettings,
)

/** Create ([itemId] null) or replace one item of an extension. */
@Serializable
data class ItemRequest(
    val extensionId: String,
    val itemId: String?,
    val data: ExtensionItemData,
)
