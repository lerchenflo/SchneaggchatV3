package org.lerchenflo.schneaggchatv3mp.feedback.data.local.entity

import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackEntry
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackStatus
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackTag
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackType
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackVote

private const val TAG_SEPARATOR = ","

fun FeedbackEntry.toEntity(): FeedbackEntryEntity = FeedbackEntryEntity(
    id = id,
    type = type.name,
    title = title,
    description = description,
    tags = tags.joinToString(TAG_SEPARATOR) { it.name },
    status = status.name,
    upvotes = upvotes,
    downvotes = downvotes,
    didntKnowCount = didntKnowCount,
    notNeededCount = notNeededCount,
    myVote = myVote.name,
    commentCount = commentCount,
    latestDevComment = latestDevComment,
    creatorName = creatorName,
    isOwn = isOwn,
    canDelete = canDelete,
    isSeeded = isSeeded,
    isPremium = isPremium,
    createdAt = createdAt,
    updatedAt = updatedAt,
    activityAt = activityAt,
)

/** Null for a row with an unknown type or status, e.g. written by a newer app version. */
fun FeedbackEntryEntity.toFeedbackEntry(): FeedbackEntry? {
    val entryType = FeedbackType.entries.firstOrNull { it.name == type } ?: return null
    val entryStatus = FeedbackStatus.entries.firstOrNull { it.name == status } ?: return null
    return FeedbackEntry(
        id = id,
        type = entryType,
        title = title,
        description = description,
        tags = tags.split(TAG_SEPARATOR).mapNotNull { name -> FeedbackTag.entries.firstOrNull { it.name == name } },
        status = entryStatus,
        upvotes = upvotes,
        downvotes = downvotes,
        didntKnowCount = didntKnowCount,
        notNeededCount = notNeededCount,
        myVote = FeedbackVote.entries.firstOrNull { it.name == myVote } ?: FeedbackVote.NONE,
        commentCount = commentCount,
        latestDevComment = latestDevComment,
        creatorName = creatorName,
        isOwn = isOwn,
        canDelete = canDelete,
        isSeeded = isSeeded,
        isPremium = isPremium,
        createdAt = createdAt,
        updatedAt = updatedAt,
        activityAt = activityAt,
    )
}
