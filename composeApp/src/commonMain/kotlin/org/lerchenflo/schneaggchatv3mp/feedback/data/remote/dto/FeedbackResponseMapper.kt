package org.lerchenflo.schneaggchatv3mp.feedback.data.remote.dto

import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackComment
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackEntry
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackEntryDetail
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackList

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
