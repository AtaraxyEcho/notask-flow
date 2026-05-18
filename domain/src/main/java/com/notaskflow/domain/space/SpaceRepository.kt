package com.notaskflow.domain.space

import com.notaskflow.domain.model.Space
import com.notaskflow.domain.model.SpaceCreate
import com.notaskflow.domain.model.SpaceInvite
import com.notaskflow.domain.model.SpaceMember

interface SpaceRepository {
    suspend fun listSpaces(): Result<List<Space>>
    suspend fun listMembers(spaceId: Long): Result<List<SpaceMember>>
    suspend fun createTeamSpace(request: SpaceCreate): Result<Space>
    suspend fun joinByInviteCode(code: String): Result<Unit>
    suspend fun permissions(spaceId: Long): Result<Set<String>>
    suspend fun createInvite(spaceId: Long, roleCode: String, expireMinutes: Int?): Result<SpaceInvite>
    suspend fun updateMemberRole(spaceId: Long, userId: Long, roleCode: String): Result<SpaceMember>
    suspend fun removeMember(spaceId: Long, userId: Long): Result<Unit>
}
