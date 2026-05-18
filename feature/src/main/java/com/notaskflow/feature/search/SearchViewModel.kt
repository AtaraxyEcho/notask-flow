package com.notaskflow.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notaskflow.domain.file.FileRepository
import com.notaskflow.domain.model.ManagedFile
import com.notaskflow.domain.model.ManagedFileQuery
import com.notaskflow.domain.model.Note
import com.notaskflow.domain.model.NoteQuery
import com.notaskflow.domain.model.Project
import com.notaskflow.domain.model.ProjectQuery
import com.notaskflow.domain.model.Page
import com.notaskflow.domain.model.Task
import com.notaskflow.domain.model.TaskQuery
import com.notaskflow.domain.model.Todo
import com.notaskflow.domain.model.TodoQuery
import com.notaskflow.domain.note.NoteRepository
import com.notaskflow.domain.project.ProjectRepository
import com.notaskflow.domain.task.TaskRepository
import com.notaskflow.domain.todo.TodoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val notes: List<Note> = emptyList(),
    val tasks: List<Task> = emptyList(),
    val todos: List<Todo> = emptyList(),
    val projects: List<Project> = emptyList(),
    val files: List<ManagedFile> = emptyList(),
    val errorMessage: String? = null,
    val hasSearched: Boolean = false
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val taskRepository: TaskRepository,
    private val todoRepository: TodoRepository,
    private val projectRepository: ProjectRepository,
    private val fileRepository: FileRepository
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = mutableUiState

    fun updateQuery(value: String) {
        mutableUiState.update { it.copy(query = value, errorMessage = null) }
    }

    fun loadDefault(spaceId: Long?, includeProjects: Boolean) {
        search(spaceId, includeProjects)
    }

    fun search(spaceId: Long?, includeProjects: Boolean) {
        val currentSpaceId = spaceId
        val keyword = mutableUiState.value.query.trim()
        if (currentSpaceId == null) {
            mutableUiState.update { it.copy(errorMessage = "请先选择空间", hasSearched = true) }
            return
        }
        viewModelScope.launch {
            mutableUiState.update { it.copy(isLoading = true, errorMessage = null, hasSearched = true) }
            val notesResult = noteRepository.page(
                spaceId = currentSpaceId,
                query = NoteQuery(pageSize = SEARCH_PAGE_SIZE, keyword = keyword.takeIf { it.isNotBlank() })
            )
            val tasksResult = taskRepository.page(
                spaceId = currentSpaceId,
                query = TaskQuery(pageSize = SEARCH_PAGE_SIZE, keyword = keyword.takeIf { it.isNotBlank() })
            )
            val todosResult = todoRepository.page(
                spaceId = currentSpaceId,
                query = TodoQuery(pageSize = SEARCH_PAGE_SIZE, keyword = keyword.takeIf { it.isNotBlank() })
            )
            val projectsResult = if (includeProjects) {
                projectRepository.page(
                    spaceId = currentSpaceId,
                    query = ProjectQuery(pageSize = SEARCH_PAGE_SIZE, keyword = keyword.takeIf { it.isNotBlank() })
                )
            } else {
                Result.success(Page(total = 0, pageNum = 1, pageSize = SEARCH_PAGE_SIZE, list = emptyList<Project>()))
            }
            val filesResult = fileRepository.page(
                spaceId = currentSpaceId,
                query = ManagedFileQuery(pageSize = SEARCH_PAGE_SIZE, keyword = keyword.takeIf { it.isNotBlank() })
            )
            mutableUiState.update {
                it.copy(
                    isLoading = false,
                    notes = notesResult.getOrNull()?.list.orEmpty(),
                    tasks = tasksResult.getOrNull()?.list.orEmpty(),
                    todos = todosResult.getOrNull()?.list.orEmpty(),
                    projects = projectsResult.getOrNull()?.list.orEmpty(),
                    files = filesResult.getOrNull()?.list.orEmpty(),
                    errorMessage = notesResult.exceptionOrNull()?.message
                        ?: tasksResult.exceptionOrNull()?.message
                        ?: todosResult.exceptionOrNull()?.message
                        ?: projectsResult.exceptionOrNull()?.message
                        ?: filesResult.exceptionOrNull()?.message
                )
            }
        }
    }

    private companion object {
        const val SEARCH_PAGE_SIZE = 20L
    }
}
