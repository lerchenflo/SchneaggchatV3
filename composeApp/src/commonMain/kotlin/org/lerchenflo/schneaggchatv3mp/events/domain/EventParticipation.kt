package org.lerchenflo.schneaggchatv3mp.events.domain

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.event_participation_accepted
import schneaggchatv3mp.composeapp.generated.resources.event_participation_dismissed
import schneaggchatv3mp.composeapp.generated.resources.event_participation_seen

@Serializable
enum class EventParticipationStatus {
    SEEN,
    ACCEPTED,
    DISMISSED,
}

/**
 * How one user responded to an event. At most one entry per user in [Event.participations];
 * [updatedAt] is always the server's timestamp.
 */
@Serializable
data class EventParticipation(
    val userId: String,
    val status: EventParticipationStatus,
    val updatedAt: Long,
)

fun Event.participationOf(userId: String): EventParticipation? =
    participations.firstOrNull { it.userId == userId }

/** True while [userId] has not opened this event yet. The creator never counts as unseen. */
fun Event.isUnseenBy(userId: String): Boolean =
    creatorId != userId && participationOf(userId) == null

/**
 * Everyone going, creator included. The creator is added unconditionally because events created
 * before participations existed carry no entry for them, and a creator can not un-accept their own
 * event anyway.
 */
fun Event.goingUserIds(): List<String> =
    (listOf(creatorId) + participations.filter { it.status == EventParticipationStatus.ACCEPTED }.map { it.userId })
        .distinct()

/**
 * The creator counts as going whatever their stored entry says - see [goingUserIds] - so the other
 * buckets never list them, or they would show up twice.
 */
fun Event.userIdsWithStatus(status: EventParticipationStatus): List<String> =
    participations.filter { it.status == status && it.userId != creatorId }.map { it.userId }

/** Own status as the UI should read it, with the creator's permanent ACCEPTED applied. */
fun Event.statusOf(userId: String): EventParticipationStatus? =
    if (creatorId == userId) EventParticipationStatus.ACCEPTED else participationOf(userId)?.status

fun EventParticipationStatus.icon(): ImageVector = when (this) {
    EventParticipationStatus.SEEN -> Icons.Default.Visibility
    EventParticipationStatus.ACCEPTED -> Icons.Default.CheckCircle
    EventParticipationStatus.DISMISSED -> Icons.Default.RemoveCircleOutline
}

fun EventParticipationStatus.labelRes(): StringResource = when (this) {
    EventParticipationStatus.SEEN -> Res.string.event_participation_seen
    EventParticipationStatus.ACCEPTED -> Res.string.event_participation_accepted
    EventParticipationStatus.DISMISSED -> Res.string.event_participation_dismissed
}
