package com.notaskflow.data.user.api

import com.notaskflow.core.network.ApiResponse
import com.notaskflow.core.network.EmptyResponse
import com.notaskflow.data.user.dto.DeviceTokenRegisterRequestDto
import com.notaskflow.data.user.dto.EmailChangeCodeRequestDto
import com.notaskflow.data.user.dto.EmailChangeConfirmRequestDto
import com.notaskflow.data.user.dto.NotificationSettingsDto
import com.notaskflow.data.user.dto.PasswordChangeRequestDto
import com.notaskflow.data.user.dto.UserDeviceTokenDto
import com.notaskflow.data.user.dto.UserProfileDto
import com.notaskflow.data.user.dto.UserProfileUpdateRequestDto
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface UserApi {
    @GET("/api/v1/user/profile")
    suspend fun profile(): ApiResponse<UserProfileDto>

    @PUT("/api/v1/user/profile")
    suspend fun updateProfile(@Body request: UserProfileUpdateRequestDto): ApiResponse<UserProfileDto>

    @Multipart
    @POST("/api/v1/user/avatar")
    suspend fun uploadAvatar(@Part file: MultipartBody.Part): ApiResponse<UserProfileDto>

    @GET("/api/v1/user/notification-settings")
    suspend fun notificationSettings(): ApiResponse<NotificationSettingsDto>

    @PUT("/api/v1/user/notification-settings")
    suspend fun updateNotificationSettings(@Body request: NotificationSettingsDto): ApiResponse<NotificationSettingsDto>

    @PUT("/api/v1/user/password")
    suspend fun changePassword(@Body request: PasswordChangeRequestDto): ApiResponse<EmptyResponse>

    @POST("/api/v1/user/email/code")
    suspend fun sendEmailChangeCode(@Body request: EmailChangeCodeRequestDto): ApiResponse<EmptyResponse>

    @PUT("/api/v1/user/email")
    suspend fun changeEmail(@Body request: EmailChangeConfirmRequestDto): ApiResponse<UserProfileDto>

    @POST("/api/v1/user/device-tokens")
    suspend fun registerDeviceToken(@Body request: DeviceTokenRegisterRequestDto): ApiResponse<UserDeviceTokenDto>

    @DELETE("/api/v1/user/device-tokens")
    suspend fun unbindDeviceToken(
        @Query("platform") platform: String,
        @Query("deviceId") deviceId: String
    ): ApiResponse<EmptyResponse>
}
