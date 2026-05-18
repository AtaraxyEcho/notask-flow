package com.notaskflow.data.user

import com.notaskflow.data.user.api.UserApi
import com.notaskflow.data.user.dto.toDto
import com.notaskflow.data.user.dto.toDomain
import com.notaskflow.domain.model.DeviceTokenRegistration
import com.notaskflow.domain.model.EmailChangeCodeRequest
import com.notaskflow.domain.model.EmailChangeConfirmRequest
import com.notaskflow.domain.model.ManagedFileUpload
import com.notaskflow.domain.model.NotificationSettings
import com.notaskflow.domain.model.PasswordChange
import com.notaskflow.domain.model.PushPlatform
import com.notaskflow.domain.model.UserDeviceToken
import com.notaskflow.domain.model.UserProfile
import com.notaskflow.domain.model.UserProfileUpdate
import com.notaskflow.domain.user.UserRepository
import javax.inject.Inject
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull

class UserRepositoryImpl @Inject constructor(
    private val userApi: UserApi
) : UserRepository {
    override suspend fun profile(): Result<UserProfile> {
        return runCatching {
            userApi.profile().getOrThrow().toDomain()
        }
    }

    override suspend fun updateProfile(request: UserProfileUpdate): Result<UserProfile> {
        return runCatching {
            userApi.updateProfile(request.toDto()).getOrThrow().toDomain()
        }
    }

    override suspend fun uploadAvatar(upload: ManagedFileUpload): Result<UserProfile> {
        return runCatching {
            val body = upload.bytes.toRequestBody(upload.mimeType.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData(
                name = "file",
                filename = upload.fileName,
                body = body
            )
            userApi.uploadAvatar(part).getOrThrow().toDomain()
        }
    }

    override suspend fun notificationSettings(): Result<NotificationSettings> {
        return runCatching {
            userApi.notificationSettings().getOrThrow().toDomain()
        }
    }

    override suspend fun updateNotificationSettings(request: NotificationSettings): Result<NotificationSettings> {
        return runCatching {
            userApi.updateNotificationSettings(request.toDto()).getOrThrow().toDomain()
        }
    }

    override suspend fun changePassword(request: PasswordChange): Result<Unit> {
        return runCatching {
            userApi.changePassword(request.toDto()).requireSuccess()
        }
    }

    override suspend fun sendEmailChangeCode(request: EmailChangeCodeRequest): Result<Unit> {
        return runCatching {
            userApi.sendEmailChangeCode(request.toDto()).requireSuccess()
        }
    }

    override suspend fun changeEmail(request: EmailChangeConfirmRequest): Result<UserProfile> {
        return runCatching {
            userApi.changeEmail(request.toDto()).getOrThrow().toDomain()
        }
    }

    override suspend fun registerDeviceToken(request: DeviceTokenRegistration): Result<UserDeviceToken> {
        return runCatching {
            userApi.registerDeviceToken(request.toDto()).getOrThrow().toDomain()
        }
    }

    override suspend fun unbindDeviceToken(platform: PushPlatform, deviceId: String): Result<Unit> {
        return runCatching {
            userApi.unbindDeviceToken(platform.name, deviceId).requireSuccess()
        }
    }
}
