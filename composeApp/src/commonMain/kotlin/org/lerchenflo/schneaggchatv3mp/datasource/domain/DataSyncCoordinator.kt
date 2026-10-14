package org.lerchenflo.schneaggchatv3mp.datasource.domain

import kotlinx.coroutines.flow.StateFlow
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.DataSyncState

/**
 * The full data sync: users, messages, groups, map entries and events in parallel, then the
 * messages of removed groups are purged, then missing media is downloaded. One sync at a time.
 */
interface DataSyncCoordinator {

    /** Progress of the running (or last) full sync, per job. */
    val dataSyncState: StateFlow<DataSyncState>

    /**
     * Runs a full sync, or - if one is already running - waits for it to finish and returns
     * without starting another. Does nothing while logged out.
     *
     * The sync runs in the app-wide scope: it finishes even when the caller is cancelled (e.g. a
     * screen that navigates away right after starting it); the caller only waits for it.
     *
     * @param reason short tag identifying what triggered this sync, logged so the origin of a
     * sync run is visible. Keep it stable and call-site specific (e.g. "appResumed").
     */
    suspend fun dataSync(reason: String)
}
