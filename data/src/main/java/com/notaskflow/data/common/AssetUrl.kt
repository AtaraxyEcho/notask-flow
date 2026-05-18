package com.notaskflow.data.common

import com.notaskflow.data.BuildConfig

internal fun String?.toAbsoluteAssetUrl(): String? {
    val value = this?.trim()?.takeIf { it.isNotBlank() } ?: return null
    if (value.startsWith("http://") || value.startsWith("https://")) {
        return value
    }
    return joinApiAssetUrl(BuildConfig.BASE_URL, value)
}

internal fun String?.toAvatarProxyUrl(userId: Long?): String? {
    val value = this?.trim()?.takeIf { it.isNotBlank() } ?: return null
    val normalizedUserId = userId?.takeIf { it > 0 } ?: return value.toAbsoluteAssetUrl()
    val proxyPath = "/api/v1/public/users/$normalizedUserId/avatar"
    if (value.startsWith(proxyPath)) {
        return value.toAbsoluteAssetUrl()
    }
    if (value.startsWith("/api/v1/user/$normalizedUserId/avatar")) {
        return proxyPath.toAbsoluteAssetUrl()
    }
    if (value.startsWith("avatars/$normalizedUserId/") || value.contains("/avatars/$normalizedUserId/")) {
        return proxyPath.toAbsoluteAssetUrl()
    }
    return value.toAbsoluteAssetUrl()
}

private fun joinApiAssetUrl(baseUrl: String, path: String): String {
    val normalizedBaseUrl = baseUrl.trimEnd('/')
    val normalizedPath = if (path.startsWith("/")) path else "/$path"
    return if (normalizedBaseUrl.endsWith(API_PREFIX) && normalizedPath.startsWith("$API_PREFIX/")) {
        "$normalizedBaseUrl${normalizedPath.removePrefix(API_PREFIX)}"
    } else {
        "$normalizedBaseUrl$normalizedPath"
    }
}

private const val API_PREFIX = "/api/v1"
