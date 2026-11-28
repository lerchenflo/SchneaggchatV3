package org.lerchenflo.schneaggchatv3mp.chat.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import org.lerchenflo.schneaggchatv3mp.LOOKEDUPUSERPICTURE_FILE_NAME
import org.lerchenflo.schneaggchatv3mp.app.domain.DataResult
import org.lerchenflo.schneaggchatv3mp.app.domain.Logger
import org.lerchenflo.schneaggchatv3mp.chat.data.local.LookedUpUserLocalDataSource
import org.lerchenflo.schneaggchatv3mp.chat.data.local.UserLocalDataSource
import org.lerchenflo.schneaggchatv3mp.chat.data.local.entity.LookedUpUserEntity
import org.lerchenflo.schneaggchatv3mp.chat.data.local.entity.toUserIdentity
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.UserRemoteDataSource
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto.USER_LOOKUP_MAX_IDS
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto.UserLookupResponse
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto.toLookedUpUserEntities
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.chat.domain.UserIdentity
import org.lerchenflo.schneaggchatv3mp.chat.domain.UserIdentityName
import org.lerchenflo.schneaggchatv3mp.chat.domain.UserIdentityRepository
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.MediaDownloadThrottle
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.trackConnectivity
import org.lerchenflo.schneaggchatv3mp.utilities.format.getCurrentTimeMillisLong
import org.lerchenflo.schneaggchatv3mp.utilities.media.PictureManager
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

/**
 * [UserIdentityRepository] on top of the users table ([UserLocalDataSource]), the looked-up users
 * ([LookedUpUserLocalDataSource]) and the server's user lookup.
 *
 * Lookups and picture downloads each run in one worker coroutine in [externalScope], one request
 * at a time. A worker checks the database again before each request, so an id that many flows ask
 * for at once is fetched only once.
 */
