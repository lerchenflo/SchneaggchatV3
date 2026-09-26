package org.lerchenflo.schneaggchatv3mp.games.domain

/** Identifies where one game's persisted run is stored (see GameSaveRepository). */
interface GameSaveSlot {
    /** Unique per game; part of the DataStore key, so it must never change once shipped. */
    val saveKey: String

    /** Daily games only restore saves taken on the same day. */
    val daily: Boolean
}

/** Games without a server leaderboard that still keep their run when the app is left. */
enum class LocalGameSaveSlot(override val saveKey: String) : GameSaveSlot {
    YATZI("yatzi"),
    DART_COUNTER("dart_counter"),
    UNDERCOVER("undercover");

    override val daily: Boolean get() = false
}
