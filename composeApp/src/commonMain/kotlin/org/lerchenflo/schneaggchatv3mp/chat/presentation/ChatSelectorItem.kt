package org.lerchenflo.schneaggchatv3mp.chat.presentation

import org.lerchenflo.schneaggchatv3mp.database.tables.GroupWithMembers
import org.lerchenflo.schneaggchatv3mp.database.tables.MessageWithReaders
import org.lerchenflo.schneaggchatv3mp.database.tables.User

//Gegnerauswahl gegner
data class ChatSelectorItem(
    val id: Long,
    val gruppe: Boolean,
    val lastmessage: MessageWithReaders?,
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
        entity = ChatEntity.GroupEntity(this)
    )
}