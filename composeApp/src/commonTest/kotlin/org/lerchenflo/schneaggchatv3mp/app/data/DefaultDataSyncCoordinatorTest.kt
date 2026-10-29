package org.lerchenflo.schneaggchatv3mp.app.data

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.lerchenflo.schneaggchatv3mp.app.logging.LogType
import org.lerchenflo.schneaggchatv3mp.app.domain.DataSyncJobStatus
import org.lerchenflo.schneaggchatv3mp.app.domain.DataSyncJobStatus.FAILED
import org.lerchenflo.schneaggchatv3mp.app.domain.DataSyncJobStatus.IDLE
import org.lerchenflo.schneaggchatv3mp.app.domain.DataSyncJobStatus.RUNNING
import org.lerchenflo.schneaggchatv3mp.app.domain.DataSyncJobStatus.SUCCESS
import org.lerchenflo.schneaggchatv3mp.app.domain.DataSyncJobType
import org.lerchenflo.schneaggchatv3mp.app.domain.DataSyncState
import org.lerchenflo.schneaggchatv3mp.fakes.FakeEventRepository
import org.lerchenflo.schneaggchatv3mp.fakes.FakeGroupRepository
import org.lerchenflo.schneaggchatv3mp.fakes.FakeLogger
import org.lerchenflo.schneaggchatv3mp.fakes.FakeMapRepository
import org.lerchenflo.schneaggchatv3mp.fakes.FakeMessageRepository
import org.lerchenflo.schneaggchatv3mp.fakes.FakeUserRepository
import org.lerchenflo.schneaggchatv3mp.testutil.loginTestUser
import org.lerchenflo.schneaggchatv3mp.testutil.resetGlobalState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

private const val OWN_ID = "me"

