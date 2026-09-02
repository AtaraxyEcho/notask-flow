package com.notaskflow.domain.stats

import com.notaskflow.domain.model.MemberTaskLoad
import com.notaskflow.domain.model.PersonalNoteTrend
import com.notaskflow.domain.model.PersonalStats
import com.notaskflow.domain.model.RoleCompletion
import com.notaskflow.domain.model.StatsActivity
import com.notaskflow.domain.model.TaskTrend

interface StatsRepository {
    suspend fun personal(): Result<PersonalStats>

    suspend fun personalNoteTrend(days: Int = 7): Result<List<PersonalNoteTrend>>

    suspend fun trend(spaceId: Long, days: Int = 7): Result<List<TaskTrend>>

    suspend fun roleCompletion(spaceId: Long): Result<List<RoleCompletion>>

    suspend fun load(spaceId: Long): Result<List<MemberTaskLoad>>

    suspend fun activities(spaceId: Long, limit: Int = 10): Result<List<StatsActivity>>
}
