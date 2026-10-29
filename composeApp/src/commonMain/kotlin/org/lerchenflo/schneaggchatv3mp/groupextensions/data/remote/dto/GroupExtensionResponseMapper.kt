package org.lerchenflo.schneaggchatv3mp.groupextensions.data.remote.dto

import kotlinx.serialization.json.JsonElement
import org.lerchenflo.schneaggchatv3mp.datasource.network.AppJson
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.GroupExtensionItemResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.GroupExtensionResponse
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.GroupExtension
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.GroupExtensionItem

/** Decodes the raw sync entries one by one; an entry this app version can't read is dropped. */
fun List<JsonElement>.decodeGroupExtensionResponses(): List<GroupExtensionResponse> = mapNotNull {
    runCatching { AppJson.instance.decodeFromJsonElement(GroupExtensionResponse.serializer(), it) }.getOrNull()
}

fun GroupExtensionResponse.toGroupExtension(): GroupExtension = GroupExtension(
    id = id,
    ownerType = ownerType,
    ownerId = ownerId,
    type = type,
    title = title,
    settings = settings,
    items = items.map { it.toGroupExtensionItem() },
    createdBy = createdBy,
    createdAt = createdAt,
    updatedBy = updatedBy,
    updatedAt = updatedAt,
)

fun GroupExtensionItemResponse.toGroupExtensionItem(): GroupExtensionItem = GroupExtensionItem(
    id = id,
    data = data,
    createdBy = createdBy,
    createdAt = createdAt,
    updatedBy = updatedBy,
    updatedAt = updatedAt,
)
