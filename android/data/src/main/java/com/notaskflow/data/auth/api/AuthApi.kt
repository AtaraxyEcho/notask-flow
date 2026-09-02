package com.notaskflow.data.auth.api

import com.notaskflow.core.network.ApiResponse
import com.notaskflow.core.network.EmptyResponse
import com.notaskflow.data.auth.dto.ForgotPasswordRequestDto
import com.notaskflow.data.auth.dto.LoginRequestDto
import com.notaskflow.data.auth.dto.LoginResponseDto
import com.notaskflow.data.auth.dto.PasswordResetVerifyResponseDto
import com.notaskflow.data.auth.dto.RegisterRequestDto
import com.notaskflow.data.auth.dto.ResetPasswordRequestDto
import com.notaskflow.data.auth.dto.SendRegisterEmailCodeRequestDto
import com.notaskflow.data.auth.dto.VerifyResetCodeRequestDto
import com.notaskflow.data.user.dto.UserProfileDto
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("/api/v1/auth/login")
    suspend fun login(@Body request: LoginRequestDto): ApiResponse<LoginResponseDto>

    @POST("/api/v1/auth/register/send-email-code")
    suspend fun sendRegisterEmailCode(@Body request: SendRegisterEmailCodeRequestDto): ApiResponse<EmptyResponse>

    @POST("/api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequestDto): ApiResponse<UserProfileDto>

    @POST("/api/v1/auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequestDto): ApiResponse<EmptyResponse>

    @POST("/api/v1/auth/verify-reset-code")
    suspend fun verifyResetCode(@Body request: VerifyResetCodeRequestDto): ApiResponse<PasswordResetVerifyResponseDto>

    @POST("/api/v1/auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequestDto): ApiResponse<EmptyResponse>

    @POST("/api/v1/auth/refresh")
    suspend fun refresh(): ApiResponse<LoginResponseDto>

    @POST("/api/v1/auth/logout")
    suspend fun logout(): ApiResponse<EmptyResponse>
}
