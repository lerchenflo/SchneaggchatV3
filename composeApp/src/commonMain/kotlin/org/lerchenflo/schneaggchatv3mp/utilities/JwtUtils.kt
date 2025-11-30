@file:OptIn(ExperimentalTime::class)

package org.lerchenflo.schneaggchatv3mp.utilities

import com.appstractive.jwt.JWT
import com.appstractive.jwt.expiresAt
import com.appstractive.jwt.from
import com.appstractive.jwt.subject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

object JwtUtils {
    fun getUserIdFromToken(token: String) : String {
        val jwt = JWT.from(token)
        val userid = jwt.subject!! //Subject of this jwt token is the users id
        return userid
    }

    fun isTokenDateValid(token: String) : Boolean {
        if (token.isBlank()) return false
        val jwt = JWT.from(token)
        val now = Clock.System.now()
        val exp = jwt.expiresAt

        val isvalid = exp != null && exp.toEpochMilliseconds() > now.toEpochMilliseconds()

        return isvalid
    }

    fun getTokenValidRemainingMinutes(token: String) : Long {
        if (token.isBlank()) return 0

        val jwt = JWT.from(token)
        val exp = jwt.expiresAt ?: return 0
        val now = Clock.System.now()

        val remainingMillis = exp.toEpochMilliseconds() - now.toEpochMilliseconds()
        val remainingMinutes = remainingMillis / 1000 / 60

        return remainingMinutes
    }
}

