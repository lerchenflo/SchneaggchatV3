package org.lerchenflo.schneaggchatv3mp.settings.data.remote

import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.UserSettingsRequest
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError

/** Pushes the personal settings that are synced across every device of the account. */
interface SettingsRemoteDataSource {

    suspend fun updateSettings(request: UserSettingsRequest): NetworkResult<Unit, NetworkingError>
}
