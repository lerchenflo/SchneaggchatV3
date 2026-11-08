package org.lerchenflo.schneaggchatv3mp.events.domain

/** What an event change alert is about. Wire names, must match the server. */
enum class EventUpdateChange {
    START,
    LOCATION,
    CANCELLED;

    companion object {
        /** Known names only; an unknown one from a newer server is skipped. */
        fun parse(names: List<String>): Set<EventUpdateChange> =
            names.mapNotNull { name -> entries.firstOrNull { it.name == name } }.toSet()
    }
}
