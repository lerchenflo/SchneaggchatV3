package org.lerchenflo.schneaggchatv3mp.groupextensions.data.local.entity

import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.GroupExtension

fun GroupExtension.toEntity(): GroupExtensionEntity = GroupExtensionEntity(
    id = id,
    ownerType = ownerType,
    ownerId = ownerId,
    type = type,
    title = title,
    settings = settings,
    items = items,
    createdBy = createdBy,
    createdAt = createdAt,
    updatedBy = updatedBy,
    updatedAt = updatedAt,
)

fun GroupExtensionEntity.toGroupExtension(): GroupExtension = GroupExtension(
    id = id,
    ownerType = ownerType,
    ownerId = ownerId,
    type = type,
    title = title,
    settings = settings,
    items = items,
    createdBy = createdBy,
    createdAt = createdAt,
    updatedBy = updatedBy,
    updatedAt = updatedAt,
)
