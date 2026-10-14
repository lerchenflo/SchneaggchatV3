package org.lerchenflo.schneaggchatv3mp.fakes

import kotlinx.coroutines.flow.MutableStateFlow
import org.lerchenflo.schneaggchatv3mp.datasource.domain.DataSyncCoordinator
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.DataSyncState

/** [DataSyncCoordinator] that records every [dataSync] reason and runs [onDataSync]. */
class FakeDataSyncCoordinator : DataSyncCoordinator {

    override val dataSyncState = MutableStateFlow(DataSyncState())

    /** Replace it to make a sync suspend (e.g. on a `CompletableDeferred`) or fail. */
    var onDataSync: suspend (reason: String) -> Unit = {}

    val syncReasons = CallLog<String>()

    override suspend fun dataSync(reason: String) {
        syncReasons.record(reason)
        onDataSync(reason)
    }
}
