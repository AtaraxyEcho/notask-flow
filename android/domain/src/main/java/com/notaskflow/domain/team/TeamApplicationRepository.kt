package com.notaskflow.domain.team

import com.notaskflow.domain.model.SpaceJoinApplication
import com.notaskflow.domain.model.SpaceJoinApply
import com.notaskflow.domain.model.SpaceJoinApprove
import com.notaskflow.domain.model.SpaceJoinReject

interface TeamApplicationRepository {
    suspend fun apply(request: SpaceJoinApply): Result<SpaceJoinApplication>

    suspend fun mine(): Result<List<SpaceJoinApplication>>

    suspend fun pending(): Result<List<SpaceJoinApplication>>

    suspend fun approve(requestId: Long, request: SpaceJoinApprove): Result<SpaceJoinApplication>

    suspend fun reject(requestId: Long, request: SpaceJoinReject): Result<SpaceJoinApplication>
}
