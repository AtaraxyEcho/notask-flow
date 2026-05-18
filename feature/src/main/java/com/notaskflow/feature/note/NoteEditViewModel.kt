package com.notaskflow.feature.note

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notaskflow.core.datastore.TokenManager
import com.notaskflow.domain.file.FileRepository
import com.notaskflow.domain.model.CollabContentSave
import com.notaskflow.domain.model.ManagedFile
import com.notaskflow.domain.model.ManagedFileQuery
import com.notaskflow.domain.model.ManagedFileUpload
import com.notaskflow.domain.model.NoteAttachment
import com.notaskflow.domain.model.NoteAttachmentUpload
import com.notaskflow.domain.model.NoteDraft
import com.notaskflow.domain.model.NoteExportFile
import com.notaskflow.domain.model.NoteExportFormat
import com.notaskflow.domain.model.NoteHistory
import com.notaskflow.domain.model.NoteSave
import com.notaskflow.domain.model.Notebook
import com.notaskflow.domain.model.NotebookSave
import com.notaskflow.domain.note.NoteDraftRepository
import com.notaskflow.domain.note.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class NoteEditUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val title: String = "",
    val content: String = "",
    val contentHtml: String? = null,
    val documentDate: String? = null,
    val notebooks: List<Notebook> = emptyList(),
    val selectedNotebookId: Long? = null,
    val histories: List<NoteHistory> = emptyList(),
    val attachments: List<NoteAttachment> = emptyList(),
    val referenceFiles: List<ManagedFile> = emptyList(),
    val previewHistory: NoteHistory? = null,
    val collabEnabled: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val isMetadataLoading: Boolean = false,
    val isAttachmentUploading: Boolean = false,
    val isInlineImageUploading: Boolean = false,
    val isExporting: Boolean = false,
    val isReferenceLoading: Boolean = false,
    val activeAttachmentId: Long? = null,
    val authToken: String? = null,
    val errorMessage: String? = null,
    val savedMessage: String? = null
)

sealed interface NoteEditEffect {
    data object Saved : NoteEditEffect
    data object Deleted : NoteEditEffect
    data class ShareReady(val shareCode: String) : NoteEditEffect
    data class ExportReady(val file: NoteExportFile) : NoteEditEffect
    data class ImageReady(
        val url: String,
        val alt: String,
        val managedFileId: Long?,
        val attachmentId: Long?
    ) : NoteEditEffect

    data class FileReady(
        val kind: InlineFileKind,
        val name: String,
        val url: String,
        val mimeType: String?,
        val fileSize: Long,
        val managedFileId: Long?,
        val attachmentId: Long?
    ) : NoteEditEffect

    data class EditorContentReady(val content: String, val contentHtml: String?) : NoteEditEffect
    data class CollabTicketReady(val requestId: String, val ticket: String) : NoteEditEffect
    data class CollabTicketFailed(val requestId: String, val message: String) : NoteEditEffect
}

enum class InlineFileKind {
    AUDIO,
    ATTACHMENT,
    REFERENCE
}

