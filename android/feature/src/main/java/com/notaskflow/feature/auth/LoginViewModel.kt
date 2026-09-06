package com.notaskflow.feature.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notaskflow.domain.auth.LoginUseCase
import com.notaskflow.feature.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val loginUseCase: LoginUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = mutableUiState

    private val mutableEffect = MutableSharedFlow<LoginEffect>()
    val effect: SharedFlow<LoginEffect> = mutableEffect.asSharedFlow()

    fun onAccountChange(value: String) {
        mutableUiState.update { it.copy(account = value, accountError = null, formError = null) }
    }

    fun onPasswordChange(value: String) {
        mutableUiState.update { it.copy(password = value, passwordError = null, formError = null) }
    }

    fun togglePasswordVisibility() {
        mutableUiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun toggleRememberMe() {
        mutableUiState.update { it.copy(rememberMe = !it.rememberMe) }
    }

    fun onLoginClick() {
        val state = mutableUiState.value
        // 登录仅做非空预检；密码长度策略只在注册/重置流程生效，避免误拦历史短密码用户
        val accountError = if (state.account.isBlank()) context.getString(R.string.auth_account_error) else null
        val passwordError = if (state.password.isBlank()) context.getString(R.string.auth_password_error) else null
        if (accountError != null || passwordError != null) {
            mutableUiState.update {
                it.copy(accountError = accountError, passwordError = passwordError, isLoading = false)
            }
            return
        }
        if (state.isLoading) {
            return
        }
        viewModelScope.launch {
            mutableUiState.update { it.copy(isLoading = true, formError = null) }
            loginUseCase(state.account, state.password)
                .onSuccess {
                    mutableUiState.update { it.copy(isLoading = false) }
                    mutableEffect.emit(LoginEffect.LoginSuccess)
                }
                .onFailure { throwable ->
                    mutableUiState.update {
                        it.copy(
                            isLoading = false,
                            formError = throwable.message ?: context.getString(R.string.auth_login_failed)
                        )
                    }
                }
        }
    }
}
