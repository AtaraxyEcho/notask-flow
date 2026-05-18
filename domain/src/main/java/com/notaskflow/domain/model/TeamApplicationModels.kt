package com.notaskflow.domain.model

data class SpaceJoinApplication(
    val id: Long,
    val applicantUserId: Long?,
    val applicantUsername: String,
    val applicantEmail: String?,
    val supervisorUserId: Long?,
    val supervisorUsername: String?,
    val targetSpaceId: Long?,
    val targetSpaceName: String?,
    val teamName: String?,
    val status: SpaceJoinApplicationStatus,
    val remark: String?,
    val rejectReason: String?,
    val reviewerUserId: Long?,
    val reviewedAt: String?,
    val gmtCreate: String?
)

enum class SpaceJoinApplicationStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED
}

data class SpaceJoinApply(
    val supervisorAccount: String,
    val teamName: String? = null,
    val remark: String? = null
)

data class SpaceJoinApprove(
    val spaceId: Long,
    val roleCode: String
)

data class SpaceJoinReject(
    val reason: String? = null
)
