package com.notaskflow.feature.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notaskflow.domain.model.Task
import com.notaskflow.domain.model.TaskQuery
import com.notaskflow.domain.model.TaskStatus
import com.notaskflow.domain.task.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TaskListUiState(
    val isLoading: Boolean = false,
    val tasks: List<Task> = emptyList(),
    val selectedFilter: TaskFilter = TaskFilter.ALL,
    val errorMessage: String? = null
)

enum class TaskFilter(val label: String, val status: TaskStatus?) {
    ALL("全部", null),
    PENDING("待开始", TaskStatus.PENDING),
    IN_PROGRESS("进行中", TaskStatus.IN_PROGRESS),
    COMPLETED("已完成", TaskStatus.COMPLETED),
    CANCELLED("已取消", TaskStatus.CANCELLED)
}

@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(TaskListUiState())
    val uiState: StateFlow<TaskListUiState> = mutableUiState

    private var currentSpaceId: Long? = null
    private var kanbanModeEnabled = false

    fun load(spaceId: Long) {
        currentSpaceId = spaceId
        refresh()
    }

    fun setKanbanMode(enabled: Boolean) {
        if (kanbanModeEnabled == enabled) {
            return
        }
        kanbanModeEnabled = enabled
        mutableUiState.update { state ->
            when {
                enabled && state.selectedFilter == TaskFilter.ALL -> state.copy(selectedFilter = TaskFilter.PENDING)
                !enabled -> state.copy(selectedFilter = TaskFilter.ALL)
                else -> state
            }
        }
        refresh()
    }

    fun selectFilter(filter: TaskFilter) {
        mutableUiState.update { it.copy(selectedFilter = filter) }
        if (!kanbanModeEnabled) {
            refresh()
        }
    }

    fun delete(task: Task) {
        val spaceId = currentSpaceId ?: return
        viewModelScope.launch {
            taskRepository.delete(spaceId, task.id)
                .onSuccess {
                    mutableUiState.update { state ->
                        state.copy(
                            tasks = state.tasks.filterNot { current -> current.id == task.id },
                            errorMessage = null
                        )
                    }
                }
                .onFailure { throwable ->
                    mutableUiState.update { it.copy(errorMessage = throwable.message ?: "删除任务失败") }
                }
        }
    }

    private fun refresh() {
        val spaceId = currentSpaceId ?: return
        val filter = mutableUiState.value.selectedFilter
        val queryStatus = if (kanbanModeEnabled) null else filter.status
        val pageSize = if (kanbanModeEnabled) KANBAN_PAGE_SIZE else DEFAULT_PAGE_SIZE
        viewModelScope.launch {
            mutableUiState.update { it.copy(isLoading = true, errorMessage = null) }
            taskRepository.page(
                spaceId = spaceId,
                query = TaskQuery(
                    pageSize = pageSize,
                    status = queryStatus
                )
            ).onSuccess { page ->
                mutableUiState.update {
                    it.copy(isLoading = false, tasks = page.list, errorMessage = null)
                }
            }.onFailure { throwable ->
                mutableUiState.update {
                    it.copy(isLoading = false, errorMessage = throwable.message ?: "加载任务失败")
                }
            }
        }
    }

    private companion object {
        const val DEFAULT_PAGE_SIZE = 20L
        const val KANBAN_PAGE_SIZE = 100L
    }
}
