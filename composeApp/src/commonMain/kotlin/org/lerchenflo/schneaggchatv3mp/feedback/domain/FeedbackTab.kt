package org.lerchenflo.schneaggchatv3mp.feedback.domain

/** Kind of entries a list shows. Features are split into open requests and implemented ones. */
enum class FeedbackSection(
    /** The entry type a new entry created from this section gets. */
    val createType: FeedbackType,
    /** Statuses the status filter offers in this section. Empty: the section has only one status. */
    val filterStatuses: List<FeedbackStatus>,
) {
    REQUESTS(FeedbackType.FEATURE, listOf(FeedbackStatus.REQUESTED, FeedbackStatus.PLANNED)),
    BUGS(FeedbackType.BUG, FeedbackStatus.forType(FeedbackType.BUG)),
    IMPLEMENTED(FeedbackType.FEATURE, emptyList());

    fun contains(entry: FeedbackEntry): Boolean = when (this) {
        REQUESTS -> entry.type == FeedbackType.FEATURE && entry.status != FeedbackStatus.IMPLEMENTED
        BUGS -> entry.type == FeedbackType.BUG
        IMPLEMENTED -> entry.type == FeedbackType.FEATURE && entry.status == FeedbackStatus.IMPLEMENTED
    }

    companion object {
        /** The section a new or changed entry shows up in. */
        fun of(entry: FeedbackEntry): FeedbackSection = entries.first { it.contains(entry) }
    }
}

/**
 * The four lists of the board. Entries tagged GAMES only show up in the Games tab, where chips
 * pick the section; every other tab is one fixed section without the GAMES entries.
 */
enum class FeedbackTab(
    /** The section this tab always shows. Null for GAMES: its chips pick the section. */
    val section: FeedbackSection?,
) {
    REQUESTS(FeedbackSection.REQUESTS),
    BUGS(FeedbackSection.BUGS),
    IMPLEMENTED(FeedbackSection.IMPLEMENTED),
    GAMES(null);

    fun contains(entry: FeedbackEntry): Boolean =
        if (this == GAMES) entry.isGamesEntry else !entry.isGamesEntry && section?.contains(entry) == true

    companion object {
        /** The tab a new or changed entry shows up in. */
        fun of(entry: FeedbackEntry): FeedbackTab = entries.first { it.contains(entry) }
    }
}

/** Games, game wishes and game bugs: the entries of the Games tab. */
internal val FeedbackEntry.isGamesEntry: Boolean get() = FeedbackTag.GAMES in tags
