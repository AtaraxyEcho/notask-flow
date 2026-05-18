package com.notaskflow.data.auth.dto

import com.notaskflow.domain.model.PasswordResetToken
import com.notaskflow.domain.model.RegisterAccount
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RegisterRequestDto(
    @param:Json(name = "username") val username: String,
    @param:Json(name = "nickname") val nickname: String?,
    @param:Json(name = "email") val email: String,
    @param:Json(name = "password") val password: String,
    @param:Json(name = "emailCode") val emailCode: String?,
    @param:Json(name = "teamMode") val teamMode: String,
    @param:Json(name = "teamName") val teamName: String?,
    @param:Json(name = "supervisorAccount") val supervisorAccount: String?,
    @param:Json(name = "teamApplyRemark") val teamApplyRemark: String?,
    @param:Json(name = "inviteCode") val inviteCode: String?
)

@JsonClass(generateAdapter = true)
data class SendRegisterEmailCodeRequestDto(
    @param:Json(name = "email") val email: String
)

@JsonClass(generateAdapter = true)
data class ForgotPasswordRequestDto(
    @param:Json(name = "email") val email: String
)

@JsonClass(generateAdapter = true)
data class VerifyResetCodeRequestDto(
    @param:Json(name = "email") val email: String,
    @param:Json(name = "code") val code: String
)

@JsonClass(generateAdapter = true)
data class PasswordResetVerifyResponseDto(
    @param:Json(name = "resetToken") val resetToken: String,
    @param:Json(name = "expireSeconds") val expireSeconds: Long?
)

@JsonClass(generateAdapter = true)
data class ResetPasswordRequestDto(
    @param:Json(name = "resetToken") val resetToken: String,
    @param:Json(name = "newPassword") val newPassword: String,
    @param:Json(name = "confirmPassword") val confirmPassword: String
)

fun RegisterAccount.toDto(): RegisterRequestDto {
    return RegisterRequestDto(
        username = username,
        nickname = nickname,
        email = email,
        password = password,
        emailCode = emailCode,
        teamMode = teamMode.name,
        teamName = teamName,
        supervisorAccount = supervisorAccount,
        teamApplyRemark = teamApplyRemark,
        inviteCode = inviteCode
    )
}

fun PasswordResetVerifyResponseDto.toDomain(): PasswordResetToken {
    return PasswordResetToken(
        resetToken = resetToken,
        expireSeconds = expireSeconds ?: 0L
    )
}
