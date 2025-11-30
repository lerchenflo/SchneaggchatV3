package org.lerchenflo.schneaggchatv3mp.datasource.network.util

interface RequestError
enum class NetworkError : RequestError {
    REQUEST_TIMEOUT,
    UNAUTHORIZED,
    CONFLICT,
    TOO_MANY_REQUESTS,
    NO_INTERNET,
    PAYLOAD_TOO_LARGE,
    SERVER_ERROR,
    SERIALIZATION,
    UNKNOWN;
}


//adre errors: Userererror etc