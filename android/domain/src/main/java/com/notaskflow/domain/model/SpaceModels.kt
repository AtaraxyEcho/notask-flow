package com.notaskflow.domain.model

enum class SpaceType {
    PERSONAL,
    TEAM
}

data class Space(
    val id: Long,
    val name: String,
    val type: SpaceType,
    val ownerUserId: Long?,
    val memberCount: Long,
    val unreadCount: Long,
    val joinApprovalRequired: Boolean
)

data class SpaceCreate(
    val name: String
)

data class SpaceInvite(
    val code: String,
    val spaceId: Long,
    val roleCode: String,
    val expiresAt: String?
)

data class SpaceMember(
    val spaceId: Long,
    val userId: Long,
    val username: String,
    val nickname: String?,
    val email: String?,
    val avatarUrl: String?,
    val roleCode: String,
    val roleName: String,
    val gmtJoined: String?,
    val online: Boolean
)
