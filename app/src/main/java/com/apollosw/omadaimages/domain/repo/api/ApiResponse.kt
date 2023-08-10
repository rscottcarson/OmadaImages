package com.apollosw.omadaimages.domain.repo.api

sealed class ApiResponse<T> {
    class Error<T> : ApiResponse<T>()
    class Success<T>(val data : T) : ApiResponse<T>()
}