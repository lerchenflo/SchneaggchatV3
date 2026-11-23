package org.lerchenflo.schneaggchatv3mp.feedback.domain


enum class FeedbackType { FEATURE, BUG }

/** Features only use REQUESTED / PLANNED / IMPLEMENTED, bugs only OPEN / CONFIRMED / FIXED. */
enum class FeedbackStatus {
    REQUESTED,
    PLANNED,
    IMPLEMENTED,
    OPEN,
    CONFIRMED,
    FIXED;

    companion object {
        fun forType(type: FeedbackType): List<FeedbackStatus> = when (type) {
            FeedbackType.FEATURE -> listOf(REQUESTED, PLANNED, IMPLEMENTED)
            FeedbackType.BUG -> listOf(OPEN, CONFIRMED, FIXED)
        }
    }
}

enum class FeedbackTag { CHAT, GROUPS, EVENTS, MAP, GAMES, SETTINGS, UI, NOTIFICATIONS, PERFORMANCE, OTHER }

/**
 * A user's vote on one entry. For bugs UP means "I have this too" and DOWN "can't reproduce".
 * DIDNT_KNOW is only allowed on implemented features. NOT_NEEDED is allowed on all features and
 * is neutral: the user has seen it and doesn't profit from it, but isn't against it either.
 * Neither changes the score. NONE removes the vote.
 */
enum class FeedbackVote { UP, DOWN, DIDNT_KNOW, NOT_NEEDED, NONE }

data class FeedbackEntry(
    val id: String,
    val type: FeedbackType,
    val title: String,
    val description: String,
    val tags: List<FeedbackTag>,
    val status: FeedbackStatus,
    val upvotes: Int,
    val downvotes: Int,
    val didntKnowCount: Int,
    val notNeededCount: Int,
    val myVote: FeedbackVote,
    val commentCount: Int,
    val latestDevComment: String?,
    /** Null for seeded entries (no creator). */
    val creatorId: String?,
    val isOwn: Boolean,
    val canDelete: Boolean,
    /** The viewer is the creator or an admin and the entry is not seeded. */
    val canEdit: Boolean,
    val isSeeded: Boolean,
    /** Stable key of a seeded entry (from the server's seed file), null for entries users created. */
    val seedKey: String?,
    /** Feature the creator marked as premium (or a seeded special feature); shown with an animated outline. */
    val isPremium: Boolean,
    val createdAt: Long,
    /** Server time of the last change of any kind (votes too). Sent back as the sync cursor. */
    val updatedAt: Long,
    /**
     * Server time of the last change worth showing: creation, a status change or a comment by
     * someone other than the creator. Votes don't move it.
     */
    val activityAt: Long,
    /** Server time of the last edit, null when never edited. An edit doesn't move [activityAt]. */
    val editedAt: Long?,
    /**
     * Comments on an own entry since the user last opened it on this device. Filled by the
     * repository from a local "seen" count, never sent by the server. Always 0 for others' entries.
     */
    val newCommentCount: Int = 0,
) {
    /** Up and downvotes cancel out, "didn't know" is neutral. */
    val score: Int get() = upvotes - downvotes

    /**
     * User-created entries stay pinned (and highlighted) at the top for [NEW_ENTRY_PIN_MILLIS].
     * [createdAt] is server time and [nowMillis] device time: a device clock behind the server
     * puts a fresh entry in the future, which still counts as new.
     */
    fun isPinned(nowMillis: Long): Boolean =
        !isSeeded && nowMillis - createdAt < NEW_ENTRY_PIN_MILLIS

    /**
     * What happened since [lastSeen] (a server time): an entry by someone else that is newer, or
     * news on one of the user's own entries. `activityAt > createdAt` keeps the user's own new
     * entry from showing as updated. Null when there is nothing new.
     */
    fun markSince(lastSeen: Long): FeedbackMark? = when {
        !isOwn && createdAt > lastSeen -> FeedbackMark.NEW
        isOwn && activityAt > lastSeen && activityAt > createdAt -> FeedbackMark.UPDATED
        else -> null
    }

    /** Local vote change, so the buttons react before the server answers. */
    fun withVote(vote: FeedbackVote): FeedbackEntry {
        fun count(current: Int, target: FeedbackVote) =
            current - (if (myVote == target) 1 else 0) + (if (vote == target) 1 else 0)

        return copy(
            upvotes = count(upvotes, FeedbackVote.UP),
            downvotes = count(downvotes, FeedbackVote.DOWN),
            didntKnowCount = count(didntKnowCount, FeedbackVote.DIDNT_KNOW),
            notNeededCount = count(notNeededCount, FeedbackVote.NOT_NEEDED),
            myVote = vote,
        )
    }

    companion object {
        const val NEW_ENTRY_PIN_MILLIS = 2L * 24 * 60 * 60 * 1000
    }
}

/** Why an entry counts for the feedback badge, see [FeedbackEntry.markSince]. */
enum class FeedbackMark { NEW, UPDATED }

data class FeedbackComment(
    val id: String,
    val authorId: String,
    val text: String,
    val isDevComment: Boolean,
    val canDelete: Boolean,
    /** The viewer wrote it. */
    val canEdit: Boolean,
    val appVersion: String?,
    val platform: String?,
    val createdAt: Long,
    /** Server time of the last edit, null when never edited. */
    val editedAt: Long?,
)

data class FeedbackEntryDetail(
    val entry: FeedbackEntry,
    val description: String,
    val location: String?,
    val appVersion: String?,
    val platform: String?,
    val comments: List<FeedbackComment>,
)

data class NewFeedbackEntry(
    val type: FeedbackType,
    val title: String,
    val description: String,
    val tags: List<FeedbackTag>,
    val location: String?,
    /** Features only, ignored for bugs. */
    val isPremium: Boolean = false,
)

/**
 * The editable fields of an existing entry. Premium is not editable. Changing [type] resets the
 * status on the server, see [FeedbackEntry.typeChangeEffect].
 */
data class FeedbackEntryChanges(
    val type: FeedbackType,
    val title: String,
    val description: String,
    val tags: List<FeedbackTag>,
    val location: String?,
) {
    /** The location as the server stores it: trimmed, blank as null, and always null for a bug. */
    val storedLocation: String?
        get() = location?.trim()?.takeIf { type == FeedbackType.FEATURE && it.isNotEmpty() }

    /** False when saving would store the same entry again (texts compared trimmed, tags as a set). */
    fun differsFrom(other: FeedbackEntryChanges): Boolean =
        type != other.type ||
            title.trim() != other.title.trim() ||
            description.trim() != other.description.trim() ||
            tags.toSet() != other.tags.toSet() ||
            storedLocation != other.storedLocation
}

/** What the server resets when an entry changes its type, so the edit form can warn first. */
enum class FeedbackTypeChangeEffect {
    /** Status starts over as OPEN; "didn't know" and "don't need it" votes and premium are dropped. */
    TO_BUG,

    /** Status starts over as REQUESTED. A bug has no votes or premium that could be lost. */
    TO_FEATURE,
}

/** Null when [newType] is the entry's current type. */
fun FeedbackEntry.typeChangeEffect(newType: FeedbackType): FeedbackTypeChangeEffect? = when {
    newType == type -> null
    newType == FeedbackType.BUG -> FeedbackTypeChangeEffect.TO_BUG
    else -> FeedbackTypeChangeEffect.TO_FEATURE
}
