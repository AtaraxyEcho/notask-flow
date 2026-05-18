package com.notaskflow.data.team.dto

import com.notaskflow.domain.model.SpaceJoinApplication
import com.notaskflow.domain.model.SpaceJoinApplicationStatus
import com.notaskflow.domain.model.SpaceJoinApply
import com.notaskflow.domain.model.SpaceJoinApprove
import com.notaskflow.domain.model.SpaceJoinReject
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SpaceJoinApplicationDto(
    @param:Json(name = "id") val id: Long,
    @param:Json(name = "applicantUserId") val applicantUserId: Long?,
    @param:Json(name = "applicantUsername") val applicantUsername: String?,
    @param:Json(name = "applicantEmail") val applicantEmail: String?,
    @param:Json(name = "supervisorUserId") val supervisorUserId: Long?,
    @param:Json(name = "supervisorUsername") val supervisorUsername: String?,
    @param:Json(name = "targetSpaceId") val targetSpaceId: Long?,
    @param:Json(name = "targetSpaceName") val targetSpaceName: String?,
    @param:Json(name = "teamName") val teamName: String?,
    @param:Json(name = "status") val status: String?,
    @param:Json(name = "remark") val remark: String?,
    @param:Json(name = "rejectReason") val rejectReason: String?,
    @param:Json(name = "reviewerUserId") val reviewerUserId: Long?,
    @param:Json(name = "reviewedAt") val reviewedAt: String?,
    @param:Json(name = "gmtCreate") val gmtCreate: String?
)

@JsonClass(generateAdapter = true)
data class SpaceJoinApplyRequestDto(
    @param:Json(name = "supervisorAccount") val supervisorAccount: String,
    @param:Json(name = "teamName") val teamName: String?,
    @param:Json(name = "remark") val remark: String?
)

@JsonClass(generateAdapter = true)
data class SpaceJoinApproveRequestDto(
    @param:Json(name = "spaceId") val spaceId: Long,
    @param:Json(name = "roleCode") val roleCode: String
)

@JsonClass(generateAdapter = true)
data class SpaceJoinRejectRequestDto(
    @param:Json(name = "reason") val reason: String?
)

fun SpaceJoinApplicationDto.toDomain(): SpaceJoinApplication {
    return SpaceJoinApplication(
        id = id,
        applicantUserId = applicantUserId,
        applicantUsername = applicantUsername.orEmpty(),
        applicantEmail = applicantEmail,
        supervisorUserId = supervisorUserId,
        supervisorUsername = supervisorUsername,
        targetSpaceId = targetSpaceId,
        targetSpaceName = targetSpaceName,
        teamName = teamName,
        status = runCatching {
            SpaceJoinApplicationStatus.valueOf(status.orEmpty())
        }.getOrDefault(SpaceJoinApplicationStatus.PENDING),
        remark = remark,
        rejectReason = rejectReason,
        reviewerUserId = reviewerUserId,
        reviewedAt = reviewedAt,
        gmtCreate = gmtCreate
    )
}

fun SpaceJoinApply.toDto(): SpaceJoinApplyRequestDto {
    return SpaceJoinApplyRequestDto(
        supervisorAccount = supervisorAccount,
        teamName = teamName,
        remark = remark
    )
}

fun SpaceJoinApprove.toDto(): SpaceJoinApproveRequestDto {
    return SpaceJoinApproveRequestDto(
        spaceId = spaceId,
        roleCode = roleCode
    )
}

fun SpaceJoinReject.toDto(): SpaceJoinRejectRequestDto {
    return SpaceJoinRejectRequestDto(reason = reason)
}
