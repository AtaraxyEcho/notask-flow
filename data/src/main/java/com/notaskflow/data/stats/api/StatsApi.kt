package com.notaskflow.data.stats.api

import com.notaskflow.core.network.ApiResponse
import com.notaskflow.data.stats.dto.MemberTaskLoadDto
import com.notaskflow.data.stats.dto.PersonalNoteTrendDto
import com.notaskflow.data.stats.dto.PersonalStatsDto
import com.notaskflow.data.stats.dto.RoleCompletionDto
import com.notaskflow.data.stats.dto.StatsActivityDto
import com.notaskflow.data.stats.dto.TaskTrendDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface StatsApi {
    @GET("/api/v1/stats/personal")
    suspend fun personal(): ApiResponse<PersonalStatsDto>

    @GET("/api/v1/stats/personal/note-trend")
    suspend fun personalNoteTrend(
        @Query("days") days: Int
    ): ApiResponse<List<PersonalNoteTrendDto>>

    @GET("/api/v1/spaces/{spaceId}/stats/trend")
    suspend fun trend(
        @Path("spaceId") spaceId: Long,
        @Query("days") days: Int
    ): ApiResponse<List<TaskTrendDto>>

    @GET("/api/v1/spaces/{spaceId}/stats/role-completion")
    suspend fun roleCompletion(@Path("spaceId") spaceId: Long): ApiResponse<List<RoleCompletionDto>>

    @GET("/api/v1/spaces/{spaceId}/stats/load")
    suspend fun load(@Path("spaceId") spaceId: Long): ApiResponse<List<MemberTaskLoadDto>>

    @GET("/api/v1/spaces/{spaceId}/stats/activities")
    suspend fun activities(
        @Path("spaceId") spaceId: Long,
        @Query("limit") limit: Int
    ): ApiResponse<List<StatsActivityDto>>
}
