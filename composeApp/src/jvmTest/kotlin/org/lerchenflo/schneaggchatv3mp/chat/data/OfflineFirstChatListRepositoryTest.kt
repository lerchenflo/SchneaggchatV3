package org.lerchenflo.schneaggchatv3mp.chat.data

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import assertk.assertions.prop
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.toDto
import org.lerchenflo.schneaggchatv3mp.chat.domain.ChatListItem
import org.lerchenflo.schneaggchatv3mp.datasource.database.AppDatabase
import org.lerchenflo.schneaggchatv3mp.chat.domain.FriendshipStatus
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageType
import org.lerchenflo.schneaggchatv3mp.datasource.network.AppJson
import org.lerchenflo.schneaggchatv3mp.datasource.network.createHttpClient
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.PinnedChat
import org.lerchenflo.schneaggchatv3mp.fakes.FakeLogger
import org.lerchenflo.schneaggchatv3mp.fakes.FakeSettingsRepository
import org.lerchenflo.schneaggchatv3mp.testutil.createInMemoryAppDatabase
import org.lerchenflo.schneaggchatv3mp.testutil.createTestSettingsRemoteDataSource
import org.lerchenflo.schneaggchatv3mp.testutil.resetGlobalState
import org.lerchenflo.schneaggchatv3mp.testutil.testGroup
import org.lerchenflo.schneaggchatv3mp.testutil.testMessage
import org.lerchenflo.schneaggchatv3mp.testutil.testUser
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.UserSettingsRequest

private const val OWN_ID = "me"

/**
 * [OfflineFirstChatListRepository] on a real in-memory Room database: which chats the list shows
 * (direct chats vs groups), their last message and counters, the unread badge, and pins.
 */
class OfflineFirstChatListRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: OfflineFirstChatListRepository
    private lateinit var userDataSource: RoomUserDataSource
    private lateinit var groupDataSource: RoomGroupDataSource
    private lateinit var messageDataSource: RoomMessageDataSource
    private val settingsRepository = FakeSettingsRepository()

    /** Bodies of every `POST /users/settings`; any other request is recorded in [unexpectedRequests]. */
    private val settingsPushes = CopyOnWriteArrayList<UserSettingsRequest>()
    private val unexpectedRequests = CopyOnWriteArrayList<String>()

    private val server = MockEngine { request ->
        if (request.method == HttpMethod.Post && request.url.encodedPath == "/users/settings") {
            settingsPushes += AppJson.instance.decodeFromString(
                UserSettingsRequest.serializer(),
                request.body.toByteArray().decodeToString(),
            )
            respond("", HttpStatusCode.OK)
        } else {
            unexpectedRequests += "${request.method.value} ${request.url}"
            respond("Not served by this test", HttpStatusCode.NotFound)
        }
    }
    private val httpClient = createHttpClient(server)

    @BeforeTest
    fun setUp() {
        resetGlobalState()
    }

    @AfterTest
    fun tearDown() {
        try {
            assertThat(unexpectedRequests).isEmpty()
        } finally {
            httpClient.close()
            if (::database.isInitialized) database.close()
            resetGlobalState()
        }
    }

    /** `runTest` that first builds the repository with its list building on the test scheduler (plan R4). */
    private fun runChatListTest(testBody: suspend TestScope.() -> Unit) = runTest {
        database = createInMemoryAppDatabase()
        userDataSource = RoomUserDataSource(database)
        groupDataSource = RoomGroupDataSource(database)
        messageDataSource = RoomMessageDataSource(database)
        repository = OfflineFirstChatListRepository(
            userLocalDataSource = userDataSource,
            groupLocalDataSource = groupDataSource,
            messageLocalDataSource = messageDataSource,
            settingsRepository = settingsRepository,
            settingsRemoteDataSource = createTestSettingsRemoteDataSource(httpClient, FakeLogger()),
            defaultDispatcher = StandardTestDispatcher(testScheduler),
        )
        testBody()
    }

    /**
     * Own user, two accepted friends (anna with messages, ben without), a pending request (dave),
     * and the group "hiking" with own, anna and ben.
     */
    private suspend fun seedUsersAndGroup() {
        listOf(
            testUser(OWN_ID, name = "Me", friendshipStatus = null),
            testUser("anna", name = "Anna"),
            testUser("ben", name = "Ben"),
            testUser("dave", name = "Dave", friendshipStatus = FriendshipStatus.PENDING, requesterId = "dave"),
        ).forEach { userDataSource.upsertUser(it.toDto()) }
        groupDataSource.upsertGroup(testGroup("hiking", name = "Hiking", memberIds = listOf(OWN_ID, "anna", "ben")))
    }

    private suspend fun insert(vararg messages: Message) = messages.forEach { messageDataSource.upsertMessage(it) }

    private fun List<ChatListItem>.chat(id: String) = single { it.id == id }

    @Test
    fun `lists accepted friends and groups, without pending users or the own user`() = runChatListTest {
        seedUsersAndGroup()
        insert(
            testMessage("m1", senderId = "anna", receiverId = OWN_ID, sendDate = 1_000L),
            testMessage("m2", senderId = OWN_ID, receiverId = "hiking", sendDate = 3_000L, group = true, myMessage = true),
            testMessage("m3", senderId = "dave", receiverId = OWN_ID, sendDate = 5_000L),
        )

        val chats = repository.getChatListFlow(OWN_ID).first()

        // Newest last message first; a chat without messages last
        assertThat(chats.map { it.id to it.isGroup }).containsExactly("hiking" to true, "anna" to false, "ben" to false)
    }

    @Test
    fun `a direct chat collects both directions, a group chat its group messages`() = runChatListTest {
        seedUsersAndGroup()
        insert(
            testMessage("m1", senderId = "anna", receiverId = OWN_ID, sendDate = 1_000L, readByMe = false),
            testMessage("m2", senderId = "anna", receiverId = OWN_ID, sendDate = 2_000L, readByMe = false),
            testMessage("m3", senderId = OWN_ID, receiverId = "anna", sendDate = 3_000L, myMessage = true),
            // still queued (offline send): no server id yet
            testMessage(null, senderId = OWN_ID, receiverId = "anna", sendDate = 4_000L, myMessage = true, sent = false),
            testMessage("g1", senderId = "anna", receiverId = "hiking", sendDate = 1_500L, group = true, readByMe = false),
            testMessage("g2", senderId = OWN_ID, receiverId = "hiking", sendDate = 2_500L, group = true, myMessage = true),
            testMessage("g3", senderId = "ben", receiverId = "hiking", sendDate = 3_500L, group = true, readByMe = true),
        )

        val chats = repository.getChatListFlow(OWN_ID).first()

        val anna = chats.chat("anna")
        assertThat(anna.unreadMessageCount).isEqualTo(2)
        assertThat(anna.unsentMessageCount).isEqualTo(1)
        assertThat(anna.lastMessage).isNotNull().prop(Message::sent).isFalse()
        assertThat(anna.lastMessage?.getSendDateAsLong()).isEqualTo(4_000L)

        val hiking = chats.chat("hiking")
        assertThat(hiking.isGroup).isTrue()
        assertThat(hiking.unreadMessageCount).isEqualTo(1)
        assertThat(hiking.unsentMessageCount).isEqualTo(0)
        assertThat(hiking.lastMessage?.id).isEqualTo("g3")
        assertThat(hiking.lastMessage?.senderAsString).isEqualTo("Ben")

        val ben = chats.chat("ben")
        assertThat(ben.lastMessage).isNull()
        assertThat(ben.unreadMessageCount).isEqualTo(0)
    }

    @Test
    fun `unread badge counts chats, only of accepted friends and stored groups`() = runChatListTest {
        seedUsersAndGroup()
        insert(
            // anna: two unread messages, one chat
            testMessage("m1", senderId = "anna", receiverId = OWN_ID, sendDate = 1_000L, readByMe = false),
            testMessage("m2", senderId = "anna", receiverId = OWN_ID, sendDate = 2_000L, readByMe = false),
            // hiking: one unread group message
            testMessage("g1", senderId = "ben", receiverId = "hiking", sendDate = 1_500L, group = true, readByMe = false),
            // not counted: pending request, unknown sender, group that is gone, SYSTEM line, read message
            testMessage("p1", senderId = "dave", receiverId = OWN_ID, sendDate = 3_000L, readByMe = false),
            testMessage("x1", senderId = "stranger", receiverId = OWN_ID, sendDate = 3_100L, readByMe = false),
            testMessage("x2", senderId = "anna", receiverId = "left-group", sendDate = 3_200L, group = true, readByMe = false),
            testMessage("s1", senderId = "ben", receiverId = OWN_ID, sendDate = 3_300L, readByMe = false, msgType = MessageType.SYSTEM),
            testMessage("r1", senderId = "ben", receiverId = OWN_ID, sendDate = 3_400L, readByMe = true),
        )

        assertThat(repository.getUnreadChatCountFlow().first()).isEqualTo(2)
    }

    @Test
    fun `pinned chats come first, newest pin on top`() = runChatListTest {
        seedUsersAndGroup()
        insert(testMessage("g1", senderId = "anna", receiverId = "hiking", sendDate = 9_000L, group = true))
        settingsRepository.pinnedChats.value = listOf(
            PinnedChat(chatId = "ben", group = false, pinTimePoint = 200L),
            PinnedChat(chatId = "anna", group = false, pinTimePoint = 100L),
        )

        val chats = repository.getChatListFlow(OWN_ID).first()

        assertThat(chats.map { it.id }).containsExactly("ben", "anna", "hiking")
        assertThat(chats.chat("ben").pinned).isEqualTo(200L)
        assertThat(chats.chat("hiking").pinned).isEqualTo(0L)
    }

    @Test
    fun `setPinnedChat stores the pin and pushes the whole list to the server`() = runChatListTest {
        settingsRepository.pinnedChats.value = listOf(PinnedChat(chatId = "anna", group = false, pinTimePoint = 100L))

        repository.setPinnedChat(chatId = "hiking", group = true, pinned = true)

        assertThat(settingsRepository.pinnedChats.value.map { it.chatId }).containsExactly("hiking", "anna")
        assertThat(settingsPushes.single().pinnedChats?.map { it.chatId }).isEqualTo(listOf("hiking", "anna"))

        repository.setPinnedChat(chatId = "anna", group = false, pinned = false)

        assertThat(settingsRepository.pinnedChats.value.map { it.chatId }).containsExactly("hiking")
        assertThat(settingsPushes.last().pinnedChats?.map { it.chatId }).isEqualTo(listOf("hiking"))
    }

    @Test
    fun `pending friends lists requests in both directions`() = runChatListTest {
        seedUsersAndGroup()
        userDataSource.upsertUser(
            testUser("emil", name = "Emil", friendshipStatus = FriendshipStatus.PENDING, requesterId = OWN_ID).toDto()
        )

        val pending = repository.getPendingFriends("").first()

        assertThat(pending.map { it.id to it.requesterId }).containsExactlyInAnyOrder("dave" to "dave", "emil" to OWN_ID)
    }
}
