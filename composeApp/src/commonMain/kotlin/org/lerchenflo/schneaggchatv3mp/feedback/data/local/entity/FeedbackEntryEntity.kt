package org.lerchenflo.schneaggchatv3mp.feedback.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One feedback board entry as the server sent it for this user (votes and flags are per viewer).
 * Enums are stored by name and [tags] as a comma-separated list, so no type converter is needed.
 */
@Entity(tableName = "feedback_entries")
data class FeedbackEntryEntity(
    @PrimaryKey val id: String,
    val type: String,
    val title: String,
    val description: String,
    val tags: String,
    val status: String,
    val upvotes: Int,
    val downvotes: Int,
    val didntKnowCount: Int,
    val notNeededCount: Int,
    val myVote: String,
    val commentCount: Int,
    val latestDevComment: String?,
    val creatorName: String?,
    val isOwn: Boolean,
    val canDelete: Boolean,
    val isSeeded: Boolean,
    val isPremium: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val activityAt: Long,
)
