package org.lerchenflo.schneaggchatv3mp.login.domain

import org.lerchenflo.schneaggchatv3mp.utilities.DEVICETYPE

/**
 * One logged-in device (server session) of the own account, as returned by
 * [org.lerchenflo.schneaggchatv3mp.login.domain.AuthRepository.getSessions].
 * Domain copy of the network DTO `SessionResponse`, field for field.
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
