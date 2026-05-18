package com.notaskflow.feature.task

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notaskflow.domain.model.Task
import com.notaskflow.domain.model.TaskAttachment
import com.notaskflow.domain.model.TaskAttachmentUpload
import com.notaskflow.domain.model.TaskClaim
import com.notaskflow.domain.model.TaskComment
import com.notaskflow.domain.model.TaskCommentSave
import com.notaskflow.domain.model.TaskMember
import com.notaskflow.domain.model.TaskMemberComplete
import com.notaskflow.domain.model.TaskStatus
import com.notaskflow.domain.model.TaskUpdate
import com.notaskflow.domain.model.SpaceMember
import com.notaskflow.domain.policy.TaskActionPolicy
import com.notaskflow.domain.space.SpaceRepository
import com.notaskflow.domain.task.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TaskDetailUiState(
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val isTeamSpace: Boolean = false,
    val task: Task? = null,
    val comments: List<TaskComment> = emptyList(),
    val members: List<SpaceMember> = emptyList(),
    val selectedMentionUserIds: Set<Long> = emptySet(),
    val attachments: List<TaskAttachment> = emptyList(),
    val commentInput: String = "",
    val isAttachmentLoading: Boolean = false,
    val isAttachmentUploading: Boolean = false,
    val activeAttachmentId: Long? = null,
    val errorMessage: String? = null
) {
    val availableStatusTargets: List<TaskStatus>
        get() = task?.let { TaskActionPolicy.availableStatusTargets(it) }.orEmpty()
}

