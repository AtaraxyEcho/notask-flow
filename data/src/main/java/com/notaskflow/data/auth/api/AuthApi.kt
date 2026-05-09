package com.notaskflow.data.auth.api

import com.notaskflow.core.network.ApiResponse
import com.notaskflow.data.auth.dto.LoginRequestDto
import com.notaskflow.data.auth.dto.LoginResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("/api/v1/auth/login")
    suspend fun login(@Body request: LoginRequestDto): ApiResponse<LoginResponseDto>

    @POST("/api/v1/auth/logout")
    suspend fun logout(): ApiResponse<Unit>
}
