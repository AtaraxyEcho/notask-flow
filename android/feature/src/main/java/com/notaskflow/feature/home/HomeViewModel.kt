package com.notaskflow.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notaskflow.domain.model.Note
import com.notaskflow.domain.model.NoteQuery
import com.notaskflow.domain.model.PersonalNoteTrend
import com.notaskflow.domain.model.Todo
import com.notaskflow.domain.model.TodoQuery
import com.notaskflow.domain.note.NoteRepository
import com.notaskflow.domain.stats.StatsRepository
import com.notaskflow.domain.todo.TodoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class PersonalHomeUiState {
    data object Loading : PersonalHomeUiState()

    data class Content(
        val recentNotes: List<Note> = emptyList(),
        val pendingTodos: List<Todo> = emptyList(),
        val personalNoteTrends: List<PersonalNoteTrend> = emptyList(),
        val noteTotal: Long = 0,
        val pendingTodoTotal: Long = 0,
        val isRefreshing: Boolean = false,
        val errorMessage: String? = null
    ) : PersonalHomeUiState()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val todoRepository: TodoRepository,
    private val statsRepository: StatsRepository
) : ViewModel() {
    private val mutableUiState = MutableStateFlow<PersonalHomeUiState>(PersonalHomeUiState.Loading)
    val uiState: StateFlow<PersonalHomeUiState> = mutableUiState

    fun load(spaceId: Long) {
        val previousContent = mutableUiState.value as? PersonalHomeUiState.Content
        mutableUiState.value = previousContent?.copy(isRefreshing = true, errorMessage = null)
            ?: PersonalHomeUiState.Loading

        viewModelScope.launch {
            val notesResult = noteRepository.page(
                spaceId = spaceId,
                query = NoteQuery(pageSize = RECENT_NOTE_LIMIT)
            )
            val todosResult = todoRepository.page(
                spaceId = spaceId,
                query = TodoQuery(pageSize = PENDING_TODO_LIMIT, isCompleted = false)
            )
            val trendsResult = statsRepository.personalNoteTrend()
            mutableUiState.value = PersonalHomeUiState.Content(
                recentNotes = notesResult.getOrNull()?.list ?: previousContent?.recentNotes.orEmpty(),
                pendingTodos = todosResult.getOrNull()?.list ?: previousContent?.pendingTodos.orEmpty(),
                personalNoteTrends = trendsResult.getOrNull()?.take(7) ?: previousContent?.personalNoteTrends.orEmpty(),
                noteTotal = notesResult.getOrNull()?.total ?: previousContent?.noteTotal ?: 0,
                pendingTodoTotal = todosResult.getOrNull()?.total ?: previousContent?.pendingTodoTotal ?: 0,
                isRefreshing = false,
                errorMessage = notesResult.exceptionOrNull()?.message
                    ?: todosResult.exceptionOrNull()?.message
                    ?: trendsResult.exceptionOrNull()?.message
            )
        }
    }

    private companion object {
        const val RECENT_NOTE_LIMIT = 3L
        const val PENDING_TODO_LIMIT = 5L
    }
}
