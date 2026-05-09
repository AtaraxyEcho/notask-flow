package com.notaskflow.domain.auth

import com.notaskflow.core.model.AuthToken
import com.notaskflow.core.model.LoginCredential

class LoginUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(account: String, password: String): Result<AuthToken> {
        return authRepository.login(
            LoginCredential(
                account = account,
                password = password
            )
        )
    }
}
