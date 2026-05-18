package com.notaskflow.feature.auth

data class LoginUiState(
    val account: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val rememberMe: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed interface LoginEffect {
    data object LoginSuccess : LoginEffect
}
