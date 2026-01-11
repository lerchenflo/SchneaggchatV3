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
import org.lerchenflo.schneaggchatv3mp.app.logging.LogEntry
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
    fun getAllUsersFlow(searchterm: String = ""): Flow<List<UserDto>>

    @Query("SELECT * FROM users")
    suspend fun getAllUsers() : List<UserDto>

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
    @Query("SELECT * FROM messages WHERE (senderId = :userId OR receiverId = :userId) AND groupMessage = :gruppe ORDER BY sendDate DESC")
    fun getMessagesByUserIdFlow(userId: String, gruppe: Boolean): Flow<List<MessageWithReadersDto>>

    @Transaction
    @Query("SELECT * FROM messages WHERE (senderId = :userId OR receiverId = :userId) AND groupMessage = :gruppe ORDER BY sendDate DESC")
    suspend fun getMessagesByUserId(userId: String, gruppe: Boolean): List<MessageWithReadersDto>

    @Transaction
    @Query("SELECT * FROM messages WHERE (senderId = :userId OR receiverId = :userId) AND groupMessage = :gruppe ORDER BY sendDate DESC LIMIT :pagesize OFFSET :offset ")
    fun getMessagesByUserIdFlowPaged(userId: String, gruppe: Boolean, pagesize: Int, offset: Int): Flow<List<MessageWithReadersDto>>

    @Transaction
    @Query("SELECT * FROM messages WHERE id = :msgid")
    suspend fun getMessageById(msgid: String): MessageWithReadersDto?

    @Query("SELECT id, changedate FROM messages WHERE id != 0")
    suspend fun getMessageIdsWithChangeDates(): List<IdChangeDate>

    @Transaction
    @Query("SELECT * FROM messages WHERE sent = 0")
    suspend fun getUnsentMessages(): List<MessageWithReadersDto>

    @Transaction
    @Query("UPDATE messages SET id = :serverId, sent = 1 WHERE localPK = :localPK")
    suspend fun markMessageAsSent(serverId: String, localPK: Long)

    @Query("UPDATE messages SET readByMe = 1, changeDate = :timestamp WHERE (senderId = :userId OR receiverId = :userId) AND groupMessage = :gruppe AND readByMe = 0")
    suspend fun markAllChatMessagesRead(userId: String, gruppe: Boolean, timestamp: String)

    @Query("INSERT OR REPLACE INTO message_readers (messageId, readerID, readDate) SELECT m.id, :ownId, :timestamp FROM messages m WHERE (m.senderId = :userId OR m.receiverId = :userId) AND m.groupMessage = :gruppe AND m.readByMe = 0 AND m.id != ''")
    suspend fun addMessageReadersForChat(userId: String, gruppe: Boolean, ownId: String, timestamp: String)

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
    fun getAllGroupsWithMembersFlow(): Flow<List<GroupWithMembersDto>>

    @Transaction
    @Query("SELECT * FROM `groups`")
    suspend fun getAllGroupsWithMembers(): List<GroupWithMembersDto>

    @Upsert
    suspend fun upsertMembers(members: List<GroupMemberDto>): List<Long>

    @Query("DELETE FROM group_members WHERE groupId = :groupId")
    suspend fun deleteMembersForGroup(groupId: String)

    @Query("SELECT * FROM `groups` WHERE id = :groupid")
    suspend fun getGroupById(groupid: String?): GroupDto?

    @Transaction
    @Query("SELECT * FROM `groups` WHERE id = :groupId")
    suspend fun getGroupWithMembersById(groupId: String): GroupWithMembersDto?

    @Transaction
    @Query("SELECT * FROM `groups` WHERE id = :groupId")
    fun getGroupWithMembersByIdFlow(groupId: String): Flow<GroupWithMembersDto?>

    @Transaction
    @Query("""
        SELECT * FROM `groups`
        WHERE id IN (
            SELECT groupId FROM group_members WHERE userId = :memberId1
        )
        AND id IN (
            SELECT groupId FROM group_members WHERE userId = :memberId2
        )
    """)
    suspend fun getCommonGroupsWithMembers(memberId1: String, memberId2: String): List<GroupWithMembersDto>
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
interface LogDao {
    @Upsert
    suspend fun upsertLog(logEntry: LogEntry)

    @Query("SELECT * FROM logentry")
    fun getLogs() : Flow<List<LogEntry>>

    @Query("DELETE FROM logentry")
    suspend fun clearLogs()
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

