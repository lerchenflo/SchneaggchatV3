package org.lerchenflo.schneaggchatv3mp.testutil

import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import kotlin.io.encoding.Base64
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.TokenPair

/** 2100-01-01T00:00:00Z: far enough in the future that a test token never expires. */
private const val FAR_FUTURE_EPOCH_SECONDS = 4_102_444_800L

/**
 * Unsigned-looking JWT whose subject is [userId]. `JwtUtils` only parses the token (it never
 * checks the signature), so this is enough for `SessionCache` to read the user id from it.
 */
fun testJwt(userId: String, expiresAtEpochSeconds: Long = FAR_FUTURE_EPOCH_SECONDS): String {
    fun encode(text: String) = Base64.UrlSafe.encode(text.encodeToByteArray()).trimEnd('=')

    val header = encode("""{"alg":"HS256","typ":"JWT"}""")
    val payload = encode("""{"sub":"$userId","exp":$expiresAtEpochSeconds}""")
    val signature = encode("test-signature")
    return "$header.$payload.$signature"
}

/**
 * Logs [userId] into the global `SessionCache` with test tokens, so code that calls
 * `SessionCache.requireLoggedIn()` runs. Undo it with `resetGlobalState()`.
 */
fun loginTestUser(userId: String) {
    SessionCache.login(
        TokenPair(
            accessToken = testJwt(userId),
            refreshToken = testJwt(userId),
        )
    )
}
