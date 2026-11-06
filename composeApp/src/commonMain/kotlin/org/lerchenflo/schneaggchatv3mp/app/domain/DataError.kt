package org.lerchenflo.schneaggchatv3mp.app.domain

import kotlin.time.Duration

/**
 * Why a data call failed. Shared by every layer, from the remote data sources up to the
 * ViewModels, which map it to UI.
 */
sealed interface DataError {
    /** The HTTP status of a reply. Failures without a reply use 408, 0 or a negative code. */
    val errorCode: Int

    /** What a user should read, shown in the error popup. Null when there is nothing readable. */
    val message: String?

    /** For logs only, never for users: the unchanged response body or the exception text. */
    val debugInfo: String?

    data class BadRequest(
        override val message: String? = null,
        override val debugInfo: String? = null,
    ) : DataError {
        override val errorCode: Int = 400
    }

    data class Unauthorized(
        override val message: String? = null,
        override val debugInfo: String? = null,
    ) : DataError {
        override val errorCode: Int = 401
    }

    data class Forbidden(
        override val message: String? = null,
        override val debugInfo: String? = null,
    ) : DataError {
        override val errorCode: Int = 403
    }

    data class NotFound(
        override val message: String? = null,
        override val debugInfo: String? = null,
    ) : DataError {
        override val errorCode: Int = 404
    }

    data class Conflict(
        override val message: String? = null,
        override val debugInfo: String? = null,
    ) : DataError {
        override val errorCode: Int = 409
    }

    data class PayloadTooLarge(
        override val message: String? = null,
        override val debugInfo: String? = null,
    ) : DataError {
        override val errorCode: Int = 413
    }

    data class TooManyRequests(
        override val message: String? = null,
        override val debugInfo: String? = null,
        /** How long the server wants the client to wait, null when it didn't say. */
        val retryAfter: Duration? = null,
    ) : DataError {
        override val errorCode: Int = 429
    }

    data class NetworkTimeout(
        override val message: String? = null,
        override val debugInfo: String? = null,
    ) : DataError {
        override val errorCode: Int = 408
    }

    data class ServerError(
        override val message: String? = null,
        override val debugInfo: String? = null,
        override val errorCode: Int = 500,
    ) : DataError

    data object NoInternetConnection : DataError {
        override val errorCode: Int = 0
        override val message: String? = null
        override val debugInfo: String? = null
    }

    data class SerializationError(
        override val message: String? = null,
        override val debugInfo: String? = null,
    ) : DataError {
        override val errorCode: Int = -1
    }

    data class Unknown(
        override val message: String? = null,
        override val debugInfo: String? = null,
        override val errorCode: Int = -2,
    ) : DataError
}

fun DataError.isNetworkerror() : Boolean {
    return this is DataError.NetworkTimeout || this is DataError.NoInternetConnection
}

/**
 * Hits every request alike and goes away by itself (no connection, rate limit, session, server
 * down), so it says nothing about the request that got it. It can repeat every few seconds.
 */
fun DataError.isTransient(): Boolean =
    isNetworkerror() ||
        this is DataError.TooManyRequests ||
        this is DataError.Unauthorized ||
        this is DataError.ServerError
