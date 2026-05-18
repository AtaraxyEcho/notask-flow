package com.notaskflow.data.auth.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoginRequestDto(
    @param:Json(name = "account") val account: String,
    @param:Json(name = "password") val password: String,
    @param:Json(name = "clientType") val clientType: String,
    @param:Json(name = "deviceId") val deviceId: String,
    @param:Json(name = "deviceName") val deviceName: String,
    @param:Json(name = "appVersion") val appVersion: String
)
