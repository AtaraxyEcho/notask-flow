package com.notaskflow.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notaskflow.domain.auth.AuthRepository
import com.notaskflow.domain.model.RegisterAccount
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegisterUiState(
    val username: String = "",
    val nickname: String = "",
    val email: String = "",
    val emailCode: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isSendingCode: Boolean = false,
    val isRegistering: Boolean = false,
    val message: String? = null,
    val errorMessage: String? = null
)

sealed interface RegisterEffect {
    data object Registered : RegisterEffect
}

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = mutableUiState

    private val mutableEffect = MutableSharedFlow<RegisterEffect>()
    val effect: SharedFlow<RegisterEffect> = mutableEffect.asSharedFlow()

    fun updateUsername(value: String) {
        mutableUiState.update { it.copy(username = value, errorMessage = null) }
    }

    fun updateNickname(value: String) {
        mutableUiState.update { it.copy(nickname = value, errorMessage = null) }
    }

    fun updateEmail(value: String) {
        mutableUiState.update { it.copy(email = value.trim(), errorMessage = null) }
    }

    fun updateEmailCode(value: String) {
        mutableUiState.update { it.copy(emailCode = value.filter { char -> char.isDigit() }, errorMessage = null) }
    }

    fun updatePassword(value: String) {
        mutableUiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun updateConfirmPassword(value: String) {
        mutableUiState.update { it.copy(confirmPassword = value, errorMessage = null) }
    }

    fun sendEmailCode() {
        val email = mutableUiState.value.email
        if (email.isBlank()) {
            mutableUiState.update { it.copy(errorMessage = "请输入邮箱") }
            return
        }
        viewModelScope.launch {
            mutableUiState.update { it.copy(isSendingCode = true, errorMessage = null, message = null) }
            authRepository.sendRegisterEmailCode(email)
                .onSuccess {
                    mutableUiState.update { it.copy(isSendingCode = false, message = "验证码已发送") }
                }
                .onFailure { throwable ->
                    mutableUiState.update {
                        it.copy(isSendingCode = false, errorMessage = throwable.message ?: "发送验证码失败")
                    }
                }
        }
    }

    fun register() {
        val state = mutableUiState.value
        val username = state.username.trim()
        val email = state.email.trim()
        when {
            username.isBlank() -> {
                mutableUiState.update { it.copy(errorMessage = "请输入用户名") }
                return
            }
            email.isBlank() -> {
                mutableUiState.update { it.copy(errorMessage = "请输入邮箱") }
                return
            }
            state.password.length < MIN_PASSWORD_LENGTH -> {
                mutableUiState.update { it.copy(errorMessage = "密码至少 8 位") }
                return
            }
            state.password != state.confirmPassword -> {
                mutableUiState.update { it.copy(errorMessage = "两次输入的密码不一致") }
                return
            }
        }
        viewModelScope.launch {
            mutableUiState.update { it.copy(isRegistering = true, errorMessage = null, message = null) }
            authRepository.register(
                RegisterAccount(
                    username = username,
                    nickname = state.nickname.trim().takeIf { it.isNotBlank() },
                    email = email,
                    password = state.password,
                    emailCode = state.emailCode.takeIf { it.isNotBlank() }
                )
            ).onSuccess {
                mutableUiState.update { it.copy(isRegistering = false, message = "注册成功，请登录") }
                mutableEffect.emit(RegisterEffect.Registered)
            }.onFailure { throwable ->
                mutableUiState.update {
                    it.copy(isRegistering = false, errorMessage = throwable.message ?: "注册失败")
                }
            }
        }
    }

    private companion object {
        const val MIN_PASSWORD_LENGTH = 8
    }
}
