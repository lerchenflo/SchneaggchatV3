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
import org.lerchenflo.schneaggchatv3mp.chat.data.local.entity.ChatAggregate
import org.lerchenflo.schneaggchatv3mp.chat.data.local.entity.GroupEntity
import org.lerchenflo.schneaggchatv3mp.chat.data.local.entity.GroupMemberEntity
import org.lerchenflo.schneaggchatv3mp.chat.data.local.entity.MessageEntity
import org.lerchenflo.schneaggchatv3mp.chat.data.local.entity.MessageReaderEntity
import org.lerchenflo.schneaggchatv3mp.chat.data.local.entity.SnailTrailPointEntity
import org.lerchenflo.schneaggchatv3mp.chat.data.local.entity.UserEntity
import org.lerchenflo.schneaggchatv3mp.chat.data.local.entity.relations.GroupWithMembers
import org.lerchenflo.schneaggchatv3mp.chat.data.local.entity.relations.MessageWithReaders
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageType
import org.lerchenflo.schneaggchatv3mp.events.data.local.entity.EventEntity
import org.lerchenflo.schneaggchatv3mp.games.data.PlayerEntity
import org.lerchenflo.schneaggchatv3mp.schneaggmap.data.local.entity.MapEntryEntity
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.NetworkFriendshipStatus

@Dao
interface UserDao {

    @Upsert()
    suspend fun upsert(userEntity: UserEntity) //Suspend: Async mit warten

    @Query("DELETE FROM users WHERE id = :userid")
    suspend fun delete(userid: String)

    @Query("SELECT * FROM users WHERE name LIKE '%' || :searchterm || '%'")
    fun getAllUsersFlow(searchterm: String = ""): Flow<List<UserEntity>>

    @Query("SELECT * FROM users")
    suspend fun getAllUsers() : List<UserEntity>

    @Query("SELECT * FROM users WHERE id = :userid")
    fun getUserbyIdFlow(userid: String?): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :userid")
    suspend fun getUserbyId(userid: String?): UserEntity?

    @Query("SELECT id, updatedAt FROM users")
    suspend fun getUserIdsWithChangeDates(): List<IdChangeDate>

}

@Dao
interface SnailTrailDao {

    @Upsert
    suspend fun upsertPoints(points: List<SnailTrailPointEntity>): List<Long>

    @Query("DELETE FROM snail_trail_points WHERE userId = :userId")
    suspend fun deletePointsForUser(userId: String)

    @Query("SELECT * FROM snail_trail_points WHERE userId = :userId ORDER BY locationTime ASC")
    fun getPointsForUserFlow(userId: String): Flow<List<SnailTrailPointEntity>>

    @Query("SELECT * FROM snail_trail_points WHERE userId = :userId ORDER BY locationTime ASC")
    suspend fun getPointsForUser(userId: String): List<SnailTrailPointEntity>
}


