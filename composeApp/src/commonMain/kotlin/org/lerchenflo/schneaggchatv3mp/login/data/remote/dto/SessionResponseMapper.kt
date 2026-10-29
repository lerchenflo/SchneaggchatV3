package org.lerchenflo.schneaggchatv3mp.login.data.remote.dto

import org.lerchenflo.schneaggchatv3mp.login.domain.DeviceSession

fun SessionResponse.toDeviceSession(): DeviceSession = DeviceSession(
    id = id,
    deviceName = deviceName,
    deviceType = deviceType,
    createdAt = createdAt,
    lastUsedAt = lastUsedAt,
)
