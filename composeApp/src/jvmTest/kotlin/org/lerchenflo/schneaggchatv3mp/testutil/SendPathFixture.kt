package org.lerchenflo.schneaggchatv3mp.testutil

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import org.lerchenflo.schneaggchatv3mp.app.data.DefaultErrorChannel
import org.lerchenflo.schneaggchatv3mp.chat.data.OfflineFirstMessageRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.RoomMessageDataSource
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.KtorMessageRemoteDataSource
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.MessageRemoteDataSource
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageContent
import org.lerchenflo.schneaggchatv3mp.datasource.database.AppDatabase
import org.lerchenflo.schneaggchatv3mp.datasource.network.createHttpClient
import org.lerchenflo.schneaggchatv3mp.fakes.FakeLogger

/** The logged-in user of the send-path tests. */
const val OWN_USER_ID = "me"

/** The chat partner the send-path tests write to. */
const val FRIEND_ID = "friend"

/**
 * Builds the object under test for the message send-path tests (plan Phase 4.5) and everything
 * around it:
 * - a real in-memory [AppDatabase] with a real [RoomMessageDataSource];
 * - a real [KtorMessageRemoteDataSource] ([createTestMessageRemoteDataSource]) on a Ktor client
 *   built by the app's own `createHttpClient()` (JSON set up like in the app), whose engine is
 *   [FakeMessageServer]'s `MockEngine`;
 * - a real [DefaultErrorChannel] and a [FakeLogger];
 * - [unusedDependency] stubs for every constructor parameter the send path never touches. Text
 *   messages never use `PictureManager` / `AudioManager`, so they are stubs too.
 *
 * The object under test is the [OfflineFirstMessageRepository] ([repository]); Phase 5b moved
 * `sendMessage`, `sendOfflineMessages` and `messageIdSync` (now `syncMessages`) there from
 * `AppRepository`. Its `externalScope` is `testScope.backgroundScope` and its `ioDispatcher` runs
 * on `testScope.testScheduler` (plan R2, R4), so build the fixture inside `runTest`.
 *
 * Call [close] in `@AfterTest`.
 */
class SendPathFixture(testScope: TestScope, val ownUserId: String = OWN_USER_ID) {

    val server = FakeMessageServer(ownUserId)

    val database: AppDatabase = createInMemoryAppDatabase()

    val logger = FakeLogger()

    val errorChannel = DefaultErrorChannel()

    private val httpClient = createHttpClient(server.engine)

    private val messageLocalDataSource = RoomMessageDataSource(database)

    val messageRemoteDataSource: MessageRemoteDataSource = createTestMessageRemoteDataSource(httpClient, logger)

    val repository = OfflineFirstMessageRepository(
        database = database,
        messageLocalDataSource = messageLocalDataSource,
        messageRemoteDataSource = messageRemoteDataSource,
        pictureManager = unusedDependency(),
        audioManager = unusedDependency(),
        loggingRepository = logger,
        errorChannel = errorChannel,
        externalScope = testScope.backgroundScope,
        ioDispatcher = StandardTestDispatcher(testScope.testScheduler),
    )

    // The three entry points under test, called exactly like the app calls them (ChatViewModel,
    // GlobalViewModel, dataSync). Kept here so a moved signature or a moved MessageContent
    // only changes this file.

    /** A new text message, as `ChatViewModel` sends it. */
    suspend fun sendText(
        text: String,
        receiverId: String = FRIEND_ID,
        isGroup: Boolean = false,
        answerId: String? = null,
    ) = repository.sendMessage(
        ownId = ownUserId,
        messageId = null,
        empfaenger = receiverId,
        gruppe = isGroup,
        content = MessageContent.TextContent(text),
        answerid = answerId,
    )

    /** Drains the offline queue, as `GlobalViewModel` does on resume / reconnect. */
    suspend fun sendOfflineMessages() = repository.sendOfflineMessages(ownUserId)

    /** One message sync; needs a logged-in `SessionCache` ([loginTestUser]). */
    suspend fun syncMessages() = repository.syncMessages()

    /** Every row of the local messages table, oldest local row first. */
    suspend fun storedMessages(): List<Message> =
        messageLocalDataSource.getAllMessages().first().sortedBy { it.localPK }

    fun close() {
        httpClient.close()
        database.close()
    }
}
