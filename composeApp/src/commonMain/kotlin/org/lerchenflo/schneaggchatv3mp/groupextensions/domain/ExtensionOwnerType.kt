package org.lerchenflo.schneaggchatv3mp.groupextensions.domain

import kotlinx.serialization.Serializable

/** What an extension belongs to. Only groups for now, FRIENDSHIP (1:1 chats) is reserved for later. */
@Serializable
enum class ExtensionOwnerType {
    GROUP
}