@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessageEntity(messageEntity: MessageEntity): Long

    @Update
    suspend fun updateMessageEntity(messageEntity: MessageEntity)

    @Query("DELETE FROM messages WHERE id = :msgid")
    suspend fun deleteMessageEntityById(msgid: String)

    @Query("DELETE FROM messages WHERE localPK = :pk")
    suspend fun deleteMessageEntityByPk(pk: Long)

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getMessageEntityById(id: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE id IN (:ids)")
    suspend fun getMessageEntitiesByIds(ids: List<String>): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE clientMessageId IN (:clientMessageIds)")
    suspend fun getMessageEntitiesByClientMessageIds(clientMessageIds: List<String>): List<MessageEntity>

    @Upsert
    suspend fun upsertReaders(readers: List<MessageReaderEntity>): List<Long>

    @Query("DELETE FROM message_readers WHERE messageId IN (:messageIds)")
    suspend fun deleteReadersForMessages(messageIds: List<String>)

    /**
     * Upserts a whole sync page in one transaction, so Room's invalidation tracker fires once for
     * the page instead of once per message - every fire re-runs the chat selector's query chain.
     */
    @Transaction
    suspend fun upsertMessagesWithReaders(messages: List<MessageWithReaders>) {
        val readersByMessageId = mutableMapOf<String, List<MessageReaderEntity>>()

        messages.forEach { message ->
            val upsertedId = upsertMessageEntity(message.messageEntity).id
            if (upsertedId != null && message.readers.isNotEmpty()) {
                readersByMessageId[upsertedId] = message.readers.map { it.copy(messageId = upsertedId) }
            }
        }

        if (readersByMessageId.isNotEmpty()) {
            deleteReadersForMessages(readersByMessageId.keys.toList())
            upsertReaders(readersByMessageId.values.flatten())
        }
    }

    /** Batch counterpart of [deleteMessageEntityById], readers included, in one transaction. */
    @Transaction
    suspend fun deleteMessagesByIds(ids: List<String>) {
        deleteReadersForMessages(ids)
        deleteMessageEntitiesByIds(ids)
    }

    @Query("DELETE FROM messages WHERE id IN (:ids)")
    suspend fun deleteMessageEntitiesByIds(ids: List<String>)

    @Transaction
    suspend fun upsertMessageEntity(messageEntity: MessageEntity): MessageEntity {
        val existing = getMessageEntityById(messageEntity.id.orEmpty())
        if (existing != null) {
            updateMessageEntity(messageEntity.copy(localPK = existing.localPK))
        } else {
            insertMessageEntity(messageEntity)
        }
        // Get the message after insert/update
        val returnedMessage = getMessageEntityById(messageEntity.id.orEmpty())
        return returnedMessage ?: messageEntity
    }



    @Transaction
    @Query("SELECT * FROM messages WHERE id = :id")
    fun getMessageWithReadersByIdFlow(id: String): Flow<MessageWithReaders>


    @Transaction
    @Query("SELECT * FROM messages")
    fun getAllMessagesWithReadersFlow(): Flow<List<MessageWithReaders>>

    /**
     * Per-chat unread and unsent counters for the chat selector, aggregated in SQL.
     *
     * The chat a message belongs to is the group for group messages and whichever participant is
     * not [ownId] for direct messages - [getLastMessagePerChatFlow] partitions by the exact same
     * expression, so both queries key their rows identically.
     *
     * Unread mirrors the rule of [getUnreadChatCountFlow]: my own messages are read by definition
     * and SYSTEM lines never raise a badge.
     */
    @Query("""
        SELECT
            CASE
                WHEN groupMessage = 1 THEN receiverId
                WHEN senderId = :ownId THEN receiverId
                ELSE senderId
            END AS chatId,
            groupMessage AS isGroup,
            SUM(CASE WHEN myMessage = 0 AND readByMe = 0 AND msgType != :systemType THEN 1 ELSE 0 END) AS unreadCount,
            SUM(CASE WHEN sent = 0 THEN 1 ELSE 0 END) AS unsentCount
        FROM messages
        GROUP BY chatId, isGroup
    """)
    fun getChatAggregatesFlow(
        ownId: String,
        systemType: String = MessageType.SYSTEM.name,
    ): Flow<List<ChatAggregate>>

    /**
     * The newest message of every chat - one row per chat instead of the whole message table.
     * Readers are deliberately not joined: the chat selector preview never reads them.
     *
     * The inner query leans on SQLite's bare-column rule: when a grouped query selects MAX() of one
     * column, the other selected columns are taken from the very row that MAX matched, so localPK
     * is the newest message's own primary key.
     */
    @Query("""
        SELECT * FROM messages WHERE localPK IN (
            SELECT localPK FROM (
                SELECT localPK, MAX(sendDate) AS lastSendDate
                FROM messages
                GROUP BY
                    CASE
                        WHEN groupMessage = 1 THEN receiverId
                        WHEN senderId = :ownId THEN receiverId
                        ELSE senderId
                    END,
                    groupMessage
            )
        )
    """)
    fun getLastMessagePerChatFlow(ownId: String): Flow<List<MessageEntity>>

    /**
     * The newest [limit] messages of one chat, newest first - the order the chat screen renders and
     * relies on (see ChatViewModel.newestMessageId and MessageDisplayMapper, which read the newest
     * message off the front and group consecutive messages by walking the list in this order).
     *
     * Limited because the chat screen re-runs this on every write to messages or readers: loading
     * the whole history (readers included) and remapping it each time made long chats slow. The
     * chat screen grows [limit] as the user scrolls up.
     *
     * Split into the sender and receiver arm on purpose: an OR over both can only be answered by
     * collecting every row of the chat and sorting them all, LIMIT or not. Each arm on its own
     * walks its (…, sendDate) index backwards and stops after [limit] rows, so only up to
     * 2 * [limit] rows ever get sorted, however long the chat is.
     */
    @Transaction
    @Query("""
        SELECT * FROM messages WHERE localPK IN (
            SELECT localPK FROM (
                SELECT localPK FROM messages
                WHERE senderId = :userId AND groupMessage = :gruppe
                ORDER BY sendDate DESC LIMIT :limit
            )
            UNION
            SELECT localPK FROM (
                SELECT localPK FROM messages
                WHERE receiverId = :userId AND groupMessage = :gruppe
                ORDER BY sendDate DESC LIMIT :limit
            )
        )
        ORDER BY sendDate DESC
        LIMIT :limit
    """)
    fun getMessagesByUserIdFlow(userId: String, gruppe: Boolean, limit: Int): Flow<List<MessageWithReaders>>

    /**
     * Number of messages in one chat sent at or after [sinceMillis] - how big the chat screen's
     * window has to be so a given message (the oldest unread, a search hit) is inside it.
     */
    @Query("""
        SELECT COUNT(*) FROM messages
        WHERE (senderId = :userId OR receiverId = :userId)
          AND groupMessage = :gruppe
          AND sendDate >= :sinceMillis
    """)
    suspend fun getMessageCountSince(userId: String, gruppe: Boolean, sinceMillis: Long): Int

    /**
     * Send date of the oldest unread message in one chat, or null when everything is read. Same
     * unread rule as ChatViewModel.captureNewMessagesBoundary: my own messages are always read.
     */
    @Query("""
        SELECT MIN(sendDate) FROM messages
        WHERE (senderId = :userId OR receiverId = :userId)
          AND groupMessage = :gruppe
          AND myMessage = 0
          AND readByMe = 0
    """)
    suspend fun getOldestUnreadSendDate(userId: String, gruppe: Boolean): Long?

    /** Every message of one chat, newest first, same order as [getMessagesByUserIdFlow]. */
    @Transaction
    @Query("SELECT * FROM messages WHERE (senderId = :userId OR receiverId = :userId) AND groupMessage = :gruppe ORDER BY sendDate DESC")
    suspend fun getMessagesByUserId(userId: String, gruppe: Boolean): List<MessageWithReaders>

    /**
     * Every image ever shared in one chat, newest first, for the chat's shared content screen.
     *
     * Readers are deliberately not joined: the grid never reads them.
     */
    @Query("""
        SELECT * FROM messages
        WHERE (senderId = :userId OR receiverId = :userId)
          AND groupMessage = :gruppe
          AND msgType = :imageType
          AND deleted = 0
        ORDER BY sendDate DESC
    """)
    fun getImageMessagesForChatFlow(
        userId: String,
        gruppe: Boolean,
        imageType: String = MessageType.IMAGE.name,
    ): Flow<List<MessageEntity>>

    /**
     * Every message of one [msgType] (a MessageType name) in one chat, newest first, for the shared
     * content screen's message list. Readers are not joined - the list never reads them.
     */
    @Query("""
        SELECT * FROM messages
        WHERE (senderId = :userId OR receiverId = :userId)
          AND groupMessage = :gruppe
          AND msgType = :msgType
          AND deleted = 0
        ORDER BY sendDate DESC
    """)
    fun getMessagesOfTypeForChatFlow(
        userId: String,
        gruppe: Boolean,
        msgType: String,
    ): Flow<List<MessageEntity>>

    /** Number of (not deleted) messages in one chat, all types, for the chat details row. */
    @Query("""
        SELECT COUNT(*) FROM messages
        WHERE (senderId = :userId OR receiverId = :userId)
          AND groupMessage = :gruppe
          AND deleted = 0
    """)
    fun getMessageCountForChatFlow(
        userId: String,
        gruppe: Boolean,
    ): Flow<Int>

    /**
     * Messages of one chat whose text might carry a url, newest first, for the shared content
     * screen's link list. The two LIKEs are only a cheap prefilter and have to stay as wide as the
     * regex in extractLinks, which is what decides whether a row really counts - 'www.' is in there
     * because a link typed without a scheme carries no 'http' to match on.
     *
     * Image captions are included on purpose: a link pasted under a picture is still a shared link.
     */
    @Query("""
        SELECT * FROM messages
        WHERE (senderId = :userId OR receiverId = :userId)
          AND groupMessage = :gruppe
          AND deleted = 0
          AND (content LIKE '%http%' OR content LIKE '%www.%')
        ORDER BY sendDate DESC
    """)
    fun getLinkCandidateMessagesForChatFlow(
        userId: String,
        gruppe: Boolean,
    ): Flow<List<MessageEntity>>

    @Transaction
    @Query("SELECT * FROM messages WHERE id = :msgid")
    suspend fun getMessageById(msgid: String): MessageWithReaders?

    @Transaction
    @Query("SELECT * FROM messages WHERE localPK = :msgid")
    suspend fun getMessageById(msgid: Long): MessageWithReaders?

    @Query("SELECT id, updatedAt FROM messages WHERE id != 0")
    suspend fun getMessageIdsWithChangeDates(): List<IdChangeDate>

    @Query("SELECT COALESCE(MAX(version), 0) FROM messages")
    suspend fun getLastSyncedMessageVersion(): Long

    @Query("DELETE FROM messages WHERE receiverId = :groupId AND groupMessage = 1")
    suspend fun deleteMessagesForGroup(groupId: String)

    @Transaction
    // Oldest first - the offline queue sends in this order
    @Query("SELECT * FROM messages WHERE sent = 0 ORDER BY localPK")
    suspend fun getUnsentMessages(): List<MessageWithReaders>

    // Group messages whose group is no longer stored locally are left out: leftover rows of a
    // left/removed group (from before the purge in GroupLocalDataSource.deleteGroup, or re-inserted
    // by a sync race) would otherwise make the media job request their files on every sync, and the
    // server rejects each one with "You are not a member of this group".
    @Transaction
    @Query("SELECT * FROM messages WHERE msgType = :type AND (groupMessage = 0 OR receiverId IN (SELECT id FROM `groups`))")
    suspend fun getImageMessages(type: String = MessageType.IMAGE.name): List<MessageWithReaders>

    @Transaction
    @Query("SELECT * FROM messages WHERE msgType = :type AND (groupMessage = 0 OR receiverId IN (SELECT id FROM `groups`))")
    suspend fun getAudioMessages(type: String = MessageType.AUDIO.name): List<MessageWithReaders>

    @Transaction
    @Query("UPDATE messages SET id = :serverId, sent = 1 WHERE localPK = :localPK")
    suspend fun markMessageAsSent(serverId: String, localPK: Long)

    /**
     * Number of chats holding at least one unread message, for the chat tab's nav bar badge - chats,
     * not messages, so ten unread lines in one group count once.
     *
     * Mirrors the per-chat unread rule of [getChatAggregatesFlow]:
     * my own messages are read by definition and SYSTEM lines never raise a badge. The EXISTS
     * clauses keep the count to the chats the selector actually lists, so messages left behind by a
     * removed friend or a group that is gone can not light a badge the user has no way to clear.
     */
    @Query("""
        SELECT COUNT(*) FROM (
            SELECT m.receiverId FROM messages m
            WHERE m.groupMessage = 1 AND m.myMessage = 0 AND m.readByMe = 0 AND m.msgType != :systemType
              AND EXISTS (SELECT 1 FROM groups g WHERE g.id = m.receiverId)
            GROUP BY m.receiverId
            UNION ALL
            SELECT m.senderId FROM messages m
            WHERE m.groupMessage = 0 AND m.myMessage = 0 AND m.readByMe = 0 AND m.msgType != :systemType
              AND EXISTS (SELECT 1 FROM users u WHERE u.id = m.senderId AND u.frienshipStatus = :acceptedFriendship)
            GROUP BY m.senderId
        )
    """)
    fun getUnreadChatCountFlow(
        systemType: String = MessageType.SYSTEM.name,
        acceptedFriendship: String = NetworkFriendshipStatus.ACCEPTED.name,
    ): Flow<Int>
/* Will not work since sendDate is a string
    @Transaction
    @Query("SELECT * FROM messages WHERE (sendDate > :time)")
    suspend fun getMessagesSinceTimeStamp(time: Long): List<MessageWithReaders>

 */

}

