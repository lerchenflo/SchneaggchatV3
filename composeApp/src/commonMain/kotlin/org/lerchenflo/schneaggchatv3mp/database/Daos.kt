package org.lerchenflo.schneaggchatv3mp.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.chat.domain.Group
import org.lerchenflo.schneaggchatv3mp.chat.domain.GroupMember
import org.lerchenflo.schneaggchatv3mp.chat.domain.GroupWithMembers
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageReader
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageWithReaders
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.todolist.data.TodoEntityDto

@Dao
interface UserDao {

    @Upsert()
    suspend fun upsert(user: User) //Suspend: Async mit warten

    @Query("DELETE FROM users WHERE id = :userid")
    suspend fun delete(userid: Long)

    @Query("SELECT * FROM users WHERE name LIKE '%' || :searchterm || '%'")
    fun getallusers(searchterm: String = ""): Flow<List<User>>


    @Query("SELECT id, changedate FROM users")
    suspend fun getUserIdsWithChangeDates(): List<IdChangeDate>

}


@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message): Long

    @Update
    suspend fun updateMessage(message: Message)

    @Upsert()
    suspend fun upsertMessage(message: Message): Long

    @Upsert
    suspend fun upsertMessages(messages: List<Message>)


    @Transaction
    @Query("SELECT * FROM messages WHERE id = :id")
    fun getMessageWithReaders(id: Long): Flow<MessageWithReaders>


    @Transaction
    @Query("SELECT * FROM messages")
    fun getAllMessagesWithReaders(): Flow<List<MessageWithReaders>>

    @Transaction
    @Query("SELECT * FROM messages WHERE (senderId = :userId OR receiverId = :userId) AND groupMessage = :gruppe ")
    fun getMessagesByUserId(userId: Long, gruppe: Boolean): Flow<List<MessageWithReaders>>

    @Query("SELECT id, changedate FROM messages WHERE id != 0")
    suspend fun getMessageIdsWithChangeDates(): List<IdChangeDate>

    @Transaction
    @Query("SELECT * FROM messages WHERE sent = 0")
    suspend fun getUnsentMessages(): List<Message>

    @Transaction
    @Query("UPDATE messages SET id = :serverId, sent = 1 WHERE localPK = :localPK")
    suspend fun markMessageAsSent(serverId: Long, localPK: Long)


    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getMessageById(id: Long): Message?


}

@Dao
interface MessageReaderDao {

    @Upsert()
    suspend fun upsertReader(reader: MessageReader): Long

    @Upsert
    suspend fun upsertReaders(readers: List<MessageReader>): List<Long>

    @Query("DELETE FROM message_readers WHERE messageId = :messageId")
    suspend fun deleteReadersForMessage(messageId: Long)


}

@Dao
interface GroupDao {
    @Upsert
    suspend fun upsertGroup(group: Group)

    @Query("SELECT id, changedate FROM `groups`")
    suspend fun getGroupIdsWithChangeDates(): List<IdChangeDate>

    @Query("DELETE FROM `groups` WHERE id = :groupid")
    suspend fun deleteGroup(groupid: Long)

    @Transaction
    @Query("SELECT * FROM `groups`")
    fun getAllGroupsWithMembers(): Flow<List<GroupWithMembers>>

    @Upsert
    suspend fun upsertMembers(members: List<GroupMember>): List<Long>

    @Query("DELETE FROM group_members WHERE group_id = :groupId")
    suspend fun deleteMembersForGroup(groupId: Long)
}


@Dao
interface TodolistDao{
    @Upsert
    suspend fun upsertTodo(todo: TodoEntityDto)

    @Query("DELETE FROM todoentitydto WHERE id = :todoid")
    suspend fun delete(todoid: Long)

    @Transaction
    @Query("SELECT * FROM todoentitydto")
    fun getAllTodos(): Flow<List<TodoEntityDto>>

    @Query("SELECT id, changedate FROM todoentitydto")
    suspend fun getTodoIdsWithChangeDates(): List<IdChangeDate>
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



    @Transaction
    suspend fun clearAll() {
        // Clear tables in proper order to respect foreign key constraints
        clearMessageReaders()  // Child table first
        clearMessages()        // Then parent table
        clearUsers()           // Finally users table
        clearGroupMembers()
        clearGroups()

    }
}

@Serializable
data class IdChangeDate(
    val id: Long,
    val changedate: String
)

@Serializable
data class IdOperation(val Status: String = "", val Id: Long = 0L)

