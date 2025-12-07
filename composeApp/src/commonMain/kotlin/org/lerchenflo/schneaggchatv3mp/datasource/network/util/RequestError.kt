package org.lerchenflo.schneaggchatv3mp.datasource.network.util

interface RequestError {
    val errorCode: Int
    val message: String?
}

sealed class NetworkError : RequestError {
    abstract override val errorCode: Int
    abstract override val message: String?

    data class RequestTimeout(
        override val errorCode: Int = 408,
        override val message: String? = null
    ) : NetworkError()

    data class Unauthorized(
        override val errorCode: Int = 401,
        override val message: String? = null
    ) : NetworkError()

    data class Conflict(
        override val errorCode: Int = 409,
        override val message: String? = null
    ) : NetworkError()

    data class TooManyRequests(
        override val errorCode: Int = 429,
        override val message: String? = null
    ) : NetworkError()

    data class NoInternet(
        override val errorCode: Int = 0,
        override val message: String? = null
    ) : NetworkError()

    data class PayloadTooLarge(
        override val errorCode: Int = 413,
        override val message: String? = null
    ) : NetworkError()

    data class ServerError(
        override val errorCode: Int = 500,
        override val message: String? = null
    ) : NetworkError()

    data class Serialization(
        override val errorCode: Int = -1,
        override val message: String? = null
    ) : NetworkError()

    data class Unknown(
        override val errorCode: Int = -2,
        override val message: String? = null
    ) : NetworkError()
}


fun errorCodeToMessage(code: Int?): String = when (code) {
    401 -> "Access denied (invalid credentials)"
    408 -> "Request timed out"
    409 -> "Conflict (resource already exists or invalid state)"
    413 -> "Payload too large"
    429 -> "Too many requests (rate limit exceeded)"
    500 -> "Server error (internal server issue)"
    0   -> "No internet connection"
    -1  -> "Serialization error"
    -2  -> "Unknown error"
    else -> ""
}
