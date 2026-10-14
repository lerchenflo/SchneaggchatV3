package org.lerchenflo.schneaggchatv3mp.datasource.domain.model

import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.DataSyncJobType.EVENTS
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.DataSyncJobType.GROUPS
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.DataSyncJobType.MAP
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.DataSyncJobType.MESSAGES
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.DataSyncJobType.USERS

// Progress of the full data sync, exposed by
// [org.lerchenflo.schneaggchatv3mp.datasource.domain.DataSyncCoordinator.dataSyncState].
// The job labels for the UI are in app/DataSyncJobTypeText.kt.

enum class DataSyncJobStatus { IDLE, RUNNING, SUCCESS, FAILED }

enum class DataSyncJobType {
    USERS,
    GROUPS,
    MESSAGES,
    MAP,
    EVENTS,

    MEDIA;
}

data class DataSyncJobState(
    val type: DataSyncJobType,
    val status: DataSyncJobStatus = DataSyncJobStatus.IDLE,
    val error: String? = null
)

data class DataSyncState(
    val isSyncing: Boolean = false,
    val exitedWithErrors: Boolean = false,
    val jobs: List<DataSyncJobState> = listOf(
        DataSyncJobState(type = USERS), DataSyncJobState(type = GROUPS),
        DataSyncJobState(type = MESSAGES), DataSyncJobState(type = MAP),
        DataSyncJobState(type = EVENTS),
        DataSyncJobState(type = DataSyncJobType.MEDIA)
    )
)