@Dao
interface MessageReaderDao {

    @Upsert()
    suspend fun upsertReader(reader: MessageReaderEntity): Long

    @Upsert
    suspend fun upsertReaders(readers: List<MessageReaderEntity>): List<Long>

    @Query("DELETE FROM message_readers WHERE messageId = :messageId")
    suspend fun deleteReadersForMessage(messageId: String)

    @Query("DELETE FROM message_readers WHERE messageId IN (SELECT id FROM messages WHERE receiverId = :groupId AND groupMessage = 1)")
    suspend fun deleteReadersForGroupMessages(groupId: String)
}



@Dao
interface GroupDao {
    @Upsert
    suspend fun upsertGroup(group: GroupEntity)

    @Query("SELECT id, updatedAt FROM `groups`")
    suspend fun getGroupIdsWithChangeDates(): List<IdChangeDate>

    @Query("DELETE FROM `groups` WHERE id = :groupid")
    suspend fun deleteGroup(groupid: String)

    @Transaction
    @Query("SELECT * FROM `groups`")
    fun getAllGroupsWithMembersFlow(): Flow<List<GroupWithMembers>>

    @Transaction
    @Query("SELECT * FROM `groups`")
    suspend fun getAllGroupsWithMembers(): List<GroupWithMembers>

    @Upsert
    suspend fun upsertMembers(members: List<GroupMemberEntity>): List<Long>

