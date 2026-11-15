package org.lerchenflo.schneaggchatv3mp.chat.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Name and picture of a user who is not in the users table (a group co-member, an event guest, a
 * mention), as the server's lookup returned it. A cache: rows are fetched again after a day and
 * deleted five days after their last fetch.
 */
@Entity(tableName = "looked_up_users")
data class LookedUpUserEntity(
    @PrimaryKey(autoGenerate = false)
    val id: String,

    // Empty for a deleted account
    val name: String,

    // Null: the user's privacy setting hides their picture from the own user
    val profilePicUpdatedAt: Long?,

    // The account no longer exists
    val deleted: Boolean,

    // Epoch millis of the last lookup that returned this row
    val fetchedAt: Long,

    // Local picture file; empty when hidden or not loaded yet
    val profilePictureUrl: String,
)
