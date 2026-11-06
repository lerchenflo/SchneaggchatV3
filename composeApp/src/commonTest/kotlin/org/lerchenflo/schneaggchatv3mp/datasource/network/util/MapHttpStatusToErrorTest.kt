package org.lerchenflo.schneaggchatv3mp.datasource.network.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds
import org.lerchenflo.schneaggchatv3mp.app.domain.DataError

/** What ends up in the popup ([DataError.message]) and what only in the logs ([DataError.debugInfo]). */
class MapHttpStatusToErrorTest {

    @Test
    fun `a plain text reason is the readable message`() {
        val error = mapHttpStatusToError(400, "Event is full")

        assertThat(error).isInstanceOf<DataError.BadRequest>()
        assertThat(error.message).isEqualTo("Event is full")
        assertThat(error.debugInfo).isEqualTo("Event is full")
    }

    @Test
    fun `validation errors become one readable line each`() {
        val body = """{"errors":["Name is too short","Invalid value"]}"""

        val error = mapHttpStatusToError(400, body)

        assertThat(error.message).isEqualTo("Name is too short\nInvalid value")
        assertThat(error.debugInfo).isEqualTo(body)
    }

    @Test
    fun `a rate limit reply shows no JSON and carries the wait time`() {
        val body = """{"error":"rate_limited","retryAfterSeconds":30}"""

        val error = mapHttpStatusToError(429, body) as DataError.TooManyRequests

        assertThat(error.message).isNull()
        assertThat(error.debugInfo).isEqualTo(body)
        assertThat(error.retryAfter).isEqualTo(30.seconds)
    }

    @Test
    fun `a rate limit reply without a usable wait time has none`() {
        val error = mapHttpStatusToError(429, """{"retryAfterSeconds":0}""") as DataError.TooManyRequests

        assertThat(error.retryAfter).isNull()
    }

    @Test
    fun `an HTML page from a proxy is not shown to the user`() {
        val body = "  <html><body>502 Bad Gateway</body></html>"

        val error = mapHttpStatusToError(502, body)

        assertThat(error.message).isNull()
        assertThat(error.debugInfo).isEqualTo(body)
    }

    @Test
    fun `other JSON is not shown to the user`() {
        val error = mapHttpStatusToError(500, """{"status":500,"error":"Internal Server Error"}""")

        assertThat(error.message).isNull()
    }

    @Test
    fun `a server error keeps its real status`() {
        val error = mapHttpStatusToError(503, "")

        assertThat(error).isInstanceOf<DataError.ServerError>()
        assertThat(error.errorCode).isEqualTo(503)
    }

    @Test
    fun `an unmapped status keeps its real status`() {
        val error = mapHttpStatusToError(422, "Nope")

        assertThat(error).isInstanceOf<DataError.Unknown>()
        assertThat(error.errorCode).isEqualTo(422)
    }

    @Test
    fun `an empty body gives no message`() {
        assertThat(mapHttpStatusToError(404, "").message).isNull()
        assertThat(mapHttpStatusToError(404, null).message).isNull()
    }
}
