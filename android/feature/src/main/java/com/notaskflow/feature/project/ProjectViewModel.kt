package com.notaskflow.feature.project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notaskflow.domain.model.MemberTaskLoad
import com.notaskflow.domain.model.Project
import com.notaskflow.domain.model.ProjectQuery
import com.notaskflow.domain.model.ProjectSave
import com.notaskflow.domain.model.RoleCompletion
import com.notaskflow.domain.model.StatsActivity
import com.notaskflow.domain.model.TaskTrend
import com.notaskflow.domain.project.ProjectRepository
import com.notaskflow.domain.stats.StatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProjectUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val projects: List<Project> = emptyList(),
    val total: Long = 0,
    val trends: List<TaskTrend> = emptyList(),
    val roleCompletions: List<RoleCompletion> = emptyList(),
    val loads: List<MemberTaskLoad> = emptyList(),
    val activities: List<StatsActivity> = emptyList(),
    val statsErrorMessage: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class ProjectViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val statsRepository: StatsRepository
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ProjectUiState())
    val uiState: StateFlow<ProjectUiState> = mutableUiState

    private var currentSpaceId: Long? = null

    fun load(spaceId: Long) {
        currentSpaceId = spaceId
        viewModelScope.launch {
            mutableUiState.update { it.copy(isLoading = true, errorMessage = null) }
            val projectsResult = projectRepository.page(spaceId, ProjectQuery(archived = false))
            val trendsResult = statsRepository.trend(spaceId)
            val roleResult = statsRepository.roleCompletion(spaceId)
            val loadResult = statsRepository.load(spaceId)
            val activityResult = statsRepository.activities(spaceId)
            mutableUiState.update { state ->
                state.copy(
                    isLoading = false,
                    projects = projectsResult.getOrNull()?.list ?: state.projects,
                    total = projectsResult.getOrNull()?.total ?: state.total,
                    trends = trendsResult.getOrDefault(emptyList()),
                    roleCompletions = roleResult.getOrDefault(emptyList()),
                    loads = loadResult.getOrDefault(emptyList()),
                    activities = activityResult.getOrDefault(emptyList()),
                    statsErrorMessage = trendsResult.exceptionOrNull()?.message
                        ?: roleResult.exceptionOrNull()?.message
                        ?: loadResult.exceptionOrNull()?.message
                        ?: activityResult.exceptionOrNull()?.message,
                    errorMessage = projectsResult.exceptionOrNull()?.message
                )
            }
        }
    }

    fun create(project: ProjectSave) {
        val spaceId = currentSpaceId ?: return
        viewModelScope.launch {
            mutableUiState.update { it.copy(isSaving = true, errorMessage = null) }
            projectRepository.create(spaceId, project)
                .onSuccess { created ->
                    mutableUiState.update { state ->
                        state.copy(
                            isSaving = false,
                            projects = listOf(created) + state.projects,
                            total = state.total + 1,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { throwable ->
                    mutableUiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = throwable.message ?: "创建项目失败"
                        )
                    }
                }
        }
    }

    fun delete(project: Project) {
        val spaceId = currentSpaceId ?: return
        viewModelScope.launch {
            mutableUiState.update { it.copy(isSaving = true, errorMessage = null) }
            projectRepository.delete(spaceId, project.id)
                .onSuccess {
                    mutableUiState.update { state ->
                        state.copy(
                            isSaving = false,
                            projects = state.projects.filterNot { current -> current.id == project.id },
                            total = (state.total - 1).coerceAtLeast(0),
                            errorMessage = null
                        )
                    }
                }
                .onFailure { throwable ->
                    mutableUiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = throwable.message ?: "删除项目失败"
                        )
                    }
                }
        }
    }
}
