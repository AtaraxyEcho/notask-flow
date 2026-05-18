package com.notaskflow.feature.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notaskflow.core.common.normalizeDateTimeInput
import com.notaskflow.domain.model.Todo
import com.notaskflow.domain.model.TodoQuery
import com.notaskflow.domain.model.TodoSave
import com.notaskflow.domain.todo.TodoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TodoListUiState(
    val isLoading: Boolean = false,
    val todos: List<Todo> = emptyList(),
    val selectedFilter: TodoFilter = TodoFilter.ALL,
    val searchQuery: String = "",
    val errorMessage: String? = null
)

enum class TodoFilter(val label: String) {
    ALL("全部"),
    ACTIVE("未完成"),
    COMPLETED("已完成")
}

@HiltViewModel
class TodoListViewModel @Inject constructor(
    private val todoRepository: TodoRepository
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(TodoListUiState())
    val uiState: StateFlow<TodoListUiState> = mutableUiState

    private var currentSpaceId: Long? = null

    fun load(spaceId: Long) {
        currentSpaceId = spaceId
        refresh()
    }

    fun updateSearchQuery(value: String) {
        mutableUiState.update { it.copy(searchQuery = value) }
        refresh()
    }

    fun selectFilter(filter: TodoFilter) {
        mutableUiState.update { it.copy(selectedFilter = filter) }
        refresh()
    }

    fun toggleComplete(todo: Todo) {
        val spaceId = currentSpaceId ?: return
        viewModelScope.launch {
            val result = if (todo.isCompleted) {
                todoRepository.uncomplete(spaceId, todo.id)
            } else {
                todoRepository.complete(spaceId, todo.id)
            }
            result.onSuccess { refresh() }
                .onFailure { throwable ->
                    mutableUiState.update {
                        it.copy(errorMessage = throwable.message ?: "更新待办状态失败")
                    }
                }
        }
    }

    fun delete(todo: Todo) {
        val spaceId = currentSpaceId ?: return
        viewModelScope.launch {
            todoRepository.delete(spaceId, todo.id)
                .onSuccess { refresh() }
                .onFailure { throwable ->
                    mutableUiState.update {
                        it.copy(errorMessage = throwable.message ?: "删除待办失败")
                    }
                }
        }
    }

    fun create(title: String, deadline: String?) {
        val spaceId = currentSpaceId ?: return
        val normalizedTitle = title.trim()
        if (normalizedTitle.isBlank()) {
            mutableUiState.update { it.copy(errorMessage = "请输入待办标题") }
            return
        }
        val normalizedDeadline = normalizeDeadline(deadline.orEmpty())
        if (normalizedDeadline == INVALID_DEADLINE) {
            mutableUiState.update { it.copy(errorMessage = "截止时间格式应为 yyyy-MM-dd 或 yyyy-MM-dd:HH:mm:ss") }
            return
        }
        viewModelScope.launch {
            todoRepository.create(
                spaceId,
                TodoSave(
                    title = normalizedTitle,
                    deadline = normalizedDeadline
                )
            )
                .onSuccess { refresh() }
                .onFailure { throwable ->
                    mutableUiState.update {
                        it.copy(errorMessage = throwable.message ?: "创建待办失败")
                    }
                }
        }
    }

    fun update(todo: Todo, title: String, deadline: String?) {
        val spaceId = currentSpaceId ?: return
        val normalizedTitle = title.trim()
        if (normalizedTitle.isBlank()) {
            mutableUiState.update { it.copy(errorMessage = "请输入待办标题") }
            return
        }
        val normalizedDeadline = normalizeDeadline(deadline.orEmpty())
        if (normalizedDeadline == INVALID_DEADLINE) {
            mutableUiState.update { it.copy(errorMessage = "截止时间格式应为 yyyy-MM-dd 或 yyyy-MM-dd:HH:mm:ss") }
            return
        }
        viewModelScope.launch {
            todoRepository.update(
                spaceId = spaceId,
                id = todo.id,
                todo = TodoSave(
                    title = normalizedTitle,
                    deadline = normalizedDeadline
                )
            ).onSuccess { refresh() }
                .onFailure { throwable ->
                    mutableUiState.update {
                        it.copy(errorMessage = throwable.message ?: "更新待办失败")
                    }
                }
        }
    }

    private fun refresh() {
        val spaceId = currentSpaceId ?: return
        val state = mutableUiState.value
        viewModelScope.launch {
            mutableUiState.update { it.copy(isLoading = true, errorMessage = null) }
            todoRepository.page(
                spaceId = spaceId,
                query = TodoQuery(
                    keyword = state.searchQuery.takeIf { it.isNotBlank() },
                    isCompleted = when (state.selectedFilter) {
                        TodoFilter.ALL -> null
                        TodoFilter.ACTIVE -> false
                        TodoFilter.COMPLETED -> true
                    }
                )
            ).onSuccess { page ->
                mutableUiState.update {
                    it.copy(isLoading = false, todos = page.list, errorMessage = null)
                }
            }.onFailure { throwable ->
                mutableUiState.update {
                    it.copy(isLoading = false, errorMessage = throwable.message ?: "加载待办失败")
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
    }
}
