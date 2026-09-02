package com.notaskflow.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notaskflow.domain.auth.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = mutableUiState

    private val mutableEffect = MutableSharedFlow<LoginEffect>()
    val effect: SharedFlow<LoginEffect> = mutableEffect.asSharedFlow()

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
        if (error != null) {
            mutableUiState.update { it.copy(errorMessage = error, isLoading = false) }
            return
        }
        if (state.isLoading) {
            return
        }
        viewModelScope.launch {
            mutableUiState.update { it.copy(isLoading = true, errorMessage = null) }
            loginUseCase(state.account, state.password)
                .onSuccess {
                    mutableUiState.update { it.copy(isLoading = false) }
                    mutableEffect.emit(LoginEffect.LoginSuccess)
                }
                .onFailure { throwable ->
                    mutableUiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "登录失败，请稍后重试"
                        )
                    }
                }
        }
    }

    private companion object {
        const val MIN_PASSWORD_LENGTH = 6
    }
}
