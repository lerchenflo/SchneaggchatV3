package org.lerchenflo.schneaggchatv3mp.datasource.network.auth

import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds

/** D10. */
class RetryAfterTest {

    @Test
    fun parsesTheServersBody() {
        val error = NetworkingError.TooManyRequests(message = """{"error":"rate_limited","retryAfterSeconds":47}""")
        assertEquals(47.seconds, RetryAfter.parse(error))
    }

    @Test
    fun toleratesWhitespaceAndExtraFields() {
        val error = NetworkingError.TooManyRequests(message = """{ "retryAfterSeconds" : 3 , "error" : "rate_limited" }""")
        assertEquals(3.seconds, RetryAfter.parse(error))
    }

    @Test
    fun nullWhenAbsentZeroOrGarbage() {
        assertNull(RetryAfter.parse(NetworkingError.TooManyRequests(message = null)))
        assertNull(RetryAfter.parse(NetworkingError.TooManyRequests(message = "")))
        assertNull(RetryAfter.parse(NetworkingError.TooManyRequests(message = "<html>Too many</html>")))
        assertNull(RetryAfter.parse(NetworkingError.TooManyRequests(message = """{"retryAfterSeconds":0}""")))
        assertNull(RetryAfter.parse(NetworkingError.TooManyRequests(message = """{"retryAfterSeconds":"soon"}""")))
    }

    @Test
    fun onlyAppliesToTooManyRequests() {
        assertNull(RetryAfter.parse(NetworkingError.ServerError(message = """{"retryAfterSeconds":47}""")))
        assertNull(RetryAfter.parse(NetworkingError.NoInternetConnection))
    }
}
