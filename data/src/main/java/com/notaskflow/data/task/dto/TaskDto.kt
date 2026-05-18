package com.notaskflow.data.task.dto

import com.notaskflow.domain.model.Task
import com.notaskflow.domain.model.TaskAssignmentCreate
import com.notaskflow.domain.model.TaskAssignmentType
import com.notaskflow.domain.model.TaskClaim
import com.notaskflow.domain.model.TaskCreate
import com.notaskflow.domain.model.TaskComment
import com.notaskflow.domain.model.TaskCommentSave
import com.notaskflow.domain.model.TaskMember
import com.notaskflow.domain.model.TaskMemberComplete
import com.notaskflow.domain.model.TaskMemberStatus
import com.notaskflow.domain.model.TaskMode
import com.notaskflow.domain.model.TaskPriority
import com.notaskflow.domain.model.TaskStatus
import com.notaskflow.domain.model.TaskUpdate
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TaskMemberDto(
    @param:Json(name = "id") val id: Long,
    @param:Json(name = "taskId") val taskId: Long,
    @param:Json(name = "userId") val userId: Long,
    @param:Json(name = "username") val username: String?,
    @param:Json(name = "responsibility") val responsibility: String?,
    @param:Json(name = "assignmentType") val assignmentType: String?,
    @param:Json(name = "status") val status: String?,
    @param:Json(name = "isRequired") val isRequired: Boolean?,
    @param:Json(name = "startedAt") val startedAt: String?,
    @param:Json(name = "completedAt") val completedAt: String?,
    @param:Json(name = "completionRemark") val completionRemark: String?,
    @param:Json(name = "version") val version: Int?
)

@JsonClass(generateAdapter = true)
data class TaskDto(
    @param:Json(name = "id") val id: Long,
    @param:Json(name = "spaceId") val spaceId: Long,
    @param:Json(name = "projectId") val projectId: Long?,
    @param:Json(name = "projectName") val projectName: String?,
    @param:Json(name = "title") val title: String,
    @param:Json(name = "description") val description: String?,
    @param:Json(name = "creatorId") val creatorId: Long?,
    @param:Json(name = "mode") val mode: String?,
    @param:Json(name = "status") val status: String?,
    @param:Json(name = "priority") val priority: String?,
    @param:Json(name = "deadline") val deadline: String?,
    @param:Json(name = "completedAt") val completedAt: String?,
    @param:Json(name = "gmtCreate") val gmtCreate: String?,
    @param:Json(name = "gmtModified") val gmtModified: String?,
    @param:Json(name = "members") val members: List<TaskMemberDto>?
)

@JsonClass(generateAdapter = true)
data class TaskCommentDto(
    @param:Json(name = "id") val id: Long,
    @param:Json(name = "taskId") val taskId: Long,
    @param:Json(name = "userId") val userId: Long,
    @param:Json(name = "username") val username: String?,
    @param:Json(name = "parentCommentId") val parentCommentId: Long?,
    @param:Json(name = "content") val content: String,
    @param:Json(name = "gmtCreate") val gmtCreate: String?,
    @param:Json(name = "mentionUserIds") val mentionUserIds: List<Long>?
)

@JsonClass(generateAdapter = true)
data class TaskStatusUpdateRequestDto(
    @param:Json(name = "status") val status: String
)

@JsonClass(generateAdapter = true)
data class TaskAssignmentRequestDto(
    @param:Json(name = "userId") val userId: Long,
    @param:Json(name = "responsibility") val responsibility: String,
    @param:Json(name = "isRequired") val isRequired: Boolean
)

@JsonClass(generateAdapter = true)
data class TaskCreateRequestDto(
    @param:Json(name = "title") val title: String,
    @param:Json(name = "description") val description: String?,
    @param:Json(name = "mode") val mode: String,
    @param:Json(name = "priority") val priority: String,
    @param:Json(name = "deadline") val deadline: String?,
    @param:Json(name = "projectId") val projectId: Long?,
    @param:Json(name = "assignments") val assignments: List<TaskAssignmentRequestDto>
)

@JsonClass(generateAdapter = true)
data class TaskUpdateRequestDto(
    @param:Json(name = "title") val title: String,
    @param:Json(name = "description") val description: String?,
    @param:Json(name = "priority") val priority: String,
    @param:Json(name = "deadline") val deadline: String?,
    @param:Json(name = "projectId") val projectId: Long?
)

