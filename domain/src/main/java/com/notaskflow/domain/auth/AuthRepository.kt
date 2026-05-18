package com.notaskflow.domain.auth

import com.notaskflow.domain.model.AuthToken
import com.notaskflow.domain.model.LoginCredential
import com.notaskflow.domain.model.PasswordResetToken
import com.notaskflow.domain.model.RegisterAccount
import com.notaskflow.domain.model.UserProfile

interface AuthRepository {
    suspend fun login(credential: LoginCredential): Result<AuthToken>
    suspend fun sendRegisterEmailCode(email: String): Result<Unit>
    suspend fun register(account: RegisterAccount): Result<UserProfile>
    suspend fun forgotPassword(email: String): Result<Unit>
    suspend fun verifyResetCode(email: String, code: String): Result<PasswordResetToken>
    suspend fun resetPassword(resetToken: String, newPassword: String, confirmPassword: String): Result<Unit>
    suspend fun refresh(): Result<AuthToken>
    suspend fun logout()
}
