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
    ROUTE_PLANNER;

    /** Whether users can create and see extensions of this type. */
    val isAvailable: Boolean
        get() = when (this) {
            SPLIT -> true
            // No screen yet ("under construction"): hidden until built (user, 2026-11-12)
            SHARED_TEXT, QUOTES, ROUTE_PLANNER -> false
        }
}
