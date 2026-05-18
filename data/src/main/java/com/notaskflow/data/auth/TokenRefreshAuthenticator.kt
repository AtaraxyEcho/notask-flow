package com.notaskflow.data.auth

import com.notaskflow.core.datastore.TokenManager
import com.notaskflow.data.auth.api.AuthApi
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject

class TokenRefreshAuthenticator @Inject constructor(
    private val refreshAuthApi: AuthApi,
    private val tokenManager: TokenManager
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= MAX_REFRESH_ATTEMPTS) {
            runBlocking {
                tokenManager.clear()
            }
            return null
        }
        return runBlocking {
            runCatching {
                val token = refreshAuthApi.refresh().getOrThrow()
                tokenManager.saveToken(
                    tokenValue = token.tokenValue,
                    expireTime = token.expireTime
                )
                response.request.newBuilder()
                    .header("Authorization", "Bearer ${token.tokenValue}")
                    .build()
            }.getOrElse {
                tokenManager.clear()
                null
            }
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var priorResponse = response.priorResponse
        while (priorResponse != null) {
            count += 1
            priorResponse = priorResponse.priorResponse
        }
        return count
    }

    private companion object {
        const val MAX_REFRESH_ATTEMPTS = 2
    }
}
