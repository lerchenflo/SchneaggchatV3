package org.lerchenflo.schneaggchatv3mp.testutil

import kotlinx.coroutines.flow.first
import org.lerchenflo.schneaggchatv3mp.app.data.DefaultErrorChannel
import org.lerchenflo.schneaggchatv3mp.chat.data.OfflineFirstMessageRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.RoomMessageDataSource
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.Message
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.MessageContent
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.datasource.database.AppDatabase
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils
import org.lerchenflo.schneaggchatv3mp.datasource.network.createHttpClient
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.fakes.FakeLogger
import org.lerchenflo.schneaggchatv3mp.fakes.FakePreferencesDataStore

/** The logged-in user of the send-path tests. */
const val OWN_USER_ID = "me"

/** The chat partner the send-path tests write to. */
const val FRIEND_ID = "friend"

/**
 * Builds the object under test for the message send-path tests (plan Phase 4.5) and everything
 * around it:
 * - a real in-memory [AppDatabase] with a real [RoomMessageDataSource];
 * - a real [NetworkUtils] on a Ktor client built by the app's own `createHttpClient()` (JSON set
 *   up like in the app), whose engine is [FakeMessageServer]'s `MockEngine`;
 * - a real `Preferencemanager` on an in-memory DataStore (`NetworkUtils` only reads the server
 *   URL from it);
 * - a real [DefaultErrorChannel] and a [FakeLogger];
 * - a real [OfflineFirstMessageRepository] on the same data source and server (Phase 5a moved the
 *   message media, edit/delete/react/poll code there; the text send path doesn't call it yet);
 * - [unusedDependency] stubs for every constructor parameter the send path never touches. Text
 *   messages never use `PictureManager` / `AudioManager`, so they are stubs too.
 *
 * The object under test is today's [AppRepository] ([repository]). When Phase 5 moves
 * `sendMessage`, `sendOfflineMessages` and `messageIdSync` into `OfflineFirstMessageRepository`
 * and leaves delegates on `AppRepository`, only this builder changes; the tests stay as they are.
 *
 * Call [close] in `@AfterTest`.
 */
class SendPathFixture(val ownUserId: String = OWN_USER_ID) {

    val server = FakeMessageServer(ownUserId)

    val database: AppDatabase = createInMemoryAppDatabase()

    val logger = FakeLogger()

    val errorChannel = DefaultErrorChannel()

    private val httpClient = createHttpClient(server.engine)

    private val messageLocalDataSource = RoomMessageDataSource(database)

    val networkUtils = NetworkUtils(
        httpClient = httpClient,
        authHttpClient = httpClient,
        preferenceManager = Preferencemanager(
            prefs = FakePreferencesDataStore(),
            securePrefs = unusableKSafe(),
            loggingRepository = logger,
        ),
        loggingRepository = logger,
        appVersion = unusedDependency(),
    )

    private val messageRepository = OfflineFirstMessageRepository(
        messageLocalDataSource = messageLocalDataSource,
        networkUtils = networkUtils,
        pictureManager = unusedDependency(),
        audioManager = unusedDependency(),
        loggingRepository = logger,
        errorChannel = errorChannel,
    )

    val repository = AppRepository(
        database = database,
        networkUtils = networkUtils,
        pictureManager = unusedDependency(),
        audioManager = unusedDependency(),
        messageLocalDataSource = messageLocalDataSource,
        appVersion = unusedDependency(),
        loggingRepository = logger,
        mapRepository = unusedDependency(),
        eventRepository = unusedDependency(),
        appInfoRepository = unusedDependency(),
        authRepository = unusedDependency(),
        sessionTerminator = unusedDependency(),
        errorChannel = errorChannel,
        settingsRepository = unusedDependency(),
        userRepository = unusedDependency(),
        groupRepository = unusedDependency(),
        chatListRepository = unusedDependency(),
        messageRepository = messageRepository,
    )

    // The three entry points under test, called exactly like the app calls them (ChatViewModel,
    // GlobalViewModel, dataSync). Kept here so a moved signature or a moved MessageContent
    // (Phase 5) only changes this file.

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
    suspend fun syncMessages() = repository.messageIdSync()

    /** Every row of the local messages table, oldest local row first. */
    suspend fun storedMessages(): List<Message> =
        messageLocalDataSource.getAllMessages().first().sortedBy { it.localPK }

    fun close() {
        httpClient.close()
        database.close()
    }
}
