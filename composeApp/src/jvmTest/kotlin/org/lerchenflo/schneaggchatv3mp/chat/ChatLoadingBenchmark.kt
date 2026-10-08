package org.lerchenflo.schneaggchatv3mp.chat

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.MessageDto
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.MessageReaderDto
import org.lerchenflo.schneaggchatv3mp.chat.domain.GroupMember
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageDisplayItem
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageType
import org.lerchenflo.schneaggchatv3mp.chat.domain.toMessage
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.MessageDisplayMapper
import org.lerchenflo.schneaggchatv3mp.datasource.database.AppDatabase
import java.io.File
import kotlin.random.Random
import kotlin.test.Test
import kotlin.time.measureTime

/**
 * Temporary benchmark: old chat loading (whole history, every emission) vs the new window.
 * Results go to composeApp/build/chat-load-bench.txt.
 */
class ChatLoadingBenchmark {

    private val groupId = "group-1"
    private val members = listOf("me", "anna", "ben", "carla", "dani")
    private val runs = 15

    @Test
    fun compareOldAndNewLoading() = runBlocking {
        val report = StringBuilder()
        report.appendLine("Group chat, ${members.size} members, ~4 readers/message, 10% replies, 3 noise chats")
        report.appendLine("Median of $runs runs after warmup, real SQLite file DB (bundled driver)")
        report.appendLine()

        for (size in listOf(1_000, 5_000, 20_000)) {
            val dbFile = File.createTempFile("chatbench", ".db").also { it.delete() }
            val db = Room.databaseBuilder<AppDatabase>(dbFile.absolutePath)
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
            try {
                seed(db, size)
                val dao = db.messageDao()
                val names = members.associateWith { it.replaceFirstChar(Char::uppercase) }
                val groupMembers = members.mapIndexed { i, id -> GroupMember(groupId = groupId, userId = id, joinDate = "0", admin = false, color = i, memberName = names.getValue(id)) }

                // OLD: same SQL the flow ran before (no LIMIT), map all, remap all.
                suspend fun oldPath(): List<MessageDisplayItem> {
                    val messages = dao.getMessagesByUserId(groupId, true).map { it.toMessage() }
                    return MessageDisplayMapper { "" }.map(messages, emptyMap(), names, groupMembers, null)
                }

                // NEW: window of 100 + out-of-window reply targets, like ChatViewModel.
                suspend fun newPath(): List<MessageDisplayItem> {
                    val messages = dao.getMessagesByUserIdFlow(groupId, true, 100).first().map { it.toMessage() }
                    val loadedIds = messages.mapNotNullTo(HashSet()) { it.id }
                    val missing = messages.mapNotNull { it.answerId }.filter { it !in loadedIds }.distinct()
                    val older = if (missing.isEmpty()) emptyMap() else
                        dao.getMessageDtosByIds(missing).mapNotNull { d -> d.id?.let { it to d.toMessage() } }.toMap()
                    return MessageDisplayMapper { "" }.map(messages, older, names, groupMembers, null)
                }

                // NEW, once on open only: size the first window so all unread fit.
                suspend fun newOpenExtra() {
                    dao.getOldestUnreadSendDate(groupId, true)?.let { dao.getMessageCountSince(groupId, true, it) }
                }

                // OLD UI: each reply row scanned the whole list for its original, per recomposition.
                fun oldReplyLookupPerScreen(items: List<MessageDisplayItem>) {
                    items.take(25).filterIsInstance<MessageDisplayItem.MessageItem>().forEach { row ->
                        val answerId = row.message.answerId ?: return@forEach
                        items.filterIsInstance<MessageDisplayItem.MessageItem>().firstOrNull { it.message.id == answerId }
                    }
                }

                repeat(3) { oldPath(); newPath(); newOpenExtra() } // warmup

                val oldTimes = List(runs) { measureTime { oldPath() }.inWholeMicroseconds }
                val newTimes = List(runs) { measureTime { newPath() }.inWholeMicroseconds }
                val openExtraTimes = List(runs) { measureTime { newOpenExtra() }.inWholeMicroseconds }
                val oldItems = oldPath()
                val newItems = newPath()
                val lookupTimes = List(runs) { measureTime { oldReplyLookupPerScreen(oldItems) }.inWholeMicroseconds }

                val oldMs = median(oldTimes) / 1000.0
                val newMs = median(newTimes) / 1000.0
                val openMs = (median(newTimes) + median(openExtraTimes)) / 1000.0
                report.appendLine("== $size messages ==")
                report.appendLine("  old load/update:        %8.2f ms  (%d display items)".format(oldMs, oldItems.size))
                report.appendLine("  new load/update:        %8.2f ms  (%d display items)".format(newMs, newItems.size))
                report.appendLine("  new open (+unread size):%8.2f ms".format(openMs))
                report.appendLine("  speedup per update:     %8.1fx".format(oldMs / newMs))
                report.appendLine("  old reply lookups/frame:%8.2f ms  (removed: now pre-resolved)".format(median(lookupTimes) / 1000.0))

                // Diagnostics: where the remaining window cost goes (raw SQL, no Room mapping).
                val conn = BundledSQLiteDriver().open(dbFile.absolutePath)
                try {
                    val where = "WHERE (senderId = '$groupId' OR receiverId = '$groupId') AND groupMessage = 1"
                    fun rawMs(sql: String): Double {
                        fun once() = conn.prepare(sql).use { st -> var n = 0; while (st.step()) n++; n }
                        repeat(3) { once() }
                        return median(List(runs) { measureTime { once() }.inWholeMicroseconds }) / 1000.0
                    }
                    val newSql = """
                        SELECT * FROM messages WHERE localPK IN (
                            SELECT localPK FROM (SELECT localPK FROM messages WHERE senderId = '$groupId' AND groupMessage = 1 ORDER BY sendDate DESC LIMIT 100)
                            UNION
                            SELECT localPK FROM (SELECT localPK FROM messages WHERE receiverId = '$groupId' AND groupMessage = 1 ORDER BY sendDate DESC LIMIT 100)
                        ) ORDER BY sendDate DESC LIMIT 100
                    """
                    conn.prepare("EXPLAIN QUERY PLAN $newSql").use { st ->
                        while (st.step()) report.appendLine("  new plan: ${st.getText(3)}")
                    }
                    report.appendLine("  raw window, OR + CAST sort (step 1): %8.2f ms".format(rawMs("SELECT * FROM messages $where ORDER BY CAST(sendDate AS INTEGER) DESC LIMIT 100")))
                    report.appendLine("  raw window, indexed arms  (step 2): %8.2f ms".format(rawMs(newSql)))

                    // Correctness: the window must be exactly the newest 100 of the old full query.
                    val oldIds = dao.getMessagesByUserId(groupId, true).take(100).map { it.messageDto.id }
                    val newIds = dao.getMessagesByUserIdFlow(groupId, true, 100).first().map { it.messageDto.id }
                    check(oldIds == newIds) { "window differs from old order" }
                    report.appendLine("  window == newest 100 of old query: OK")
                } finally {
                    conn.close()
                }
                report.appendLine()
            } finally {
                db.close()
                dbFile.delete()
                File(dbFile.absolutePath + "-wal").delete()
                File(dbFile.absolutePath + "-shm").delete()
            }
        }

        val out = File("build/chat-load-bench.txt")
        out.writeText(report.toString())
        println(report)
    }

