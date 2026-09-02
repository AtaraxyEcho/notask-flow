package com.notaskflow.data.task

import com.notaskflow.data.common.toDomain
import com.notaskflow.data.attachment.dto.taskAttachmentUnbindRequestDto
import com.notaskflow.data.attachment.dto.toTaskBindRequestDto
import com.notaskflow.data.attachment.dto.toTaskDomain
import com.notaskflow.data.task.api.TaskApi
import com.notaskflow.data.task.dto.TaskStatusUpdateRequestDto
import com.notaskflow.data.task.dto.toDomain
import com.notaskflow.data.task.dto.toDto
import com.notaskflow.domain.model.Page
import com.notaskflow.domain.model.Task
import com.notaskflow.domain.model.TaskAttachment
import com.notaskflow.domain.model.TaskAttachmentUpload
import com.notaskflow.domain.model.TaskClaim
import com.notaskflow.domain.model.TaskCreate
import com.notaskflow.domain.model.TaskComment
import com.notaskflow.domain.model.TaskCommentSave
import com.notaskflow.domain.model.TaskMember
import com.notaskflow.domain.model.TaskMemberComplete
import com.notaskflow.domain.model.TaskQuery
import com.notaskflow.domain.model.TaskStatus
import com.notaskflow.domain.model.TaskUpdate
import com.notaskflow.domain.task.TaskRepository
import javax.inject.Inject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class TaskRepositoryImpl @Inject constructor(
    private val taskApi: TaskApi
) : TaskRepository {
    override suspend fun page(spaceId: Long, query: TaskQuery): Result<Page<Task>> {
        return runCatching {
            taskApi.page(
                spaceId = spaceId,
                pageNum = query.pageNum,
                pageSize = query.pageSize,
                keyword = query.keyword,
                status = query.status?.name,
                mode = query.mode?.name,
                assigneeId = query.assigneeId,
                projectId = query.projectId
            ).getOrThrow().toDomain { it.toDomain() }
        }
    }

    override suspend fun create(spaceId: Long, task: TaskCreate): Result<Task> {
        return runCatching {
            taskApi.create(spaceId, task.toDto()).getOrThrow().toDomain()
        }
    }

    override suspend fun get(spaceId: Long, id: Long): Result<Task> {
        return runCatching {
            taskApi.get(spaceId, id).getOrThrow().toDomain()
        }
    }

    override suspend fun update(spaceId: Long, id: Long, task: TaskUpdate): Result<Task> {
        return runCatching {
            taskApi.update(spaceId, id, task.toDto()).getOrThrow().toDomain()
        }
    }

    override suspend fun delete(spaceId: Long, id: Long): Result<Unit> {
        return runCatching {
            taskApi.delete(spaceId, id).requireSuccess()
        }
    }

    override suspend fun updateStatus(spaceId: Long, id: Long, status: TaskStatus): Result<Task> {
        return runCatching {
            taskApi.updateStatus(
                spaceId = spaceId,
                id = id,
                request = TaskStatusUpdateRequestDto(status = status.name)
            ).getOrThrow().toDomain()
        }
    }

    override suspend fun startMember(spaceId: Long, taskId: Long, memberId: Long): Result<TaskMember> {
        return runCatching {
            taskApi.startMember(spaceId, taskId, memberId).getOrThrow().toDomain()
        }
    }

    override suspend fun completeMember(
        spaceId: Long,
        taskId: Long,
        memberId: Long,
        request: TaskMemberComplete
    ): Result<TaskMember> {
        return runCatching {
            taskApi.completeMember(spaceId, taskId, memberId, request.toDto()).getOrThrow().toDomain()
        }
    }

    override suspend fun claim(spaceId: Long, taskId: Long, request: TaskClaim): Result<TaskMember> {
        return runCatching {
            taskApi.claim(spaceId, taskId, request.toDto()).getOrThrow().toDomain()
        }
    }

    override suspend fun comments(spaceId: Long, id: Long): Result<List<TaskComment>> {
        return runCatching {
            taskApi.comments(spaceId, id).getOrThrow().map { it.toDomain() }
        }
    }

    override suspend fun addComment(
        spaceId: Long,
        id: Long,
        comment: TaskCommentSave
    ): Result<TaskComment> {
        return runCatching {
            taskApi.addComment(spaceId, id, comment.toDto()).getOrThrow().toDomain()
        }
    }

    override suspend fun attachments(spaceId: Long, id: Long): Result<List<TaskAttachment>> {
        return runCatching {
            taskApi.attachments(spaceId, id).getOrThrow().map { it.toTaskDomain() }
        }
    }

    override suspend fun uploadAttachment(
        spaceId: Long,
        id: Long,
        upload: TaskAttachmentUpload
    ): Result<TaskAttachment> {
        return runCatching {
            val body = upload.bytes.toRequestBody(upload.mimeType.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData(
                name = "file",
                filename = upload.fileName,
                body = body
            )
            val attachment = taskApi.uploadAttachment(spaceId, part).getOrThrow()
            try {
                taskApi.bindAttachment(spaceId, upload.toTaskBindRequestDto(attachment.id, id)).requireSuccess()
                attachment.toTaskDomain()
            } catch (throwable: Throwable) {
                runCatching {
                    taskApi.deleteAttachment(spaceId, attachment.id).requireSuccess()
                }
                throw throwable
            }
        }
    }

    override suspend fun unbindAttachment(
        spaceId: Long,
        id: Long,
        attachmentId: Long,
        referenceKey: String?
    ): Result<Unit> {
        return runCatching {
            taskApi.unbindAttachment(
                spaceId = spaceId,
                attachmentId = attachmentId,
                request = taskAttachmentUnbindRequestDto(id, referenceKey)
            ).requireSuccess()
        }
    }
}
