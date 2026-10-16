package org.lerchenflo.schneaggchatv3mp.groupextensions.domain

import kotlinx.serialization.Serializable

/**
 * The kinds of extension a group can hold. The names are the wire and database values shared with
 * the server, so never rename an entry.
 */
@Serializable
enum class GroupExtensionType {
    SHARED_TEXT,
    SPLIT,
    QUOTES,
    ROUTE_PLANNER
}
