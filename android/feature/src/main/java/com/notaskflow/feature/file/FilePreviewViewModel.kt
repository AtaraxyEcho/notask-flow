package com.notaskflow.feature.file

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notaskflow.core.datastore.TokenManager
import com.notaskflow.data.BuildConfig
import com.notaskflow.domain.file.FileRepository
import com.notaskflow.domain.model.FileFolder
import com.notaskflow.domain.model.ManagedFile
import com.notaskflow.domain.model.ManagedFileUpdate
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface FilePreviewUiState {
    data object Loading : FilePreviewUiState

    data class Content(
        val file: ManagedFile,
        val payload: FilePreviewPayload,
        val authToken: String?,
        val folders: List<FileFolder> = emptyList(),
        val isSaving: Boolean = false,
        val errorMessage: String? = null
    ) : FilePreviewUiState

    data class Error(
        val message: String
    ) : FilePreviewUiState
}

sealed interface FilePreviewPayload {
    data class Image(
        val url: String
    ) : FilePreviewPayload

    data class Pdf(
        val url: String?
    ) : FilePreviewPayload

    data class Text(
        val content: String
    ) : FilePreviewPayload

    data class Html(
        val content: String
    ) : FilePreviewPayload

    data class Link(
        val url: String?
    ) : FilePreviewPayload
}

