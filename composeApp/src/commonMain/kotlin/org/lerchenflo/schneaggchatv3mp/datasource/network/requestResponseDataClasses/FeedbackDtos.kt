package org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses

import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackComment
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackEntry
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackEntryDetail
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackList
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackStatus
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackTag
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackType
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackVote

@Serializable
data class FeedbackEntryResponse(
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
    val latestDevComment: String? = null,
    val creatorName: String? = null,
    val isOwn: Boolean,
    val canDelete: Boolean,
    val isSeeded: Boolean,
    val createdAt: Long,
)

@Serializable
data class FeedbackListResponse(
    val viewerIsAdmin: Boolean,
    val entries: List<FeedbackEntryResponse>,
)

@Serializable
data class FeedbackCommentResponse(
    val id: String,
    val authorId: String,
    val authorName: String? = null,
    val text: String,
    val isDevComment: Boolean,
    val canDelete: Boolean,
    val appVersion: String? = null,
    val platform: String? = null,
    val createdAt: Long,
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
    val type: FeedbackType,
    val title: String,
    val description: String,
    val tags: List<FeedbackTag>,
    val location: String?,
    val appVersion: String?,
    val platform: String?,
)

@Serializable
data class FeedbackVoteRequest(
    val entryId: String,
    val vote: FeedbackVote,
)

@Serializable
data class FeedbackCommentRequest(
    val entryId: String,
    val text: String,
    val appVersion: String?,
    val platform: String?,
)

@Serializable
data class FeedbackStatusRequest(
    val entryId: String,
    val status: FeedbackStatus,
)

fun FeedbackEntryResponse.toFeedbackEntry(): FeedbackEntry = FeedbackEntry(
    id = id,
    type = type,
    title = title,
    descriptionPreview = descriptionPreview,
    tags = tags,
    status = status,
    upvotes = upvotes,
    downvotes = downvotes,
    didntKnowCount = didntKnowCount,
    myVote = myVote,
    commentCount = commentCount,
    latestDevComment = latestDevComment,
    creatorName = creatorName,
    isOwn = isOwn,
    canDelete = canDelete,
    isSeeded = isSeeded,
    createdAt = createdAt,
)

fun FeedbackListResponse.toFeedbackList(): FeedbackList = FeedbackList(
    viewerIsAdmin = viewerIsAdmin,
    entries = entries.map { it.toFeedbackEntry() },
)

fun FeedbackCommentResponse.toFeedbackComment(): FeedbackComment = FeedbackComment(
    id = id,
    authorId = authorId,
    authorName = authorName,
    text = text,
    isDevComment = isDevComment,
    canDelete = canDelete,
    appVersion = appVersion,
    platform = platform,
    createdAt = createdAt,
)

fun FeedbackEntryDetailResponse.toFeedbackEntryDetail(): FeedbackEntryDetail = FeedbackEntryDetail(
    entry = entry.toFeedbackEntry(),
    description = description,
    location = location,
    appVersion = appVersion,
    platform = platform,
    comments = comments.map { it.toFeedbackComment() },
)
