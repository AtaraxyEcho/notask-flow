package com.notaskflow.feature.space

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notaskflow.domain.model.SpaceCreate
import com.notaskflow.domain.model.SpaceJoinApplication
import com.notaskflow.domain.model.SpaceJoinApply
import com.notaskflow.domain.space.SpaceRepository
import com.notaskflow.domain.team.TeamApplicationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SpaceFormUiState(
    val teamName: String = "",
    val inviteCode: String = "",
    val supervisorAccount: String = "",
    val applicationTeamName: String = "",
    val applicationRemark: String = "",
    val mineApplications: List<SpaceJoinApplication> = emptyList(),
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
)

sealed interface SpaceFormEffect {
    data object Done : SpaceFormEffect
}

@HiltViewModel
class SpaceFormViewModel @Inject constructor(
    private val spaceRepository: SpaceRepository,
    private val teamApplicationRepository: TeamApplicationRepository
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(SpaceFormUiState())
    val uiState: StateFlow<SpaceFormUiState> = mutableUiState

    private val mutableEffect = MutableSharedFlow<SpaceFormEffect>()
    val effect: SharedFlow<SpaceFormEffect> = mutableEffect.asSharedFlow()

    fun updateTeamName(value: String) {
        mutableUiState.update { it.copy(teamName = value, errorMessage = null) }
    }

    fun updateInviteCode(value: String) {
        mutableUiState.update { it.copy(inviteCode = value.trim(), errorMessage = null) }
    }

    fun updateSupervisorAccount(value: String) {
        mutableUiState.update { it.copy(supervisorAccount = value.trim(), errorMessage = null) }
    }

    fun updateApplicationTeamName(value: String) {
        mutableUiState.update { it.copy(applicationTeamName = value, errorMessage = null) }
    }

    fun updateApplicationRemark(value: String) {
        mutableUiState.update { it.copy(applicationRemark = value, errorMessage = null) }
    }

    fun loadMineApplications() {
        viewModelScope.launch {
            teamApplicationRepository.mine()
                .onSuccess { applications ->
                    mutableUiState.update { it.copy(mineApplications = applications, errorMessage = null) }
                }
        }
    }

    fun createTeamSpace() {
        val name = mutableUiState.value.teamName.trim()
        if (name.isBlank()) {
            mutableUiState.update { it.copy(errorMessage = "请输入团队空间名称") }
            return
        }
        viewModelScope.launch {
            mutableUiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            spaceRepository.createTeamSpace(SpaceCreate(name = name))
                .onSuccess {
                    mutableUiState.update { it.copy(isSubmitting = false) }
                    mutableEffect.emit(SpaceFormEffect.Done)
                }
                .onFailure { throwable ->
                    mutableUiState.update {
                        it.copy(isSubmitting = false, errorMessage = throwable.message ?: "创建团队空间失败")
                    }
                }
        }
    }

    fun joinByInviteCode() {
        val code = mutableUiState.value.inviteCode.trim()
        if (code.isBlank()) {
            mutableUiState.update { it.copy(errorMessage = "请输入邀请码") }
            return
        }
        viewModelScope.launch {
            mutableUiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            spaceRepository.joinByInviteCode(code)
                .onSuccess {
                    mutableUiState.update { it.copy(isSubmitting = false) }
                    mutableEffect.emit(SpaceFormEffect.Done)
                }
                .onFailure { throwable ->
                    mutableUiState.update {
                        it.copy(isSubmitting = false, errorMessage = throwable.message ?: "加入团队失败")
                    }
                }
        }
    }

    fun applyJoinTeam() {
        val supervisorAccount = mutableUiState.value.supervisorAccount.trim()
        if (supervisorAccount.isBlank()) {
            mutableUiState.update { it.copy(errorMessage = "请输入上级账号") }
            return
        }
        viewModelScope.launch {
            val state = mutableUiState.value
            mutableUiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            teamApplicationRepository.apply(
                SpaceJoinApply(
                    supervisorAccount = supervisorAccount,
                    teamName = state.applicationTeamName.trim().takeIf { it.isNotBlank() },
                    remark = state.applicationRemark.trim().takeIf { it.isNotBlank() }
                )
            ).onSuccess {
                mutableUiState.update { it.copy(isSubmitting = false) }
                mutableEffect.emit(SpaceFormEffect.Done)
            }.onFailure { throwable ->
                mutableUiState.update {
                    it.copy(isSubmitting = false, errorMessage = throwable.message ?: "申请加入团队失败")
                }
            }
        }
    }
}
