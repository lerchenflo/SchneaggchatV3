package org.lerchenflo.schneaggchatv3mp.datasource.domain.model

import org.lerchenflo.schneaggchatv3mp.utilities.DEVICETYPE

/**
 * One logged-in device (server session) of the own account, as returned by
 * [org.lerchenflo.schneaggchatv3mp.datasource.domain.AuthRepository.getSessions].
 * Domain copy of the network DTO `NetworkUtils.SessionResponse`, field for field.
 * [deviceName] / [deviceType] are null when the device never sent them; the timestamps are
 * epoch millis.
 */
data class DeviceSession(
    val id: String,
    val deviceName: String?,
    val deviceType: DEVICETYPE?,
    val createdAt: Long,
    val lastUsedAt: Long,
)
