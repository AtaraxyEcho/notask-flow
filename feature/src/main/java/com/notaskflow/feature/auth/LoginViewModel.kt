package com.notaskflow.feature.auth

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class LoginViewModel : ViewModel() {
    private val mutableUiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = mutableUiState

    fun onAccountChange(value: String) {
        mutableUiState.update { it.copy(account = value, errorMessage = null) }
    }

    fun onPasswordChange(value: String) {
        mutableUiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun togglePasswordVisibility() {
        mutableUiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun toggleRememberMe() {
        mutableUiState.update { it.copy(rememberMe = !it.rememberMe) }
    }

    fun onLoginClick() {
        val state = mutableUiState.value
        val error = when {
            state.account.isBlank() -> "请输入账号或邮箱"
            state.password.length < MIN_PASSWORD_LENGTH -> "密码至少 6 位"
            else -> null
        }
        mutableUiState.update { it.copy(errorMessage = error, isLoading = error == null) }
    }

    fun resetLoading() {
        mutableUiState.update { it.copy(isLoading = false) }
    }

    private companion object {
        const val MIN_PASSWORD_LENGTH = 6
    }
}
