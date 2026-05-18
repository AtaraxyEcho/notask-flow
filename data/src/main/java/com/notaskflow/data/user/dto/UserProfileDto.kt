package com.notaskflow.data.user.dto

import com.notaskflow.data.common.toAvatarProxyUrl
import com.notaskflow.domain.model.NotificationSettings
import com.notaskflow.domain.model.PasswordChange
import com.notaskflow.domain.model.DeviceTokenRegistration
import com.notaskflow.domain.model.EmailChangeCodeRequest
import com.notaskflow.domain.model.EmailChangeConfirmRequest
import com.notaskflow.domain.model.PushPlatform
import com.notaskflow.domain.model.PushProvider
import com.notaskflow.domain.model.UserDeviceToken
import com.notaskflow.domain.model.UserProfile
import com.notaskflow.domain.model.UserProfileUpdate
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserProfileDto(
    @param:Json(name = "id") val id: Long,
    @param:Json(name = "username") val username: String,
    @param:Json(name = "nickname") val nickname: String?,
    @param:Json(name = "email") val email: String,
    @param:Json(name = "avatarUrl") val avatarUrl: String?
)

@JsonClass(generateAdapter = true)
data class UserProfileUpdateRequestDto(
    @param:Json(name = "nickname") val nickname: String?,
    @param:Json(name = "email") val email: String?,
    @param:Json(name = "avatarUrl") val avatarUrl: String?
)

@JsonClass(generateAdapter = true)
data class PasswordChangeRequestDto(
    @param:Json(name = "oldPassword") val oldPassword: String,
    @param:Json(name = "newPassword") val newPassword: String
)

@JsonClass(generateAdapter = true)
data class EmailChangeCodeRequestDto(
    @param:Json(name = "newEmail") val newEmail: String
)

@JsonClass(generateAdapter = true)
data class EmailChangeConfirmRequestDto(
    @param:Json(name = "newEmail") val newEmail: String,
    @param:Json(name = "code") val code: String
)

@JsonClass(generateAdapter = true)
data class NotificationSettingsDto(
    @param:Json(name = "themeMode") val themeMode: String?,
    @param:Json(name = "personalThemePreset") val personalThemePreset: String?,
    @param:Json(name = "sidebarMode") val sidebarMode: String?,
    @param:Json(name = "taskNoticeEnabled") val taskNoticeEnabled: Boolean?,
    @param:Json(name = "noteNoticeEnabled") val noteNoticeEnabled: Boolean?,
    @param:Json(name = "mentionNoticeEnabled") val mentionNoticeEnabled: Boolean?,
    @param:Json(name = "systemNoticeEnabled") val systemNoticeEnabled: Boolean?,
    @param:Json(name = "emailEnabled") val emailEnabled: Boolean?,
    @param:Json(name = "taskEmailEnabled") val taskEmailEnabled: Boolean?,
    @param:Json(name = "todoEmailEnabled") val todoEmailEnabled: Boolean?,
    @param:Json(name = "mentionEmailEnabled") val mentionEmailEnabled: Boolean?,
    @param:Json(name = "quietEnabled") val quietEnabled: Boolean?,
    @param:Json(name = "quietStartTime") val quietStartTime: String?,
    @param:Json(name = "quietEndTime") val quietEndTime: String?
)

@JsonClass(generateAdapter = true)
data class DeviceTokenRegisterRequestDto(
    @param:Json(name = "platform") val platform: String,
    @param:Json(name = "provider") val provider: String,
    @param:Json(name = "deviceId") val deviceId: String,
    @param:Json(name = "deviceName") val deviceName: String?,
    @param:Json(name = "deviceToken") val deviceToken: String,
    @param:Json(name = "appVersion") val appVersion: String?
)

@JsonClass(generateAdapter = true)
data class UserDeviceTokenDto(
    @param:Json(name = "id") val id: Long?,
    @param:Json(name = "platform") val platform: String?,
    @param:Json(name = "provider") val provider: String?,
    @param:Json(name = "deviceId") val deviceId: String?,
    @param:Json(name = "deviceName") val deviceName: String?,
    @param:Json(name = "appVersion") val appVersion: String?,
    @param:Json(name = "enabled") val enabled: Boolean?,
    @param:Json(name = "lastActiveAt") val lastActiveAt: String?
)

