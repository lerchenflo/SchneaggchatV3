package org.lerchenflo.schneaggchatv3mp.datasource.network.util

sealed interface NetworkResult<out D, out E: NetworkingError> {
    data class Success<out D>(val data: D): NetworkResult<D, Nothing>
    data class Error<out E: org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError>(val error: E): NetworkResult<Nothing, E>
}

inline fun <T, E: NetworkingError, R> NetworkResult<T, E>.map(map: (T) -> R): NetworkResult<R, E> {
    return when(this) {
        is NetworkResult.Error -> NetworkResult.Error(error)
        is NetworkResult.Success -> NetworkResult.Success(map(data))
    }
}

fun <T, E: NetworkingError> NetworkResult<T, E>.asEmptyDataResult(): EmptyResult<E> {
    return map {  }
}

inline fun <T, E: NetworkingError> NetworkResult<T, E>.onSuccess(action: (T) -> Unit): NetworkResult<T, E> {
    return when(this) {
        is NetworkResult.Error -> this
        is NetworkResult.Success -> {
            action(data)
            this
        }
    }
}
inline fun <T, E: NetworkingError> NetworkResult<T, E>.onError(action: (E) -> Unit): NetworkResult<T, E> {
    return when(this) {
        is NetworkResult.Error -> {
            action(error)
            this
        }
        is NetworkResult.Success -> this
    }
}

typealias EmptyResult<E> = NetworkResult<Unit, E>


