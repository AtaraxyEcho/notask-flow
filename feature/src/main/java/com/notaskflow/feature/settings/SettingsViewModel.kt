package com.notaskflow.feature.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notaskflow.domain.model.EmailChangeCodeRequest
import com.notaskflow.domain.model.EmailChangeConfirmRequest
import com.notaskflow.domain.model.ManagedFileUpload
import com.notaskflow.domain.model.NotificationSettings
import com.notaskflow.domain.model.PasswordChange
import com.notaskflow.domain.model.UserProfile
import com.notaskflow.domain.model.UserProfileUpdate
import com.notaskflow.domain.user.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isEmailCodeSending: Boolean = false,
    val profile: UserProfile? = null,
    val avatarVersion: Long = 0L,
    val notificationSettings: NotificationSettings? = null,
    val message: String? = null,
    val errorMessage: String? = null
)

sealed interface SettingsEffect {
    data object SettingsChanged : SettingsEffect
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = mutableUiState

    private val mutableEffect = MutableSharedFlow<SettingsEffect>()
    val effect: SharedFlow<SettingsEffect> = mutableEffect.asSharedFlow()

    fun load() {
        viewModelScope.launch {
            mutableUiState.update { it.copy(isLoading = true, errorMessage = null) }
            val profileResult = userRepository.profile()
            val settingsResult = userRepository.notificationSettings()
            mutableUiState.update {
                it.copy(
                    isLoading = false,
                    profile = profileResult.getOrNull() ?: it.profile,
                    notificationSettings = settingsResult.getOrNull() ?: it.notificationSettings,
                    errorMessage = profileResult.exceptionOrNull()?.message
                        ?: settingsResult.exceptionOrNull()?.message
                )
            }
        }
    }

    fun updateProfile(nickname: String) {
        viewModelScope.launch {
            mutableUiState.update { it.copy(isSaving = true, errorMessage = null, message = null) }
            userRepository.updateProfile(
                UserProfileUpdate(
                    nickname = nickname.trim().takeIf { it.isNotBlank() },
                    email = null,
                    avatarUrl = null
                )
            ).onSuccess { profile ->
                mutableUiState.update {
                    it.copy(isSaving = false, profile = profile, message = "资料已更新")
                }
                mutableEffect.emit(SettingsEffect.SettingsChanged)
            }.onFailure { throwable ->
                mutableUiState.update {
                    it.copy(isSaving = false, errorMessage = throwable.message ?: "更新资料失败")
                }
            }
        }
    }

    fun uploadAvatar(upload: ManagedFileUpload) {
        viewModelScope.launch {
            mutableUiState.update { it.copy(isSaving = true, errorMessage = null, message = null) }
            userRepository.uploadAvatar(upload)
                .onSuccess { profile ->
                    val refreshedProfile = userRepository.profile().getOrDefault(profile)
                    mutableUiState.update {
                        it.copy(
                            isSaving = false,
                            profile = refreshedProfile,
                            avatarVersion = System.currentTimeMillis(),
                            message = "头像已更新"
                        )
                    }
                    mutableEffect.emit(SettingsEffect.SettingsChanged)
                }
                .onFailure { throwable ->
                    mutableUiState.update {
                        it.copy(isSaving = false, errorMessage = throwable.message ?: "头像上传失败")
                    }
                }
        }
    }

    fun changePassword(oldPassword: String, newPassword: String) {
        viewModelScope.launch {
            mutableUiState.update { it.copy(isSaving = true, errorMessage = null, message = null) }
            userRepository.changePassword(
                PasswordChange(
                    oldPassword = oldPassword,
                    newPassword = newPassword
                )
            ).onSuccess {
                mutableUiState.update { it.copy(isSaving = false, message = "密码已修改") }
            }.onFailure { throwable ->
                mutableUiState.update {
                    it.copy(isSaving = false, errorMessage = throwable.message ?: "修改密码失败")
                }
            }
        }
    }

    fun sendEmailChangeCode(newEmail: String) {
        viewModelScope.launch {
            mutableUiState.update { it.copy(isEmailCodeSending = true, errorMessage = null, message = null) }
            userRepository.sendEmailChangeCode(EmailChangeCodeRequest(newEmail = newEmail.trim()))
                .onSuccess {
                    mutableUiState.update {
                        it.copy(isEmailCodeSending = false, message = "验证码已发送到当前邮箱")
                    }
                }
                .onFailure { throwable ->
                    mutableUiState.update {
                        it.copy(isEmailCodeSending = false, errorMessage = throwable.message ?: "验证码发送失败")
                    }
                }
        }
    }

    fun changeEmail(newEmail: String, code: String) {
        viewModelScope.launch {
            mutableUiState.update { it.copy(isSaving = true, errorMessage = null, message = null) }
            userRepository.changeEmail(
                EmailChangeConfirmRequest(
                    newEmail = newEmail.trim(),
                    code = code.trim()
                )
            ).onSuccess { profile ->
                mutableUiState.update {
                    it.copy(isSaving = false, profile = profile, message = "邮箱已修改")
                }
                mutableEffect.emit(SettingsEffect.SettingsChanged)
            }.onFailure { throwable ->
                mutableUiState.update {
                    it.copy(isSaving = false, errorMessage = throwable.message ?: "邮箱修改失败")
                }
            }
        }
    }

    fun updateNotificationSettings(settings: NotificationSettings) {
        viewModelScope.launch {
            mutableUiState.update {
                it.copy(isSaving = true, notificationSettings = settings, errorMessage = null, message = null)
            }
            userRepository.updateNotificationSettings(settings)
                .onSuccess { saved ->
                    mutableUiState.update {
                        it.copy(isSaving = false, notificationSettings = saved, message = "偏好已保存")
                    }
                    mutableEffect.emit(SettingsEffect.SettingsChanged)
                }
                .onFailure { throwable ->
                    mutableUiState.update {
                        it.copy(isSaving = false, errorMessage = throwable.message ?: "保存偏好失败")
                    }
                }
        }
    }

    fun dismissMessage() {
        mutableUiState.update { it.copy(message = null, errorMessage = null) }
    }
}
