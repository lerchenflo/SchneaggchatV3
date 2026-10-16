package org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses

import kotlinx.serialization.json.JsonElement
import org.lerchenflo.schneaggchatv3mp.datasource.network.AppJson
import org.lerchenflo.schneaggchatv3mp.groupextensions.data.dtos.GroupExtensionDto
import org.lerchenflo.schneaggchatv3mp.groupextensions.data.dtos.GroupExtensionItemDto
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.ExtensionItemData
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.ExtensionOwnerType
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.ExtensionSettings
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.GroupExtension
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.GroupExtensionItem
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.GroupExtensionType

// ─── Raw sync entries ─────────────────────────────────────────────────────────

/**
 * Decodes the raw extension entries of a group sync one by one. A malformed entry is dropped
 * instead of failing the whole group sync.
 */
fun List<JsonElement>.decodeGroupExtensionResponses(): List<GroupExtensionResponse> = mapNotNull {
    runCatching { AppJson.instance.decodeFromJsonElement(GroupExtensionResponse.serializer(), it) }.getOrNull()
}

/** Same as [decodeGroupExtensionResponses] for the items. */
fun List<JsonElement>.decodeGroupExtensionItemResponses(): List<GroupExtensionItemResponse> = mapNotNull {
    runCatching { AppJson.instance.decodeFromJsonElement(GroupExtensionItemResponse.serializer(), it) }.getOrNull()
}

// ─── GroupExtension ───────────────────────────────────────────────────────────

fun GroupExtensionResponse.toDto(): GroupExtensionDto = GroupExtensionDto(
    id = id,
    ownerType = ownerType,
    ownerId = ownerId,
    type = type,
    title = title,
    settings = settings?.toString(),
    createdBy = createdBy,
    createdAt = createdAt,
    updatedBy = updatedBy,
    updatedAt = updatedAt,
    version = version,
)

/** Null when the owner type, the extension type or the settings are unknown to this app version. */
fun GroupExtensionDto.toGroupExtension(): GroupExtension? {
    val decodedOwnerType = ownerType.toExtensionOwnerTypeOrNull() ?: return null
    val decodedType = type.toGroupExtensionTypeOrNull() ?: return null
    val decodedSettings = settings?.let { json ->
        runCatching { AppJson.instance.decodeFromString(ExtensionSettings.serializer(), json) }.getOrNull()
            ?: return null
    } ?: ExtensionSettings.None

    return GroupExtension(
        id = id,
        ownerType = decodedOwnerType,
        ownerId = ownerId,
        type = decodedType,
        title = title,
        settings = decodedSettings,
        createdBy = createdBy,
        createdAt = createdAt,
        updatedBy = updatedBy,
        updatedAt = updatedAt,
    )
}

// ─── GroupExtensionItem ───────────────────────────────────────────────────────

fun GroupExtensionItemResponse.toDto(): GroupExtensionItemDto = GroupExtensionItemDto(
    id = id,
    extensionId = extensionId,
    ownerType = ownerType,
    ownerId = ownerId,
    type = type,
    data = data.toString(),
    createdBy = createdBy,
    createdAt = createdAt,
    updatedBy = updatedBy,
    updatedAt = updatedAt,
    version = version,
)

/** Null when the owner type, the extension type or the payload are unknown to this app version. */
fun GroupExtensionItemDto.toGroupExtensionItem(): GroupExtensionItem? {
    val decodedOwnerType = ownerType.toExtensionOwnerTypeOrNull() ?: return null
    val decodedType = type.toGroupExtensionTypeOrNull() ?: return null
    val decodedData = runCatching { AppJson.instance.decodeFromString(ExtensionItemData.serializer(), data) }.getOrNull()
        ?: return null

    return GroupExtensionItem(
        id = id,
        extensionId = extensionId,
        ownerType = decodedOwnerType,
        ownerId = ownerId,
        type = decodedType,
        data = decodedData,
        createdBy = createdBy,
        createdAt = createdAt,
        updatedBy = updatedBy,
        updatedAt = updatedAt,
    )
}

// ─── Enum names ───────────────────────────────────────────────────────────────

private fun String.toExtensionOwnerTypeOrNull(): ExtensionOwnerType? =
    ExtensionOwnerType.entries.firstOrNull { it.name == this }

private fun String.toGroupExtensionTypeOrNull(): GroupExtensionType? =
    GroupExtensionType.entries.firstOrNull { it.name == this }