fun UserProfileDto.toDomain(): UserProfile {
    return UserProfile(
        id = id,
        username = username,
        email = email,
        nickname = nickname,
        avatarUrl = avatarUrl.toAvatarProxyUrl(id)
    )
}

fun UserProfileUpdate.toDto(): UserProfileUpdateRequestDto {
    return UserProfileUpdateRequestDto(
        nickname = nickname,
        email = email,
        avatarUrl = avatarUrl
    )
}

fun PasswordChange.toDto(): PasswordChangeRequestDto {
    return PasswordChangeRequestDto(
        oldPassword = oldPassword,
        newPassword = newPassword
    )
}

fun EmailChangeCodeRequest.toDto(): EmailChangeCodeRequestDto {
    return EmailChangeCodeRequestDto(newEmail = newEmail)
}

fun EmailChangeConfirmRequest.toDto(): EmailChangeConfirmRequestDto {
    return EmailChangeConfirmRequestDto(
        newEmail = newEmail,
        code = code
    )
}

fun NotificationSettings.toDto(): NotificationSettingsDto {
    return NotificationSettingsDto(
        themeMode = themeMode,
        personalThemePreset = personalThemePreset,
        sidebarMode = sidebarMode,
        taskNoticeEnabled = taskNoticeEnabled,
        noteNoticeEnabled = noteNoticeEnabled,
        mentionNoticeEnabled = mentionNoticeEnabled,
        systemNoticeEnabled = systemNoticeEnabled,
        emailEnabled = emailEnabled,
        taskEmailEnabled = taskEmailEnabled,
        todoEmailEnabled = todoEmailEnabled,
        mentionEmailEnabled = mentionEmailEnabled,
        quietEnabled = quietEnabled,
        quietStartTime = quietStartTime,
        quietEndTime = quietEndTime
    )
}

fun NotificationSettingsDto.toDomain(): NotificationSettings {
    return NotificationSettings(
        themeMode = themeMode ?: DEFAULT_THEME_MODE,
        personalThemePreset = personalThemePreset ?: DEFAULT_PERSONAL_THEME_PRESET,
        sidebarMode = sidebarMode ?: DEFAULT_SIDEBAR_MODE,
        taskNoticeEnabled = taskNoticeEnabled ?: true,
        noteNoticeEnabled = noteNoticeEnabled ?: true,
        mentionNoticeEnabled = mentionNoticeEnabled ?: true,
        systemNoticeEnabled = systemNoticeEnabled ?: true,
        emailEnabled = emailEnabled ?: false,
        taskEmailEnabled = taskEmailEnabled ?: false,
        todoEmailEnabled = todoEmailEnabled ?: false,
        mentionEmailEnabled = mentionEmailEnabled ?: false,
        quietEnabled = quietEnabled ?: false,
        quietStartTime = quietStartTime,
        quietEndTime = quietEndTime
    )
}

fun DeviceTokenRegistration.toDto(): DeviceTokenRegisterRequestDto {
    return DeviceTokenRegisterRequestDto(
        platform = platform.name,
        provider = provider.name,
        deviceId = deviceId,
        deviceName = deviceName,
        deviceToken = deviceToken,
        appVersion = appVersion
    )
}

fun UserDeviceTokenDto.toDomain(): UserDeviceToken {
    return UserDeviceToken(
        id = id ?: 0,
        platform = parsePushPlatform(platform),
        provider = parsePushProvider(provider),
        deviceId = deviceId.orEmpty(),
        deviceName = deviceName,
        appVersion = appVersion,
        enabled = enabled == true,
        lastActiveAt = lastActiveAt
    )
}

private fun parsePushPlatform(value: String?): PushPlatform {
    return runCatching {
        PushPlatform.valueOf(value.orEmpty())
    }.getOrDefault(PushPlatform.ANDROID)
}

private fun parsePushProvider(value: String?): PushProvider {
    return runCatching {
        PushProvider.valueOf(value.orEmpty())
    }.getOrDefault(PushProvider.FCM)
}

private const val DEFAULT_THEME_MODE = "light"
private const val DEFAULT_PERSONAL_THEME_PRESET = "sunrise"
private const val DEFAULT_SIDEBAR_MODE = "auto"
