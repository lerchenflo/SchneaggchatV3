package org.lerchenflo.schneaggchatv3mp.schneaggmap.domain

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

private val POLICE_ACTIVE_TIME = 3.hours

/** A police sighting counts for 3 hours, a mobile radar until its day is over. */
private fun LocationData.isActiveSighting(now: Instant, timeZone: TimeZone): Boolean = when (this) {

    //Police highlighted for 3 hours
    is LocationData.Police -> policeLastSeenValue
        ?.let { now - Instant.fromEpochMilliseconds(it) < POLICE_ACTIVE_TIME } == true

    //Radar highlighted for whole day
    is LocationData.Radar -> radarMobileValue == true && radarLastSeenValue
        ?.let { Instant.fromEpochMilliseconds(it).toLocalDateTime(timeZone).date == now.toLocalDateTime(timeZone).date } == true

    else -> false
}

/** The location types of this entry that have an active sighting right now. */
fun MapEntry.activeSightingTypes(now: Instant, timeZone: TimeZone): Set<LocationType> =
    locationData.filter { it.isActiveSighting(now, timeZone) }
        .mapTo(mutableSetOf()) { it.locationtype }
