// file: com/example/network/Result.kt
package org.lerchenflo.schneaggchatv3mp.network.util

sealed class NetworkResult<out T, out E> {
    data class Success<T>(val data: T, val body: String) : NetworkResult<T, Nothing>()
    data class Error<E>(val error: E) : NetworkResult<Nothing, E>()
}

fun <T, E> NetworkResult<T, E>.onSuccess(block: (T) -> Unit): NetworkResult<T, E> {
    if (this is NetworkResult.Success) {
        block(this.data)
    }
    return this
}

fun <T, E> NetworkResult<T, E>.onError(block: (E) -> Unit): NetworkResult<T, E> {
    if (this is NetworkResult.Error) {
        block(this.error)
    }
    return this
}

fun <T, E>  NetworkResult<T, E>.onSuccessWithBody(block: (T, String) -> Unit):  NetworkResult<T, E> {
    if (this is  NetworkResult.Success) {
        block(this.data, this.body)
    }
    return this
}



// 2. Define NetworkError (example - adjust as needed)
enum class ResponseReason {
    NO_INTERNET,
    TIMEOUT,
    notfound,
    exists,
    email_exists,
    forbidden,
    nomember,
    same,
    wrong,
    feature_disabled,
    too_big,
    account_temp_locked,
    invalid_birthdate,
    unknown_error,
    none
}

inline fun <reified T : Enum<T>> String.toEnumOrNull(ignoreCase: Boolean = true): T? {
    return enumValues<T>().firstOrNull { it.name.equals(this, ignoreCase) }
}
