package com.notaskflow.domain.model

data class ProjectMember(
    val projectId: Long,
    val userId: Long,
    val username: String,
    val nickname: String?,
    val email: String?,
    val avatarUrl: String?,
    val role: ProjectMemberRole,
    val joinedAt: String?
)

enum class ProjectMemberRole {
    OWNER,
    MEMBER
}

data class Project(
    val id: Long,
    val spaceId: Long,
    val name: String,
    val description: String?,
    val coverColor: String?,
    val coverImageUrl: String?,
    val archived: Boolean,
    val ownerUserId: Long?,
    val taskCount: Long,
    val completedTaskCount: Long,
    val overdueTaskCount: Long,
    val documentCount: Long,
    val completionRate: Int,
    val gmtCreate: String?,
    val gmtModified: String?,
    val members: List<ProjectMember>
)

data class ProjectSave(
    val name: String,
    val description: String?,
    val coverColor: String?,
    val coverImageUrl: String?,
    val ownerUserId: Long?
)

data class ProjectMemberSave(
    val userId: Long,
    val role: ProjectMemberRole
)

data class ProjectQuery(
    val pageNum: Long = 1,
    val pageSize: Long = 20,
    val keyword: String? = null,
    val archived: Boolean? = null
)
