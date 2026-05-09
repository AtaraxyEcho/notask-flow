package com.notaskflow.data.auth.dto

import com.notaskflow.core.model.AuthToken
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoginResponseDto(
    @Json(name = "userId") val userId: Long,
    @Json(name = "tokenName") val tokenName: String,
    @Json(name = "tokenValue") val tokenValue: String,
    @Json(name = "expireTime") val expireTime: Long
)

fun LoginResponseDto.toModel(): AuthToken {
    return AuthToken(
        userId = userId,
        tokenName = tokenName,
        tokenValue = tokenValue,
        expireTime = expireTime
    )
}
