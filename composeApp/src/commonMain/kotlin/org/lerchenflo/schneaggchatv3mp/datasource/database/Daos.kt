package org.lerchenflo.schneaggchatv3mp.datasource.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.GroupDto
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.GroupMemberDto
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.relations.GroupWithMembersDto
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.MessageDto
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.MessageReaderDto
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.relations.MessageWithReadersDto
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.UserDto
import org.lerchenflo.schneaggchatv3mp.todolist.data.TodoEntityDto

@Dao
interface UserDao {

    @Upsert()
    suspend fun upsert(userDto: UserDto) //Suspend: Async mit warten

    @Query("DELETE FROM users WHERE id = :userid")
    suspend fun delete(userid: String)

    @Query("SELECT * FROM users WHERE name LIKE '%' || :searchterm || '%'")
    fun getallusers(searchterm: String = ""): Flow<List<UserDto>>

    @Query("SELECT * FROM users WHERE id = :userid")
    fun getUserbyIdFlow(userid: String?): Flow<UserDto?>

    @Query("SELECT * FROM users WHERE id = :userid")
    suspend fun getUserbyId(userid: String?): UserDto?

    @Query("SELECT id, changedate FROM users")
    suspend fun getUserIdsWithChangeDates(): List<IdChangeDate>

}


@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessageDto(messageDto: MessageDto): Long

    @Update
    suspend fun updateMessageDto(messageDto: MessageDto)

    @Query("DELETE FROM messages WHERE id = :msgid")
    suspend fun deleteMessageDtoById(msgid: String)

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getMessageDtoById(id: String): MessageDto?

    @Transaction
    suspend fun upsertMessageDto(messageDto: MessageDto): MessageDto {
        val existing = getMessageDtoById(messageDto.id.orEmpty())
        if (existing != null) {
            updateMessageDto(messageDto.copy(localPK = existing.localPK))
        } else {
            insertMessageDto(messageDto)
        }
        // Get the message after insert/update
        val returnedMessage = getMessageDtoById(messageDto.id.orEmpty())
        return returnedMessage ?: messageDto
    }



    @Transaction
    @Query("SELECT * FROM messages WHERE id = :id")
    fun getMessageWithReadersByIdFlow(id: String): Flow<MessageWithReadersDto>


    @Transaction
    @Query("SELECT * FROM messages")
    fun getAllMessagesWithReadersFlow(): Flow<List<MessageWithReadersDto>>

    @Transaction
    @Query("SELECT * FROM messages WHERE (senderId = :userId OR receiverId = :userId) AND groupMessage = :gruppe ")
    fun getMessagesByUserIdFlow(userId: String, gruppe: Boolean): Flow<List<MessageWithReadersDto>>

    @Transaction
    @Query("SELECT * FROM messages WHERE (senderId = :userId OR receiverId = :userId) AND groupMessage = :gruppe ")
    fun getMessagesByUserId(userId: String, gruppe: Boolean): List<MessageWithReadersDto>

    @Transaction
    @Query("SELECT * FROM messages WHERE id = :Id")
    fun getMessageById(Id: String): MessageWithReadersDto?

    @Query("SELECT id, changedate FROM messages WHERE id != 0")
    suspend fun getMessageIdsWithChangeDates(): List<IdChangeDate>

    @Transaction
    @Query("SELECT * FROM messages WHERE sent = 0")
    suspend fun getUnsentMessages(): List<MessageDto>

    @Transaction
    @Query("UPDATE messages SET id = :serverId, sent = 1 WHERE localPK = :localPK")
    suspend fun markMessageAsSent(serverId: String, localPK: Long)

}

@Dao
interface MessageReaderDao {

    @Upsert()
    suspend fun upsertReader(reader: MessageReaderDto): Long

    @Upsert
    suspend fun upsertReaders(readers: List<MessageReaderDto>): List<Long>

    @Query("DELETE FROM message_readers WHERE messageId = :messageId")
    suspend fun deleteReadersForMessage(messageId: String)
}



@Dao
interface GroupDao {
    @Upsert
    suspend fun upsertGroup(group: GroupDto)

    @Query("SELECT id, changedate FROM `groups`")
    suspend fun getGroupIdsWithChangeDates(): List<IdChangeDate>

    @Query("DELETE FROM `groups` WHERE id = :groupid")
    suspend fun deleteGroup(groupid: String)

    @Transaction
    @Query("SELECT * FROM `groups`")
    fun getAllGroupsWithMembers(): Flow<List<GroupWithMembersDto>>

    @Upsert
    suspend fun upsertMembers(members: List<GroupMemberDto>): List<Long>

    @Query("DELETE FROM group_members WHERE group_id = :groupId")
    suspend fun deleteMembersForGroup(groupId: String)
}


@Dao
interface TodolistDao{
    @Upsert
    suspend fun upsertTodo(todo: TodoEntityDto)

    @Update
    suspend fun updateTodo(todo: TodoEntityDto)

    @Insert
    suspend fun insertTodo(todo: TodoEntityDto)

    @Query("DELETE FROM todoentitydto WHERE id = :todoid")
    suspend fun delete(todoid: String)

    @Transaction
    @Query("SELECT * FROM todoentitydto")
    fun getAllTodos(): Flow<List<TodoEntityDto>>

    @Query("SELECT id, changedate FROM todoentitydto")
    suspend fun getTodoIdsWithChangeDates(): List<IdChangeDate>

    @Query("SELECT * FROM todoentitydto WHERE id = :id")
    suspend fun getTodoById(id: String) : TodoEntityDto?
}


@Dao
interface AllDatabaseDao {
    @Query("DELETE FROM message_readers")
    suspend fun clearMessageReaders()

    @Query("DELETE FROM messages")
    suspend fun clearMessages()

    @Query("DELETE FROM users")
    suspend fun clearUsers()

    @Query("DELETE FROM `groups`")
    suspend fun clearGroups()

    @Query("DELETE FROM group_members")
    suspend fun clearGroupMembers()

    @Query("DELETE FROM todoentitydto")
    suspend fun clearTodos()



    @Transaction
    suspend fun clearAll() {
        // Clear tables in proper order to respect foreign key constraints
        clearMessageReaders()  // Child table first
        clearMessages()        // Then parent table
        clearUsers()           // Finally users table
        clearGroupMembers()
        clearGroups()
        clearTodos()

    }
}

@Serializable
data class IdChangeDate(
    val id: String,
    val changedate: String
)