sealed interface TaskDetailEffect {
    data object Deleted : TaskDetailEffect
}

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val taskRepository: TaskRepository,
    private val spaceRepository: SpaceRepository
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(TaskDetailUiState())
    val uiState: StateFlow<TaskDetailUiState> = mutableUiState

    private val mutableEffect = MutableSharedFlow<TaskDetailEffect>()
    val effect: SharedFlow<TaskDetailEffect> = mutableEffect.asSharedFlow()

    private var currentSpaceId: Long? = null
    private var currentTaskId: Long? = null

    fun load(spaceId: Long, taskId: Long, isTeamSpace: Boolean) {
        currentSpaceId = spaceId
        currentTaskId = taskId
        viewModelScope.launch {
            mutableUiState.update {
                it.copy(
                    isLoading = true,
                    isTeamSpace = isTeamSpace,
                    selectedMentionUserIds = emptySet(),
                    errorMessage = null
                )
            }
            val taskResult = taskRepository.get(spaceId, taskId)
            val commentsResult = taskRepository.comments(spaceId, taskId)
            val membersResult = if (isTeamSpace) {
                spaceRepository.listMembers(spaceId)
            } else {
                Result.success(emptyList())
            }
            val attachmentsResult = taskRepository.attachments(spaceId, taskId)
            mutableUiState.update { state ->
                state.copy(
                    isLoading = false,
                    task = taskResult.getOrNull() ?: state.task,
                    comments = commentsResult.getOrNull() ?: state.comments,
                    members = membersResult.getOrNull() ?: state.members,
                    attachments = attachmentsResult.getOrNull() ?: state.attachments,
                    errorMessage = taskResult.exceptionOrNull()?.message
                        ?: commentsResult.exceptionOrNull()?.message
                        ?: membersResult.exceptionOrNull()?.message
                        ?: attachmentsResult.exceptionOrNull()?.message
                )
            }
        }
    }

    fun updateCommentInput(value: String) {
        mutableUiState.update { it.copy(commentInput = value, errorMessage = null) }
    }

    fun toggleMention(member: SpaceMember) {
        mutableUiState.update { state ->
            val selected = state.selectedMentionUserIds + member.userId
            val mentionText = "@${member.displayName()}"
            val nextInput = when {
                state.commentInput.endsWith("@") -> state.commentInput.dropLast(1) + mentionText + " "
                state.commentInput.contains(mentionText) -> state.commentInput
                else -> listOf(state.commentInput, mentionText)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                    .plus(" ")
            }
            state.copy(
                selectedMentionUserIds = selected,
                commentInput = nextInput,
                errorMessage = null
            )
        }
    }

    fun updateStatus(status: TaskStatus) {
        val spaceId = currentSpaceId ?: return
        val taskId = currentTaskId ?: return
        viewModelScope.launch {
            mutableUiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            taskRepository.updateStatus(spaceId, taskId, status)
                .onSuccess { task ->
                    mutableUiState.update { it.copy(isSubmitting = false, task = task) }
                }
                .onFailure { throwable ->
                    mutableUiState.update {
                        it.copy(isSubmitting = false, errorMessage = throwable.message ?: "更新任务状态失败")
                    }
                }
        }
    }

    fun deleteTask() {
        val spaceId = currentSpaceId ?: return
        val taskId = currentTaskId ?: return
        viewModelScope.launch {
            mutableUiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            taskRepository.delete(spaceId, taskId)
                .onSuccess {
                    mutableUiState.update { it.copy(isSubmitting = false) }
                    mutableEffect.emit(TaskDetailEffect.Deleted)
                }
                .onFailure { throwable ->
                    mutableUiState.update {
                        it.copy(isSubmitting = false, errorMessage = throwable.message ?: "删除任务失败")
                    }
                }
        }
    }

    fun updateTask(title: String, description: String) {
        val spaceId = currentSpaceId ?: return
        val task = mutableUiState.value.task ?: return
        val trimmedTitle = title.trim()
        if (trimmedTitle.isBlank()) {
            mutableUiState.update { it.copy(errorMessage = "请输入任务标题") }
            return
        }
        viewModelScope.launch {
            mutableUiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            taskRepository.update(
                spaceId = spaceId,
                id = task.id,
                task = TaskUpdate(
                    title = trimmedTitle,
                    description = description.trim().takeIf { it.isNotBlank() },
                    priority = task.priority,
                    deadline = task.deadline,
                    projectId = task.projectId
                )
            ).onSuccess { updated ->
                mutableUiState.update { it.copy(isSubmitting = false, task = updated) }
            }.onFailure { throwable ->
                mutableUiState.update {
                    it.copy(isSubmitting = false, errorMessage = throwable.message ?: "更新任务失败")
                }
            }
        }
    }

    fun startMember(memberId: Long) {
        val spaceId = currentSpaceId ?: return
        val taskId = currentTaskId ?: return
        viewModelScope.launch {
            mutableUiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            taskRepository.startMember(spaceId, taskId, memberId)
                .onSuccess { member -> updateMember(member) }
                .onFailure { throwable ->
                    mutableUiState.update {
                        it.copy(isSubmitting = false, errorMessage = throwable.message ?: "开始职责失败")
                    }
                }
        }
    }

    fun completeMember(memberId: Long, remark: String) {
        val spaceId = currentSpaceId ?: return
        val taskId = currentTaskId ?: return
        viewModelScope.launch {
            mutableUiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            taskRepository.completeMember(
                spaceId = spaceId,
                taskId = taskId,
                memberId = memberId,
                request = TaskMemberComplete(completionRemark = remark.trim().takeIf { it.isNotBlank() })
            ).onSuccess { member ->
                updateMember(member)
            }.onFailure { throwable ->
                mutableUiState.update {
                    it.copy(isSubmitting = false, errorMessage = throwable.message ?: "完成职责失败")
                }
            }
        }
    }

    fun claim(responsibility: String) {
        val spaceId = currentSpaceId ?: return
        val taskId = currentTaskId ?: return
        val trimmedResponsibility = responsibility.trim()
        if (trimmedResponsibility.isBlank()) {
            mutableUiState.update { it.copy(errorMessage = "请输入认领职责") }
            return
        }
        viewModelScope.launch {
            mutableUiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            taskRepository.claim(
                spaceId = spaceId,
                taskId = taskId,
                request = TaskClaim(responsibility = trimmedResponsibility)
            ).onSuccess {
                load(spaceId, taskId, mutableUiState.value.isTeamSpace)
            }.onFailure { throwable ->
                mutableUiState.update {
                    it.copy(isSubmitting = false, errorMessage = throwable.message ?: "认领任务失败")
                }
            }
        }
    }

    fun addComment() {
        val spaceId = currentSpaceId ?: return
        val taskId = currentTaskId ?: return
        val content = mutableUiState.value.commentInput.trim()
        if (content.isBlank()) {
            mutableUiState.update { it.copy(errorMessage = "请输入评论内容") }
            return
        }
        viewModelScope.launch {
            val mentionUserIds = mutableUiState.value.selectedMentionUserIds.filter { userId ->
                val member = mutableUiState.value.members.firstOrNull { item -> item.userId == userId }
                member?.let { content.contains("@${it.displayName()}") } == true
            }
            mutableUiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            taskRepository.addComment(
                spaceId = spaceId,
                id = taskId,
                comment = TaskCommentSave(
                    content = content,
                    mentionUserIds = mentionUserIds
                )
            ).onSuccess { comment ->
                mutableUiState.update {
                    it.copy(
                        isSubmitting = false,
                        commentInput = "",
                        selectedMentionUserIds = emptySet(),
                        comments = it.comments + comment
                    )
                }
            }.onFailure { throwable ->
                mutableUiState.update {
                    it.copy(isSubmitting = false, errorMessage = throwable.message ?: "发表评论失败")
                }
            }
        }
    }

    private fun SpaceMember.displayName(): String {
        return nickname?.takeIf { it.isNotBlank() } ?: username
    }

    fun loadAttachments() {
        val spaceId = currentSpaceId ?: return
        val taskId = currentTaskId ?: return
        viewModelScope.launch {
            refreshAttachments(spaceId, taskId, showLoading = true)
        }
    }

    fun uploadAttachment(uri: Uri) {
        val spaceId = currentSpaceId ?: return
        val taskId = currentTaskId ?: return
        viewModelScope.launch {
            mutableUiState.update {
                it.copy(
                    isAttachmentUploading = true,
                    activeAttachmentId = null,
                    errorMessage = null
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
            taskRepository.uploadAttachment(spaceId, taskId, upload)
                .onSuccess {
                    refreshAttachments(spaceId, taskId)
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
        val spaceId = currentSpaceId ?: return
        val taskId = currentTaskId ?: return
        viewModelScope.launch {
            mutableUiState.update {
                it.copy(
                    activeAttachmentId = attachmentId,
                    errorMessage = null
                )
            }
            taskRepository.unbindAttachment(spaceId, taskId, attachmentId)
                .onSuccess {
                    refreshAttachments(spaceId, taskId)
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

    private fun updateMember(member: TaskMember) {
        mutableUiState.update { state ->
            val currentTask = state.task
            state.copy(
                isSubmitting = false,
                task = currentTask?.copy(
                    members = currentTask.members.map { existing ->
                        if (existing.id == member.id) member else existing
                    }
                )
            )
        }
    }

    private suspend fun refreshAttachments(
        spaceId: Long,
        taskId: Long,
        showLoading: Boolean = false
    ) {
        if (showLoading) {
            mutableUiState.update { it.copy(isAttachmentLoading = true, errorMessage = null) }
        }
        taskRepository.attachments(spaceId, taskId)
            .onSuccess { attachments ->
                mutableUiState.update {
                    it.copy(
                        attachments = attachments,
                        isAttachmentLoading = false,
                        isAttachmentUploading = false,
                        activeAttachmentId = null,
                        errorMessage = null
                    )
                }
            }
            .onFailure { throwable ->
                mutableUiState.update {
                    it.copy(
                        isAttachmentLoading = false,
                        isAttachmentUploading = false,
                        activeAttachmentId = null,
                        errorMessage = throwable.message ?: "加载附件失败"
                    )
                }
            }
    }

    private fun readSelectedFile(uri: Uri): TaskAttachmentUpload {
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
        return TaskAttachmentUpload(
            fileName = fileName,
            mimeType = mimeType,
            bytes = bytes
        )
    }

    private companion object {
        const val DEFAULT_UPLOAD_NAME = "attachment.bin"
        const val DEFAULT_MIME_TYPE = "application/octet-stream"
        const val MAX_DIRECT_UPLOAD_BYTES = 20L * 1024L * 1024L
    }
}
