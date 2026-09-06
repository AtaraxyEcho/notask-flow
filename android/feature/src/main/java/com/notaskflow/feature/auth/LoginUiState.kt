package com.notaskflow.feature.auth

data class LoginUiState(
    val account: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val rememberMe: Boolean = false,
    val isLoading: Boolean = false,
    val accountError: String? = null,
    val passwordError: String? = null,
    val formError: String? = null
)

sealed interface LoginEffect {
    data object LoginSuccess : LoginEffect
}
