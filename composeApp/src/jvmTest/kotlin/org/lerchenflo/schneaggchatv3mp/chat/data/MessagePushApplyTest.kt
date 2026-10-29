package org.lerchenflo.schneaggchatv3mp.chat.data

import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import assertk.assertions.single
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.lerchenflo.schneaggchatv3mp.chat.data.local.RoomMessageDataSource
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageType
import org.lerchenflo.schneaggchatv3mp.chat.domain.Reaction
import org.lerchenflo.schneaggchatv3mp.testutil.FRIEND_ID
import org.lerchenflo.schneaggchatv3mp.testutil.OWN_USER_ID
import org.lerchenflo.schneaggchatv3mp.testutil.SendPathFixture
import org.lerchenflo.schneaggchatv3mp.testutil.loginTestUser
import org.lerchenflo.schneaggchatv3mp.testutil.resetGlobalState
import org.lerchenflo.schneaggchatv3mp.testutil.testMessage
import org.lerchenflo.schneaggchatv3mp.utilities.notifications.DecodedNotification
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * `OfflineFirstMessageRepository.applyPushMessage`: a message push (FCM/APNs) becomes a
 * provisional local row until the next sync, a reaction push updates the stored message. Never a
 * network call. Built on [SendPathFixture], whose server fails the test on any request.
 */
class MessagePushApplyTest {

    private lateinit var fixture: SendPathFixture
    private lateinit var messages: RoomMessageDataSource

    @BeforeTest
    fun setUp() {
        resetGlobalState()
        loginTestUser(OWN_USER_ID)
    }

    private fun runPushTest(testBody: suspend TestScope.() -> Unit) = runTest(StandardTestDispatcher()) {
        fixture = SendPathFixture(this)
        messages = RoomMessageDataSource(fixture.database)
        testBody()
    }

    @AfterTest
    fun tearDown() {
        try {
            assertThat(fixture.server.unexpectedRequests).isEmpty()
        } finally {
            fixture.close()
            resetGlobalState()
        }
    }

    private fun textPush(
        msgId: String = "srv-9",
        content: String = "Hi there",
        groupId: String = "",
        type: MessageType = MessageType.TEXT,
        reaction: Boolean = false,
        sendDate: Long = 1_700_000_000_123L,
        answerId: String = "",
    ) = DecodedNotification.Message(
        msgId = msgId,
        senderName = "Friend",
        groupMessage = groupId.isNotEmpty(),
        messageType = type,
        groupName = if (groupId.isNotEmpty()) "Group" else "",
        content = content,
        senderId = FRIEND_ID,
        receiverId = OWN_USER_ID,
        reaction = reaction,
        groupId = groupId,
        sendDate = sendDate,
        answerId = answerId,
    )

    @Test
    fun `a text push is stored as an unread, provisional row of the sender's chat`() = runPushTest {
        fixture.repository.applyPushMessage(textPush(answerId = "srv-1"))

        val row = fixture.storedMessages().single()
        assertThat(row.id).isEqualTo("srv-9")
        assertThat(row.content).isEqualTo("Hi there")
        assertThat(row.senderId).isEqualTo(FRIEND_ID)
        assertThat(row.receiverId).isEqualTo(OWN_USER_ID)
        assertThat(row.groupMessage).isFalse()
        assertThat(row.answerId).isEqualTo("srv-1")
        assertThat(row.sendDate).isEqualTo("1700000000123")
        assertThat(row.sent).isTrue()
        assertThat(row.myMessage).isFalse()
        assertThat(row.readByMe).isFalse()
        // Provisional: version 0 never moves the sync cursor, the next sync replaces the row
        assertThat(row.version).isEqualTo(0L)
    }

    @Test
    fun `a group push lands in the group chat`() = runPushTest {
        fixture.repository.applyPushMessage(textPush(groupId = "hiking"))

        val row = fixture.storedMessages().single()
        assertThat(row.receiverId).isEqualTo("hiking")
        assertThat(row.groupMessage).isTrue()
        assertThat(row.answerId).isNull()
    }

    @Test
    fun `a push never overwrites a message that is already stored`() = runPushTest {
        messages.upsertMessage(testMessage("srv-9", senderId = FRIEND_ID, receiverId = OWN_USER_ID, sendDate = 5L, content = "synced copy"))

        fixture.repository.applyPushMessage(textPush(content = "push copy"))

        assertThat(fixture.storedMessages()).single().transform { it.content }.isEqualTo("synced copy")
    }

    @Test
    fun `image, audio and poll pushes and pushes without text are not stored`() = runPushTest {
        fixture.repository.applyPushMessage(textPush(msgId = "srv-1", type = MessageType.IMAGE))
        fixture.repository.applyPushMessage(textPush(msgId = "srv-2", type = MessageType.AUDIO))
        fixture.repository.applyPushMessage(textPush(msgId = "srv-3", type = MessageType.POLL))
        fixture.repository.applyPushMessage(textPush(msgId = "srv-4", content = " "))

        assertThat(fixture.storedMessages()).isEmpty()
    }

    @Test
    fun `a reaction push replaces the sender's reaction on the stored message`() = runPushTest {
        messages.upsertMessage(
            testMessage("srv-9", senderId = OWN_USER_ID, receiverId = FRIEND_ID, sendDate = 5L, myMessage = true).copy(
                reactions = listOf(Reaction(userId = FRIEND_ID, content = "👍"), Reaction(userId = "carla", content = "😂")),
            )
        )

        fixture.repository.applyPushMessage(textPush(content = "🍻", reaction = true))

        val row = fixture.storedMessages().single()
        assertThat(row.reactions.map { it.userId to it.content })
            .containsExactlyInAnyOrder(FRIEND_ID to "🍻", "carla" to "😂")
        assertThat(row.content).isEqualTo("message srv-9")
    }

    @Test
    fun `a reaction push for an unknown message creates nothing`() = runPushTest {
        fixture.repository.applyPushMessage(textPush(content = "🍻", reaction = true))

        assertThat(fixture.storedMessages()).isEmpty()
    }

    @Test
    fun `nothing is stored while logged out`() = runPushTest {
        resetGlobalState() // logs out

        fixture.repository.applyPushMessage(textPush())

        assertThat(fixture.storedMessages()).isEmpty()
    }
}
