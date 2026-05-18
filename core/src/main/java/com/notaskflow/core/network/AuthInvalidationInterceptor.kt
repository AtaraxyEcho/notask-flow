package com.notaskflow.core.network

import com.notaskflow.core.datastore.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInvalidationInterceptor(
    private val tokenManager: TokenManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.shouldInvalidateToken()) {
            runBlocking {
                tokenManager.clear()
            }
        }
        return response
    }

    private fun Response.shouldInvalidateToken(): Boolean {
        if (code == HTTP_UNAUTHORIZED) {
            return true
        }
        if (code != HTTP_OK) {
            return false
        }
        val responseBody = body ?: return false
        val contentType = responseBody.contentType()?.subtype.orEmpty()
        if (!contentType.contains(JSON_SUBTYPE, ignoreCase = true)) {
            return false
        }
        val bodyText = peekBody(MAX_PEEK_BYTES).string()
        return BUSINESS_UNAUTHORIZED_REGEX.containsMatchIn(bodyText)
    }

    private companion object {
        const val HTTP_OK = 200
        const val HTTP_UNAUTHORIZED = 401
        const val JSON_SUBTYPE = "json"
        const val MAX_PEEK_BYTES = 1024L
        val BUSINESS_UNAUTHORIZED_REGEX = Regex("\"code\"\\s*:\\s*401")
    }
}
