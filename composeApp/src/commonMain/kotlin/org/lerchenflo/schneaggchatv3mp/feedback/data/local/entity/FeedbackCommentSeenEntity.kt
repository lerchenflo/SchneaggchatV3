package org.lerchenflo.schneaggchatv3mp.feedback.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Comment count of a feedback entry the user has seen on this device. Its own table, so a sync
 * that replaces the entry row never resets it. Only own entries get a row.
 */
@Entity(tableName = "feedback_comments_seen")
data class FeedbackCommentSeenEntity(
    @PrimaryKey val entryId: String,
    val seenCommentCount: Int,
)
