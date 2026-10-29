package org.lerchenflo.schneaggchatv3mp.feedback.data.remote.dto

import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackComment
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackEntry
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackEntryDetail
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackList
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackStatus
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackTag
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackType
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackVote

// ─── Enums ───────────────────────────────────────────────────────────────────

fun NetworkFeedbackType.toFeedbackType(): FeedbackType = when (this) {
    NetworkFeedbackType.FEATURE -> FeedbackType.FEATURE
    NetworkFeedbackType.BUG -> FeedbackType.BUG
}

fun FeedbackType.toNetworkFeedbackType(): NetworkFeedbackType = when (this) {
    FeedbackType.FEATURE -> NetworkFeedbackType.FEATURE
    FeedbackType.BUG -> NetworkFeedbackType.BUG
}

fun NetworkFeedbackStatus.toFeedbackStatus(): FeedbackStatus = when (this) {
    NetworkFeedbackStatus.REQUESTED -> FeedbackStatus.REQUESTED
    NetworkFeedbackStatus.PLANNED -> FeedbackStatus.PLANNED
    NetworkFeedbackStatus.IMPLEMENTED -> FeedbackStatus.IMPLEMENTED
    NetworkFeedbackStatus.OPEN -> FeedbackStatus.OPEN
    NetworkFeedbackStatus.CONFIRMED -> FeedbackStatus.CONFIRMED
    NetworkFeedbackStatus.FIXED -> FeedbackStatus.FIXED
}

fun FeedbackStatus.toNetworkFeedbackStatus(): NetworkFeedbackStatus = when (this) {
    FeedbackStatus.REQUESTED -> NetworkFeedbackStatus.REQUESTED
    FeedbackStatus.PLANNED -> NetworkFeedbackStatus.PLANNED
    FeedbackStatus.IMPLEMENTED -> NetworkFeedbackStatus.IMPLEMENTED
    FeedbackStatus.OPEN -> NetworkFeedbackStatus.OPEN
    FeedbackStatus.CONFIRMED -> NetworkFeedbackStatus.CONFIRMED
    FeedbackStatus.FIXED -> NetworkFeedbackStatus.FIXED
}

fun NetworkFeedbackTag.toFeedbackTag(): FeedbackTag = when (this) {
    NetworkFeedbackTag.CHAT -> FeedbackTag.CHAT
    NetworkFeedbackTag.GROUPS -> FeedbackTag.GROUPS
    NetworkFeedbackTag.EVENTS -> FeedbackTag.EVENTS
    NetworkFeedbackTag.MAP -> FeedbackTag.MAP
    NetworkFeedbackTag.GAMES -> FeedbackTag.GAMES
    NetworkFeedbackTag.SETTINGS -> FeedbackTag.SETTINGS
    NetworkFeedbackTag.UI -> FeedbackTag.UI
    NetworkFeedbackTag.NOTIFICATIONS -> FeedbackTag.NOTIFICATIONS
    NetworkFeedbackTag.PERFORMANCE -> FeedbackTag.PERFORMANCE
    NetworkFeedbackTag.OTHER -> FeedbackTag.OTHER
}

fun FeedbackTag.toNetworkFeedbackTag(): NetworkFeedbackTag = when (this) {
    FeedbackTag.CHAT -> NetworkFeedbackTag.CHAT
    FeedbackTag.GROUPS -> NetworkFeedbackTag.GROUPS
    FeedbackTag.EVENTS -> NetworkFeedbackTag.EVENTS
    FeedbackTag.MAP -> NetworkFeedbackTag.MAP
    FeedbackTag.GAMES -> NetworkFeedbackTag.GAMES
    FeedbackTag.SETTINGS -> NetworkFeedbackTag.SETTINGS
    FeedbackTag.UI -> NetworkFeedbackTag.UI
    FeedbackTag.NOTIFICATIONS -> NetworkFeedbackTag.NOTIFICATIONS
    FeedbackTag.PERFORMANCE -> NetworkFeedbackTag.PERFORMANCE
    FeedbackTag.OTHER -> NetworkFeedbackTag.OTHER
}

fun NetworkFeedbackVote.toFeedbackVote(): FeedbackVote = when (this) {
    NetworkFeedbackVote.UP -> FeedbackVote.UP
    NetworkFeedbackVote.DOWN -> FeedbackVote.DOWN
    NetworkFeedbackVote.DIDNT_KNOW -> FeedbackVote.DIDNT_KNOW
    NetworkFeedbackVote.NONE -> FeedbackVote.NONE
}

fun FeedbackVote.toNetworkFeedbackVote(): NetworkFeedbackVote = when (this) {
    FeedbackVote.UP -> NetworkFeedbackVote.UP
    FeedbackVote.DOWN -> NetworkFeedbackVote.DOWN
    FeedbackVote.DIDNT_KNOW -> NetworkFeedbackVote.DIDNT_KNOW
    FeedbackVote.NONE -> NetworkFeedbackVote.NONE
}

// ─── Entries ─────────────────────────────────────────────────────────────────

fun FeedbackEntryResponse.toFeedbackEntry(): FeedbackEntry = FeedbackEntry(
    id = id,
    type = type.toFeedbackType(),
    title = title,
    descriptionPreview = descriptionPreview,
    tags = tags.map { it.toFeedbackTag() },
    status = status.toFeedbackStatus(),
    upvotes = upvotes,
    downvotes = downvotes,
    didntKnowCount = didntKnowCount,
    myVote = myVote.toFeedbackVote(),
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
