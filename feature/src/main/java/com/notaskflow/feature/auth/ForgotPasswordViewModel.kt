package com.notaskflow.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notaskflow.domain.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ForgotPasswordUiState(
    val email: String = "",
    val code: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val resetToken: String? = null,
    val isSubmitting: Boolean = false,
    val message: String? = null,
    val errorMessage: String? = null
)

sealed interface ForgotPasswordEffect {
    data object ResetDone : ForgotPasswordEffect
}

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = mutableUiState

    private val mutableEffect = MutableSharedFlow<ForgotPasswordEffect>()
    val effect: SharedFlow<ForgotPasswordEffect> = mutableEffect.asSharedFlow()

    fun updateEmail(value: String) {
        mutableUiState.update { it.copy(email = value.trim(), errorMessage = null) }
    }

    fun updateCode(value: String) {
        mutableUiState.update { it.copy(code = value.filter { char -> char.isDigit() }, errorMessage = null) }
    }

    fun updateNewPassword(value: String) {
        mutableUiState.update { it.copy(newPassword = value, errorMessage = null) }
    }

    fun updateConfirmPassword(value: String) {
        mutableUiState.update { it.copy(confirmPassword = value, errorMessage = null) }
    }

    fun sendCode() {
        val email = mutableUiState.value.email
        if (email.isBlank()) {
            mutableUiState.update { it.copy(errorMessage = "请输入邮箱") }
            return
        }
        viewModelScope.launch {
            mutableUiState.update { it.copy(isSubmitting = true, errorMessage = null, message = null) }
            authRepository.forgotPassword(email)
                .onSuccess {
                    mutableUiState.update { it.copy(isSubmitting = false, message = "重置验证码已发送") }
                }
                .onFailure { throwable ->
                    mutableUiState.update {
                        it.copy(isSubmitting = false, errorMessage = throwable.message ?: "发送验证码失败")
                    }
                }
        }
    }

    fun verifyCode() {
        val state = mutableUiState.value
        if (state.email.isBlank() || state.code.length != RESET_CODE_LENGTH) {
            mutableUiState.update { it.copy(errorMessage = "请输入邮箱和 6 位验证码") }
            return
        }
        viewModelScope.launch {
            mutableUiState.update { it.copy(isSubmitting = true, errorMessage = null, message = null) }
            authRepository.verifyResetCode(state.email, state.code)
                .onSuccess { token ->
                    mutableUiState.update {
                        it.copy(
                            isSubmitting = false,
                            resetToken = token.resetToken,
                            message = "验证码已通过，请设置新密码"
                        )
                    }
                }
                .onFailure { throwable ->
                    mutableUiState.update {
                        it.copy(isSubmitting = false, errorMessage = throwable.message ?: "验证码校验失败")
                    }
                }
        }
    }

    fun resetPassword() {
        val state = mutableUiState.value
        val token = state.resetToken
        when {
            token.isNullOrBlank() -> {
                mutableUiState.update { it.copy(errorMessage = "请先校验验证码") }
                return
            }
            state.newPassword.length < MIN_PASSWORD_LENGTH -> {
                mutableUiState.update { it.copy(errorMessage = "新密码至少 8 位") }
                return
            }
            state.newPassword != state.confirmPassword -> {
                mutableUiState.update { it.copy(errorMessage = "两次输入的密码不一致") }
                return
            }
        }
        viewModelScope.launch {
            mutableUiState.update { it.copy(isSubmitting = true, errorMessage = null, message = null) }
            authRepository.resetPassword(token, state.newPassword, state.confirmPassword)
                .onSuccess {
                    mutableUiState.update { it.copy(isSubmitting = false, message = "密码已重置") }
                    mutableEffect.emit(ForgotPasswordEffect.ResetDone)
                }
                .onFailure { throwable ->
                    mutableUiState.update {
                        it.copy(isSubmitting = false, errorMessage = throwable.message ?: "重置密码失败")
                    }
                }
        }
    }

    private companion object {
        const val RESET_CODE_LENGTH = 6
        const val MIN_PASSWORD_LENGTH = 8
    }
}