class OfflineFirstUserIdentityRepository(
    private val userLocalDataSource: UserLocalDataSource,
    private val lookedUpUserLocalDataSource: LookedUpUserLocalDataSource,
    private val userRemoteDataSource: UserRemoteDataSource,
    private val pictureManager: PictureManager,
    private val externalScope: CoroutineScope,
    private val logger: Logger,
    private val mediaDownloadThrottle: MediaDownloadThrottle,
) : UserIdentityRepository {

    private val lookupRequests = Channel<Collection<String>>(Channel.UNLIMITED)
    private val pictureRequests = Channel<LookedUpUserEntity>(Channel.UNLIMITED)

    // Lookups run one at a time (worker and getUser), so an id is never fetched twice at once
    private val lookupMutex = Mutex()

    // Only touched under lookupMutex (failedLookupAt) or inside the picture worker (triedPictures)
    private val failedLookupAt = mutableMapOf<String, Long>()
    private val triedPictures = mutableSetOf<Pair<String, Long>>()

    init {
        externalScope.launch { runLookupWorker() }
        externalScope.launch { runPictureWorker() }
    }

    // ─── Reads ────────────────────────────────────────────────────────────────

    override fun observeUser(userId: String): Flow<UserIdentity> =
        observeUsers(listOf(userId)).map { it.getValue(userId) }

    override suspend fun getUser(userId: String, maxWait: Duration): UserIdentity {
        val rows = knownRows(listOf(userId))
        if (!rows.needsLookup(userId, getCurrentTimeMillisLong())) return rows.identityOf(userId)

        // Null when maxWait ran out (also while the worker held the lock); the cancelled request
        // is sent again in the background so the next notification has the name
        val finished = withTimeoutOrNull(maxWait) { lookupMutex.withLock { lookUpBatch(listOf(userId)) } }
        if (finished == null) prefetch(listOf(userId))
        // An outdated looked-up row still gives its old name here
        return knownRows(listOf(userId)).identityOf(userId)
    }

    private suspend fun knownRows(ids: List<String>): KnownRows = KnownRows(
        storedUsers = userLocalDataSource.getUsersByIds(ids).associateBy { it.id },
        lookedUpUsers = lookedUpUserLocalDataSource.getByIds(ids).associateBy { it.id },
    )

    override fun observeUsers(userIds: Collection<String>): Flow<Map<String, UserIdentity>> {
        val ids = userIds.filter { it.isNotBlank() }.distinct()
        if (ids.isEmpty()) return flowOf(userIds.associateWith { UserIdentity(it, UserIdentityName.Unknown) })

        return combine(
            userLocalDataSource.getUsersByIdsFlow(ids),
            lookedUpUserLocalDataSource.getByIdsFlow(ids),
        ) { storedUsers, lookedUpUsers ->
            KnownRows(storedUsers.associateBy { it.id }, lookedUpUsers.associateBy { it.id })
        }
            .onEach { rows -> requestWhatIsMissing(ids, rows) }
            .map { rows -> ids.associateWith { id -> rows.identityOf(id) } }
            // The users table changes with every presence update; emit only real changes
            .distinctUntilChanged()
    }

    override fun observeKnownNames(): Flow<Map<String, UserIdentityName>> =
        combine(
            userLocalDataSource.getAllUsersFlow(),
            lookedUpUserLocalDataSource.getAllFlow(),
        ) { storedUsers, lookedUpUsers ->
            // Stored users go last so they win over an older looked-up row of the same id
            lookedUpUsers.associate { it.id to it.toUserIdentity().name } +
                storedUsers.associate { it.id to it.toUserIdentity().name }
        }.distinctUntilChanged()

    override fun prefetch(userIds: Collection<String>) {
        val ids = userIds.filter { it.isNotBlank() }.toSet()
        if (ids.isNotEmpty()) lookupRequests.trySend(ids)
    }

    override suspend fun cleanUp() {
        val staleRows = lookedUpUserLocalDataSource.getFetchedBefore(getCurrentTimeMillisLong() - DELETE_AFTER.inWholeMilliseconds)
        staleRows.forEach { deleteStoredPicture(it.id) }
        lookedUpUserLocalDataSource.deleteByIds(staleRows.map { it.id })

        // iOS moves the app folder on updates, so a stored path can point to nothing
        val lostPictureIds = lookedUpUserLocalDataSource.getWithProfilePicture()
            .filter { !pictureManager.checkImageExists(it.profilePictureUrl) }
            .map { it.id }
        if (lostPictureIds.isNotEmpty()) lookedUpUserLocalDataSource.clearProfilePictureUrls(lostPictureIds)
    }

    /** The stored rows for one emission of [observeUsers]. */
    private class KnownRows(
        val storedUsers: Map<String, User>,
        val lookedUpUsers: Map<String, LookedUpUserEntity>,
    ) {
        fun identityOf(id: String): UserIdentity =
            storedUsers[id]?.toUserIdentity()
                ?: lookedUpUsers[id]?.toUserIdentity()
                ?: UserIdentity(id, UserIdentityName.Unknown)

        /** Neither stored nor looked up within the last day. */
        fun needsLookup(id: String, now: Long): Boolean =
            id !in storedUsers && lookedUpUsers[id]?.isFresh(now) != true
    }

    private fun requestWhatIsMissing(ids: List<String>, rows: KnownRows) {
        val now = getCurrentTimeMillisLong()
        val notStored = ids.filter { it !in rows.storedUsers }

        val toLookUp = ids.filter { rows.needsLookup(it, now) }
        if (toLookUp.isNotEmpty()) lookupRequests.trySend(toLookUp)

        notStored.mapNotNull { rows.lookedUpUsers[it] }
            .filter { it.needsPicture() }
            .forEach { pictureRequests.trySend(it) }
    }

    // ─── Lookup worker ────────────────────────────────────────────────────────

    private suspend fun runLookupWorker() {
        for (firstRequest in lookupRequests) {
            val ids = firstRequest.toMutableSet()
            // Flows that start together (a list of rows) ask one after another; wait for them
            delay(COLLECT_WINDOW)
            while (true) {
                ids += lookupRequests.tryReceive().getOrNull() ?: break
            }
            ids.chunked(USER_LOOKUP_MAX_IDS).forEach { batch -> lookupMutex.withLock { lookUpBatch(batch) } }
        }
    }

    private suspend fun lookUpBatch(requestedIds: List<String>) {
        val now = getCurrentTimeMillisLong()
        val storedIds = userLocalDataSource.getUsersByIds(requestedIds).map { it.id }.toSet()
        val existingRows = lookedUpUserLocalDataSource.getByIds(requestedIds).associateBy { it.id }

        val ids = requestedIds.filter { id ->
            id !in storedIds &&
                existingRows[id]?.isFresh(now) != true &&
                !failedRecently(id, now)
        }
        if (ids.isEmpty()) return

        when (val result = userRemoteDataSource.lookUpUsers(ids).trackConnectivity()) {
            is DataResult.Error -> {
                ids.forEach { failedLookupAt[it] = now }
                logger.logWarning("user lookup error for ${ids.size} ids: ${result.error}")
            }
            is DataResult.Success -> {
                ids.forEach { failedLookupAt.remove(it) }
                storeLookups(result.data, existingRows, now)
            }
        }
    }

    private suspend fun storeLookups(
        lookup: UserLookupResponse,
        existingRows: Map<String, LookedUpUserEntity>,
        now: Long,
    ) {
        val rows = lookup.toLookedUpUserEntities(existingRows, fetchedAt = now)
        // A hidden picture or a deleted account leaves no picture on the device
        rows.filter { it.profilePicUpdatedAt == null && existingRows[it.id] != null }
            .forEach { deleteStoredPicture(it.id) }
        lookedUpUserLocalDataSource.upsertAll(rows)
    }

    private fun failedRecently(id: String, now: Long): Boolean {
        val failedAt = failedLookupAt[id] ?: return false
        return now - failedAt < RETRY_FAILED_AFTER.inWholeMilliseconds
    }

    // ─── Picture worker ───────────────────────────────────────────────────────

    private suspend fun runPictureWorker() {
        for (row in pictureRequests) {
            val pictureUpdatedAt = row.profilePicUpdatedAt ?: continue
            // Each picture version is tried once per app run, so a refused or lost picture is not
            // asked for again on every emission
            if (!triedPictures.add(row.id to pictureUpdatedAt)) continue
            downloadPicture(row.id, pictureUpdatedAt)
        }
    }

    private suspend fun downloadPicture(userId: String, pictureUpdatedAt: Long) {
        when (val picture = mediaDownloadThrottle.download { userRemoteDataSource.getProfilePicForUserId(userId) }.trackConnectivity()) {
            is DataResult.Error -> logger.logWarning("looked-up profile picture error for userid $userId")
            is DataResult.Success -> {
                val path = pictureManager.savePictureToStorage(picture.data, userId + LOOKEDUPUSERPICTURE_FILE_NAME)
                val stored = lookedUpUserLocalDataSource.updateProfilePictureUrl(userId, path, pictureUpdatedAt)
                // A lookup hid or changed the picture during the download
                if (!stored) deleteStoredPicture(userId)
            }
        }
    }

    private suspend fun deleteStoredPicture(userId: String) {
        pictureManager.deletePicture(userId + LOOKEDUPUSERPICTURE_FILE_NAME)
    }


    private fun LookedUpUserEntity.needsPicture(): Boolean =
        !deleted && profilePicUpdatedAt != null && profilePictureUrl.isEmpty()

    private companion object {
        val COLLECT_WINDOW = 100.milliseconds
        val DELETE_AFTER = 5.days
        val RETRY_FAILED_AFTER = 1.minutes
    }
}

/** A looked-up row is fetched again once it is older than this (plan N8). */
private val REFRESH_AFTER = 1.days

private fun LookedUpUserEntity.isFresh(now: Long): Boolean =
    now - fetchedAt < REFRESH_AFTER.inWholeMilliseconds
