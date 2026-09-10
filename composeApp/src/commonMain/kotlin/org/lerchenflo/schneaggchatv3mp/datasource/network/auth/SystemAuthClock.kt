@file:OptIn(ExperimentalTime::class)

package org.lerchenflo.schneaggchatv3mp.datasource.network.auth

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

object SystemAuthClock : AuthClock {
    override fun now(): Instant = Clock.System.now()
}