    private fun median(values: List<Long>): Long = values.sorted()[values.size / 2]

    private suspend fun seed(db: AppDatabase, size: Int) {
        val dao = db.messageDao()
        val random = Random(42)
        val start = 1_700_000_000_000L
        val readers = mutableListOf<MessageReaderDto>()

        suspend fun insertChat(chatId: String, group: Boolean, count: Int, withReaders: Boolean) {
            for (i in 0 until count) {
                val id = "$chatId-$i"
                val sender = members[random.nextInt(members.size)]
                val answerId = if (i > 10 && random.nextInt(10) == 0) "$chatId-${random.nextInt(i)}" else null
                dao.insertMessageDto(
                    MessageDto(
                        id = id,
                        msgType = MessageType.TEXT,
                        content = "Message $i " + "lorem ipsum ".repeat(random.nextInt(1, 8)),
                        senderId = if (group) sender else chatId,
                        receiverId = if (group) chatId else "me",
                        sendDate = start + i * 45_000L,
                        updatedAt = (start + i * 45_000L).toString(),
                        myMessage = sender == "me",
                        readByMe = true,
                        groupMessage = group,
                        answerId = answerId,
                        sent = true,
                    )
                )
                if (withReaders) {
                    members.filter { it != sender }.forEach { reader ->
                        readers += MessageReaderDto(messageId = id, readerID = reader, readDate = (start + i * 45_000L + 5_000L).toString())
                    }
                }
            }
        }

        insertChat(groupId, group = true, count = size, withReaders = true)
        // Noise: other chats in the same table, like a real database.
        insertChat("group-2", group = true, count = size / 2, withReaders = false)
        insertChat("anna", group = false, count = size / 2, withReaders = false)
        insertChat("ben", group = false, count = size / 2, withReaders = false)
        readers.chunked(5_000).forEach { dao.upsertReaders(it) }
    }
}
