package org.lerchenflo.schneaggchatv3mp.app.domain

sealed interface DataResult<out D, out E: DataError> {
    data class Success<out D>(val data: D): DataResult<D, Nothing>
    data class Error<out E: DataError>(val error: E): DataResult<Nothing, E>
}

inline fun <T, E: DataError, R> DataResult<T, E>.map(map: (T) -> R): DataResult<R, E> {
    return when(this) {
        is DataResult.Error -> DataResult.Error(error)
        is DataResult.Success -> DataResult.Success(map(data))
    }
}

fun <T, E: DataError> DataResult<T, E>.asEmptyDataResult(): EmptyDataResult<E> {
    return map {  }
}

inline fun <T, E: DataError> DataResult<T, E>.onSuccess(action: (T) -> Unit): DataResult<T, E> {
    return when(this) {
        is DataResult.Error -> this
        is DataResult.Success -> {
            action(data)
            this
        }
    }
}
inline fun <T, E: DataError> DataResult<T, E>.onError(action: (E) -> Unit): DataResult<T, E> {
    return when(this) {
        is DataResult.Error -> {
            action(error)
            this
        }
        is DataResult.Success -> this
    }
}

val DataResult<*, *>.isSuccess: Boolean
    get() = this is DataResult.Success

fun <T> DataResult<T, *>.getOrNull(): T? = (this as? DataResult.Success)?.data

typealias EmptyDataResult<E> = DataResult<Unit, E>