/**
 * Job order, the lock, the progress state and the purge of removed groups of the full data sync.
 * All on one `StandardTestDispatcher` scheduler (plan R12): `externalScope` is the test's
 * `backgroundScope`, and a sync is held in the middle by letting a fake suspend on a gate.
 *
 * The tests drive the scheduler with `runCurrent()`, not `advanceUntilIdle()`: the sync body runs
 * in `backgroundScope`, and `advanceUntilIdle()` stops as soon as only background work is left.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DefaultDataSyncCoordinatorTest {

    private val userRepository = FakeUserRepository()
    private val groupRepository = FakeGroupRepository()
    private val messageRepository = FakeMessageRepository()
    private val mapRepository = FakeMapRepository()
    private val eventRepository = FakeEventRepository()
    private val logger = FakeLogger()

    @BeforeTest
    fun setUp() {
        resetGlobalState()
        loginTestUser(OWN_ID)
    }

    @AfterTest
    fun tearDown() {
        resetGlobalState()
    }

    private fun TestScope.createCoordinator() = DefaultDataSyncCoordinator(
        userRepository = userRepository,
        groupRepository = groupRepository,
        messageRepository = messageRepository,
        mapRepository = mapRepository,
        eventRepository = eventRepository,
        loggingRepository = logger,
        externalScope = backgroundScope,
    )

    private fun DataSyncState.statusOf(type: DataSyncJobType): DataSyncJobStatus = jobs.single { it.type == type }.status

    @Test
    fun `runs the five syncs then purges the removed groups then fetches the missing media`() = runTest {
        val log = mutableListOf<String>()
        userRepository.onSyncUsers = { log += "users" }
        messageRepository.onSyncMessages = { log += "messages" }
        groupRepository.onSyncGroups = { log += "groups"; listOf("left-group") }
        mapRepository.onSyncMapEntries = { log += "map" }
        eventRepository.onSyncEvents = { log += "events" }
        userRepository.onFetchMissingProfilePictures = { log += "media:users" }
        groupRepository.onFetchMissingGroupPictures = { log += "media:groups" }
        messageRepository.onFetchMissingMessageMedia = {
            log += "media:messages after purge of ${messageRepository.deletedGroupMessages.calls}"
        }

        createCoordinator().dataSync(reason = "test")

        assertThat(log.take(5)).containsExactlyInAnyOrder("users", "messages", "groups", "map", "events")
        assertThat(log.drop(5)).containsExactlyInAnyOrder(
            "media:users",
            "media:groups",
            "media:messages after purge of [[left-group]]",
        )
        // The full sync downloads media in its own MEDIA job, not inside the message sync
        assertThat(messageRepository.syncCalls.calls).containsExactly(false)
    }

    @Test
    fun `purges the removed groups only after the parallel message sync finished`() = runTest {
        val messageSyncGate = CompletableDeferred<Unit>()
        messageRepository.onSyncMessages = { messageSyncGate.await() }
        groupRepository.onSyncGroups = { listOf("left-group") }
        val coordinator = createCoordinator()

        launch { coordinator.dataSync(reason = "test") }
        runCurrent()

        assertThat(groupRepository.syncCalls.count).isEqualTo(1)
        assertThat(messageRepository.deletedGroupMessages.calls).isEmpty()

        messageSyncGate.complete(Unit)
        runCurrent()

        assertThat(messageRepository.deletedGroupMessages.calls).containsExactly(listOf("left-group"))
    }

    @Test
    fun `state goes from idle to running to success and back to not syncing`() = runTest {
        val messageSyncGate = CompletableDeferred<Unit>()
        messageRepository.onSyncMessages = { messageSyncGate.await() }
        val coordinator = createCoordinator()
        assertThat(coordinator.dataSyncState.value).isEqualTo(DataSyncState())

        launch { coordinator.dataSync(reason = "test") }
        runCurrent()

        val running = coordinator.dataSyncState.value
        assertThat(running.isSyncing).isTrue()
        assertThat(running.statusOf(DataSyncJobType.MESSAGES)).isEqualTo(RUNNING)
        assertThat(running.statusOf(DataSyncJobType.USERS)).isEqualTo(SUCCESS)
        assertThat(running.statusOf(DataSyncJobType.MEDIA)).isEqualTo(IDLE)

        messageSyncGate.complete(Unit)
        runCurrent()

        val done = coordinator.dataSyncState.value
        assertThat(done.isSyncing).isFalse()
        assertThat(done.exitedWithErrors).isFalse()
        assertThat(done.jobs.map { it.status }.distinct()).containsExactly(SUCCESS)
    }

    @Test
    fun `a failing job is marked failed while the others still run and the next sync starts clean`() = runTest {
        var usersFail = true
        userRepository.onSyncUsers = { if (usersFail) throw IllegalStateException("users endpoint down") }
        groupRepository.onSyncGroups = { listOf("left-group") }
        val coordinator = createCoordinator()

        coordinator.dataSync(reason = "first")

        val failed = coordinator.dataSyncState.value
        assertThat(failed.exitedWithErrors).isTrue()
        assertThat(failed.jobs.single { it.type == DataSyncJobType.USERS }.error).isEqualTo("users endpoint down")
        assertThat(failed.statusOf(DataSyncJobType.USERS)).isEqualTo(FAILED)
        assertThat(failed.jobs.filter { it.type != DataSyncJobType.USERS }.map { it.status }.distinct())
            .containsExactly(SUCCESS)
        assertThat(messageRepository.deletedGroupMessages.calls).containsExactly(listOf("left-group"))
        assertThat(logger.logs.map { it.type to it.message })
            .containsExactly(LogType.WARNING to "User sync failed: users endpoint down")

        usersFail = false
        coordinator.dataSync(reason = "second")

        assertThat(coordinator.dataSyncState.value.exitedWithErrors).isFalse()
        assertThat(coordinator.dataSyncState.value.statusOf(DataSyncJobType.USERS)).isEqualTo(SUCCESS)
    }

    @Test
    fun `a sync requested while one runs waits for it and does not start another`() = runTest {
        val messageSyncGate = CompletableDeferred<Unit>()
        messageRepository.onSyncMessages = { messageSyncGate.await() }
        val coordinator = createCoordinator()

        val first = launch { coordinator.dataSync(reason = "first") }
        runCurrent()
        val second = launch { coordinator.dataSync(reason = "second") }
        runCurrent()

        assertThat(second.isCompleted).isFalse() // waits for the running sync
        assertThat(userRepository.syncCalls.count).isEqualTo(1)

        messageSyncGate.complete(Unit)
        runCurrent()

        assertThat(first.isCompleted).isTrue()
        assertThat(second.isCompleted).isTrue()
        assertThat(userRepository.syncCalls.count).isEqualTo(1)
        assertThat(messageRepository.deletedGroupMessages.count).isEqualTo(1)

        coordinator.dataSync(reason = "third") // nothing running any more: a full sync again

        assertThat(userRepository.syncCalls.count).isEqualTo(2)
    }

    @Test
    fun `the sync finishes even when its caller is cancelled`() = runTest {
        val messageSyncGate = CompletableDeferred<Unit>()
        messageRepository.onSyncMessages = { messageSyncGate.await() }
        val coordinator = createCoordinator()

        val caller = launch { coordinator.dataSync(reason = "screen") }
        runCurrent()
        caller.cancel() // e.g. the screen that started it was closed
        messageSyncGate.complete(Unit)
        runCurrent()

        assertThat(caller.isCancelled).isTrue()
        assertThat(messageRepository.deletedGroupMessages.count).isEqualTo(1)
        assertThat(messageRepository.fetchMissingMediaCalls.count).isEqualTo(1)
        assertThat(coordinator.dataSyncState.value.isSyncing).isFalse()
    }

    @Test
    fun `does nothing while logged out`() = runTest {
        resetGlobalState() // logs out
        val coordinator = createCoordinator()

        coordinator.dataSync(reason = "test")

        assertThat(userRepository.syncCalls.calls).isEmpty()
        assertThat(messageRepository.syncCalls.calls).isEmpty()
        assertThat(coordinator.dataSyncState.value).isEqualTo(DataSyncState())
    }
}
