package com.notaskflow.feature.note

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notaskflow.domain.model.Note
import com.notaskflow.domain.model.NoteQuery
import com.notaskflow.domain.model.Notebook
import com.notaskflow.domain.model.NotebookSave
import com.notaskflow.domain.note.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NoteListUiState(
    val isLoading: Boolean = false,
    val notebooks: List<Notebook> = emptyList(),
    val notes: List<Note> = emptyList(),
    val selectedNotebookId: Long? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class NoteListViewModel @Inject constructor(
    private val noteRepository: NoteRepository
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(NoteListUiState())
    val uiState: StateFlow<NoteListUiState> = mutableUiState

    private var currentSpaceId: Long? = null

    fun load(spaceId: Long) {
        currentSpaceId = spaceId
        viewModelScope.launch {
            mutableUiState.update { it.copy(isLoading = true, errorMessage = null) }
            val notebooksResult = noteRepository.notebooks(spaceId)
            val notesResult = noteRepository.page(
                spaceId = spaceId,
                query = NoteQuery(notebookId = mutableUiState.value.selectedNotebookId)
            )
            mutableUiState.update { state ->
                state.copy(
                    isLoading = false,
                    notebooks = notebooksResult.getOrDefault(state.notebooks),
                    notes = notesResult.getOrNull()?.list ?: state.notes,
                    errorMessage = notebooksResult.exceptionOrNull()?.message
                        ?: notesResult.exceptionOrNull()?.message
                )
            }
        }
    }

    fun selectNotebook(notebookId: Long?) {
        mutableUiState.update { it.copy(selectedNotebookId = notebookId) }
        currentSpaceId?.let { load(it) }
    }

    fun createNotebook(name: String) {
        val spaceId = currentSpaceId ?: return
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            mutableUiState.update { it.copy(errorMessage = "请输入笔记本名称") }
            return
        }
        viewModelScope.launch {
            noteRepository.createNotebook(spaceId, NotebookSave(name = trimmedName))
                .onSuccess {
                    load(spaceId)
                }
                .onFailure { throwable ->
                    mutableUiState.update { it.copy(errorMessage = throwable.message ?: "创建笔记本失败") }
                }
        }
    }

    fun moveNotebook(notebook: Notebook, parentId: Long?) {
        val spaceId = currentSpaceId ?: return
        if (notebook.id == parentId || notebook.children.containsNotebook(parentId)) {
            mutableUiState.update { it.copy(errorMessage = "不能移动到自身或子笔记本下") }
            return
        }
        viewModelScope.launch {
            noteRepository.updateNotebook(
                spaceId = spaceId,
                id = notebook.id,
                notebook = NotebookSave(parentId = parentId, name = notebook.name, sortOrder = notebook.sortOrder)
            ).onSuccess {
                load(spaceId)
            }.onFailure { throwable ->
                mutableUiState.update { it.copy(errorMessage = throwable.message ?: "移动笔记本失败") }
            }
        }
    }

    fun deleteNotebook(notebook: Notebook) {
        val spaceId = currentSpaceId ?: return
        viewModelScope.launch {
            noteRepository.deleteNotebook(spaceId, notebook.id)
                .onSuccess {
                    mutableUiState.update { state ->
                        state.copy(
                            selectedNotebookId = state.selectedNotebookId.takeIf { it != notebook.id },
                            errorMessage = null
                        )
                    }
                    load(spaceId)
                }
                .onFailure { throwable ->
                    mutableUiState.update { it.copy(errorMessage = throwable.message ?: "删除笔记本失败") }
                }
        }
    }

    fun delete(note: Note) {
        val spaceId = currentSpaceId ?: return
        viewModelScope.launch {
            noteRepository.delete(spaceId, note.id)
                .onSuccess {
                    mutableUiState.update { state ->
                        state.copy(
                            notes = state.notes.filterNot { current -> current.id == note.id },
                            errorMessage = null
                        )
                    }
                }
                .onFailure { throwable ->
                    mutableUiState.update { it.copy(errorMessage = throwable.message ?: "删除笔记失败") }
                }
        }
    }
}

private fun List<Notebook>.containsNotebook(notebookId: Long?): Boolean {
    if (notebookId == null) {
        return false
    }
    return any { notebook ->
        notebook.id == notebookId || notebook.children.containsNotebook(notebookId)
    }
}
