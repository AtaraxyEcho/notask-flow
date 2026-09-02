package com.notaskflow.feature.members

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notaskflow.domain.model.SpaceInvite
import com.notaskflow.domain.model.SpaceJoinApplication
import com.notaskflow.domain.model.SpaceJoinApprove
import com.notaskflow.domain.model.SpaceJoinReject
import com.notaskflow.domain.model.SpaceMember
import com.notaskflow.domain.space.SpaceRepository
import com.notaskflow.domain.team.TeamApplicationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MembersUiState(
    val isLoading: Boolean = false,
    val isMutating: Boolean = false,
    val members: List<SpaceMember> = emptyList(),
    val pendingApplications: List<SpaceJoinApplication> = emptyList(),
    val invite: SpaceInvite? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class MembersViewModel @Inject constructor(
    private val spaceRepository: SpaceRepository,
    private val teamApplicationRepository: TeamApplicationRepository
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(MembersUiState())
    val uiState: StateFlow<MembersUiState> = mutableUiState

    private var currentSpaceId: Long? = null

    fun load(spaceId: Long) {
        currentSpaceId = spaceId
        viewModelScope.launch {
            mutableUiState.update { it.copy(isLoading = true, errorMessage = null) }
            val membersResult = spaceRepository.listMembers(spaceId)
            val applicationsResult = teamApplicationRepository.pending()
            mutableUiState.update { state ->
                state.copy(
                    isLoading = false,
                    isMutating = false,
                    members = membersResult.getOrDefault(state.members),
                    pendingApplications = applicationsResult.getOrDefault(state.pendingApplications)
                        .filter { application ->
                            application.targetSpaceId == null || application.targetSpaceId == spaceId
                        },
                    errorMessage = membersResult.exceptionOrNull()?.message
                        ?: applicationsResult.exceptionOrNull()?.message
                )
            }
        }
    }

    fun createInvite(roleCode: String, expireMinutes: Int?) {
        val spaceId = currentSpaceId ?: return
        viewModelScope.launch {
            mutableUiState.update { it.copy(isMutating = true, errorMessage = null, invite = null) }
            spaceRepository.createInvite(spaceId, roleCode, expireMinutes)
                .onSuccess { invite ->
                    mutableUiState.update {
                        it.copy(isMutating = false, invite = invite, errorMessage = null)
                    }
                }
                .onFailure { throwable ->
                    mutableUiState.update {
                        it.copy(
                            isMutating = false,
                            errorMessage = throwable.message ?: "邀请码创建失败"
                        )
                    }
                }
        }
    }

    fun clearInvite() {
        mutableUiState.update { it.copy(invite = null) }
    }

    fun updateRole(member: SpaceMember, roleCode: String) {
        val spaceId = currentSpaceId ?: return
        viewModelScope.launch {
            mutableUiState.update { it.copy(isMutating = true, errorMessage = null) }
            spaceRepository.updateMemberRole(spaceId, member.userId, roleCode)
                .onSuccess { updated ->
                    mutableUiState.update { state ->
                        state.copy(
                            isMutating = false,
                            members = state.members.map { current ->
                                if (current.userId == updated.userId) {
                                    updated
                                } else {
                                    current
                                }
                            },
                            errorMessage = null
                        )
                    }
                }
                .onFailure { throwable ->
                    mutableUiState.update {
                        it.copy(
                            isMutating = false,
                            errorMessage = throwable.message ?: "成员角色更新失败"
                        )
                    }
                }
        }
    }

    fun removeMember(member: SpaceMember) {
        val spaceId = currentSpaceId ?: return
        viewModelScope.launch {
            mutableUiState.update { it.copy(isMutating = true, errorMessage = null) }
            spaceRepository.removeMember(spaceId, member.userId)
                .onSuccess {
                    mutableUiState.update { state ->
                        state.copy(
                            isMutating = false,
                            members = state.members.filterNot { memberItem -> memberItem.userId == member.userId },
                            errorMessage = null
                        )
                    }
                }
                .onFailure { throwable ->
                    mutableUiState.update {
                        it.copy(
                            isMutating = false,
                            errorMessage = throwable.message ?: "成员移除失败"
                        )
                    }
                }
        }
    }

    fun approveApplication(application: SpaceJoinApplication) {
        val spaceId = currentSpaceId ?: return
        viewModelScope.launch {
            mutableUiState.update { it.copy(isMutating = true, errorMessage = null) }
            teamApplicationRepository.approve(
                requestId = application.id,
                request = SpaceJoinApprove(spaceId = spaceId, roleCode = DEFAULT_APPROVE_ROLE)
            ).onSuccess {
                load(spaceId)
            }.onFailure { throwable ->
                mutableUiState.update {
                    it.copy(
                        isMutating = false,
                        errorMessage = throwable.message ?: "通过申请失败"
                    )
                }
            }
        }
    }

    fun rejectApplication(application: SpaceJoinApplication, reason: String? = null) {
        val spaceId = currentSpaceId ?: return
        viewModelScope.launch {
            mutableUiState.update { it.copy(isMutating = true, errorMessage = null) }
            teamApplicationRepository.reject(
                requestId = application.id,
                request = SpaceJoinReject(reason = reason)
            ).onSuccess {
                load(spaceId)
            }.onFailure { throwable ->
                mutableUiState.update {
                    it.copy(
                        isMutating = false,
                        errorMessage = throwable.message ?: "拒绝申请失败"
                    )
                }
            }
        }
    }

    private companion object {
        const val DEFAULT_APPROVE_ROLE = "SPACE_MEMBER"
    }
}
