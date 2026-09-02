package com.notaskflow.data.project.dto

import com.notaskflow.data.common.toAvatarProxyUrl
import com.notaskflow.domain.model.Project
import com.notaskflow.domain.model.ProjectMember
import com.notaskflow.domain.model.ProjectMemberRole
import com.notaskflow.domain.model.ProjectMemberSave
import com.notaskflow.domain.model.ProjectSave
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ProjectMemberDto(
    @param:Json(name = "projectId") val projectId: Long?,
    @param:Json(name = "userId") val userId: Long?,
    @param:Json(name = "username") val username: String?,
    @param:Json(name = "nickname") val nickname: String?,
    @param:Json(name = "email") val email: String?,
    @param:Json(name = "avatarUrl") val avatarUrl: String?,
    @param:Json(name = "role") val role: String?,
    @param:Json(name = "joinedAt") val joinedAt: String?
)

@JsonClass(generateAdapter = true)
data class ProjectDto(
    @param:Json(name = "id") val id: Long,
    @param:Json(name = "spaceId") val spaceId: Long,
    @param:Json(name = "name") val name: String,
    @param:Json(name = "description") val description: String?,
    @param:Json(name = "coverColor") val coverColor: String?,
    @param:Json(name = "coverImageUrl") val coverImageUrl: String?,
    @param:Json(name = "archived") val archived: Boolean?,
    @param:Json(name = "ownerUserId") val ownerUserId: Long?,
    @param:Json(name = "taskCount") val taskCount: Long?,
    @param:Json(name = "completedTaskCount") val completedTaskCount: Long?,
    @param:Json(name = "overdueTaskCount") val overdueTaskCount: Long?,
    @param:Json(name = "documentCount") val documentCount: Long?,
    @param:Json(name = "completionRate") val completionRate: Int?,
    @param:Json(name = "gmtCreate") val gmtCreate: String?,
    @param:Json(name = "gmtModified") val gmtModified: String?,
    @param:Json(name = "members") val members: List<ProjectMemberDto>?
)

@JsonClass(generateAdapter = true)
data class ProjectSaveRequestDto(
    @param:Json(name = "name") val name: String,
    @param:Json(name = "description") val description: String?,
    @param:Json(name = "coverColor") val coverColor: String?,
    @param:Json(name = "coverImageUrl") val coverImageUrl: String?,
    @param:Json(name = "ownerUserId") val ownerUserId: Long?
)

@JsonClass(generateAdapter = true)
data class ProjectArchiveRequestDto(
    @param:Json(name = "archived") val archived: Boolean
)

@JsonClass(generateAdapter = true)
data class ProjectMemberSaveRequestDto(
    @param:Json(name = "userId") val userId: Long,
    @param:Json(name = "role") val role: String
)

@JsonClass(generateAdapter = true)
data class ProjectMemberRoleUpdateRequestDto(
    @param:Json(name = "role") val role: String
)

fun ProjectDto.toDomain(): Project {
    return Project(
        id = id,
        spaceId = spaceId,
        name = name,
        description = description,
        coverColor = coverColor,
        coverImageUrl = coverImageUrl,
        archived = archived == true,
        ownerUserId = ownerUserId,
        taskCount = taskCount ?: 0,
        completedTaskCount = completedTaskCount ?: 0,
        overdueTaskCount = overdueTaskCount ?: 0,
        documentCount = documentCount ?: 0,
        completionRate = completionRate ?: 0,
        gmtCreate = gmtCreate,
        gmtModified = gmtModified,
        members = members.orEmpty().map { it.toDomain(fallbackProjectId = id) }
    )
}

fun ProjectMemberDto.toDomain(fallbackProjectId: Long? = null): ProjectMember {
    return ProjectMember(
        projectId = projectId ?: fallbackProjectId ?: 0,
        userId = userId ?: 0,
        username = username.orEmpty(),
        nickname = nickname,
        email = email,
        avatarUrl = avatarUrl.toAvatarProxyUrl(userId),
        role = parseProjectMemberRole(role),
        joinedAt = joinedAt
    )
}

private fun parseProjectMemberRole(value: String?): ProjectMemberRole {
    return runCatching {
        ProjectMemberRole.valueOf(value.orEmpty())
    }.getOrDefault(ProjectMemberRole.MEMBER)
}

fun ProjectSave.toDto(): ProjectSaveRequestDto {
    return ProjectSaveRequestDto(
        name = name,
        description = description,
        coverColor = coverColor,
        coverImageUrl = coverImageUrl,
        ownerUserId = ownerUserId
    )
}

fun ProjectMemberSave.toDto(): ProjectMemberSaveRequestDto {
    return ProjectMemberSaveRequestDto(
        userId = userId,
        role = role.name
    )
}
