package com.notaskflow.core.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ApiResponse<T>(
    @Json(name = "code") val code: Int,
    @Json(name = "message") val message: String?,
    @Json(name = "data") val data: T?
) {
    fun isSuccess(): Boolean = code == SUCCESS_CODE

    fun getOrThrow(): T {
        if (isSuccess() && data != null) {
            return data
        }
        throw ApiException(code = code, responseMessage = message ?: "接口请求失败")
    }

    private companion object {
        const val SUCCESS_CODE = 200
    }
}

class ApiException(
    val code: Int,
    val responseMessage: String
) : RuntimeException(responseMessage)
