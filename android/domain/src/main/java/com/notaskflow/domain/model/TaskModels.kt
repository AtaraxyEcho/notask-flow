package com.notaskflow.domain.model

enum class TaskMode {
    ASSIGNED,
    OPEN
}

enum class TaskStatus {
    PENDING,
    OPEN,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}

enum class TaskPriority {
    LOW,
    MEDIUM,
    HIGH
}

enum class TaskMemberStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED
}

enum class TaskAssignmentType {
    ASSIGNED,
    CLAIMED
}

data class TaskMember(
    val id: Long,
    val taskId: Long,
    val userId: Long,
    val username: String,
    val responsibility: String?,
    val assignmentType: TaskAssignmentType,
    val status: TaskMemberStatus,
    val isRequired: Boolean,
    val startedAt: String?,
    val completedAt: String?,
    val completionRemark: String?,
    val version: Int
)

data class Task(
    val id: Long,
    val spaceId: Long,
    val projectId: Long?,
    val projectName: String?,
    val title: String,
    val description: String?,
    val creatorId: Long?,
    val mode: TaskMode,
    val status: TaskStatus,
    val priority: TaskPriority,
    val deadline: String?,
    val completedAt: String?,
    val gmtCreate: String?,
    val gmtModified: String?,
    val members: List<TaskMember>
)

data class TaskComment(
    val id: Long,
    val taskId: Long,
    val userId: Long,
    val username: String,
    val parentCommentId: Long?,
    val content: String,
    val gmtCreate: String?,
    val mentionUserIds: List<Long>
)

data class TaskAttachment(
    val id: Long,
    val fileName: String,
    val fileSize: Long,
    val mimeType: String?,
    val downloadUrl: String?,
    val gmtCreate: String?
)

data class TaskAttachmentUpload(
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray,
    val referenceKey: String? = null
)

data class TaskQuery(
    val pageNum: Long = 1,
    val pageSize: Long = 20,
    val keyword: String? = null,
    val status: TaskStatus? = null,
    val mode: TaskMode? = null,
    val assigneeId: Long? = null,
    val projectId: Long? = null
)

data class TaskCreate(
    val title: String,
    val description: String? = null,
    val mode: TaskMode = TaskMode.ASSIGNED,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val deadline: String? = null,
    val projectId: Long? = null,
    val assignments: List<TaskAssignmentCreate> = emptyList()
)

data class TaskUpdate(
    val title: String,
    val description: String? = null,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val deadline: String? = null,
    val projectId: Long? = null
)

data class TaskAssignmentCreate(
    val userId: Long,
    val responsibility: String,
    val isRequired: Boolean = true
)

data class TaskMemberComplete(
    val completionRemark: String? = null
)

data class TaskClaim(
    val responsibility: String,
    val isRequired: Boolean = true
)

data class TaskCommentSave(
    val parentCommentId: Long? = null,
    val content: String,
    val mentionUserIds: List<Long> = emptyList()
)
