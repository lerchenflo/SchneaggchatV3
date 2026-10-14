package org.lerchenflo.schneaggchatv3mp.app

import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.DataSyncJobType
import org.lerchenflo.schneaggchatv3mp.utilities.UiText
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.eventssync
import schneaggchatv3mp.composeapp.generated.resources.groupsync
import schneaggchatv3mp.composeapp.generated.resources.mapsync
import schneaggchatv3mp.composeapp.generated.resources.mediasync
import schneaggchatv3mp.composeapp.generated.resources.messagesync
import schneaggchatv3mp.composeapp.generated.resources.usersync

/**
 * Label of one sync job in the sync status popup. Kept out of `datasource/domain` so the model
 * there doesn't pull in Compose resources (was a member of `AppRepository.DataSyncJobType`).
 */
fun DataSyncJobType.toUiText() : UiText {
    return when (this) {
        DataSyncJobType.USERS -> UiText.StringResourceText(Res.string.usersync)
        DataSyncJobType.GROUPS -> UiText.StringResourceText(Res.string.groupsync)
        DataSyncJobType.MESSAGES -> UiText.StringResourceText(Res.string.messagesync)
        DataSyncJobType.MAP -> UiText.StringResourceText(Res.string.mapsync)
        DataSyncJobType.EVENTS -> UiText.StringResourceText(Res.string.eventssync)
        DataSyncJobType.MEDIA -> UiText.StringResourceText(Res.string.mediasync)
    }
}
