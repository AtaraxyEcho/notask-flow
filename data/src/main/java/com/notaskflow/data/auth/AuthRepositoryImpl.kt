package com.notaskflow.data.auth

import com.notaskflow.core.datastore.TokenManager
import com.notaskflow.core.model.AuthToken
import com.notaskflow.core.model.LoginCredential
import com.notaskflow.data.auth.api.AuthApi
import com.notaskflow.data.auth.dto.LoginRequestDto
import com.notaskflow.data.auth.dto.toModel
import com.notaskflow.domain.auth.AuthRepository
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager
) : AuthRepository {
    override suspend fun login(credential: LoginCredential): Result<AuthToken> {
        return runCatching {
            val response = authApi.login(
                LoginRequestDto(
                    account = credential.account,
                    password = credential.password
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

    override suspend fun logout() {
        runCatching {
            authApi.logout()
        }
        tokenManager.clear()
    }
}
