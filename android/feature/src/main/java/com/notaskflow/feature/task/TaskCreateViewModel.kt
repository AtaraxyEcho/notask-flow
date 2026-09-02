package com.notaskflow.feature.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notaskflow.core.common.normalizeDateTimeInput
import com.notaskflow.domain.model.Project
import com.notaskflow.domain.model.ProjectQuery
import com.notaskflow.domain.model.SpaceMember
import com.notaskflow.domain.model.TaskAssignmentCreate
import com.notaskflow.domain.model.TaskCreate
import com.notaskflow.domain.model.TaskMode
import com.notaskflow.domain.model.TaskPriority
import com.notaskflow.domain.project.ProjectRepository
import com.notaskflow.domain.space.SpaceRepository
import com.notaskflow.domain.task.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TaskCreateUiState(
    val title: String = "",
    val description: String = "",
    val mode: TaskMode = TaskMode.ASSIGNED,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val deadline: String = "",
    val isPersonalSpace: Boolean = false,
    val currentUserId: Long? = null,
    val projects: List<Project> = emptyList(),
    val selectedProjectId: Long? = null,
    val members: List<SpaceMember> = emptyList(),
    val selectedAssigneeIds: Set<Long> = emptySet(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

sealed interface TaskCreateEffect {
    data class Created(
        val taskId: Long
    ) : TaskCreateEffect
}

@HiltViewModel
class TaskCreateViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val spaceRepository: SpaceRepository,
    private val projectRepository: ProjectRepository
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(TaskCreateUiState())
    val uiState: StateFlow<TaskCreateUiState> = mutableUiState

    private val mutableEffect = MutableSharedFlow<TaskCreateEffect>()
    val effect: SharedFlow<TaskCreateEffect> = mutableEffect.asSharedFlow()

    private var currentSpaceId: Long? = null

    fun bindSpace(spaceId: Long?, isPersonalSpace: Boolean, currentUserId: Long?) {
        currentSpaceId = spaceId
        mutableUiState.update {
            it.copy(
                isPersonalSpace = isPersonalSpace,
                currentUserId = currentUserId,
                mode = if (isPersonalSpace) TaskMode.ASSIGNED else it.mode,
                projects = if (isPersonalSpace) emptyList() else it.projects,
                selectedProjectId = if (isPersonalSpace) null else it.selectedProjectId,
                members = if (isPersonalSpace) emptyList() else it.members,
                selectedAssigneeIds = if (isPersonalSpace) {
                    currentUserId?.let { userId -> setOf(userId) } ?: emptySet()
                } else {
                    it.selectedAssigneeIds
                },
                errorMessage = null
            )
        }
        if (spaceId == null) {
            return
        }
        if (isPersonalSpace) {
            return
        }
        viewModelScope.launch {
            val membersResult = spaceRepository.listMembers(spaceId)
            val projectsResult = projectRepository.page(
                spaceId = spaceId,
                query = ProjectQuery(pageSize = PROJECT_SELECTOR_LIMIT, archived = false)
            )
            mutableUiState.update {
                it.copy(
                    members = membersResult.getOrDefault(it.members),
                    projects = projectsResult.getOrNull()?.list ?: it.projects,
                    errorMessage = membersResult.exceptionOrNull()?.message
                        ?: projectsResult.exceptionOrNull()?.message
                )
            }
        }
    }

    fun updateTitle(value: String) {
        mutableUiState.update { it.copy(title = value, errorMessage = null) }
    }

    fun updateDescription(value: String) {
        mutableUiState.update { it.copy(description = value, errorMessage = null) }
    }

    fun updateMode(value: TaskMode) {
        mutableUiState.update { it.copy(mode = value, errorMessage = null) }
    }

    fun toggleAssignee(userId: Long) {
        mutableUiState.update { state ->
            val selected = if (state.selectedAssigneeIds.contains(userId)) {
                state.selectedAssigneeIds - userId
            } else {
                state.selectedAssigneeIds + userId
            }
            state.copy(selectedAssigneeIds = selected, errorMessage = null)
        }
    }

    fun updatePriority(value: TaskPriority) {
        mutableUiState.update { it.copy(priority = value, errorMessage = null) }
    }

    fun updateDeadline(value: String) {
        mutableUiState.update { it.copy(deadline = value, errorMessage = null) }
    }

    fun selectProject(projectId: Long?) {
        mutableUiState.update { it.copy(selectedProjectId = projectId, errorMessage = null) }
    }

    fun save() {
        val spaceId = currentSpaceId ?: run {
            mutableUiState.update { it.copy(errorMessage = "请先选择空间") }
            return
        }
        val state = mutableUiState.value
        val title = state.title.trim()
        if (title.isBlank()) {
            mutableUiState.update { it.copy(errorMessage = "请输入任务标题") }
            return
        }
        val deadline = normalizeDeadline(state.deadline.trim())
        if (deadline == INVALID_DEADLINE) {
            mutableUiState.update { it.copy(errorMessage = "截止时间格式应为 yyyy-MM-dd 或 yyyy-MM-dd:HH:mm:ss") }
            return
        }
        if (state.isPersonalSpace && state.currentUserId == null) {
            mutableUiState.update { it.copy(errorMessage = "当前账号信息未就绪，请稍后重试") }
            return
        }
        val projectId = if (state.isPersonalSpace) null else state.selectedProjectId
        val assignments = if (state.isPersonalSpace) {
            listOf(
                TaskAssignmentCreate(
                    userId = requireNotNull(state.currentUserId),
                    responsibility = state.description.trim().takeIf { it.isNotBlank() } ?: title,
                    isRequired = true
                )
            )
        } else if (state.mode == TaskMode.ASSIGNED) {
            state.members
                .filter { member -> state.selectedAssigneeIds.contains(member.userId) }
                .map { member ->
                    TaskAssignmentCreate(
                        userId = member.userId,
                        responsibility = "参与处理",
                        isRequired = true
                    )
                }
        } else {
            emptyList()
        }
        val mode = if (state.isPersonalSpace) TaskMode.ASSIGNED else state.mode
        viewModelScope.launch {
            mutableUiState.update { it.copy(isSaving = true, errorMessage = null) }
            val request = TaskCreate(
                title = title,
                description = state.description.trim().takeIf { it.isNotBlank() },
                mode = mode,
                priority = state.priority,
                deadline = deadline,
                projectId = projectId,
                assignments = assignments
            )
            taskRepository.create(spaceId, request)
                .onSuccess { task ->
                    mutableUiState.update { it.copy(isSaving = false) }
                    mutableEffect.emit(TaskCreateEffect.Created(task.id))
                }
                .onFailure { throwable ->
                    mutableUiState.update {
                        it.copy(isSaving = false, errorMessage = throwable.message ?: "创建任务失败")
                    }
                }
        }
    }

    private fun normalizeDeadline(value: String): String? {
        if (value.isBlank()) {
            return null
        }
        return normalizeDateTimeInput(value) ?: INVALID_DEADLINE
    }

    private companion object {
        const val INVALID_DEADLINE = "__invalid_deadline__"
        const val PROJECT_SELECTOR_LIMIT = 100L
    }
}