    @Query("DELETE FROM group_members WHERE groupId = :groupId")
    suspend fun deleteMembersForGroup(groupId: String)

    // Atomic so a concurrent upsert for the same group (e.g. socket GroupChange during a sync) can't interleave delete+insert and leave duplicate member rows.
    @Transaction
    suspend fun upsertGroupWithMembers(group: GroupEntity, members: List<GroupMemberEntity>) {
        upsertGroup(group)
        deleteMembersForGroup(group.id)
        if (members.isNotEmpty()) {
            upsertMembers(members)
        }
    }

    @Query("SELECT * FROM `groups` WHERE id = :groupid")
    suspend fun getGroupById(groupid: String?): GroupEntity?

    @Transaction
    @Query("SELECT * FROM `groups` WHERE id = :groupId")
    suspend fun getGroupWithMembersById(groupId: String): GroupWithMembers?

    @Transaction
    @Query("SELECT * FROM `groups` WHERE id = :groupId")
    fun getGroupWithMembersByIdFlow(groupId: String): Flow<GroupWithMembers?>

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
    suspend fun getCommonGroupsWithMembers(memberId1: String, memberId2: String): List<GroupWithMembers>
}


@Dao
interface LogDao {
    @Upsert
    suspend fun upsertLog(logEntry: LogEntry)

