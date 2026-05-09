package com.notaskflow.domain.auth

import com.notaskflow.core.model.AuthToken
import com.notaskflow.core.model.LoginCredential

interface AuthRepository {
    suspend fun login(credential: LoginCredential): Result<AuthToken>
    suspend fun logout()
}