@JsonClass(generateAdapter = true)
data class TaskMemberCompleteRequestDto(
    @param:Json(name = "completionRemark") val completionRemark: String?
)

@JsonClass(generateAdapter = true)
data class TaskClaimRequestDto(
    @param:Json(name = "responsibility") val responsibility: String,
    @param:Json(name = "isRequired") val isRequired: Boolean
)

@JsonClass(generateAdapter = true)
data class TaskCommentCreateRequestDto(
    @param:Json(name = "parentCommentId") val parentCommentId: Long?,
    @param:Json(name = "content") val content: String,
    @param:Json(name = "mentionUserIds") val mentionUserIds: List<Long>
)

fun TaskDto.toDomain(): Task {
    return Task(
        id = id,
        spaceId = spaceId,
        projectId = projectId,
        projectName = projectName,
        title = title,
        description = description,
        creatorId = creatorId,
        mode = parseMode(mode),
        status = parseStatus(status),
        priority = parsePriority(priority),
        deadline = deadline,
        completedAt = completedAt,
        gmtCreate = gmtCreate,
        gmtModified = gmtModified,
        members = members.orEmpty().map { it.toDomain() }
    )
}

fun TaskCommentDto.toDomain(): TaskComment {
    return TaskComment(
        id = id,
        taskId = taskId,
        userId = userId,
        username = username.orEmpty(),
        parentCommentId = parentCommentId,
        content = content,
        gmtCreate = gmtCreate,
        mentionUserIds = mentionUserIds.orEmpty()
    )
}

fun TaskCommentSave.toDto(): TaskCommentCreateRequestDto {
    return TaskCommentCreateRequestDto(
        parentCommentId = parentCommentId,
        content = content,
        mentionUserIds = mentionUserIds
    )
}

fun TaskCreate.toDto(): TaskCreateRequestDto {
    return TaskCreateRequestDto(
        title = title,
        description = description,
        mode = mode.name,
        priority = priority.name,
        deadline = deadline,
        projectId = projectId,
        assignments = assignments.map { it.toDto() }
    )
}

fun TaskUpdate.toDto(): TaskUpdateRequestDto {
    return TaskUpdateRequestDto(
        title = title,
        description = description,
        priority = priority.name,
        deadline = deadline,
        projectId = projectId
    )
}

fun TaskMemberComplete.toDto(): TaskMemberCompleteRequestDto {
    return TaskMemberCompleteRequestDto(completionRemark = completionRemark)
}

fun TaskClaim.toDto(): TaskClaimRequestDto {
    return TaskClaimRequestDto(
        responsibility = responsibility,
        isRequired = isRequired
    )
}

private fun TaskAssignmentCreate.toDto(): TaskAssignmentRequestDto {
    return TaskAssignmentRequestDto(
        userId = userId,
        responsibility = responsibility,
        isRequired = isRequired
    )
}

fun TaskMemberDto.toDomain(): TaskMember {
    return TaskMember(
        id = id,
        taskId = taskId,
        userId = userId,
        username = username.orEmpty(),
        responsibility = responsibility,
        assignmentType = parseAssignmentType(assignmentType),
        status = parseMemberStatus(status),
        isRequired = isRequired == true,
        startedAt = startedAt,
        completedAt = completedAt,
        completionRemark = completionRemark,
        version = version ?: 0
    )
}

private fun parseMode(value: String?): TaskMode {
    return runCatching { TaskMode.valueOf(value.orEmpty()) }.getOrDefault(TaskMode.ASSIGNED)
}

private fun parseStatus(value: String?): TaskStatus {
    return runCatching { TaskStatus.valueOf(value.orEmpty()) }.getOrDefault(TaskStatus.PENDING)
}

private fun parsePriority(value: String?): TaskPriority {
    return runCatching { TaskPriority.valueOf(value.orEmpty()) }.getOrDefault(TaskPriority.MEDIUM)
}

private fun parseMemberStatus(value: String?): TaskMemberStatus {
    return runCatching { TaskMemberStatus.valueOf(value.orEmpty()) }.getOrDefault(TaskMemberStatus.PENDING)
}

private fun parseAssignmentType(value: String?): TaskAssignmentType {
    return runCatching { TaskAssignmentType.valueOf(value.orEmpty()) }.getOrDefault(TaskAssignmentType.ASSIGNED)
}
