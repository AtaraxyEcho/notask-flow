package com.notaskflow.core.model

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

data class UserProfile(
    val id: Long,
    val username: String,
    val email: String,
    val nickname: String?
)
