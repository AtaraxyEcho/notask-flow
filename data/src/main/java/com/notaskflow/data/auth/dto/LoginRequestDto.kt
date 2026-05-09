package com.notaskflow.data.auth.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoginRequestDto(
    @Json(name = "account") val account: String,
    @Json(name = "password") val password: String
)
