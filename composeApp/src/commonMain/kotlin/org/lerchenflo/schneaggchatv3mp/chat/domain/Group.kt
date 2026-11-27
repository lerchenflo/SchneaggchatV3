package org.lerchenflo.schneaggchatv3mp.chat.domain

data class Group(
    val id: String,
    val name: String,
    val profilePictureUrl: String,
    val description: String,
    val createDate: Long,

    val updatedAt: Long,
    val profilePicUpdatedAt: Long,

    val expiresAt: Long? = null,

    /** On: every member may add their own friends (in an event group: invite them to the event). Off: only admins add. */
    val membersCanAdd: Boolean = false,

    val notisMuted: Boolean = false,
    val members: List<GroupMember>
)

/** Admins always add members; everyone else only while [Group.membersCanAdd] is on. */
fun Group.canAddMembers(userId: String): Boolean =
    membersCanAdd || members.any { it.userId == userId && it.admin }
