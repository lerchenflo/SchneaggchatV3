package org.lerchenflo.schneaggchatv3mp.feedback.data.remote.dto

import kotlinx.serialization.Serializable

// Wire copies of the domain enums (same entry names), so renaming a domain entry never changes
// what goes over the network. Mapped in FeedbackResponseMapper.kt.

@Serializable
enum class NetworkFeedbackType { FEATURE, BUG }

@Serializable
enum class NetworkFeedbackStatus { REQUESTED, PLANNED, IMPLEMENTED, OPEN, CONFIRMED, FIXED }

@Serializable
enum class NetworkFeedbackTag { CHAT, GROUPS, EVENTS, MAP, GAMES, SETTINGS, UI, NOTIFICATIONS, PERFORMANCE, OTHER }

@Serializable
enum class NetworkFeedbackVote { UP, DOWN, DIDNT_KNOW, NOT_NEEDED, NONE }

@Serializable
data class FeedbackEntryResponse(
    val id: String,
    val type: NetworkFeedbackType,
    val title: String,
    val description: String,
    val tags: List<NetworkFeedbackTag>,
    val status: NetworkFeedbackStatus,
    val upvotes: Int,
    val downvotes: Int,
    val didntKnowCount: Int,
    /** Missing on servers before the neutral "don't need it" vote: then 0. */
    val notNeededCount: Int = 0,
    /**
     * Never NOT_NEEDED: released clients can't decode it, so the server sends NONE plus
     * [isNotNeeded] = true instead.
     */
    val myVote: NetworkFeedbackVote,
    val isNotNeeded: Boolean = false,
    val commentCount: Int,
    val latestDevComment: String? = null,
    /** Null for seeded entries (no creator). */
    val creatorId: String? = null,
    val isOwn: Boolean,
    val canDelete: Boolean,
    /** Viewer is the creator or an admin and the entry is not seeded. Missing on older servers: then false. */
    val canEdit: Boolean = false,
    val isSeeded: Boolean,
    /** Missing on servers before the premium mark: then false. */
    val isPremium: Boolean = false,
    val createdAt: Long,
    /** Sync cursor, sent back as the IdTimeStamp timestamp on /feedback/sync. */
    val updatedAt: Long,
    val activityAt: Long,
    /** Epoch millis of the last edit, null when never edited. */
    val editedAt: Long? = null,
)

@Serializable
data class FeedbackSyncResponse(
    val viewerIsAdmin: Boolean,
    val updatedEntries: List<FeedbackEntryResponse>,
    /** Ids the client sent that are deleted or gone. Only filled on page 0. */
    val deletedEntries: List<String>,
    val moreEntries: Boolean,
    /** Server clock (epoch millis), read before the entries. Stored as the "last seen" time. */
    val serverTime: Long,
)

@Serializable
data class FeedbackCommentResponse(
    val id: String,
    val authorId: String,
    val text: String,
    val isDevComment: Boolean,
    val canDelete: Boolean,
    /** Viewer is the author. Missing on older servers: then false. */
    val canEdit: Boolean = false,
    val appVersion: String? = null,
    val platform: String? = null,
    val createdAt: Long,
    /** Epoch millis of the last edit, null when never edited. */
    val editedAt: Long? = null,
)

@Serializable
data class FeedbackEntryDetailResponse(
    val entry: FeedbackEntryResponse,
    val description: String,
    val location: String? = null,
    val appVersion: String? = null,
    val platform: String? = null,
    val comments: List<FeedbackCommentResponse>,
)

@Serializable
data class FeedbackCreateRequest(
    val type: NetworkFeedbackType,
    val title: String,
    val description: String,
    val tags: List<NetworkFeedbackTag>,
    val location: String?,
    val appVersion: String?,
    val platform: String?,
    val premium: Boolean,
)

/** Body of /feedback/edit. [location] is FEATURE only: null for a bug. */
@Serializable
data class FeedbackEditRequest(
    val entryId: String,
    val type: NetworkFeedbackType,
    val title: String,
    val description: String,
    val tags: List<NetworkFeedbackTag>,
    val location: String?,
)

@Serializable
data class FeedbackVoteRequest(
    val entryId: String,
    val vote: NetworkFeedbackVote,
)

@Serializable
data class FeedbackCommentRequest(
    val entryId: String,
    val text: String,
    val appVersion: String?,
    val platform: String?,
)

/** Body of /feedback/editcomment. */
@Serializable
data class FeedbackCommentEditRequest(
    val commentId: String,
    val text: String,
)

@Serializable
data class FeedbackStatusRequest(
    val entryId: String,
    val status: NetworkFeedbackStatus,
)
