package com.notaskflow.core.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ApiResponse<T>(
    @param:Json(name = "code") val code: Int,
    @param:Json(name = "message") val message: String?,
    @param:Json(name = "data") val data: T?
) {
    fun isSuccess(): Boolean = code == SUCCESS_CODE

    fun getOrThrow(): T {
        if (isSuccess() && data != null) {
            return data
        }
        throw ApiException(code = code, responseMessage = message ?: "接口请求失败")
    }

    fun requireSuccess() {
        if (!isSuccess()) {
            throw ApiException(code = code, responseMessage = message ?: "接口请求失败")
        }
    }

    private companion object {
        const val SUCCESS_CODE = 200
    }
}

@JsonClass(generateAdapter = true)
data class EmptyResponse(
    @param:Json(name = "ok") val ok: Boolean? = null
)

class ApiException(
    val code: Int,
    val responseMessage: String
) : RuntimeException(responseMessage)
