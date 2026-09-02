package com.notaskflow.data.team.api

import com.notaskflow.core.network.ApiResponse
import com.notaskflow.data.team.dto.SpaceJoinApplicationDto
import com.notaskflow.data.team.dto.SpaceJoinApplyRequestDto
import com.notaskflow.data.team.dto.SpaceJoinApproveRequestDto
import com.notaskflow.data.team.dto.SpaceJoinRejectRequestDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface TeamApplicationApi {
    @POST("/api/v1/team-applications")
    suspend fun apply(@Body request: SpaceJoinApplyRequestDto): ApiResponse<SpaceJoinApplicationDto>

    @GET("/api/v1/team-applications/mine")
    suspend fun mine(): ApiResponse<List<SpaceJoinApplicationDto>>

    @GET("/api/v1/team-applications/pending")
    suspend fun pending(): ApiResponse<List<SpaceJoinApplicationDto>>

    @POST("/api/v1/team-applications/{requestId}/approve")
    suspend fun approve(
        @Path("requestId") requestId: Long,
        @Body request: SpaceJoinApproveRequestDto
    ): ApiResponse<SpaceJoinApplicationDto>

    @POST("/api/v1/team-applications/{requestId}/reject")
    suspend fun reject(
        @Path("requestId") requestId: Long,
        @Body request: SpaceJoinRejectRequestDto
    ): ApiResponse<SpaceJoinApplicationDto>
}
