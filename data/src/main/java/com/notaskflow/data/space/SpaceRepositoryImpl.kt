package com.notaskflow.data.space

import com.notaskflow.data.space.api.SpaceApi
import com.notaskflow.data.space.dto.SpaceInviteCreateRequestDto
import com.notaskflow.data.space.dto.SpaceMemberRoleUpdateRequestDto
import com.notaskflow.data.space.dto.toDomain
import com.notaskflow.data.space.dto.toDto
import com.notaskflow.domain.model.Space
import com.notaskflow.domain.model.SpaceCreate
import com.notaskflow.domain.model.SpaceInvite
import com.notaskflow.domain.model.SpaceMember
import com.notaskflow.domain.space.SpaceRepository
import javax.inject.Inject

class SpaceRepositoryImpl @Inject constructor(
    private val spaceApi: SpaceApi
) : SpaceRepository {
    override suspend fun listSpaces(): Result<List<Space>> {
        return runCatching {
            spaceApi.listSpaces().getOrThrow().map { it.toDomain() }
        }
    }

    override suspend fun listMembers(spaceId: Long): Result<List<SpaceMember>> {
        return runCatching {
            spaceApi.listMembers(spaceId).getOrThrow().map { it.toDomain() }
        }
    }

    override suspend fun createTeamSpace(request: SpaceCreate): Result<Space> {
        return runCatching {
            spaceApi.createTeamSpace(request.toDto()).getOrThrow().toDomain()
        }
    }

    override suspend fun joinByInviteCode(code: String): Result<Unit> {
        return runCatching {
            spaceApi.joinByInviteCode(code).getOrThrow()
            Unit
        }
    }

    override suspend fun permissions(spaceId: Long): Result<Set<String>> {
        return runCatching {
            spaceApi.permissions(spaceId).getOrThrow().toSet()
        }
    }

    override suspend fun createInvite(spaceId: Long, roleCode: String, expireMinutes: Int?): Result<SpaceInvite> {
        return runCatching {
            spaceApi.createInvite(
                spaceId = spaceId,
                request = SpaceInviteCreateRequestDto(
                    roleCode = roleCode,
                    expireMinutes = expireMinutes
                )
            ).getOrThrow().toDomain()
        }
    }

    override suspend fun updateMemberRole(spaceId: Long, userId: Long, roleCode: String): Result<SpaceMember> {
        return runCatching {
            spaceApi.updateMemberRole(
                spaceId = spaceId,
                userId = userId,
                request = SpaceMemberRoleUpdateRequestDto(roleCode = roleCode)
            ).getOrThrow().toDomain()
        }
    }

    override suspend fun removeMember(spaceId: Long, userId: Long): Result<Unit> {
        return runCatching {
            spaceApi.removeMember(spaceId, userId).requireSuccess()
        }
    }
}
