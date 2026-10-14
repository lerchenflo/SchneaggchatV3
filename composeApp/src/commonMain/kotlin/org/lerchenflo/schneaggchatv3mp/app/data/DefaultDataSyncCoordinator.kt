package org.lerchenflo.schneaggchatv3mp.app.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.datasource.domain.DataSyncCoordinator
import org.lerchenflo.schneaggchatv3mp.datasource.domain.EventRepository
import org.lerchenflo.schneaggchatv3mp.datasource.domain.GroupRepository
import org.lerchenflo.schneaggchatv3mp.datasource.domain.Logger
import org.lerchenflo.schneaggchatv3mp.datasource.domain.MapRepository
import org.lerchenflo.schneaggchatv3mp.datasource.domain.MessageRepository
import org.lerchenflo.schneaggchatv3mp.datasource.domain.UserRepository
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.DataSyncJobStatus
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.DataSyncJobType
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.DataSyncJobType.EVENTS
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.DataSyncJobType.GROUPS
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.DataSyncJobType.MAP
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.DataSyncJobType.MESSAGES
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.DataSyncJobType.USERS
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.DataSyncState

/**
 * [DataSyncCoordinator] running the repositories' own syncs. Moved verbatim from AppRepository
 * (dataSync, dataSyncLock, dataSyncState, and getMissingPics as the MEDIA job).
 *
 * @param externalScope app-wide scope (Koin passes `ApplicationScope`) that [dataSync] runs in,
 * so a sync finishes even when its caller (e.g. a screen that navigates away) is cancelled
 * (plan R2). Tests pass `backgroundScope`.
 */
