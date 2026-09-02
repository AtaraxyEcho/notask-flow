package com.notaskflow.data.space.dto

import com.notaskflow.data.common.toAvatarProxyUrl
import com.notaskflow.domain.model.Space
import com.notaskflow.domain.model.SpaceCreate
import com.notaskflow.domain.model.SpaceInvite
import com.notaskflow.domain.model.SpaceMember
import com.notaskflow.domain.model.SpaceType
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SpaceDto(
    @param:Json(name = "id") val id: Long,
    @param:Json(name = "name") val name: String,
    @param:Json(name = "type") val type: String,
    @param:Json(name = "ownerUserId") val ownerUserId: Long?,
    @param:Json(name = "memberCount") val memberCount: Long?,
    @param:Json(name = "unreadCount") val unreadCount: Long?,
    @param:Json(name = "joinApprovalRequired") val joinApprovalRequired: Boolean?
)

@JsonClass(generateAdapter = true)
data class SpaceCreateRequestDto(
    @param:Json(name = "name") val name: String
)

@JsonClass(generateAdapter = true)
data class SpaceMemberDto(
    @param:Json(name = "spaceId") val spaceId: Long?,
    @param:Json(name = "userId") val userId: Long?,
    @param:Json(name = "username") val username: String?,
    @param:Json(name = "nickname") val nickname: String?,
    @param:Json(name = "email") val email: String?,
    @param:Json(name = "avatarUrl") val avatarUrl: String?,
    @param:Json(name = "roleCode") val roleCode: String?,
    @param:Json(name = "roleName") val roleName: String?,
    @param:Json(name = "gmtJoined") val gmtJoined: String?,
    @param:Json(name = "online") val online: Boolean?
)

@JsonClass(generateAdapter = true)
data class SpaceInviteCreateRequestDto(
    @param:Json(name = "roleCode") val roleCode: String,
    @param:Json(name = "expireMinutes") val expireMinutes: Int?
)

@JsonClass(generateAdapter = true)
data class SpaceInviteDto(
    @param:Json(name = "code") val code: String?,
    @param:Json(name = "spaceId") val spaceId: Long?,
    @param:Json(name = "roleCode") val roleCode: String?,
    @param:Json(name = "expiresAt") val expiresAt: String?
)

@JsonClass(generateAdapter = true)
data class SpaceMemberRoleUpdateRequestDto(
    @param:Json(name = "roleCode") val roleCode: String
)

fun SpaceDto.toDomain(): Space {
    return Space(
        id = id,
        name = name,
        type = runCatching { SpaceType.valueOf(type) }.getOrDefault(SpaceType.PERSONAL),
        ownerUserId = ownerUserId,
        memberCount = memberCount ?: 0L,
        unreadCount = unreadCount ?: 0L,
        joinApprovalRequired = joinApprovalRequired == true
    )
}

fun SpaceCreate.toDto(): SpaceCreateRequestDto {
    return SpaceCreateRequestDto(name = name)
}

fun SpaceMemberDto.toDomain(): SpaceMember {
    return SpaceMember(
        spaceId = spaceId ?: 0L,
        userId = userId ?: 0L,
        username = username.orEmpty(),
        nickname = nickname,
        email = email,
        avatarUrl = avatarUrl.toAvatarProxyUrl(userId),
        roleCode = roleCode.orEmpty(),
        roleName = roleName.orEmpty(),
        gmtJoined = gmtJoined,
        online = online == true
    )
}

fun SpaceInviteDto.toDomain(): SpaceInvite {
    return SpaceInvite(
        code = code.orEmpty(),
        spaceId = spaceId ?: 0L,
        roleCode = roleCode.orEmpty(),
        expiresAt = expiresAt
    )
}
