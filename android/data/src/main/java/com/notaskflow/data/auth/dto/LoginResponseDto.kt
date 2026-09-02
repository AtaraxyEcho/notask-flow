package com.notaskflow.data.auth.dto

import com.notaskflow.domain.model.AuthToken
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoginResponseDto(
    @param:Json(name = "userId") val userId: Long,
    @param:Json(name = "tokenName") val tokenName: String,
    @param:Json(name = "tokenValue") val tokenValue: String,
    @param:Json(name = "expireTime") val expireTime: Long
)

fun LoginResponseDto.toModel(): AuthToken {
    return AuthToken(
        userId = userId,
        tokenName = tokenName,
        tokenValue = tokenValue,
        expireTime = expireTime
    )
}
