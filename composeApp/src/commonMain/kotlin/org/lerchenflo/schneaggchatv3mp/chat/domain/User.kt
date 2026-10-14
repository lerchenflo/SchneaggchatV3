package org.lerchenflo.schneaggchatv3mp.chat.domain

import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

data class User(
    val id: String,
    val lastChanged: Long = 0L,
    val name: String = "",
    val nickName: String? = null,
    val description: String?,
    val status: String?,
    val location: UserLocation? = null,
    val locationShared: Boolean = false,
    // Per-friend advanced-location settings (what we share TOWARDS this friend)
    val shareSpeedHeading: Boolean = false,
    val snailTrail: Boolean = false,
    val wakeupEnabled: Boolean = false,
    val profilePictureUrl: String = "",
    val friendshipStatus: FriendshipStatus?,
    val requesterId: String? = null,
    val notisMuted: Boolean = false,
    // Epoch millis this friend was last seen online, null if unknown/never. Not to be confused
    // with "online right now", which lives in UserLocalDataSource.onlineFriendIdsFlow and is never
    // persisted.
    val lastSeen: Long? = null,
    val birthDate: String? = null, //YYYY-MM-dd

    // Optional phone number, shared with accepted friends
    val phoneNumber: String? = null,

    val email: String? = null,
    val emailVerifiedAt: Long?,

    val createdAt: Long?,

    val profilePicUpdatedAt: Long,
) {
    fun isEmailVerified() : Boolean {
        return emailVerifiedAt != null
    }

    fun isLocationValid() : Boolean {
        if (location == null) return false
        val dateI = Instant.fromEpochMilliseconds(location.date)
        return Clock.System.now() < (dateI + 24.hours)

    }

    val displayName: String get() = nickName?.takeIf { it.isNotBlank() } ?: name

    override fun toString(): String {
        return """
        User(
            id='$id',
            lastChanged=$lastChanged,
            name='$name',
            nickName='$nickName',
            description=$description,
            status=$status,
            location=$location,
            locationShared=$locationShared,
            shareSpeedHeading=$shareSpeedHeading,
            snailTrail=$snailTrail,
            wakeupEnabled=$wakeupEnabled,
            profilePictureUrl='$profilePictureUrl',
            friendshipStatus=$friendshipStatus,
            requesterId=$requesterId,
            notisMuted=$notisMuted,
            lastSeen=$lastSeen,
            birthDate=$birthDate,
            phoneNumber=$phoneNumber,
            email=$email,
            emailVerifiedAt=$emailVerifiedAt,
            createdAt=$createdAt,
        )
    """.trimIndent()
    }
}
