package com.notaskflow.domain.auth

import com.notaskflow.domain.model.AuthToken
import com.notaskflow.domain.model.LoginCredential
import javax.inject.Inject

class LoginUseCase @Inject constructor(
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
