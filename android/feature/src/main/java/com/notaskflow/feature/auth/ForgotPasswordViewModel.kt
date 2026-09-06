package com.notaskflow.feature.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notaskflow.domain.auth.AuthRepository
import com.notaskflow.feature.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.delay
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
    val hasSentCode: Boolean = false,
    val resetToken: String? = null,
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val isSubmitting: Boolean = false,
    val resendCountdown: Int = 0,
    val message: String? = null,
    val errorMessage: String? = null
)

sealed interface ForgotPasswordEffect {
    data object ResetDone : ForgotPasswordEffect
}

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = mutableUiState

    private val mutableEffect = MutableSharedFlow<ForgotPasswordEffect>()
    val effect: SharedFlow<ForgotPasswordEffect> = mutableEffect.asSharedFlow()

    fun updateEmail(value: String) {
        mutableUiState.update { it.copy(email = value.trim(), errorMessage = null) }
    }

    /** 六位分离验证码的合并值，仅保留数字并截断至 6 位。 */
    fun updateCode(value: String) {
        mutableUiState.update { it.copy(code = value.filter { char -> char.isDigit() }.take(CODE_LENGTH)) }
    }

    fun updateNewPassword(value: String) {
        mutableUiState.update { it.copy(newPassword = value, errorMessage = null) }
    }

    fun updateConfirmPassword(value: String) {
        mutableUiState.update { it.copy(confirmPassword = value, errorMessage = null) }
    }

    fun togglePasswordVisibility() {
        mutableUiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun toggleConfirmPasswordVisibility() {
        mutableUiState.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
    }

    /** 返回第一步重新编辑邮箱，并清空已输入的验证码与倒计时。 */
    fun editEmail() {
        mutableUiState.update {
            it.copy(hasSentCode = false, code = "", resendCountdown = 0, errorMessage = null, message = null)
        }
    }

    fun sendCode() {
        val email = mutableUiState.value.email.trim()
        when {
            email.isBlank() -> {
                mutableUiState.update { it.copy(errorMessage = context.getString(R.string.auth_forgot_email_error)) }
                return
            }
            !AUTH_EMAIL_REGEX.matches(email) -> {
                mutableUiState.update { it.copy(errorMessage = context.getString(R.string.auth_email_invalid)) }
                return
            }
        }
        viewModelScope.launch {
            mutableUiState.update { it.copy(isSubmitting = true, errorMessage = null, message = null) }
            authRepository.forgotPassword(email)
                .onSuccess {
                    mutableUiState.update {
                        it.copy(
                            isSubmitting = false,
                            hasSentCode = true,
                            code = "",
                            message = context.getString(R.string.auth_code_sent)
                        )
                    }
                    startResendCountdown()
                }
                .onFailure { throwable ->
                    mutableUiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = throwable.message
                                ?: context.getString(R.string.auth_send_code_failed)
                        )
                    }
                }
        }
    }

    fun verifyCode() {
        val state = mutableUiState.value
        if (state.email.isBlank() || state.code.length != CODE_LENGTH) {
            mutableUiState.update { it.copy(errorMessage = context.getString(R.string.auth_forgot_code_error)) }
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
                            message = context.getString(R.string.auth_code_verified)
                        )
                    }
                }
                .onFailure { throwable ->
                    mutableUiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = throwable.message
                                ?: context.getString(R.string.auth_verify_failed)
                        )
                    }
                }
        }
    }

    fun resetPassword() {
        val state = mutableUiState.value
        val token = state.resetToken
        when {
            token.isNullOrBlank() -> {
                mutableUiState.update { it.copy(errorMessage = context.getString(R.string.auth_reset_token_missing)) }
                return
            }
            state.newPassword.length < AUTH_MIN_PASSWORD_LENGTH -> {
                mutableUiState.update {
                    it.copy(errorMessage = context.getString(R.string.auth_new_password_min_error))
                }
                return
            }
            state.newPassword != state.confirmPassword -> {
                mutableUiState.update { it.copy(errorMessage = context.getString(R.string.auth_password_mismatch)) }
                return
            }
        }
        viewModelScope.launch {
            mutableUiState.update { it.copy(isSubmitting = true, errorMessage = null, message = null) }
            authRepository.resetPassword(token, state.newPassword, state.confirmPassword)
                .onSuccess {
                    mutableUiState.update {
                        it.copy(
                            isSubmitting = false,
                            message = context.getString(R.string.auth_reset_success)
                        )
                    }
                    mutableEffect.emit(ForgotPasswordEffect.ResetDone)
                }
                .onFailure { throwable ->
                    mutableUiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = throwable.message
                                ?: context.getString(R.string.auth_reset_failed)
                        )
                    }
                }
        }
    }

    private fun startResendCountdown() {
        mutableUiState.update { it.copy(resendCountdown = RESEND_SECONDS) }
        viewModelScope.launch {
            while (mutableUiState.value.resendCountdown > 0) {
                delay(1_000)
                mutableUiState.update { it.copy(resendCountdown = (it.resendCountdown - 1).coerceAtLeast(0)) }
            }
        }
    }

    private companion object {
        val AUTH_EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
        const val CODE_LENGTH = 6
        const val RESEND_SECONDS = 60
    }
}
