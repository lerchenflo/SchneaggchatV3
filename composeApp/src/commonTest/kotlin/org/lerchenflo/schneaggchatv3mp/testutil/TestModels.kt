package org.lerchenflo.schneaggchatv3mp.testutil

import org.lerchenflo.schneaggchatv3mp.chat.domain.ChatListItem
import org.lerchenflo.schneaggchatv3mp.chat.domain.FriendshipStatus
import org.lerchenflo.schneaggchatv3mp.chat.domain.Group
import org.lerchenflo.schneaggchatv3mp.chat.domain.GroupMember
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageType
import org.lerchenflo.schneaggchatv3mp.chat.domain.User

// Builders for the domain models the tests need, with every field a test usually doesn't care
// about filled in. Ids double as names unless a name is given.

/** A user; an accepted friend unless [friendshipStatus] says otherwise. */
fun testUser(
    id: String,
    name: String = id,
    friendshipStatus: FriendshipStatus? = FriendshipStatus.ACCEPTED,
    requesterId: String? = null,
    nickName: String? = null,
) = User(
    id = id,
    name = name,
    nickName = nickName,
    description = null,
    status = null,
    friendshipStatus = friendshipStatus,
    requesterId = requesterId,
    emailVerifiedAt = null,
    createdAt = null,
    profilePicUpdatedAt = 0L,
)

/** A group with one non-admin member per id in [memberIds], except the ids in [adminIds]. */
fun testGroup(
    id: String,
    name: String = id,
    memberIds: List<String> = emptyList(),
    adminIds: List<String> = emptyList(),
    description: String = "",
) = Group(
    id = id,
    name = name,
    profilePictureUrl = "",
    description = description,
    createDate = 0L,
    updatedAt = 0L,
    profilePicUpdatedAt = 0L,
    members = memberIds.mapIndexed { index, userId ->
        GroupMember(
            groupId = id,
            userId = userId,
            joinDate = "0",
            admin = userId in adminIds,
            color = index,
            memberName = userId,
        )
    },
)

/**
 * A sent text message. For a direct message [receiverId] is the other user (or the own user for
 * an incoming one), for a group message ([group] true) it is the group id.
 */
fun testMessage(
    id: String?,
    senderId: String,
    receiverId: String,
    sendDate: Long,
    content: String = "message $id",
    group: Boolean = false,
    myMessage: Boolean = false,
    readByMe: Boolean = myMessage,
    sent: Boolean = true,
    localPK: Long = 0L,
    msgType: MessageType = MessageType.TEXT,
) = Message(
    localPK = localPK,
    id = id,
    msgType = msgType,
    content = content,
    senderId = senderId,
    receiverId = receiverId,
    sendDate = sendDate.toString(),
    changeDate = sendDate.toString(),
    groupMessage = group,
    sent = sent,
    myMessage = myMessage,
    readByMe = readByMe,
    readers = emptyList(),
)

/** A chat row as the chat list shows it. */
fun testChatListItem(
    id: String,
    isGroup: Boolean = false,
    name: String = id,
    unreadMessageCount: Int = 0,
    pinned: Long = 0L,
    friendshipStatus: FriendshipStatus? = if (isGroup) null else FriendshipStatus.ACCEPTED,
    requesterId: String? = null,
) = ChatListItem(
    id = id,
    isGroup = isGroup,
    name = name,
    unreadMessageCount = unreadMessageCount,
    pinned = pinned,
    friendshipStatus = friendshipStatus,
    requesterId = requesterId,
)
