package com.notaskflow.feature.file

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notaskflow.domain.file.FileRepository
import com.notaskflow.domain.model.FileFolder
import com.notaskflow.domain.model.FileFolderSave
import com.notaskflow.domain.model.ManagedFile
import com.notaskflow.domain.model.ManagedFileQuery
import com.notaskflow.domain.model.ManagedFileUpdate
import com.notaskflow.domain.model.ManagedFileUpload
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface FileBrowserUiState {
    data object Loading : FileBrowserUiState

    data class Content(
        val files: List<ManagedFile> = emptyList(),
        val folders: List<FileFolder> = emptyList(),
        val selectedFolderId: Long? = null,
        val selectedFolderName: String = "全部文件",
        val isTrashMode: Boolean = false,
        val isRefreshing: Boolean = false,
        val isUploading: Boolean = false,
        val errorMessage: String? = null
    ) : FileBrowserUiState

    data class Error(
        val message: String
    ) : FileBrowserUiState
}

sealed interface FileBrowserEffect {
    data class ConfirmDelete(val file: ManagedFile, val referenceCount: Int) : FileBrowserEffect
}

@HiltViewModel
class FileBrowserViewModel @Inject constructor(
    private val fileRepository: FileRepository,
    @param:ApplicationContext private val context: Context
) : ViewModel() {
    private val mutableUiState = MutableStateFlow<FileBrowserUiState>(FileBrowserUiState.Loading)
    val uiState: StateFlow<FileBrowserUiState> = mutableUiState

    private val mutableEffect = MutableSharedFlow<FileBrowserEffect>()
    val effect: SharedFlow<FileBrowserEffect> = mutableEffect.asSharedFlow()

    private var currentSpaceId: Long? = null

    fun load(spaceId: Long) {
        currentSpaceId = spaceId
        viewModelScope.launch {
            mutableUiState.value = FileBrowserUiState.Loading
            val foldersResult = fileRepository.folders(spaceId)
            val filesResult = fileRepository.page(spaceId, ManagedFileQuery(pageSize = PAGE_SIZE, trashed = false))
            val folders = foldersResult.getOrDefault(emptyList())
            val files = filesResult.getOrNull()?.list.orEmpty()
            if (foldersResult.isFailure && filesResult.isFailure) {
                mutableUiState.value = FileBrowserUiState.Error(
                    foldersResult.exceptionOrNull()?.message
                        ?: filesResult.exceptionOrNull()?.message
                        ?: "文件加载失败"
                )
                return@launch
            }
            mutableUiState.value = FileBrowserUiState.Content(
                files = files,
                folders = folders,
                errorMessage = foldersResult.exceptionOrNull()?.message
                    ?: filesResult.exceptionOrNull()?.message
            )
        }
    }

    fun refresh() {
        val spaceId = currentSpaceId ?: return
        val content = currentContent() ?: return
        refreshFiles(spaceId, content.selectedFolderId, content.selectedFolderName, trashMode = content.isTrashMode)
    }

    fun selectFolder(folder: FileFolder?) {
        val spaceId = currentSpaceId ?: return
        refreshFiles(
            spaceId = spaceId,
            folderId = folder?.id,
            folderName = folder?.name ?: "全部文件",
            trashMode = false
        )
    }

    fun selectTrash() {
        val spaceId = currentSpaceId ?: return
        refreshFiles(
            spaceId = spaceId,
            folderId = null,
            folderName = "回收站",
            trashMode = true
        )
    }

    fun upload(uri: Uri) {
        val spaceId = currentSpaceId ?: return
        val content = currentContent() ?: return
        viewModelScope.launch {
            mutableUiState.update {
                content.copy(isUploading = true, errorMessage = null)
            }
            val selectedFile = runCatching {
                withContext(Dispatchers.IO) {
                    readSelectedFile(uri, content.selectedFolderId)
                }
            }.getOrElse { throwable ->
                mutableUiState.update {
                    content.copy(
                        isUploading = false,
                        errorMessage = throwable.message ?: "读取文件失败"
                    )
                }
                return@launch
            }
            if (selectedFile.bytes.size.toLong() > MAX_DIRECT_UPLOAD_BYTES) {
                mutableUiState.update {
                    content.copy(
                        isUploading = false,
                        errorMessage = "当前版本支持 20 MB 内的文件直接上传"
                    )
                }
                return@launch
            }
            val result = fileRepository.upload(spaceId, selectedFile)
            result.onSuccess {
                refreshFiles(
                    spaceId = spaceId,
                    folderId = content.selectedFolderId,
                    folderName = content.selectedFolderName,
                    isUploading = false,
                    trashMode = false
                )
            }.onFailure { throwable ->
                mutableUiState.update {
                    content.copy(
                        isUploading = false,
                        errorMessage = throwable.message ?: "上传失败"
                    )
                }
            }
        }
    }

    fun prepareDelete(file: ManagedFile) {
        val spaceId = currentSpaceId ?: return
        viewModelScope.launch {
            fileRepository.references(spaceId, file.id)
                .onSuccess { references ->
                    mutableEffect.emit(FileBrowserEffect.ConfirmDelete(file, references.size))
                }
                .onFailure {
                    mutableEffect.emit(FileBrowserEffect.ConfirmDelete(file, 0))
                }
        }
    }

    fun delete(file: ManagedFile) {
        val spaceId = currentSpaceId ?: return
        val content = currentContent() ?: return
        viewModelScope.launch {
            mutableUiState.update {
                content.copy(isRefreshing = true, errorMessage = null)
            }
            fileRepository.delete(spaceId, file.id)
                .onSuccess {
                    mutableUiState.update {
                        currentContent()?.copy(
                            files = currentContent()?.files.orEmpty().filterNot { current -> current.id == file.id },
                            isRefreshing = false,
                            errorMessage = null
                        ) ?: FileBrowserUiState.Content()
                    }
                }
                .onFailure { throwable ->
                    mutableUiState.update {
                        currentContent()?.copy(
                            isRefreshing = false,
                            errorMessage = throwable.message ?: "删除文件失败"
                        ) ?: FileBrowserUiState.Error(throwable.message ?: "删除文件失败")
                    }
                }
        }
    }

    fun restore(file: ManagedFile) {
        val spaceId = currentSpaceId ?: return
        viewModelScope.launch {
            mutableUiState.update {
                currentContent()?.copy(isRefreshing = true, errorMessage = null) ?: it
            }
            fileRepository.restore(spaceId, file.id)
                .onSuccess {
                    refresh()
                }
                .onFailure { throwable ->
                    mutableUiState.update {
                        currentContent()?.copy(
                            isRefreshing = false,
                            errorMessage = throwable.message ?: "恢复文件失败"
                        ) ?: FileBrowserUiState.Error(throwable.message ?: "恢复文件失败")
                    }
                }
        }
    }

    fun physicalDelete(file: ManagedFile) {
        val spaceId = currentSpaceId ?: return
        viewModelScope.launch {
            mutableUiState.update {
                currentContent()?.copy(isRefreshing = true, errorMessage = null) ?: it
            }
            fileRepository.physicalDelete(spaceId, file.id)
                .onSuccess {
                    mutableUiState.update {
                        currentContent()?.copy(
                            files = currentContent()?.files.orEmpty().filterNot { current -> current.id == file.id },
                            isRefreshing = false,
                            errorMessage = null
                        ) ?: FileBrowserUiState.Content()
                    }
                }
                .onFailure { throwable ->
                    mutableUiState.update {
                        currentContent()?.copy(
                            isRefreshing = false,
                            errorMessage = throwable.message ?: "彻底删除失败"
                        ) ?: FileBrowserUiState.Error(throwable.message ?: "彻底删除失败")
                    }
                }
        }
    }

    fun createFolder(name: String) {
        val spaceId = currentSpaceId ?: return
        val content = currentContent() ?: return
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            mutableUiState.update {
                content.copy(errorMessage = "请输入文件夹名称")
            }
            return
        }
        viewModelScope.launch {
            mutableUiState.update { content.copy(isRefreshing = true, errorMessage = null) }
            fileRepository.createFolder(
                spaceId = spaceId,
                folder = FileFolderSave(name = trimmedName, parentId = content.selectedFolderId)
            ).onSuccess {
                load(spaceId)
            }.onFailure { throwable ->
                mutableUiState.update {
                    currentContent()?.copy(
                        isRefreshing = false,
                        errorMessage = throwable.message ?: "创建文件夹失败"
                    ) ?: FileBrowserUiState.Error(throwable.message ?: "创建文件夹失败")
                }
            }
        }
    }

    fun renameFolder(folder: FileFolder, name: String) {
        val spaceId = currentSpaceId ?: return
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            mutableUiState.update {
                currentContent()?.copy(errorMessage = "请输入文件夹名称") ?: it
            }
            return
        }
        viewModelScope.launch {
            mutableUiState.update {
                currentContent()?.copy(isRefreshing = true, errorMessage = null) ?: it
            }
            fileRepository.updateFolder(
                spaceId = spaceId,
                folderId = folder.id,
                folder = FileFolderSave(name = trimmedName, parentId = folder.parentId)
            ).onSuccess {
                load(spaceId)
            }.onFailure { throwable ->
                mutableUiState.update {
                    currentContent()?.copy(
                        isRefreshing = false,
                        errorMessage = throwable.message ?: "重命名文件夹失败"
                    ) ?: FileBrowserUiState.Error(throwable.message ?: "重命名文件夹失败")
                }
            }
        }
    }

    fun moveFolder(folder: FileFolder, parentId: Long?) {
        val spaceId = currentSpaceId ?: return
        if (folder.id == parentId || folder.children.containsFolder(parentId)) {
            mutableUiState.update {
                currentContent()?.copy(errorMessage = "不能移动到自身或子文件夹下") ?: it
            }
            return
        }
        viewModelScope.launch {
            mutableUiState.update {
                currentContent()?.copy(isRefreshing = true, errorMessage = null) ?: it
            }
            fileRepository.updateFolder(
                spaceId = spaceId,
                folderId = folder.id,
                folder = FileFolderSave(name = folder.name, parentId = parentId)
            ).onSuccess {
                load(spaceId)
            }.onFailure { throwable ->
                mutableUiState.update {
                    currentContent()?.copy(
                        isRefreshing = false,
                        errorMessage = throwable.message ?: "移动文件夹失败"
                    ) ?: FileBrowserUiState.Error(throwable.message ?: "移动文件夹失败")
                }
            }
        }
    }

    fun deleteFolder(folder: FileFolder) {
        val spaceId = currentSpaceId ?: return
        viewModelScope.launch {
            mutableUiState.update {
                currentContent()?.copy(isRefreshing = true, errorMessage = null) ?: it
            }
            fileRepository.deleteFolder(spaceId, folder.id)
                .onSuccess {
                    load(spaceId)
                }
                .onFailure { throwable ->
                    mutableUiState.update {
                        currentContent()?.copy(
                            isRefreshing = false,
                            errorMessage = throwable.message ?: "删除文件夹失败"
                        ) ?: FileBrowserUiState.Error(throwable.message ?: "删除文件夹失败")
                    }
                }
        }
    }

    fun renameFile(file: ManagedFile, displayName: String) {
        val spaceId = currentSpaceId ?: return
        val trimmedName = displayName.trim()
        if (trimmedName.isBlank()) {
            mutableUiState.update {
                currentContent()?.copy(errorMessage = "请输入文件名") ?: it
            }
            return
        }
        viewModelScope.launch {
            mutableUiState.update {
                currentContent()?.copy(isRefreshing = true, errorMessage = null) ?: it
            }
            fileRepository.updateFile(
                spaceId = spaceId,
                fileId = file.id,
                file = ManagedFileUpdate(displayName = trimmedName, folderId = file.folderId)
            ).onSuccess { updated ->
                mutableUiState.update { state ->
                    val contentState = state as? FileBrowserUiState.Content
                    contentState?.copy(
                        files = contentState.files.map { current ->
                            if (current.id == updated.id) updated else current
                        },
                        isRefreshing = false,
                        errorMessage = null
                    ) ?: state
                }
            }.onFailure { throwable ->
                mutableUiState.update {
                    currentContent()?.copy(
                        isRefreshing = false,
                        errorMessage = throwable.message ?: "重命名文件失败"
                    ) ?: FileBrowserUiState.Error(throwable.message ?: "重命名文件失败")
                }
            }
        }
    }

    private fun refreshFiles(
        spaceId: Long,
        folderId: Long?,
        folderName: String,
        isUploading: Boolean = currentContent()?.isUploading ?: false,
        trashMode: Boolean = currentContent()?.isTrashMode ?: false
    ) {
        val current = currentContent() ?: FileBrowserUiState.Content()
        mutableUiState.value = current.copy(
            selectedFolderId = folderId,
            selectedFolderName = folderName,
            isTrashMode = trashMode,
            isRefreshing = true,
            isUploading = isUploading,
            errorMessage = null
        )
        viewModelScope.launch {
            val result = fileRepository.page(
                spaceId = spaceId,
                query = ManagedFileQuery(
                    folderId = folderId.takeIf { !trashMode },
                    pageSize = PAGE_SIZE,
                    trashed = trashMode
                )
            )
            result.onSuccess { page ->
                mutableUiState.update {
                    currentContent()?.copy(
                        files = page.list,
                        selectedFolderId = folderId,
                        selectedFolderName = folderName,
                        isTrashMode = trashMode,
                        isRefreshing = false,
                        isUploading = false
                    ) ?: FileBrowserUiState.Content(files = page.list)
                }
            }.onFailure { throwable ->
                mutableUiState.update {
                    currentContent()?.copy(
                        isRefreshing = false,
                        isUploading = false,
                        errorMessage = throwable.message ?: "文件加载失败"
                    ) ?: FileBrowserUiState.Error(throwable.message ?: "文件加载失败")
                }
            }
        }
    }

    private fun currentContent(): FileBrowserUiState.Content? {
        return mutableUiState.value as? FileBrowserUiState.Content
    }

    private fun readSelectedFile(uri: Uri, folderId: Long?): ManagedFileUpload {
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
            throw IllegalArgumentException("当前版本支持 20 MB 内的文件直接上传")
        }
        val bytes = resolver.openInputStream(uri)?.use { inputStream ->
            inputStream.readBytes()
        } ?: throw IllegalArgumentException("无法读取所选文件")
        val mimeType = resolver.getType(uri) ?: DEFAULT_MIME_TYPE
        return ManagedFileUpload(
            fileName = fileName,
            mimeType = mimeType,
            bytes = bytes,
            folderId = folderId
        )
    }

    private companion object {
        const val PAGE_SIZE = 50L
        const val DEFAULT_UPLOAD_NAME = "upload.bin"
        const val DEFAULT_MIME_TYPE = "application/octet-stream"
        const val MAX_DIRECT_UPLOAD_BYTES = 20L * 1024L * 1024L
    }
}

private fun List<FileFolder>.containsFolder(folderId: Long?): Boolean {
    if (folderId == null) {
        return false
    }
    return any { folder -> folder.id == folderId || folder.children.containsFolder(folderId) }
}
