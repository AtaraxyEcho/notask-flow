package com.notaskflow.data.space.api

import com.notaskflow.core.network.ApiResponse
import com.notaskflow.core.network.EmptyResponse
import com.notaskflow.data.space.dto.SpaceDto
import com.notaskflow.data.space.dto.SpaceCreateRequestDto
import com.notaskflow.data.space.dto.SpaceInviteCreateRequestDto
import com.notaskflow.data.space.dto.SpaceInviteDto
import com.notaskflow.data.space.dto.SpaceMemberDto
import com.notaskflow.data.space.dto.SpaceMemberRoleUpdateRequestDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.PUT

interface SpaceApi {
    @GET("/api/v1/spaces")
    suspend fun listSpaces(): ApiResponse<List<SpaceDto>>

    @GET("/api/v1/spaces/{spaceId}/members")
    suspend fun listMembers(@Path("spaceId") spaceId: Long): ApiResponse<List<SpaceMemberDto>>

    @POST("/api/v1/spaces")
    suspend fun createTeamSpace(@Body request: SpaceCreateRequestDto): ApiResponse<SpaceDto>

    @POST("/api/v1/spaces/{spaceId}/invites")
    suspend fun createInvite(
        @Path("spaceId") spaceId: Long,
        @Body request: SpaceInviteCreateRequestDto
    ): ApiResponse<SpaceInviteDto>

    @POST("/api/v1/spaces/invites/{code}/join")
    suspend fun joinByInviteCode(@Path("code") code: String): ApiResponse<SpaceMemberDto>

    @GET("/api/v1/spaces/{spaceId}/permissions")
    suspend fun permissions(@Path("spaceId") spaceId: Long): ApiResponse<List<String>>

    @PUT("/api/v1/spaces/{spaceId}/members/{userId}")
    suspend fun updateMemberRole(
        @Path("spaceId") spaceId: Long,
        @Path("userId") userId: Long,
        @Body request: SpaceMemberRoleUpdateRequestDto
    ): ApiResponse<SpaceMemberDto>

    @DELETE("/api/v1/spaces/{spaceId}/members/{userId}")
    suspend fun removeMember(
        @Path("spaceId") spaceId: Long,
        @Path("userId") userId: Long
    ): ApiResponse<EmptyResponse>
}