class DefaultDataSyncCoordinator(
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository,
    private val messageRepository: MessageRepository,
    private val mapRepository: MapRepository,
    private val eventRepository: EventRepository,
    private val loggingRepository: Logger,
    private val externalScope: CoroutineScope,
) : DataSyncCoordinator {

    private val dataSyncLock = Mutex()

    private fun updateSyncJob(type: DataSyncJobType, status: DataSyncJobStatus, error: String? = null) {
        _dataSyncState.update { current ->
            current.copy(
                jobs = current.jobs.map {
                    if (it.type == type) {
                        it.copy(
                            status = status, error = error
                        )
                    } else it
                },
                exitedWithErrors = if (current.exitedWithErrors) true else error != null //Keep error if one is set
            )
        }
    }

    private val _dataSyncState = MutableStateFlow(DataSyncState())
    override val dataSyncState: StateFlow<DataSyncState> = _dataSyncState.asStateFlow()

    override suspend fun dataSync(reason: String) {

        // externalScope (plan R2): started before the first suspension point, so the sync
        // finishes even when the caller (e.g. a screen that navigates away) is cancelled meanwhile.
        externalScope.async {
            SessionCache.requireLoggedIn() ?: return@async

            if (dataSyncLock.isLocked) {
                println("Datasync started but already running (reason: $reason)")
                dataSyncLock.withLock {  } //Awaiting finish and return to apply normal data sync await logic
                return@async
            }

            dataSyncLock.withLock {
                _dataSyncState.update {
                    it.copy(
                        isSyncing = true,
                        exitedWithErrors = false, //Reset error when synching again
                        // Reset stale statuses from the previous run, otherwise a job that hasn't
                        // started yet (e.g. MEDIA, which runs last) would still show its old
                        // SUCCESS/FAILED status while other jobs are already RUNNING.
                        jobs = it.jobs.map { job -> job.copy(status = DataSyncJobStatus.IDLE, error = null) }
                    )
                }
                println("Starting datasync (reason: $reason)")

                try {
                    coroutineScope {
                        // Groups deleted this sync - purged again after awaitAll below, since the
                        // parallel messageJob could re-insert rows right after groupIdSync's own
                        // inline purge (GroupLocalDataSource.deleteGroup) ran.
                        val removedGroupIds = mutableListOf<String>()

                        // Launch all sync operations concurrently
                        val userJob = async {
                            try {
                                updateSyncJob(
                                    type = USERS,
                                    status = DataSyncJobStatus.RUNNING,
                                    error = null
                                )
                                userRepository.syncUsers()
                                updateSyncJob(
                                    type = USERS,
                                    status = DataSyncJobStatus.SUCCESS,
                                    error = null
                                )
                                //println("User sync completed successfully")
                            } catch (e: Exception) {
                                ensureActive()
                                updateSyncJob(
                                    type = USERS,
                                    status = DataSyncJobStatus.FAILED,
                                    error = e.message
                                )
                                loggingRepository.logWarning("User sync failed: ${e.message}")
                            }
                        }

                        val messageJob = async {
                            try {
                                updateSyncJob(
                                    type = MESSAGES,
                                    status = DataSyncJobStatus.RUNNING,
                                    error = null
                                )
                                // Media is fetched afterwards as part of the MEDIA job so its
                                // progress indicator covers the actual download work.
                                messageRepository.syncMessages(fetchMedia = false)
                                updateSyncJob(
                                    type = MESSAGES,
                                    status = DataSyncJobStatus.SUCCESS,
                                    error = null
                                )
                                //println("Message sync completed successfully")
                            } catch (e: Exception) {
                                ensureActive()
                                updateSyncJob(
                                    type = MESSAGES,
                                    status = DataSyncJobStatus.FAILED,
                                    error = e.message
                                )
                                loggingRepository.logWarning("Message sync failed: ${e.message}")
                            }
                        }

                        val groupJob = async {
                            try {
                                updateSyncJob(
                                    type = GROUPS,
                                    status = DataSyncJobStatus.RUNNING,
                                    error = null
                                )
                                removedGroupIds += groupRepository.syncGroups()
                                updateSyncJob(
                                    type = GROUPS,
                                    status = DataSyncJobStatus.SUCCESS,
                                    error = null
                                )
                                //println("Group sync completed successfully")
                            } catch (e: Exception) {
                                ensureActive()
                                updateSyncJob(
                                    type = GROUPS,
                                    status = DataSyncJobStatus.FAILED,
                                    error = e.message
                                )
                                loggingRepository.logWarning("Group sync failed: ${e.message}")
                            }
                        }

                        val mapJob = async {
                            try {
                                updateSyncJob(
                                    type = MAP,
                                    status = DataSyncJobStatus.RUNNING,
                                    error = null
                                )
                                mapRepository.syncMapEntries()
                                updateSyncJob(
                                    type = MAP,
                                    status = DataSyncJobStatus.SUCCESS,
                                    error = null
                                )
                                //println("Map sync completed successfully")
                            } catch (e: Exception) {
                                ensureActive()
                                updateSyncJob(
                                    type = MAP,
                                    status = DataSyncJobStatus.FAILED,
                                    error = e.message
                                )
                                loggingRepository.logWarning("Map sync failed: ${e.message}")
                            }
                        }

                        val eventJob = async {
                            try {
                                updateSyncJob(
                                    type = EVENTS,
                                    status = DataSyncJobStatus.RUNNING,
                                    error = null
                                )
                                eventRepository.syncEvents()
                                updateSyncJob(
                                    type = EVENTS,
                                    status = DataSyncJobStatus.SUCCESS,
                                    error = null
                                )
                            } catch (e: Exception) {
                                ensureActive()
                                updateSyncJob(
                                    type = EVENTS,
                                    status = DataSyncJobStatus.FAILED,
                                    error = e.message
                                )
                                loggingRepository.logWarning("Event sync failed: ${e.message}")
                            }
                        }

                        // Wait for all to complete (errors are already handled)
                        awaitAll(userJob, messageJob, groupJob, mapJob, eventJob)

                        // The message job runs in parallel and may have re-inserted rows just
                        // after groupIdSync's inline purge ran, so repeat it once everything
                        // has settled.
                        messageRepository.deleteMessagesForGroups(removedGroupIds)

                        try {
                            updateSyncJob(
                                type = DataSyncJobType.MEDIA,
                                status = DataSyncJobStatus.RUNNING,
                                error = null
                            )
                            getMissingPics()
                            updateSyncJob(
                                type = DataSyncJobType.MEDIA,
                                status = DataSyncJobStatus.SUCCESS,
                                error = null
                            )
                        } catch (e: Exception) {
                            currentCoroutineContext().ensureActive()
                            updateSyncJob(
                                type = DataSyncJobType.MEDIA,
                                status = DataSyncJobStatus.FAILED,
                                error = e.message
                            )
                            loggingRepository.logWarning("Media sync failed: ${e.message}")
                        }
                    }

                    println("Data sync finished, started from $reason")
                } finally {
                    _dataSyncState.update {
                        it.copy(
                            isSyncing = false
                        )
                    }
                }
            }
        }.await()
    }

    /**
     * Get missing profile pics (On ios after update installation, there are no more pictures)
     */
    private suspend fun getMissingPics() = coroutineScope {
        // Check user profile pics (UserRepository), in parallel with the parts below
        launch { userRepository.fetchMissingProfilePictures() }

        // Check group profile pics (GroupRepository), in parallel with the parts below
        launch { groupRepository.fetchMissingGroupPictures() }

        // Check image and audio messages (MessageRepository), in parallel with the parts above
        launch { messageRepository.fetchMissingMessageMedia() }
    }
}
