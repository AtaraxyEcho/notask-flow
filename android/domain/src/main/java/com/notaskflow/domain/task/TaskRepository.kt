package com.notaskflow.domain.task

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

interface TaskRepository {
    suspend fun page(spaceId: Long, query: TaskQuery): Result<Page<Task>>

    suspend fun create(spaceId: Long, task: TaskCreate): Result<Task>

    suspend fun get(spaceId: Long, id: Long): Result<Task>

    suspend fun update(spaceId: Long, id: Long, task: TaskUpdate): Result<Task>

    suspend fun delete(spaceId: Long, id: Long): Result<Unit>

    suspend fun updateStatus(spaceId: Long, id: Long, status: TaskStatus): Result<Task>

    suspend fun startMember(spaceId: Long, taskId: Long, memberId: Long): Result<TaskMember>

    suspend fun completeMember(
        spaceId: Long,
        taskId: Long,
        memberId: Long,
        request: TaskMemberComplete
    ): Result<TaskMember>

    suspend fun claim(spaceId: Long, taskId: Long, request: TaskClaim): Result<TaskMember>

    suspend fun comments(spaceId: Long, id: Long): Result<List<TaskComment>>

    suspend fun addComment(spaceId: Long, id: Long, comment: TaskCommentSave): Result<TaskComment>

    suspend fun attachments(spaceId: Long, id: Long): Result<List<TaskAttachment>>

    suspend fun uploadAttachment(spaceId: Long, id: Long, upload: TaskAttachmentUpload): Result<TaskAttachment>

    suspend fun unbindAttachment(spaceId: Long, id: Long, attachmentId: Long, referenceKey: String? = null): Result<Unit>
}
