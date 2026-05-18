package com.notaskflow.data.auth

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.notaskflow.core.datastore.TokenManager
import com.notaskflow.data.auth.api.AuthApi
import com.notaskflow.data.auth.dto.ForgotPasswordRequestDto
import com.notaskflow.data.auth.dto.LoginRequestDto
import com.notaskflow.data.auth.dto.ResetPasswordRequestDto
import com.notaskflow.data.auth.dto.SendRegisterEmailCodeRequestDto
import com.notaskflow.data.auth.dto.VerifyResetCodeRequestDto
import com.notaskflow.data.auth.dto.toDomain
import com.notaskflow.data.auth.dto.toModel
import com.notaskflow.data.auth.dto.toDto
import com.notaskflow.data.user.dto.toDomain
import com.notaskflow.domain.auth.AuthRepository
import com.notaskflow.domain.model.AuthToken
import com.notaskflow.domain.model.LoginCredential
import com.notaskflow.domain.model.PasswordResetToken
import com.notaskflow.domain.model.RegisterAccount
import com.notaskflow.domain.model.UserProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager,
    @param:ApplicationContext private val context: Context
) : AuthRepository {
    override suspend fun login(credential: LoginCredential): Result<AuthToken> {
        return runCatching {
            val response = authApi.login(
                LoginRequestDto(
                    account = credential.account,
                    password = credential.password,
                    clientType = ANDROID_CLIENT_TYPE,
                    deviceId = resolveDeviceId(),
                    deviceName = resolveDeviceName(),
                    appVersion = resolveAppVersion()
                )
            )
            val token = response.getOrThrow().toModel()
            tokenManager.saveToken(
                tokenValue = token.tokenValue,
                expireTime = token.expireTime
            )
            token
        }
    }

    override suspend fun sendRegisterEmailCode(email: String): Result<Unit> {
        return runCatching {
            authApi.sendRegisterEmailCode(SendRegisterEmailCodeRequestDto(email = email)).requireSuccess()
        }
    }

    override suspend fun register(account: RegisterAccount): Result<UserProfile> {
        return runCatching {
            authApi.register(account.toDto()).getOrThrow().toDomain()
        }
    }

    override suspend fun forgotPassword(email: String): Result<Unit> {
        return runCatching {
            authApi.forgotPassword(ForgotPasswordRequestDto(email = email)).requireSuccess()
        }
    }

    override suspend fun verifyResetCode(email: String, code: String): Result<PasswordResetToken> {
        return runCatching {
            authApi.verifyResetCode(VerifyResetCodeRequestDto(email = email, code = code))
                .getOrThrow()
                .toDomain()
        }
    }

    override suspend fun resetPassword(
        resetToken: String,
        newPassword: String,
        confirmPassword: String
    ): Result<Unit> {
        return runCatching {
            authApi.resetPassword(
                ResetPasswordRequestDto(
                    resetToken = resetToken,
                    newPassword = newPassword,
                    confirmPassword = confirmPassword
                )
            ).requireSuccess()
        }
    }

    override suspend fun refresh(): Result<AuthToken> {
        return runCatching {
            val token = authApi.refresh().getOrThrow().toModel()
            tokenManager.saveToken(
                tokenValue = token.tokenValue,
                expireTime = token.expireTime
            )
            token
        }
    }

    override suspend fun logout() {
        runCatching {
            authApi.logout()
        }
        tokenManager.clear()
    }

    private fun resolveDeviceId(): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        return if (androidId.isNullOrBlank()) {
            ANDROID_DEFAULT_DEVICE_ID
        } else {
            "$ANDROID_CLIENT_TYPE_PREFIX-$androidId"
        }
    }

    private fun resolveDeviceName(): String {
        val manufacturer = Build.MANUFACTURER.orEmpty().replaceFirstChar { it.uppercase() }
        val model = Build.MODEL.orEmpty()
        return listOf(manufacturer, model)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { "Android" }
    }

    private fun resolveAppVersion(): String {
        return runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ANDROID_CLIENT_TYPE_PREFIX
        }.getOrDefault(ANDROID_CLIENT_TYPE_PREFIX)
    }

    private companion object {
        const val ANDROID_CLIENT_TYPE = "ANDROID"
        const val ANDROID_CLIENT_TYPE_PREFIX = "android"
        const val ANDROID_DEFAULT_DEVICE_ID = "android-default"
    }
}
