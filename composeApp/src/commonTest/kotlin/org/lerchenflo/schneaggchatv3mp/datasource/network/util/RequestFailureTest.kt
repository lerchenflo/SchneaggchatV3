package org.lerchenflo.schneaggchatv3mp.datasource.network.util

import assertk.assertThat
import assertk.assertions.doesNotContain
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import assertk.assertions.single
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import org.lerchenflo.schneaggchatv3mp.app.domain.DataError
import org.lerchenflo.schneaggchatv3mp.app.domain.Logger
import org.lerchenflo.schneaggchatv3mp.fakes.FakeLogger

private const val SEARCH_REQUEST = "GET /users/availableusers?searchterm=secret"

/** The log table keeps 50 rows and must neither fill up with connection noise nor hold user input. */
class RequestFailureTest {

    private val logger = FakeLogger()

    @Test
    fun `a connection failure stays out of the log table`() = runTest {
        logger.logRequestFailure(SEARCH_REQUEST, DataError.NoInternetConnection, cause = IOException("gone"))

        assertThat(logger.logs).isEmpty()
    }

    @Test
    fun `a server error and a rate limit stay out of the log table`() = runTest {
        logger.logRequestFailure(SEARCH_REQUEST, DataError.ServerError(errorCode = 502))
        logger.logRequestFailure(SEARCH_REQUEST, DataError.TooManyRequests())

        assertThat(logger.logs).isEmpty()
    }

    @Test
    fun `a rejected request is logged without its query string`() = runTest {
        logger.logRequestFailure(SEARCH_REQUEST, DataError.BadRequest(debugInfo = "Search term too short"))

        assertThat(logger.logs).single().transform { it.message }
            .isEqualTo("GET /users/availableusers failed (400): Search term too short")
    }

    @Test
    fun `an exception text loses the query string of its url`() = runTest {
        val cause = IllegalStateException("In response from `https://server/users?searchterm=secret` it broke")

        logger.logRequestFailure(SEARCH_REQUEST, cause.toDataError(), cause)

        assertThat(logger.logs).single().transform { it.message }.doesNotContain("secret")
    }

    @Test
    fun `a failing log write does not fail the request`() = runTest {
        val brokenLogger = object : Logger by FakeLogger() {
            override suspend fun logWarning(message: String) = error("database is closed")
        }

        brokenLogger.logRequestFailure(SEARCH_REQUEST, DataError.BadRequest())
    }

    @Test
    fun `a dropped connection is no internet`() {
        assertThat(IOException("gone").toDataError()).isEqualTo(DataError.NoInternetConnection)
    }

    @Test
    fun `an exception text never reaches the popup`() {
        val serialization = SerializationException("Expected a string").toDataError()
        val unknown = IllegalStateException("broken").toDataError()

        assertThat(serialization).isInstanceOf<DataError.SerializationError>()
        assertThat(serialization.message).isNull()
        assertThat(serialization.debugInfo).isEqualTo("Expected a string")
        assertThat(unknown).isInstanceOf<DataError.Unknown>()
        assertThat(unknown.message).isNull()
        assertThat(unknown.debugInfo).isEqualTo("broken")
    }
}
