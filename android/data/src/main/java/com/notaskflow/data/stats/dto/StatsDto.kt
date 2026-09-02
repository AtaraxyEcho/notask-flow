package com.notaskflow.data.stats.dto

import com.notaskflow.domain.model.MemberTaskLoad
import com.notaskflow.domain.model.PersonalNoteTrend
import com.notaskflow.domain.model.PersonalStats
import com.notaskflow.domain.model.RoleCompletion
import com.notaskflow.domain.model.StatsActivity
import com.notaskflow.domain.model.TaskTrend
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PersonalStatsDto(
    @param:Json(name = "noteCount") val noteCount: Long?,
    @param:Json(name = "unfinishedTaskMemberCount") val unfinishedTaskMemberCount: Long?,
    @param:Json(name = "completedTaskCountThisMonth") val completedTaskCountThisMonth: Long?
)

@JsonClass(generateAdapter = true)
data class PersonalNoteTrendDto(
    @param:Json(name = "date") val date: String?,
    @param:Json(name = "createdCount") val createdCount: Long?,
    @param:Json(name = "updatedCount") val updatedCount: Long?
)

@JsonClass(generateAdapter = true)
data class TaskTrendDto(
    @param:Json(name = "date") val date: String?,
    @param:Json(name = "createdCount") val createdCount: Long?,
    @param:Json(name = "completedCount") val completedCount: Long?
)

@JsonClass(generateAdapter = true)
data class RoleCompletionDto(
    @param:Json(name = "roleId") val roleId: Long?,
    @param:Json(name = "roleCode") val roleCode: String?,
    @param:Json(name = "roleName") val roleName: String?,
    @param:Json(name = "completedCount") val completedCount: Long?
)

@JsonClass(generateAdapter = true)
data class MemberTaskLoadDto(
    @param:Json(name = "userId") val userId: Long?,
    @param:Json(name = "username") val username: String?,
    @param:Json(name = "loadCount") val loadCount: Long?,
    @param:Json(name = "completedCount") val completedCount: Long?
)

@JsonClass(generateAdapter = true)
data class StatsActivityDto(
    @param:Json(name = "time") val time: String?,
    @param:Json(name = "memberUserId") val memberUserId: Long?,
    @param:Json(name = "member") val member: String?,
    @param:Json(name = "type") val type: String?,
    @param:Json(name = "content") val content: String?,
    @param:Json(name = "impact") val impact: String?
)

fun PersonalStatsDto.toDomain(): PersonalStats {
    return PersonalStats(
        noteCount = noteCount ?: 0L,
        unfinishedTaskMemberCount = unfinishedTaskMemberCount ?: 0L,
        completedTaskCountThisMonth = completedTaskCountThisMonth ?: 0L
    )
}

fun PersonalNoteTrendDto.toDomain(): PersonalNoteTrend {
    return PersonalNoteTrend(
        date = date.orEmpty(),
        createdCount = createdCount ?: 0L,
        updatedCount = updatedCount ?: 0L
    )
}

fun TaskTrendDto.toDomain(): TaskTrend {
    return TaskTrend(
        date = date.orEmpty(),
        createdCount = createdCount ?: 0L,
        completedCount = completedCount ?: 0L
    )
}

fun RoleCompletionDto.toDomain(): RoleCompletion {
    return RoleCompletion(
        roleId = roleId,
        roleCode = roleCode,
        roleName = roleName.orEmpty(),
        completedCount = completedCount ?: 0L
    )
}

fun MemberTaskLoadDto.toDomain(): MemberTaskLoad {
    return MemberTaskLoad(
        userId = userId ?: 0L,
        username = username.orEmpty(),
        loadCount = loadCount ?: 0L,
        completedCount = completedCount ?: 0L
    )
}

fun StatsActivityDto.toDomain(): StatsActivity {
    return StatsActivity(
        time = time,
        memberUserId = memberUserId,
        member = member.orEmpty(),
        type = type.orEmpty(),
        content = content.orEmpty(),
        impact = impact
    )
}
