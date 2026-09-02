package com.notaskflow.domain.model

data class TaskTrend(
    val date: String,
    val createdCount: Long,
    val completedCount: Long
)

data class PersonalStats(
    val noteCount: Long,
    val unfinishedTaskMemberCount: Long,
    val completedTaskCountThisMonth: Long
)

data class PersonalNoteTrend(
    val date: String,
    val createdCount: Long,
    val updatedCount: Long
)

data class RoleCompletion(
    val roleId: Long?,
    val roleCode: String?,
    val roleName: String,
    val completedCount: Long
)

data class MemberTaskLoad(
    val userId: Long,
    val username: String,
    val loadCount: Long,
    val completedCount: Long
)

data class StatsActivity(
    val time: String?,
    val memberUserId: Long?,
    val member: String,
    val type: String,
    val content: String,
    val impact: String?
)
