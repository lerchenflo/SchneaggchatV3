package org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses

import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.DeviceSession
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils

fun NetworkUtils.SessionResponse.toDeviceSession(): DeviceSession = DeviceSession(
    id = id,
    deviceName = deviceName,
    deviceType = deviceType,
    createdAt = createdAt,
    lastUsedAt = lastUsedAt,
)
