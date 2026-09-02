package com.notaskflow.domain.model

data class LoginCredential(
    val account: String,
    val password: String
)

data class AuthToken(
    val userId: Long,
    val tokenName: String,
    val tokenValue: String,
    val expireTime: Long
)

enum class RegisterTeamMode {
    PERSONAL_ONLY,
    CREATE_TEAM,
    APPLY_SUPERVISOR,
    JOIN_INVITE_CODE
}

data class RegisterAccount(
    val username: String,
    val nickname: String?,
    val email: String,
    val password: String,
    val emailCode: String?,
    val teamMode: RegisterTeamMode = RegisterTeamMode.PERSONAL_ONLY,
    val teamName: String? = null,
    val supervisorAccount: String? = null,
    val teamApplyRemark: String? = null,
    val inviteCode: String? = null
)

data class PasswordResetToken(
    val resetToken: String,
    val expireSeconds: Long
)

data class UserProfile(
    val id: Long,
    val username: String,
    val email: String,
    val nickname: String?,
    val avatarUrl: String?
)

data class UserProfileUpdate(
    val nickname: String?,
    val email: String?,
    val avatarUrl: String?
)

data class PasswordChange(
    val oldPassword: String,
    val newPassword: String
)

data class EmailChangeCodeRequest(
    val newEmail: String
)

data class EmailChangeConfirmRequest(
    val newEmail: String,
    val code: String
)

data class NotificationSettings(
    val themeMode: String,
    val personalThemePreset: String,
    val sidebarMode: String,
    val taskNoticeEnabled: Boolean,
    val noteNoticeEnabled: Boolean,
    val mentionNoticeEnabled: Boolean,
    val systemNoticeEnabled: Boolean,
    val emailEnabled: Boolean,
    val taskEmailEnabled: Boolean,
    val todoEmailEnabled: Boolean,
    val mentionEmailEnabled: Boolean,
    val quietEnabled: Boolean,
    val quietStartTime: String?,
    val quietEndTime: String?
)

enum class PushPlatform {
    ANDROID,
    IOS
}

enum class PushProvider {
    FCM,
    APNS
}

data class DeviceTokenRegistration(
    val platform: PushPlatform,
    val provider: PushProvider,
    val deviceId: String,
    val deviceName: String?,
    val deviceToken: String,
    val appVersion: String?
)

data class UserDeviceToken(
    val id: Long,
    val platform: PushPlatform,
    val provider: PushProvider,
    val deviceId: String,
    val deviceName: String?,
    val appVersion: String?,
    val enabled: Boolean,
    val lastActiveAt: String?
)
