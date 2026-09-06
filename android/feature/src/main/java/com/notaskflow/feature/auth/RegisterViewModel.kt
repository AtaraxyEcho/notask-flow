package com.notaskflow.feature.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notaskflow.domain.auth.AuthRepository
import com.notaskflow.domain.model.RegisterAccount
import com.notaskflow.domain.model.RegisterTeamMode
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

data class RegisterUiState(
    val username: String = "",
    val nickname: String = "",
    val email: String = "",
    val emailCode: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val inviteCode: String = "",
    val acceptedTerms: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val isSendingCode: Boolean = false,
    val isRegistering: Boolean = false,
    val resendCountdown: Int = 0,
    val emailError: String? = null,
    val message: String? = null,
    val errorMessage: String? = null
)

sealed interface RegisterEffect {
    data object Registered : RegisterEffect
}

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
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
        mutableUiState.update { it.copy(email = value.trim(), emailError = null, errorMessage = null) }
    }

    /** 六位分离验证码的合并值，仅保留数字并截断至 6 位。 */
    fun updateEmailCode(value: String) {
        val digits = value.filter { it.isDigit() }.take(CODE_LENGTH)
        mutableUiState.update { it.copy(emailCode = digits, errorMessage = null) }
    }

    fun updatePassword(value: String) {
        mutableUiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun updateConfirmPassword(value: String) {
        mutableUiState.update { it.copy(confirmPassword = value, errorMessage = null) }
    }

    fun updateInviteCode(value: String) {
        mutableUiState.update { it.copy(inviteCode = value.trim(), errorMessage = null) }
    }

    fun toggleAcceptedTerms() {
        mutableUiState.update { it.copy(acceptedTerms = !it.acceptedTerms, errorMessage = null) }
    }

    fun togglePasswordVisibility() {
        mutableUiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun toggleConfirmPasswordVisibility() {
        mutableUiState.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
    }

    fun sendEmailCode() {
        val email = mutableUiState.value.email.trim()
        when {
            email.isBlank() -> {
                mutableUiState.update { it.copy(emailError = context.getString(R.string.auth_email_error)) }
                return
            }
            !AUTH_EMAIL_REGEX.matches(email) -> {
                mutableUiState.update { it.copy(emailError = context.getString(R.string.auth_email_invalid)) }
                return
            }
        }
        viewModelScope.launch {
            mutableUiState.update {
                it.copy(isSendingCode = true, emailError = null, errorMessage = null, message = null)
            }
            authRepository.sendRegisterEmailCode(email)
                .onSuccess {
                    mutableUiState.update {
                        it.copy(isSendingCode = false, message = context.getString(R.string.auth_code_sent))
                    }
                    startResendCountdown()
                }
                .onFailure { throwable ->
                    mutableUiState.update {
                        it.copy(
                            isSendingCode = false,
                            errorMessage = throwable.message
                                ?: context.getString(R.string.auth_send_code_failed)
                        )
                    }
                }
        }
    }

    fun register() {
        val state = mutableUiState.value
        val username = state.username.trim()
        val email = state.email.trim()
        val inviteCode = state.inviteCode.trim()
        when {
            username.isBlank() -> {
                mutableUiState.update { it.copy(errorMessage = context.getString(R.string.auth_username_error)) }
                return
            }
            email.isBlank() -> {
                mutableUiState.update { it.copy(errorMessage = context.getString(R.string.auth_email_error)) }
                return
            }
            !AUTH_EMAIL_REGEX.matches(email) -> {
                mutableUiState.update { it.copy(errorMessage = context.getString(R.string.auth_email_invalid)) }
                return
            }
            state.emailCode.isNotEmpty() && state.emailCode.length != CODE_LENGTH -> {
                mutableUiState.update { it.copy(errorMessage = context.getString(R.string.auth_code_error)) }
                return
            }
            state.password.length < AUTH_MIN_PASSWORD_LENGTH -> {
                mutableUiState.update { it.copy(errorMessage = context.getString(R.string.auth_password_min_error)) }
                return
            }
            state.password != state.confirmPassword -> {
                mutableUiState.update { it.copy(errorMessage = context.getString(R.string.auth_password_mismatch)) }
                return
            }
            !state.acceptedTerms -> {
                mutableUiState.update { it.copy(errorMessage = context.getString(R.string.auth_accept_terms_error)) }
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
                    emailCode = state.emailCode.takeIf { it.isNotEmpty() },
                    teamMode = if (inviteCode.isBlank()) {
                        RegisterTeamMode.PERSONAL_ONLY
                    } else {
                        RegisterTeamMode.JOIN_INVITE_CODE
                    },
                    inviteCode = inviteCode.takeIf { it.isNotBlank() }
                )
            ).onSuccess {
                mutableUiState.update {
                    it.copy(
                        isRegistering = false,
                        message = context.getString(R.string.auth_register_success)
                    )
                }
                mutableEffect.emit(RegisterEffect.Registered)
            }.onFailure { throwable ->
                mutableUiState.update {
                    it.copy(
                        isRegistering = false,
                        errorMessage = throwable.message ?: context.getString(R.string.auth_register_failed)
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
