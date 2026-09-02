package com.notaskflow.domain.user

import com.notaskflow.domain.model.NotificationSettings
import com.notaskflow.domain.model.PasswordChange
import com.notaskflow.domain.model.DeviceTokenRegistration
import com.notaskflow.domain.model.EmailChangeCodeRequest
import com.notaskflow.domain.model.EmailChangeConfirmRequest
import com.notaskflow.domain.model.ManagedFileUpload
import com.notaskflow.domain.model.PushPlatform
import com.notaskflow.domain.model.UserDeviceToken
import com.notaskflow.domain.model.UserProfile
import com.notaskflow.domain.model.UserProfileUpdate

interface UserRepository {
    suspend fun profile(): Result<UserProfile>

    suspend fun updateProfile(request: UserProfileUpdate): Result<UserProfile>

    suspend fun uploadAvatar(upload: ManagedFileUpload): Result<UserProfile>

    suspend fun notificationSettings(): Result<NotificationSettings>

    suspend fun updateNotificationSettings(request: NotificationSettings): Result<NotificationSettings>

    suspend fun changePassword(request: PasswordChange): Result<Unit>

    suspend fun sendEmailChangeCode(request: EmailChangeCodeRequest): Result<Unit>

    suspend fun changeEmail(request: EmailChangeConfirmRequest): Result<UserProfile>

    suspend fun registerDeviceToken(request: DeviceTokenRegistration): Result<UserDeviceToken>

    suspend fun unbindDeviceToken(platform: PushPlatform, deviceId: String): Result<Unit>
}
