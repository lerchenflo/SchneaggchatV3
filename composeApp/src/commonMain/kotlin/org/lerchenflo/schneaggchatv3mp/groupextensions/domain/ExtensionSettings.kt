package org.lerchenflo.schneaggchatv3mp.groupextensions.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Per-type config of one extension container, set when the extension is created.
 * Serial names and property names must match the server (see the header of [ExtensionItemData]).
 */
@Serializable
sealed class ExtensionSettings {

    /** Types without config. */
    @Serializable
    @SerialName("none")
    data object None : ExtensionSettings()

    /** SPLIT: one currency per container (ISO 4217); expenses carry no currency of their own. */
    @Serializable
    @SerialName("split")
    data class Split(
        val currency: String = "EUR",
    ) : ExtensionSettings()

    /** ROUTE_PLANNER */
    @Serializable
    @SerialName("route")
    data class Route(
        val transportMode: String? = null,
        val startDate: Long? = null,
    ) : ExtensionSettings()
}
