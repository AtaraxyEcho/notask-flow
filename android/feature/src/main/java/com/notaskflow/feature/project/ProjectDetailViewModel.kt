package com.notaskflow.feature.project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notaskflow.domain.model.Note
import com.notaskflow.domain.model.Project
import com.notaskflow.domain.model.ProjectMember
import com.notaskflow.domain.model.ProjectMemberRole
import com.notaskflow.domain.model.ProjectMemberSave
import com.notaskflow.domain.model.ProjectSave
import com.notaskflow.domain.model.SpaceMember
import com.notaskflow.domain.model.Task
import com.notaskflow.domain.model.NoteQuery
import com.notaskflow.domain.model.TaskQuery
import com.notaskflow.domain.note.NoteRepository
import com.notaskflow.domain.project.ProjectRepository
import com.notaskflow.domain.space.SpaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProjectDetailUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isMemberMutating: Boolean = false,
    val project: Project? = null,
    val tasks: List<Task> = emptyList(),
    val notes: List<Note> = emptyList(),
    val members: List<ProjectMember> = emptyList(),
    val availableMembers: List<SpaceMember> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class ProjectDetailViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val noteRepository: NoteRepository,
    private val spaceRepository: SpaceRepository
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ProjectDetailUiState())
    val uiState: StateFlow<ProjectDetailUiState> = mutableUiState

    private var currentSpaceId: Long? = null
    private var currentProjectId: Long? = null

    fun load(spaceId: Long, projectId: Long) {
        currentSpaceId = spaceId
        currentProjectId = projectId
        viewModelScope.launch {
            mutableUiState.update { it.copy(isLoading = true, errorMessage = null) }
            val projectResult = projectRepository.get(spaceId, projectId)
            val tasksResult = projectRepository.tasks(spaceId, projectId, TaskQuery(pageSize = TASK_LIMIT))
            val notesResult = noteRepository.page(spaceId, NoteQuery(pageSize = NOTE_LIMIT, projectId = projectId))
            val membersResult = projectRepository.members(spaceId, projectId)
            val spaceMembersResult = spaceRepository.listMembers(spaceId)
            val members = membersResult.getOrNull() ?: mutableUiState.value.members
            mutableUiState.update { state ->
                state.copy(
                    isLoading = false,
                    project = projectResult.getOrNull() ?: state.project,
                    tasks = tasksResult.getOrNull()?.list ?: state.tasks,
                    notes = notesResult.getOrNull()?.list ?: state.notes,
                    members = members,
                    availableMembers = availableMembers(
                        projectMembers = members,
                        spaceMembers = spaceMembersResult.getOrNull() ?: state.availableMembers
                    ),
                    errorMessage = projectResult.exceptionOrNull()?.message
                        ?: tasksResult.exceptionOrNull()?.message
                        ?: notesResult.exceptionOrNull()?.message
                        ?: membersResult.exceptionOrNull()?.message
                        ?: spaceMembersResult.exceptionOrNull()?.message
                )
            }
        }
    }

    fun update(spaceId: Long, projectId: Long, project: ProjectSave) {
        viewModelScope.launch {
            mutableUiState.update { it.copy(isSaving = true, errorMessage = null) }
            projectRepository.update(spaceId, projectId, project)
                .onSuccess { updated ->
                    mutableUiState.update {
                        it.copy(isSaving = false, project = updated, errorMessage = null)
                    }
                }
                .onFailure { throwable ->
                    mutableUiState.update {
                        it.copy(isSaving = false, errorMessage = throwable.message ?: "更新项目失败")
                    }
                }
        }
    }

    fun archive(spaceId: Long, projectId: Long, archived: Boolean) {
        viewModelScope.launch {
            mutableUiState.update { it.copy(isSaving = true, errorMessage = null) }
            projectRepository.archive(spaceId, projectId, archived)
                .onSuccess { updated ->
                    mutableUiState.update {
                        it.copy(isSaving = false, project = updated, errorMessage = null)
                    }
                }
                .onFailure { throwable ->
                    mutableUiState.update {
                        it.copy(isSaving = false, errorMessage = throwable.message ?: "更新归档状态失败")
                    }
                }
        }
    }

    fun addMember(userId: Long) {
        val spaceId = currentSpaceId ?: return
        val projectId = currentProjectId ?: return
        viewModelScope.launch {
            mutableUiState.update { it.copy(isMemberMutating = true, errorMessage = null) }
            projectRepository.addMember(
                spaceId = spaceId,
                projectId = projectId,
                member = ProjectMemberSave(userId = userId, role = ProjectMemberRole.MEMBER)
            ).onSuccess { member ->
                mutableUiState.update { state ->
                    val updatedMembers = (state.members + member).sortedBy { it.displayName() }
                    state.copy(
                        isMemberMutating = false,
                        members = updatedMembers,
                        availableMembers = availableMembers(updatedMembers, state.availableMembers),
                        errorMessage = null
                    )
                }
            }.onFailure { throwable ->
                mutableUiState.update {
                    it.copy(
                        isMemberMutating = false,
                        errorMessage = throwable.message ?: "添加项目成员失败"
                    )
                }
            }
        }
    }

    fun removeMember(member: ProjectMember) {
        val spaceId = currentSpaceId ?: return
        val projectId = currentProjectId ?: return
        viewModelScope.launch {
            mutableUiState.update { it.copy(isMemberMutating = true, errorMessage = null) }
            projectRepository.removeMember(spaceId, projectId, member.userId)
                .onSuccess {
                    mutableUiState.update { state ->
                        val updatedMembers = state.members.filterNot { current -> current.userId == member.userId }
                        state.copy(
                            isMemberMutating = false,
                            members = updatedMembers,
                            availableMembers = availableMembers(updatedMembers, state.availableMembers + member.toSpaceMember(spaceId)),
                            errorMessage = null
                        )
                    }
                }
                .onFailure { throwable ->
                    mutableUiState.update {
                        it.copy(
                            isMemberMutating = false,
                            errorMessage = throwable.message ?: "移除项目成员失败"
                        )
                    }
                }
        }
    }

    private fun availableMembers(
        projectMembers: List<ProjectMember>,
        spaceMembers: List<SpaceMember>
    ): List<SpaceMember> {
        val projectMemberIds = projectMembers.map { it.userId }.toSet()
        return spaceMembers
            .filterNot { member -> member.userId in projectMemberIds }
            .sortedBy { member ->
                member.nickname?.takeIf { it.isNotBlank() } ?: member.username
            }
    }

    private fun ProjectMember.displayName(): String {
        return nickname?.takeIf { it.isNotBlank() } ?: username.ifBlank { "成员" }
    }

    private fun ProjectMember.toSpaceMember(spaceId: Long): SpaceMember {
        return SpaceMember(
            spaceId = spaceId,
            userId = userId,
            username = username,
            nickname = nickname,
            email = email,
            avatarUrl = avatarUrl,
            roleCode = role.name,
            roleName = role.label(),
            gmtJoined = joinedAt,
            online = false
        )
    }

    private fun ProjectMemberRole.label(): String {
        return when (this) {
            ProjectMemberRole.OWNER -> "负责人"
            ProjectMemberRole.MEMBER -> "成员"
        }
    }

    private companion object {
        const val TASK_LIMIT = 10L
        const val NOTE_LIMIT = 10L
    }
}
