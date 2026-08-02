package com.zenox.arrowmaze.core.common

/**
 * Result type for operations that can fail. Preferred over Kotlin's `Result`
 * because it carries a typed error and is exception-free on the happy path.
 */
sealed interface Result<out T> {
    data class Success<T>(val data: T) : Result<T>
    data class Failure(val error: AppError) : Result<Nothing>
    data object Loading : Result<Nothing>
}

inline fun <T> resultOf(block: () -> T): Result<T> = try {
    Result.Success(block())
} catch (e: Throwable) {
    Result.Failure(AppError.from(e))
}

inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> = when (this) {
    is Result.Success -> Result.Success(transform(data))
    is Result.Failure -> this as Result<R>
    Result.Loading -> this as Result<R>
}

inline fun <T> Result<T>.onSuccess(block: (T) -> Unit): Result<T> {
    if (this is Result.Success) block(data)
    return this
}

inline fun <T> Result<T>.onFailure(block: (AppError) -> Unit): Result<T> {
    if (this is Result.Failure) block(error)
    return this
}

fun <T> Result<T>.getOrNull(): T? = (this as? Result.Success)?.data

fun <T> Result<T>.getOrThrow(): T = when (this) {
    is Result.Success -> data
    is Result.Failure -> throw error.asException()
    Result.Loading -> throw IllegalStateException("Result is Loading")
}

/**
 * Categorised application errors. Pure data — does not extend Throwable so
 * that subclasses stay clean data classes. Use [asException] when an actual
 * throwable is required (e.g. for `getOrThrow`).
 */
sealed class AppError(open val message: String) {
    data class Network(override val message: String, val cause: Throwable? = null) : AppError(message)
    data class Auth(override val message: String, val code: String? = null) : AppError(message)
    data class Firestore(override val message: String, val code: String? = null) : AppError(message)
    data class Database(override val message: String) : AppError(message)
    data class Billing(override val message: String, val code: Int? = null) : AppError(message)
    data class Ads(override val message: String) : AppError(message)
    data class Validation(override val message: String, val field: String? = null) : AppError(message)
    data class NotFound(override val message: String) : AppError(message)
    data class Unknown(override val message: String, val cause: Throwable? = null) : AppError(message)

    fun asException(): RuntimeException = AppErrorException(this)

    private class AppErrorException(val error: AppError) : RuntimeException(error.message)

    companion object {
        fun from(throwable: Throwable): AppError {
            val msg = throwable.message ?: "Unknown error"
            return when (throwable) {
                is java.net.UnknownHostException -> Network("No internet connection", throwable)
                is java.net.SocketTimeoutException -> Network("Request timed out", throwable)
                is kotlinx.coroutines.TimeoutCancellationException -> Network("Operation timed out", throwable)
                else -> Unknown(msg, throwable)
            }
        }
    }
}
