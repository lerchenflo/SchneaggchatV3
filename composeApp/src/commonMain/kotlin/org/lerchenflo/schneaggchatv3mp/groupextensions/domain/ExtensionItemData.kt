package org.lerchenflo.schneaggchatv3mp.groupextensions.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong

/**
 * HOW TO ADD A NEW EXTENSION TYPE
 * ===============================
 *
 * SERVER SIDE (SchneaggchatV3server):
 * 1. Add the enum value to GroupExtensionType.
 * 2. Add the data class to ExtensionItemData with @TypeAlias("snake_name") and register it in
 *    @JsonSubTypes with the same name.
 * 3. If the type needs config, add an ExtensionSettings subclass the same way and allow it in the
 *    container validator.
 * 4. Add the exhaustive `when` branch in ExtensionItemValidator (compiler enforced).
 * 5. If the type is single-item (like shared text), add it to the single-item set checked in upsertItems.
 *
 * CLIENT SIDE (SchneaggchatV3):
 * 1. Add the enum value to GroupExtensionType (domain) and wire toUiText() + icon() in
 *    presentation/GroupExtensionTypeUi.kt (compiler enforced).
 * 2. Add the data class to ExtensionItemData with the same @SerialName and property names as the server.
 * 3. Add strings for the type name in values, values-de, values-it.
 * 4. Add the screen package (State, Action, ViewModel, Screen, Root, preview), register the ViewModel
 *    in Modules.kt, add the `when` branch in GroupExtensionScreen.
 * 5. If the type has settings, add its fields to AddExtensionDialog.
 * 6. Optionally add a pure domain calculator for derived values.
 *
 * Nothing else changes: no endpoint, no sync change, no Room table, no DB version bump. The payload
 * is stored as its wire JSON, so items of a type this app version does not know yet are kept in
 * Room and show up as soon as an app update adds the type.
 *
 * IMPORTANT: Keep the serial/type-alias names ("quote", ...) and the property names identical
 * between client and server - they are the wire JSON keys, the Mongo field names and the stored
 * Room JSON at the same time.
 */
@Serializable
sealed class ExtensionItemData {

    /** Fixed per subclass; the server rejects an item whose type does not match its container. */
    abstract val extensionType: GroupExtensionType

    /** SHARED_TEXT: exactly one item per container holds the whole document. */
    @Serializable
    @SerialName("shared_text")
    data class SharedText(
        val text: String,
    ) : ExtensionItemData() {
        override val extensionType: GroupExtensionType get() = GroupExtensionType.SHARED_TEXT
    }

    /** SPLIT: one item per expense or settlement transfer. Amounts in minor units of the container currency. */
    @Serializable
    @SerialName("split_expense")
    data class SplitExpense(
        val title: String,
        val amountCents: Long,
        /** userId of the payer */
        val paidBy: String,
        /** userIds sharing the cost */
        val participants: List<String>,
        /** Optional split by shares per userId, null = equal split */
        val weights: Map<String, Int>? = null,
        /** Optional fixed amount per userId (cents), adding up to [amountCents]; wins over [weights] */
        val amounts: Map<String, Long>? = null,
        val date: Long,
        /** true = "A paid B back", participants = [B] */
        val isSettlement: Boolean = false,
        /** Icon chosen by the user, any emoji (at most 10 chars) */
        val emoji: String? = null,
    ) : ExtensionItemData() {
        override val extensionType: GroupExtensionType get() = GroupExtensionType.SPLIT
    }

    /** QUOTES: one item per quote. Either [saidByUserId] (a member) or [saidByName] (free text) is set. */
    @Serializable
    @SerialName("quote")
    data class Quote(
        val text: String,
        val saidByUserId: String? = null,
        val saidByName: String? = null,
        val saidAt: Long? = null,
        val context: String? = null,
    ) : ExtensionItemData() {
        override val extensionType: GroupExtensionType get() = GroupExtensionType.QUOTES
    }

    /** ROUTE_PLANNER: one item per point; the route is the items ordered by [sortIndex]. */
    @Serializable
    @SerialName("route_point")
    data class RoutePoint(
        val coordinates: LatLong,
        val name: String,
        val description: String = "",
        val sortIndex: Int,
        val kind: RoutePointKind = RoutePointKind.STOP,
        /** Optional planned day/time */
        val plannedAt: Long? = null,
        /** Set when the point was picked from schneaggmap. Soft link: coordinates and name are copied, not referenced */
        val mapEntryId: String? = null,
    ) : ExtensionItemData() {
        override val extensionType: GroupExtensionType get() = GroupExtensionType.ROUTE_PLANNER
    }
}
