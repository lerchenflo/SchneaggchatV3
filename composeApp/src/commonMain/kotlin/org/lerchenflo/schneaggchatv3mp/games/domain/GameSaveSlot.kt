package org.lerchenflo.schneaggchatv3mp.games.domain

/** Identifies where one game's persisted run is stored (see GameSaveRepository). */
interface GameSaveSlot {
    /** Unique per game; part of the DataStore key, so it must never change once shipped. */
    val saveKey: String

    /** Daily games only restore saves taken on the same day. */
    val daily: Boolean
}
