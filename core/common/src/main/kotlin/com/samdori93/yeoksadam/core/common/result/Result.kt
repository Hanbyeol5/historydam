package com.samdori93.yeoksadam.core.common.result

import com.samdori93.yeoksadam.core.common.error.AppError

/**
 * 단발성 작업 결과를 표현하는 sealed 타입.
 * Repository 의 단발 작업은 [Result] 를, 스트림/목록은 Flow 를 반환한다(CLAUDE.md §4).
 */
sealed interface Result<out T> {
    data class Success<T>(val data: T) : Result<T>
    data class Failure(val error: AppError) : Result<Nothing>
}

inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> = when (this) {
    is Result.Success -> Result.Success(transform(data))
    is Result.Failure -> this
}

inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) action(data)
    return this
}

inline fun <T> Result<T>.onFailure(action: (AppError) -> Unit): Result<T> {
    if (this is Result.Failure) action(error)
    return this
}

fun <T> Result<T>.getOrNull(): T? = (this as? Result.Success)?.data