    @Query("SELECT * FROM logentry")
    fun getLogs() : Flow<List<LogEntry>>

    @Query("DELETE FROM logentry")
    suspend fun clearLogs()

    @Query("SELECT COUNT(*) FROM logentry")
    suspend fun getLogCount(): Int

    @Query("DELETE FROM logentry WHERE id NOT IN (SELECT id FROM logentry ORDER BY id DESC LIMIT 50)")
    suspend fun deleteOldLogs()
}



@Dao
interface AllDatabaseDao {
    @Query("DELETE FROM message_readers")
    suspend fun clearMessageReaders()

    @Query("DELETE FROM messages")
    suspend fun clearMessages()

    @Query("DELETE FROM users")
    suspend fun clearUsers()

    @Query("DELETE FROM snail_trail_points")
    suspend fun clearSnailTrailPoints()

    @Query("DELETE FROM `groups`")
    suspend fun clearGroups()

    @Query("DELETE FROM group_members")
    suspend fun clearGroupMembers()

    @Query("DELETE FROM map_entries")
    suspend fun clearMapEntries()

    @Query("DELETE FROM events")
    suspend fun clearEvents()


    @Transaction
    suspend fun clearAll() {
        // Clear tables in proper order to respect foreign key constraints
        clearMessageReaders()  // Child table first
        clearMessages()        // Then parent table
        clearSnailTrailPoints() // Child of users - clear before users
        clearUsers()           // Finally users table
        clearGroupMembers()
        clearGroups()
        clearMapEntries()
        clearEvents()
    }
}

@Dao
interface MapEntryDao {

    @Upsert
    suspend fun upsert(mapEntryEntity: MapEntryEntity)

    @Query("SELECT id, updatedAt FROM map_entries")
    suspend fun getMapEntryIdsWithChangeDates(): List<IdChangeDate>

    @Query("SELECT * FROM map_entries")
    fun getAllFlow(): Flow<List<MapEntryEntity>>

    @Query("DELETE FROM map_entries WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface EventDao {

    @Upsert
    suspend fun upsert(eventEntity: EventEntity)

    @Query("SELECT id, updatedAt FROM events")
    suspend fun getEventIdsWithChangeDates(): List<IdChangeDate>

    @Query("SELECT * FROM events ORDER BY startDate ASC")
    fun getAllFlow(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE groupId = :groupId LIMIT 1")
    fun getEventFlowByGroupId(groupId: String): Flow<EventEntity?>

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface PlayerDao {
    @Query("SELECT * FROM players")
    fun getAllPlayersFlow(): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players")
    suspend fun getAllPlayers(): List<PlayerEntity>

    @Upsert
    suspend fun upsert(player: PlayerEntity)

    @Query("DELETE FROM players WHERE id = :playerId")
    suspend fun delete(playerId: String)
}

@Serializable
data class IdChangeDate(
    val id: String,
    val updatedAt: String
)

