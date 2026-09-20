package com.dlab.sirinium.core.model

sealed interface Resource<out T> {
    data class Success<T>(val data: T) : Resource<T>
    data class Error(val message: String, val cause: Throwable? = null) : Resource<Nothing>
    data object Loading : Resource<Nothing>

    val dataOrNull: T?
        get() = (this as? Success)?.data
}
