package org.lerchenflo.schneaggchatv3mp.datasource.network.util

interface RequestError{
    val message:String?
}
sealed class NetworkError : RequestError {
    abstract override val message: String?

    data class RequestTimeout(override val message: String? = null) : NetworkError()
    data class Unauthorized(override val message: String? = null) : NetworkError()
    data class Conflict(override val message: String? = null) : NetworkError()
    data class TooManyRequests(override val message: String? = null) : NetworkError()
    data class NoInternet(override val message: String? = null) : NetworkError()
    data class PayloadTooLarge(override val message: String? = null) : NetworkError()
    data class ServerError(override val message: String? = null) : NetworkError()
    data class Serialization(override val message: String? = null) : NetworkError()
    data class Unknown(override val message: String? = null) : NetworkError()
}