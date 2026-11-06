package org.lerchenflo.schneaggchatv3mp.settings.data.remote

import org.lerchenflo.schneaggchatv3mp.app.domain.DataError
import org.lerchenflo.schneaggchatv3mp.app.domain.DataResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.UserSettingsRequest

/** Pushes the personal settings that are synced across every device of the account. */
interface SettingsRemoteDataSource {

    suspend fun updateSettings(request: UserSettingsRequest): DataResult<Unit, DataError>
}