@HiltViewModel
class NoteEditViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val fileRepository: FileRepository,
    private val noteDraftRepository: NoteDraftRepository,
    private val tokenManager: TokenManager,
    @param:ApplicationContext private val context: Context
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(NoteEditUiState())
    val uiState: StateFlow<NoteEditUiState> = mutableUiState

    private val mutableEffect = MutableSharedFlow<NoteEditEffect>()
    val effect: SharedFlow<NoteEditEffect> = mutableEffect.asSharedFlow()

    private var spaceId: Long? = null
    private var noteId: Long? = null
    private var draftSavingEnabled = false

    fun load(spaceId: Long, noteId: Long?) {
        this.spaceId = spaceId
        this.noteId = noteId
        draftSavingEnabled = false
        viewModelScope.launch {
            val authToken = tokenManager.currentToken()
            mutableUiState.update {
                it.copy(
                    isLoading = true,
                    histories = emptyList(),
                    attachments = emptyList(),
                    referenceFiles = emptyList(),
                    previewHistory = null,
                    hasUnsavedChanges = false,
                    isMetadataLoading = false,
                    isAttachmentUploading = false,
                    isInlineImageUploading = false,
                    isExporting = false,
                    isReferenceLoading = false,
                    activeAttachmentId = null,
                    authToken = authToken,
                    errorMessage = null,
                    savedMessage = null
                )
            }
            val notebooksResult = noteRepository.notebooks(spaceId)
            val notebooks = notebooksResult.getOrDefault(emptyList())
            val draft = noteDraftRepository.get(spaceId, noteId)
            if (noteId == null) {
                mutableUiState.update {
                    it.copy(
                        isLoading = false,
                        notebooks = notebooks,
                        title = draft?.title ?: it.title,
                        content = draft?.content ?: it.content,
                        contentHtml = draft?.content?.let(::renderPlainDocumentHtml),
                        documentDate = null,
                        selectedNotebookId = draft?.notebookId ?: notebooks.firstOrNull()?.id,
                        collabEnabled = it.collabEnabled,
                        errorMessage = notebooksResult.exceptionOrNull()?.message,
                        hasUnsavedChanges = draft != null,
                        savedMessage = null
                    )
                }
                draftSavingEnabled = true
                return@launch
            }
            val noteResult = noteRepository.get(spaceId, noteId)
            mutableUiState.update { state ->
                val note = noteResult.getOrNull()
                state.copy(
                    isLoading = false,
                    notebooks = notebooks,
                    title = draft?.title ?: note?.title ?: state.title,
                    content = draft?.content ?: note?.content ?: state.content,
                    contentHtml = draft?.content?.let(::renderPlainDocumentHtml)
                        ?: note?.contentHtml
                        ?: note?.content?.let(::renderPlainDocumentHtml),
                    documentDate = note?.gmtModified ?: note?.gmtCreate,
                    selectedNotebookId = draft?.notebookId ?: note?.notebookId ?: notebooks.firstOrNull()?.id,
                    collabEnabled = note?.collabEnabled == true,
                    errorMessage = notebooksResult.exceptionOrNull()?.message
                        ?: noteResult.exceptionOrNull()?.message,
                    hasUnsavedChanges = draft != null,
                    savedMessage = null
                )
            }
            draftSavingEnabled = true
        }
    }

    fun updateTitle(value: String) {
        mutableUiState.update { it.copy(title = value, hasUnsavedChanges = true, errorMessage = null, savedMessage = null) }
        saveDraftSnapshot()
    }

    fun updateContent(value: String, contentHtml: String? = null) {
        mutableUiState.update {
            it.copy(
                content = value,
                contentHtml = contentHtml ?: it.contentHtml,
                hasUnsavedChanges = true,
                errorMessage = null,
                savedMessage = null
            )
        }
        saveDraftSnapshot()
    }

    fun updateCollabContent(value: String, contentHtml: String? = null) {
        Log.d(
            COLLAB_LOG_TAG,
            "collab content changed noteId=$noteId textLength=${value.length} htmlLength=${contentHtml?.length ?: 0}"
        )
        mutableUiState.update {
            it.copy(
                content = value,
                contentHtml = contentHtml ?: it.contentHtml,
                hasUnsavedChanges = true,
                errorMessage = null,
                savedMessage = null
            )
        }
    }

    fun requestCollabTicket(requestId: String) {
        val currentSpaceId = spaceId
        val currentNoteId = noteId
        if (currentSpaceId == null || currentNoteId == null) {
            viewModelScope.launch {
                mutableEffect.emit(NoteEditEffect.CollabTicketFailed(requestId, "协作笔记信息不完整"))
            }
            return
        }
        viewModelScope.launch {
            Log.d(COLLAB_LOG_TAG, "request ticket noteId=$currentNoteId spaceId=$currentSpaceId")
            noteRepository.createCollabTicket(currentSpaceId, currentNoteId)
                .onSuccess { ticket ->
                    Log.d(COLLAB_LOG_TAG, "ticket ready requestId=$requestId expiresIn=${ticket.expiresIn}")
                    mutableEffect.emit(NoteEditEffect.CollabTicketReady(requestId, ticket.ticket))
                }
                .onFailure { throwable ->
                    if (throwable is CancellationException) {
                        Log.d(COLLAB_LOG_TAG, "ticket request cancelled requestId=$requestId")
                        return@onFailure
                    }
                    val message = throwable.message ?: "协作票据获取失败"
                    Log.w(COLLAB_LOG_TAG, "ticket failed requestId=$requestId message=$message")
                    mutableEffect.emit(NoteEditEffect.CollabTicketFailed(requestId, message))
                }
        }
    }

    fun saveSnapshot(content: String, contentHtml: String?) {
        mutableUiState.update {
            it.copy(
                content = content,
                contentHtml = contentHtml ?: it.contentHtml,
                hasUnsavedChanges = true,
                errorMessage = null,
                savedMessage = null
            )
        }
        save()
    }

    fun selectNotebook(notebookId: Long) {
        mutableUiState.update { it.copy(selectedNotebookId = notebookId, hasUnsavedChanges = true) }
        saveDraftSnapshot()
    }

    fun save(closeAfterSave: Boolean = false) {
        val currentSpaceId = spaceId ?: return
        val state = mutableUiState.value
        if (state.title.isBlank()) {
            mutableUiState.update { it.copy(errorMessage = "请输入笔记标题") }
            return
        }
        viewModelScope.launch {
            mutableUiState.update { it.copy(isSaving = true, errorMessage = null) }
            val notebookId = state.selectedNotebookId ?: ensureDefaultNotebook(currentSpaceId)
            if (notebookId == null) {
                mutableUiState.update { it.copy(isSaving = false, errorMessage = "默认笔记本创建失败，请稍后重试") }
                return@launch
            }
            val request = NoteSave(
                notebookId = notebookId,
                title = state.title.trim(),
                content = state.content,
                contentHtml = state.contentHtml ?: renderPlainDocumentHtml(state.content),
                isPublic = false
            )
            val currentNoteId = noteId
            val result = if (currentNoteId == null) {
                noteRepository.create(currentSpaceId, request)
            } else if (state.collabEnabled) {
                Log.d(COLLAB_LOG_TAG, "manual save collab content noteId=$currentNoteId")
                noteRepository.saveCollabContent(
                    spaceId = currentSpaceId,
                    id = currentNoteId,
                    content = CollabContentSave(
                        content = request.content.orEmpty(),
                        contentHtml = request.contentHtml
                    )
                )
            } else {
                noteRepository.update(currentSpaceId, currentNoteId, request)
            }
            result.onSuccess {
                noteDraftRepository.delete(currentSpaceId, noteId)
                noteId = it.id
                mutableUiState.update { state ->
                    state.copy(
                        isSaving = false,
                        documentDate = it.gmtModified ?: it.gmtCreate ?: state.documentDate,
                        hasUnsavedChanges = false,
                        savedMessage = "已保存"
                    )
                }
                if (closeAfterSave) {
                    mutableEffect.emit(NoteEditEffect.Saved)
                }
            }.onFailure { throwable ->
                mutableUiState.update {
                    it.copy(isSaving = false, errorMessage = throwable.message ?: "保存失败")
                }
            }
        }
    }

    private suspend fun ensureDefaultNotebook(spaceId: Long): Long? {
        val currentNotebooks = mutableUiState.value.notebooks
        currentNotebooks.firstOrNull()?.id?.let { notebookId -> return notebookId }
        return noteRepository.createNotebook(spaceId, NotebookSave(name = DEFAULT_NOTEBOOK_NAME))
            .onSuccess { notebook ->
                mutableUiState.update { state ->
                    state.copy(
                        notebooks = state.notebooks + notebook,
                        selectedNotebookId = notebook.id
                    )
                }
            }
            .getOrNull()
            ?.id
    }

    fun share(expireAt: String? = null) {
        val currentSpaceId = spaceId ?: return
        val currentNoteId = noteId ?: run {
            mutableUiState.update { it.copy(errorMessage = "请先保存笔记后再分享") }
            return
        }
        viewModelScope.launch {
            mutableUiState.update { it.copy(isSaving = true, errorMessage = null) }
            noteRepository.share(currentSpaceId, currentNoteId, expireAt)
                .onSuccess { note ->
                    mutableUiState.update {
                        it.copy(isSaving = false, savedMessage = "分享链接已生成")
                    }
                    note.shareCode?.let { shareCode ->
                        mutableEffect.emit(NoteEditEffect.ShareReady(shareCode))
                    }
                }
                .onFailure { throwable ->
                    mutableUiState.update {
                        it.copy(isSaving = false, errorMessage = throwable.message ?: "生成分享链接失败")
                    }
                }
        }
    }

    fun deleteCurrentNote() {
        val currentSpaceId = spaceId ?: return
        val currentNoteId = noteId ?: run {
            mutableUiState.update { it.copy(errorMessage = "当前笔记尚未保存，无需删除") }
            return
        }
        viewModelScope.launch {
            mutableUiState.update { it.copy(isSaving = true, errorMessage = null) }
            noteRepository.delete(currentSpaceId, currentNoteId)
                .onSuccess {
                    noteDraftRepository.delete(currentSpaceId, currentNoteId)
                    mutableUiState.update { it.copy(isSaving = false, hasUnsavedChanges = false) }
                    mutableEffect.emit(NoteEditEffect.Deleted)
                }
                .onFailure { throwable ->
                    mutableUiState.update {
                        it.copy(isSaving = false, errorMessage = throwable.message ?: "删除笔记失败")
                    }
                }
        }
    }

    fun exportCurrentNote(format: NoteExportFormat, content: String?, contentHtml: String?) {
        val currentSpaceId = spaceId ?: return
        val state = mutableUiState.value
        val exportTitle = state.title.trim()
        if (exportTitle.isBlank()) {
            mutableUiState.update { it.copy(errorMessage = "请输入笔记标题") }
            return
        }
        viewModelScope.launch {
            mutableUiState.update { it.copy(isExporting = true, errorMessage = null, savedMessage = null) }
            val notebookId = state.selectedNotebookId ?: ensureDefaultNotebook(currentSpaceId)
            if (notebookId == null) {
                mutableUiState.update { it.copy(isExporting = false, errorMessage = "默认笔记本创建失败，请稍后重试") }
                return@launch
            }
            val request = NoteSave(
                notebookId = notebookId,
                title = exportTitle,
                content = content ?: state.content,
                contentHtml = contentHtml ?: state.contentHtml ?: renderPlainDocumentHtml(content ?: state.content),
                isPublic = false
            )
            val savedResult = noteId?.let { id ->
                noteRepository.update(currentSpaceId, id, request)
            } ?: noteRepository.create(currentSpaceId, request)
            val savedNote = savedResult.getOrElse { throwable ->
                mutableUiState.update {
                    it.copy(isExporting = false, errorMessage = throwable.message ?: "导出前保存失败")
                }
                return@launch
            }
            noteId = savedNote.id
            mutableUiState.update {
                it.copy(
                    title = savedNote.title,
                    content = request.content.orEmpty(),
                    contentHtml = request.contentHtml,
                    documentDate = savedNote.gmtModified ?: savedNote.gmtCreate ?: it.documentDate,
                    selectedNotebookId = savedNote.notebookId ?: notebookId,
                    hasUnsavedChanges = false
                )
            }
            noteRepository.export(currentSpaceId, savedNote.id, format)
                .onSuccess { file ->
                    mutableUiState.update { it.copy(isExporting = false, savedMessage = "导出完成") }
                    mutableEffect.emit(NoteEditEffect.ExportReady(file))
                }
                .onFailure { throwable ->
                    mutableUiState.update {
                        it.copy(isExporting = false, errorMessage = throwable.message ?: "导出失败")
                    }
                }
        }
    }

    fun loadHistories() {
        val currentSpaceId = spaceId ?: return
        val currentNoteId = noteId ?: run {
            mutableUiState.update { it.copy(errorMessage = "请先保存笔记后再查看版本历史") }
            return
        }
        viewModelScope.launch {
            mutableUiState.update { it.copy(isMetadataLoading = true, errorMessage = null) }
            noteRepository.histories(currentSpaceId, currentNoteId)
                .onSuccess { histories ->
                    val sortedHistories = histories.sortedByDescending { history -> history.version }
                    mutableUiState.update {
                        it.copy(
                            isMetadataLoading = false,
                            histories = sortedHistories,
                            previewHistory = sortedHistories.firstOrNull()
                        )
                    }
                }
                .onFailure { throwable ->
                    mutableUiState.update {
                        it.copy(isMetadataLoading = false, errorMessage = throwable.message ?: "加载版本历史失败")
                    }
                }
        }
    }

    fun loadHistory(version: Int) {
        val currentSpaceId = spaceId ?: return
        val currentNoteId = noteId ?: return
        viewModelScope.launch {
            mutableUiState.update { it.copy(isMetadataLoading = true, errorMessage = null) }
            noteRepository.history(currentSpaceId, currentNoteId, version)
                .onSuccess { history ->
                    mutableUiState.update {
                        it.copy(
                            isMetadataLoading = false,
                            previewHistory = history
                        )
                    }
                }
                .onFailure { throwable ->
                    mutableUiState.update {
                        it.copy(
                            isMetadataLoading = false,
                            errorMessage = throwable.message ?: "加载历史版本失败"
                        )
                    }
                }
        }
    }

    fun restoreHistory(version: Int) {
        val currentSpaceId = spaceId ?: return
        val currentNoteId = noteId ?: return
        viewModelScope.launch {
            mutableUiState.update { it.copy(isSaving = true, errorMessage = null) }
            noteRepository.restore(currentSpaceId, currentNoteId, version)
                .onSuccess { note ->
                    val restoredContent = note.content.orEmpty()
                    val restoredHtml = note.contentHtml ?: note.content?.let(::renderPlainDocumentHtml)
                    mutableUiState.update {
                        it.copy(
                            isSaving = false,
                            title = note.title,
                            content = restoredContent,
                            contentHtml = restoredHtml,
                            documentDate = note.gmtModified ?: note.gmtCreate ?: it.documentDate,
                            selectedNotebookId = note.notebookId ?: it.selectedNotebookId,
                            hasUnsavedChanges = false,
                            savedMessage = "已恢复到版本 $version"
                        )
                    }
                    mutableEffect.emit(
                        NoteEditEffect.EditorContentReady(
                            content = restoredContent,
                            contentHtml = restoredHtml
                        )
                    )
                }
                .onFailure { throwable ->
                    mutableUiState.update {
                        it.copy(isSaving = false, errorMessage = throwable.message ?: "恢复版本失败")
                    }
                }
        }
    }

    fun uploadInlineImage(uri: Uri) {
        val currentSpaceId = spaceId ?: return
        viewModelScope.launch {
            mutableUiState.update {
                it.copy(
                    isInlineImageUploading = true,
                    errorMessage = null,
                    savedMessage = null
                )
            }
            val upload = runCatching {
                withContext(Dispatchers.IO) {
                    readSelectedFile(uri)
                }
            }.getOrElse { throwable ->
                mutableUiState.update {
                    it.copy(
                        isInlineImageUploading = false,
                        errorMessage = throwable.message ?: "读取图片失败"
                    )
                }
                return@launch
            }
            if (!upload.isImage()) {
                mutableUiState.update {
                    it.copy(
                        isInlineImageUploading = false,
                        errorMessage = "请选择图片文件"
                    )
                }
                return@launch
            }
            fileRepository.editorUpload(currentSpaceId, upload.toManagedFileUpload())
                .onSuccess { managedFile ->
                    val imageUrl = managedFile.downloadUrl.orEmpty()
                    if (imageUrl.isBlank()) {
                        mutableUiState.update {
                            it.copy(
                                isInlineImageUploading = false,
                                errorMessage = "图片上传成功但没有可访问链接"
                            )
                        }
                        return@onSuccess
                    }
                    mutableUiState.update {
                        it.copy(
                            isInlineImageUploading = false,
                            savedMessage = "图片已插入"
                        )
                    }
                    mutableEffect.emit(
                        NoteEditEffect.ImageReady(
                            url = imageUrl,
                            alt = managedFile.displayName.ifBlank { upload.fileName },
                            managedFileId = managedFile.id,
                            attachmentId = managedFile.attachmentId
                        )
                    )
                }
                .onFailure { throwable ->
                    mutableUiState.update {
                        it.copy(
                            isInlineImageUploading = false,
                            errorMessage = throwable.message ?: "图片上传失败"
                        )
                    }
                }
        }
    }

    fun uploadInlineFile(uri: Uri, kind: InlineFileKind) {
        val currentSpaceId = spaceId ?: return
        viewModelScope.launch {
            mutableUiState.update {
                it.copy(
                    isInlineImageUploading = true,
                    errorMessage = null,
                    savedMessage = null
                )
            }
            val upload = runCatching {
                withContext(Dispatchers.IO) {
                    readSelectedFile(uri)
                }
            }.getOrElse { throwable ->
                mutableUiState.update {
                    it.copy(
                        isInlineImageUploading = false,
                        errorMessage = throwable.message ?: "读取文件失败"
                    )
                }
                return@launch
            }
            fileRepository.editorUpload(currentSpaceId, upload.toManagedFileUpload())
                .onSuccess { managedFile ->
                    val fileUrl = managedFile.downloadUrl.orEmpty()
                    if (fileUrl.isBlank()) {
                        mutableUiState.update {
                            it.copy(
                                isInlineImageUploading = false,
                                errorMessage = "文件上传成功但没有可访问链接"
                            )
                        }
                        return@onSuccess
                    }
                    mutableUiState.update {
                        it.copy(
                            isInlineImageUploading = false,
                            savedMessage = "文件已插入"
                        )
                    }
                    val displayName = managedFile.displayName.ifBlank { upload.fileName }
                    val resolvedMimeType = managedFile.mimeType ?: upload.mimeType
                    when {
                        kind == InlineFileKind.ATTACHMENT && isImageFile(resolvedMimeType, displayName) -> {
                            mutableEffect.emit(
                                NoteEditEffect.ImageReady(
                                    url = fileUrl,
                                    alt = displayName,
                                    managedFileId = managedFile.id,
                                    attachmentId = managedFile.attachmentId
                                )
                            )
                        }
                        kind == InlineFileKind.ATTACHMENT && isAudioFile(resolvedMimeType, displayName) -> {
                            mutableEffect.emit(
                                NoteEditEffect.FileReady(
                                    kind = InlineFileKind.AUDIO,
                                    name = displayName,
                                    url = fileUrl,
                                    mimeType = resolvedMimeType,
                                    fileSize = managedFile.fileSize,
                                    managedFileId = managedFile.id,
                                    attachmentId = managedFile.attachmentId
                                )
                            )
                        }
                        else -> {
                            mutableEffect.emit(
                                NoteEditEffect.FileReady(
                                    kind = kind,
                                    name = displayName,
                                    url = fileUrl,
                                    mimeType = resolvedMimeType,
                                    fileSize = managedFile.fileSize,
                                    managedFileId = managedFile.id,
                                    attachmentId = managedFile.attachmentId
                                )
                            )
                        }
                    }
                }
                .onFailure { throwable ->
                    mutableUiState.update {
                        it.copy(
                            isInlineImageUploading = false,
                            errorMessage = throwable.message ?: "文件上传失败"
                        )
                    }
                }
        }
    }

    fun loadReferenceFiles() {
        val currentSpaceId = spaceId ?: return
        viewModelScope.launch {
            mutableUiState.update { it.copy(isReferenceLoading = true, errorMessage = null) }
            fileRepository.page(currentSpaceId, ManagedFileQuery(pageSize = REFERENCE_FILE_LIMIT))
                .onSuccess { page ->
                    mutableUiState.update {
                        it.copy(
                            isReferenceLoading = false,
                            referenceFiles = page.list
                        )
                    }
                }
                .onFailure { throwable ->
                    mutableUiState.update {
                        it.copy(
                            isReferenceLoading = false,
                            errorMessage = throwable.message ?: "加载文件引用失败"
                        )
                    }
                }
        }
    }

    fun insertReferenceFile(file: ManagedFile) {
        val currentSpaceId = spaceId ?: return
        viewModelScope.launch {
            mutableUiState.update { it.copy(isReferenceLoading = true, errorMessage = null) }
            fileRepository.downloadUrl(currentSpaceId, file.id)
                .onSuccess { managedFile ->
                    val fileUrl = managedFile.downloadUrl.orEmpty()
                    if (fileUrl.isBlank()) {
                        mutableUiState.update {
                            it.copy(
                                isReferenceLoading = false,
                                errorMessage = "当前文件暂无可访问链接"
                            )
                        }
                        return@onSuccess
                    }
                    mutableUiState.update { it.copy(isReferenceLoading = false) }
                    val displayName = managedFile.displayName.ifBlank { managedFile.fileName }
                    val resolvedMimeType = managedFile.mimeType
                    when {
                        isImageFile(resolvedMimeType, displayName) -> {
                            mutableEffect.emit(
                                NoteEditEffect.ImageReady(
                                    url = fileUrl,
                                    alt = displayName,
                                    managedFileId = managedFile.id,
                                    attachmentId = managedFile.attachmentId
                                )
                            )
                        }
                        isAudioFile(resolvedMimeType, displayName) -> {
                            mutableEffect.emit(
                                NoteEditEffect.FileReady(
                                    kind = InlineFileKind.AUDIO,
                                    name = displayName,
                                    url = fileUrl,
                                    mimeType = resolvedMimeType,
                                    fileSize = managedFile.fileSize,
                                    managedFileId = managedFile.id,
                                    attachmentId = managedFile.attachmentId
                                )
                            )
                        }
                        else -> {
                            mutableEffect.emit(
                                NoteEditEffect.FileReady(
                                    kind = InlineFileKind.REFERENCE,
                                    name = displayName,
                                    url = fileUrl,
                                    mimeType = resolvedMimeType,
                                    fileSize = managedFile.fileSize,
                                    managedFileId = managedFile.id,
                                    attachmentId = managedFile.attachmentId
                                )
                            )
                        }
                    }
                }
                .onFailure { throwable ->
                    mutableUiState.update {
                        it.copy(
                            isReferenceLoading = false,
                            errorMessage = throwable.message ?: "引用文件失败"
                        )
                    }
                }
        }
    }

    fun loadAttachments() {
        val currentSpaceId = spaceId ?: return
        val currentNoteId = noteId ?: run {
            mutableUiState.update { it.copy(errorMessage = "请先保存笔记后再查看附件") }
            return
        }
        viewModelScope.launch {
            refreshAttachments(
                currentSpaceId = currentSpaceId,
                currentNoteId = currentNoteId,
                showLoading = true
            )
        }
    }

    fun uploadAttachment(uri: Uri) {
        val currentSpaceId = spaceId ?: return
        val currentNoteId = noteId ?: run {
            mutableUiState.update { it.copy(errorMessage = "请先保存笔记后再上传附件") }
            return
        }
        viewModelScope.launch {
            mutableUiState.update {
                it.copy(
                    isAttachmentUploading = true,
                    activeAttachmentId = null,
                    errorMessage = null,
                    savedMessage = null
                )
            }
            val upload = runCatching {
                withContext(Dispatchers.IO) {
                    readSelectedFile(uri)
                }
            }.getOrElse { throwable ->
                mutableUiState.update {
                    it.copy(
                        isAttachmentUploading = false,
                        errorMessage = throwable.message ?: "读取附件失败"
                    )
                }
                return@launch
            }
            noteRepository.uploadAttachment(currentSpaceId, currentNoteId, upload)
                .onSuccess {
                    refreshAttachments(
                        currentSpaceId = currentSpaceId,
                        currentNoteId = currentNoteId,
                        successMessage = "附件已上传"
                    )
                }
                .onFailure { throwable ->
                    mutableUiState.update {
                        it.copy(
                            isAttachmentUploading = false,
                            errorMessage = throwable.message ?: "上传附件失败"
                        )
                    }
                }
        }
    }

    fun unbindAttachment(attachmentId: Long) {
        val currentSpaceId = spaceId ?: return
        val currentNoteId = noteId ?: run {
            mutableUiState.update { it.copy(errorMessage = "请先保存笔记后再移除附件") }
            return
        }
        viewModelScope.launch {
            mutableUiState.update {
                it.copy(
                    activeAttachmentId = attachmentId,
                    errorMessage = null,
                    savedMessage = null
                )
            }
            noteRepository.unbindAttachment(currentSpaceId, currentNoteId, attachmentId)
                .onSuccess {
                    refreshAttachments(
                        currentSpaceId = currentSpaceId,
                        currentNoteId = currentNoteId,
                        successMessage = "附件已移除"
                    )
                }
                .onFailure { throwable ->
                    mutableUiState.update {
                        it.copy(
                            activeAttachmentId = null,
                            errorMessage = throwable.message ?: "移除附件失败"
                        )
                    }
                }
        }
    }

    private suspend fun refreshAttachments(
        currentSpaceId: Long,
        currentNoteId: Long,
        showLoading: Boolean = false,
        successMessage: String? = null
    ) {
        if (showLoading) {
            mutableUiState.update { it.copy(isMetadataLoading = true, errorMessage = null) }
        }
        noteRepository.attachments(currentSpaceId, currentNoteId)
            .onSuccess { attachments ->
                mutableUiState.update { state ->
                    state.copy(
                        attachments = attachments,
                        isMetadataLoading = false,
                        isAttachmentUploading = false,
                        activeAttachmentId = null,
                        errorMessage = null,
                        savedMessage = successMessage ?: state.savedMessage
                    )
                }
            }
            .onFailure { throwable ->
                mutableUiState.update {
                    it.copy(
                        isMetadataLoading = false,
                        isAttachmentUploading = false,
                        activeAttachmentId = null,
                        errorMessage = throwable.message ?: "加载附件失败"
                    )
                }
            }
    }

    private fun readSelectedFile(uri: Uri): NoteAttachmentUpload {
        val resolver = context.contentResolver
        var fileName = uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
            ?: DEFAULT_UPLOAD_NAME
        var size: Long? = null
        resolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex >= 0) {
                    fileName = cursor.getString(nameIndex)?.takeIf { it.isNotBlank() } ?: fileName
                }
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                    size = cursor.getLong(sizeIndex)
                }
            }
        }
        if ((size ?: 0L) > MAX_DIRECT_UPLOAD_BYTES) {
            throw IllegalArgumentException("当前版本支持 20 MB 内的附件直接上传")
        }
        val bytes = resolver.openInputStream(uri)?.use { inputStream ->
            inputStream.readBytes()
        } ?: throw IllegalArgumentException("无法读取所选附件")
        val mimeType = resolver.getType(uri) ?: DEFAULT_MIME_TYPE
        return NoteAttachmentUpload(
            fileName = fileName,
            mimeType = mimeType,
            bytes = bytes
        )
    }

    private fun NoteAttachmentUpload.isImage(): Boolean {
        return isImageFile(mimeType, fileName)
    }

    private fun isImageFile(mimeType: String?, fileName: String): Boolean {
        if (mimeType?.startsWith(IMAGE_MIME_PREFIX, ignoreCase = true) == true) {
            return true
        }
        return fileName.extension() in IMAGE_EXTENSIONS
    }

    private fun isAudioFile(mimeType: String?, fileName: String): Boolean {
        if (mimeType?.startsWith(AUDIO_MIME_PREFIX, ignoreCase = true) == true) {
            return true
        }
        return fileName.extension() in AUDIO_EXTENSIONS
    }

    private fun String.extension(): String {
        return substringAfterLast('.', "").lowercase()
    }

    private fun NoteAttachmentUpload.toManagedFileUpload(): ManagedFileUpload {
        return ManagedFileUpload(
            fileName = fileName,
            mimeType = mimeType,
            bytes = bytes
        )
    }

    fun checkpointCollabContent(closeAfterSave: Boolean = false) {
        val currentSpaceId = spaceId ?: return
        val currentNoteId = noteId ?: return
        val state = mutableUiState.value
        if (!state.collabEnabled) {
            save(closeAfterSave = closeAfterSave)
            return
        }
        viewModelScope.launch {
            mutableUiState.update { it.copy(isSaving = true, errorMessage = null, savedMessage = null) }
            val content = CollabContentSave(
                content = state.content,
                contentHtml = state.contentHtml ?: renderPlainDocumentHtml(state.content)
            )
            Log.d(COLLAB_LOG_TAG, "create checkpoint noteId=$currentNoteId")
            noteRepository.createCheckpoint(currentSpaceId, currentNoteId, content)
                .onSuccess { note ->
                    mutableUiState.update {
                        it.copy(
                            isSaving = false,
                            documentDate = note.gmtModified ?: note.gmtCreate ?: it.documentDate,
                            hasUnsavedChanges = false,
                            savedMessage = "已保存"
                        )
                    }
                    if (closeAfterSave) {
                        mutableEffect.emit(NoteEditEffect.Saved)
                    }
                }
                .onFailure { throwable ->
                    if (throwable is CancellationException) {
                        Log.d(COLLAB_LOG_TAG, "checkpoint cancelled noteId=$currentNoteId")
                        return@onFailure
                    }
                    mutableUiState.update {
                        it.copy(isSaving = false, errorMessage = throwable.message ?: "协作内容保存失败")
                    }
                }
        }
    }

    private fun saveDraftSnapshot() {
        val currentSpaceId = spaceId ?: return
        if (!draftSavingEnabled) {
            return
        }
        val state = mutableUiState.value
        if (state.collabEnabled) {
            return
        }
        if (state.title.isBlank() && state.content.isBlank()) {
            return
        }
        viewModelScope.launch {
            noteDraftRepository.save(
                NoteDraft(
                    spaceId = currentSpaceId,
                    noteId = noteId,
                    notebookId = state.selectedNotebookId,
                    title = state.title,
                    content = state.content,
                    updatedAt = System.currentTimeMillis()
                )
            )
            mutableUiState.update {
                if (it.isSaving) {
                    it
                } else {
                    it.copy(savedMessage = null)
                }
            }
        }
    }

    private fun renderPlainDocumentHtml(content: String?): String? {
        val normalizedContent = content?.takeIf { it.isNotBlank() } ?: return null
        return normalizedContent.lines()
            .joinToString("\n") { line -> renderPlainLineHtml(line) }
    }

    private fun renderPlainLineHtml(line: String): String {
        val trimmedLine = line.trim()
        if (trimmedLine.isBlank()) {
            return "<p><br></p>"
        }
        return "<p>${trimmedLine.escapeHtml()}</p>"
    }

    private fun String.escapeHtml(): String {
        return replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
    }

    private companion object {
        const val COLLAB_LOG_TAG = "NotaskCollab"
        const val DEFAULT_NOTEBOOK_NAME = "默认笔记本"
        const val DEFAULT_UPLOAD_NAME = "attachment.bin"
        const val DEFAULT_MIME_TYPE = "application/octet-stream"
        const val IMAGE_MIME_PREFIX = "image/"
        const val AUDIO_MIME_PREFIX = "audio/"
        const val MAX_DIRECT_UPLOAD_BYTES = 20L * 1024L * 1024L
        const val REFERENCE_FILE_LIMIT = 50L
        val IMAGE_EXTENSIONS = setOf("apng", "avif", "gif", "jpeg", "jpg", "png", "svg", "webp")
        val AUDIO_EXTENSIONS = setOf("aac", "flac", "m4a", "mp3", "oga", "ogg", "opus", "wav", "weba")
    }
}
