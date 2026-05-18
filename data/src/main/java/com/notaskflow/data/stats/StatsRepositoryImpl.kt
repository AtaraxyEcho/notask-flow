package com.notaskflow.data.stats

import com.notaskflow.data.stats.api.StatsApi
import com.notaskflow.data.stats.dto.toDomain
import com.notaskflow.domain.model.MemberTaskLoad
import com.notaskflow.domain.model.PersonalNoteTrend
import com.notaskflow.domain.model.PersonalStats
import com.notaskflow.domain.model.RoleCompletion
import com.notaskflow.domain.model.StatsActivity
import com.notaskflow.domain.model.TaskTrend
import com.notaskflow.domain.stats.StatsRepository
import javax.inject.Inject

class StatsRepositoryImpl @Inject constructor(
    private val statsApi: StatsApi
) : StatsRepository {
    override suspend fun personal(): Result<PersonalStats> {
        return runCatching {
            statsApi.personal().getOrThrow().toDomain()
        }
    }

    override suspend fun personalNoteTrend(days: Int): Result<List<PersonalNoteTrend>> {
        return runCatching {
            statsApi.personalNoteTrend(days).getOrThrow().map { it.toDomain() }
        }
    }

    override suspend fun trend(spaceId: Long, days: Int): Result<List<TaskTrend>> {
        return runCatching {
            statsApi.trend(spaceId, days).getOrThrow().map { it.toDomain() }
        }
    }

    override suspend fun roleCompletion(spaceId: Long): Result<List<RoleCompletion>> {
        return runCatching {
            statsApi.roleCompletion(spaceId).getOrThrow().map { it.toDomain() }
        }
    }

    override suspend fun load(spaceId: Long): Result<List<MemberTaskLoad>> {
        return runCatching {
            statsApi.load(spaceId).getOrThrow().map { it.toDomain() }
        }
    }

    override suspend fun activities(spaceId: Long, limit: Int): Result<List<StatsActivity>> {
        return runCatching {
            statsApi.activities(spaceId, limit).getOrThrow().map { it.toDomain() }
        }
    }
}
