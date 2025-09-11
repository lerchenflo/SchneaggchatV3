package org.lerchenflo.schneaggchatv3mp.chat.presentation.chatselector

import org.lerchenflo.schneaggchatv3mp.chat.domain.GroupWithMembers
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageWithReaders
import org.lerchenflo.schneaggchatv3mp.chat.domain.User

//Gegnerauswahl gegner
data class ChatSelectorItem(
    val id: Long,
    val gruppe: Boolean,
    val lastmessage: MessageWithReaders?,
    val unreadMessageCount: Int,
    val unsentMessageCount: Int,
    val entity: ChatEntity
){
    fun getName(): String {
        return when (entity) {
            is ChatEntity.UserEntity -> entity.user.name
            is ChatEntity.GroupEntity -> entity.groupWithMembers.group.name
        }
    }


    fun getStatus(): String {
        return when (entity) {
            is ChatEntity.UserEntity -> entity.user.status
            is ChatEntity.GroupEntity -> entity.groupWithMembers.group.description
        }
    }

    fun getDescription(): String {
        return when (entity) {
            is ChatEntity.UserEntity -> entity.user.description
            is ChatEntity.GroupEntity -> entity.groupWithMembers.group.description
        }
    }
}


sealed class ChatEntity {
    data class UserEntity(val user: User) : ChatEntity()
    data class GroupEntity(val groupWithMembers: GroupWithMembers) : ChatEntity()
}


fun User.asChatSelectorItem(): ChatSelectorItem {

    return ChatSelectorItem(
        id = this.id,
        gruppe = false,
        lastmessage = null,
        unreadMessageCount = 0,
        unsentMessageCount = 0,
        entity = ChatEntity.UserEntity(this)
    )
}

// -- extension for GroupWithMembers
fun GroupWithMembers.asChatSelectorItem(): ChatSelectorItem {
    // adjust the access if your GroupWithMembers uses a different property name
    val groupId = this.group.id

    return ChatSelectorItem(
        id = groupId,
        gruppe = true,
        lastmessage = null,
        unreadMessageCount = 0,
        unsentMessageCount = 0,
        entity = ChatEntity.GroupEntity(this)
    )
}