@HiltViewModel
class FilePreviewViewModel @Inject constructor(
    private val fileRepository: FileRepository,
    private val tokenManager: TokenManager
) : ViewModel() {
    private val mutableUiState = MutableStateFlow<FilePreviewUiState>(FilePreviewUiState.Loading)
    val uiState: StateFlow<FilePreviewUiState> = mutableUiState

    private var currentSpaceId: Long? = null
    private var currentFileId: Long? = null

    fun load(spaceId: Long, fileId: Long) {
        currentSpaceId = spaceId
        currentFileId = fileId
        viewModelScope.launch {
            mutableUiState.value = FilePreviewUiState.Loading
            val authToken = tokenManager.currentToken()
            val fileResult = fileRepository.get(spaceId, fileId)
            val file = fileResult.getOrElse { throwable ->
                mutableUiState.value = FilePreviewUiState.Error(throwable.message ?: "文件加载失败")
                return@launch
            }
            val content = when {
                file.isImage() -> loadImagePayload(spaceId, file, authToken)
                file.isHtml() -> loadHtmlPayload(spaceId, file, authToken)
                file.isTextLike() -> loadTextPayload(spaceId, file, authToken)
                file.isPdf() -> loadPdfPayload(spaceId, file, authToken)
                else -> loadLinkPayload(spaceId, file, authToken)
            }
            val folders = fileRepository.folders(spaceId).getOrDefault(emptyList())
            mutableUiState.value = content.copy(folders = folders)
        }
    }

    fun download() {
        val spaceId = currentSpaceId ?: return
        val content = currentContent() ?: return
        viewModelScope.launch {
            mutableUiState.value = content.copy(isSaving = true, errorMessage = null)
            fileRepository.downloadUrl(spaceId, content.file.id)
                .onSuccess { file ->
                    mutableUiState.value = currentContent()?.copy(
                        file = file,
                        isSaving = false,
                        errorMessage = null
                    ) ?: content.copy(file = file, isSaving = false)
                }
                .onFailure { throwable ->
                    mutableUiState.value = currentContent()?.copy(
                        isSaving = false,
                        errorMessage = throwable.message ?: "获取下载链接失败"
                    ) ?: FilePreviewUiState.Error(throwable.message ?: "获取下载链接失败")
                }
        }
    }

    fun move(folderId: Long?) {
        val spaceId = currentSpaceId ?: return
        val fileId = currentFileId ?: return
        val content = currentContent() ?: return
        viewModelScope.launch {
            mutableUiState.value = content.copy(isSaving = true, errorMessage = null)
            fileRepository.updateFile(
                spaceId = spaceId,
                fileId = fileId,
                file = ManagedFileUpdate(displayName = content.file.displayName, folderId = folderId)
            ).onSuccess { updated ->
                mutableUiState.value = currentContent()?.copy(
                    file = updated,
                    isSaving = false,
                    errorMessage = null
                ) ?: content.copy(file = updated, isSaving = false)
            }.onFailure { throwable ->
                mutableUiState.value = currentContent()?.copy(
                    isSaving = false,
                    errorMessage = throwable.message ?: "移动文件失败"
                ) ?: FilePreviewUiState.Error(throwable.message ?: "移动文件失败")
            }
        }
    }

    fun consumeDownloadUrl() {
        val content = currentContent() ?: return
        mutableUiState.value = content.copy(file = content.file.copy(downloadUrl = null))
    }

    private fun loadImagePayload(
        spaceId: Long,
        file: ManagedFile,
        authToken: String?
    ): FilePreviewUiState.Content {
        return FilePreviewUiState.Content(
            file = file,
            payload = FilePreviewPayload.Image(filePreviewUrl(spaceId, file.id)),
            authToken = authToken
        )
    }

    private fun loadPdfPayload(
        spaceId: Long,
        file: ManagedFile,
        authToken: String?
    ): FilePreviewUiState.Content {
        return FilePreviewUiState.Content(
            file = file,
            payload = FilePreviewPayload.Pdf(filePreviewUrl(spaceId, file.id)),
            authToken = authToken
        )
    }

    private suspend fun loadTextPayload(
        spaceId: Long,
        file: ManagedFile,
        authToken: String?
    ): FilePreviewUiState.Content {
        val textResult = fileRepository.previewText(spaceId, file.id)
        return textResult.fold(
            onSuccess = { preview ->
                FilePreviewUiState.Content(
                    file = file,
                    payload = FilePreviewPayload.Text(preview.textContent),
                    authToken = authToken
                )
            },
            onFailure = { throwable ->
                FilePreviewUiState.Content(
                    file = file,
                    payload = FilePreviewPayload.Link(filePreviewUrl(spaceId, file.id)),
                    authToken = authToken,
                    errorMessage = throwable.message ?: "文本预览失败"
                )
            }
        )
    }

    private suspend fun loadHtmlPayload(
        spaceId: Long,
        file: ManagedFile,
        authToken: String?
    ): FilePreviewUiState.Content {
        val htmlResult = fileRepository.previewHtml(spaceId, file.id)
        return htmlResult.fold(
            onSuccess = { preview ->
                FilePreviewUiState.Content(
                    file = file,
                    payload = FilePreviewPayload.Html(preview.htmlContent),
                    authToken = authToken
                )
            },
            onFailure = { throwable ->
                loadTextPayload(spaceId, file, authToken).copy(errorMessage = throwable.message ?: "HTML 预览失败")
            }
        )
    }

    private fun loadLinkPayload(
        spaceId: Long,
        file: ManagedFile,
        authToken: String?
    ): FilePreviewUiState.Content {
        return FilePreviewUiState.Content(
            file = file,
            payload = FilePreviewPayload.Link(filePreviewUrl(spaceId, file.id)),
            authToken = authToken
        )
    }

    private fun filePreviewUrl(spaceId: Long, fileId: Long): String {
        return "${BuildConfig.BASE_URL.trimEnd('/')}/api/v1/spaces/$spaceId/files/$fileId/preview"
    }

    private fun currentContent(): FilePreviewUiState.Content? {
        return mutableUiState.value as? FilePreviewUiState.Content
    }

    private fun ManagedFile.isImage(): Boolean {
        return mimeType?.startsWith("image/") == true
    }

    private fun ManagedFile.isPdf(): Boolean {
        return mimeType == PDF_MIME_TYPE || fileName.endsWith(".pdf", ignoreCase = true)
    }

    private fun ManagedFile.isHtml(): Boolean {
        return mimeType == HTML_MIME_TYPE ||
            fileName.endsWith(".html", ignoreCase = true) ||
            fileName.endsWith(".htm", ignoreCase = true)
    }

    private fun ManagedFile.isTextLike(): Boolean {
        val type = mimeType.orEmpty()
        return type.startsWith("text/") ||
            type in TEXT_LIKE_MIME_TYPES ||
            fileName.endsWith(".md", ignoreCase = true) ||
            fileName.endsWith(".json", ignoreCase = true)
    }

    private companion object {
        const val PDF_MIME_TYPE = "application/pdf"
        const val HTML_MIME_TYPE = "text/html"
        val TEXT_LIKE_MIME_TYPES = setOf(
            "application/json",
            "application/xml",
            "application/yaml",
            "application/x-yaml"
        )
    }
}
