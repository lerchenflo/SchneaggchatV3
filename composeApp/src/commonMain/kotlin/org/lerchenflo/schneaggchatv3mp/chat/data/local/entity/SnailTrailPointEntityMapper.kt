package org.lerchenflo.schneaggchatv3mp.chat.data.local.entity

import org.lerchenflo.schneaggchatv3mp.chat.domain.SnailTrailPoint

fun SnailTrailPointEntity.toSnailTrailPoint(): SnailTrailPoint = SnailTrailPoint(
    lat = lat,
    long = long,
    locationTime = locationTime,
    speed = speed,
    heading = heading,
)
