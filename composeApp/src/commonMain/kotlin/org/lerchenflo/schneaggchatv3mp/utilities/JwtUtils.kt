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
        if (token.isBlank()) return ""
        return runCatching {
            val jwt = JWT.from(token)
            jwt.subject.orEmpty() //Subject of this jwt token is the users id
        }.getOrDefault("")
    }

    /**
     * The token's `exp` claim as epoch millis, or null when the token is blank, malformed or has
     * no `exp` at all. Callers decide what "now" is, so expiry can be evaluated against an
     * injected clock (see `AuthSessionManager`) instead of `Clock.System`.
     */
    fun expiresAtEpochMillis(token: String): Long? {
        if (token.isBlank()) return null
        return runCatching {
            JWT.from(token).expiresAt?.toEpochMilliseconds()
        }.getOrNull()
    }

    /** True when the token parses and its `exp` lies after [nowEpochMillis]. Missing `exp` counts as expired. */
    fun isValidAt(token: String, nowEpochMillis: Long): Boolean {
        val exp = expiresAtEpochMillis(token) ?: return false
        return exp > nowEpochMillis
    }

    fun isTokenDateValid(token: String) : Boolean =
        isValidAt(token, Clock.System.now().toEpochMilliseconds())

    fun getTokenValidRemainingMinutes(token: String) : Long {
        val exp = expiresAtEpochMillis(token) ?: return 0L
        val remainingMillis = exp - Clock.System.now().toEpochMilliseconds()
        return remainingMillis / 1000 / 60
    }
}
