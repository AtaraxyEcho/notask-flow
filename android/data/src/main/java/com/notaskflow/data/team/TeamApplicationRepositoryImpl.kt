package com.notaskflow.data.team

import com.notaskflow.data.common.toDomain
import com.notaskflow.data.team.api.TeamApplicationApi
import com.notaskflow.data.team.dto.toDomain
import com.notaskflow.data.team.dto.toDto
import com.notaskflow.domain.model.SpaceJoinApplication
import com.notaskflow.domain.model.SpaceJoinApply
import com.notaskflow.domain.model.SpaceJoinApprove
import com.notaskflow.domain.model.SpaceJoinReject
import com.notaskflow.domain.team.TeamApplicationRepository
import javax.inject.Inject

class TeamApplicationRepositoryImpl @Inject constructor(
    private val teamApplicationApi: TeamApplicationApi
) : TeamApplicationRepository {
    override suspend fun apply(request: SpaceJoinApply): Result<SpaceJoinApplication> {
        return runCatching {
            teamApplicationApi.apply(request.toDto()).getOrThrow().toDomain()
        }
    }

    override suspend fun mine(): Result<List<SpaceJoinApplication>> {
        return runCatching {
            teamApplicationApi.mine().getOrThrow().map { it.toDomain() }
        }
    }

    override suspend fun pending(): Result<List<SpaceJoinApplication>> {
        return runCatching {
            teamApplicationApi.pending().getOrThrow().map { it.toDomain() }
        }
    }

    override suspend fun approve(
        requestId: Long,
        request: SpaceJoinApprove
    ): Result<SpaceJoinApplication> {
        return runCatching {
            teamApplicationApi.approve(requestId, request.toDto()).getOrThrow().toDomain()
        }
    }

    override suspend fun reject(
        requestId: Long,
        request: SpaceJoinReject
    ): Result<SpaceJoinApplication> {
        return runCatching {
            teamApplicationApi.reject(requestId, request.toDto()).getOrThrow().toDomain()
        }
    }
}
