package org.lerchenflo.schneaggchatv3mp.feedback.domain

import kotlinx.serialization.Serializable

@Serializable
enum class FeedbackType { FEATURE, BUG }

/** Features only use REQUESTED / PLANNED / IMPLEMENTED, bugs only OPEN / CONFIRMED / FIXED. */
@Serializable
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

@Serializable
enum class FeedbackTag { CHAT, GROUPS, EVENTS, MAP, GAMES, SETTINGS, UI, NOTIFICATIONS, PERFORMANCE, OTHER }

/**
 * A user's vote on one entry. For bugs UP means "I have this too" and DOWN "can't reproduce".
 * DIDNT_KNOW is only allowed on implemented features and does not change the score.
 * NONE removes the vote.
 */
@Serializable
enum class FeedbackVote { UP, DOWN, DIDNT_KNOW, NONE }

data class FeedbackEntry(
    val id: String,
    val type: FeedbackType,
    val title: String,
    val descriptionPreview: String,
    val tags: List<FeedbackTag>,
    val status: FeedbackStatus,
    val upvotes: Int,
    val downvotes: Int,
    val didntKnowCount: Int,
    val myVote: FeedbackVote,
    val commentCount: Int,
    val latestDevComment: String?,
    val creatorName: String?,
    val isOwn: Boolean,
    val canDelete: Boolean,
    val isSeeded: Boolean,
    val createdAt: Long,
) {
    /** Up and downvotes cancel out, "didn't know" is neutral. */
    val score: Int get() = upvotes - downvotes

    val isDone: Boolean get() = status == FeedbackStatus.IMPLEMENTED || status == FeedbackStatus.FIXED

    /** User-created entries stay pinned (and highlighted) at the top for [NEW_ENTRY_PIN_MILLIS]. */
    fun isPinned(nowMillis: Long): Boolean =
        !isSeeded && nowMillis - createdAt in 0 until NEW_ENTRY_PIN_MILLIS

    /** Local vote change, so the buttons react before the server answers. */
    fun withVote(vote: FeedbackVote): FeedbackEntry {
        fun count(current: Int, target: FeedbackVote) =
            current - (if (myVote == target) 1 else 0) + (if (vote == target) 1 else 0)

        return copy(
            upvotes = count(upvotes, FeedbackVote.UP),
            downvotes = count(downvotes, FeedbackVote.DOWN),
            didntKnowCount = count(didntKnowCount, FeedbackVote.DIDNT_KNOW),
            myVote = vote,
        )
    }

    companion object {
        const val NEW_ENTRY_PIN_MILLIS = 2L * 24 * 60 * 60 * 1000
    }
}

data class FeedbackComment(
    val id: String,
    val authorId: String,
    val authorName: String?,
    val text: String,
    val isDevComment: Boolean,
    val canDelete: Boolean,
    val appVersion: String?,
    val platform: String?,
    val createdAt: Long,
)

data class FeedbackEntryDetail(
    val entry: FeedbackEntry,
    val description: String,
    val location: String?,
    val appVersion: String?,
    val platform: String?,
    val comments: List<FeedbackComment>,
)

data class FeedbackList(
    val viewerIsAdmin: Boolean,
    val entries: List<FeedbackEntry>,
)

data class NewFeedbackEntry(
    val type: FeedbackType,
    val title: String,
    val description: String,
    val tags: List<FeedbackTag>,
    val location: String?,
)
