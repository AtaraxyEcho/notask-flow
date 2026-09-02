package com.notaskflow.data.project.api

import com.notaskflow.core.network.ApiResponse
import com.notaskflow.core.network.EmptyResponse
import com.notaskflow.core.network.PageResponse
import com.notaskflow.data.project.dto.ProjectArchiveRequestDto
import com.notaskflow.data.project.dto.ProjectDto
import com.notaskflow.data.project.dto.ProjectMemberDto
import com.notaskflow.data.project.dto.ProjectMemberRoleUpdateRequestDto
import com.notaskflow.data.project.dto.ProjectMemberSaveRequestDto
import com.notaskflow.data.project.dto.ProjectSaveRequestDto
import com.notaskflow.data.task.dto.TaskDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface ProjectApi {
    @GET("/api/v1/spaces/{spaceId}/projects")
    suspend fun page(
        @Path("spaceId") spaceId: Long,
        @Query("pageNum") pageNum: Long,
        @Query("pageSize") pageSize: Long,
        @Query("keyword") keyword: String?,
        @Query("archived") archived: Boolean?
    ): ApiResponse<PageResponse<ProjectDto>>

    @POST("/api/v1/spaces/{spaceId}/projects")
    suspend fun create(
        @Path("spaceId") spaceId: Long,
        @Body body: ProjectSaveRequestDto
    ): ApiResponse<ProjectDto>

    @GET("/api/v1/spaces/{spaceId}/projects/{projectId}")
    suspend fun get(
        @Path("spaceId") spaceId: Long,
        @Path("projectId") projectId: Long
    ): ApiResponse<ProjectDto>

    @PUT("/api/v1/spaces/{spaceId}/projects/{projectId}")
    suspend fun update(
        @Path("spaceId") spaceId: Long,
        @Path("projectId") projectId: Long,
        @Body body: ProjectSaveRequestDto
    ): ApiResponse<ProjectDto>

    @DELETE("/api/v1/spaces/{spaceId}/projects/{projectId}")
    suspend fun delete(
        @Path("spaceId") spaceId: Long,
        @Path("projectId") projectId: Long
    ): ApiResponse<EmptyResponse>

    @PUT("/api/v1/spaces/{spaceId}/projects/{projectId}/archive")
    suspend fun archive(
        @Path("spaceId") spaceId: Long,
        @Path("projectId") projectId: Long,
        @Body body: ProjectArchiveRequestDto
    ): ApiResponse<ProjectDto>

    @GET("/api/v1/spaces/{spaceId}/projects/{projectId}/members")
    suspend fun members(
        @Path("spaceId") spaceId: Long,
        @Path("projectId") projectId: Long
    ): ApiResponse<List<ProjectMemberDto>>

    @POST("/api/v1/spaces/{spaceId}/projects/{projectId}/members")
    suspend fun addMember(
        @Path("spaceId") spaceId: Long,
        @Path("projectId") projectId: Long,
        @Body body: ProjectMemberSaveRequestDto
    ): ApiResponse<ProjectMemberDto>

    @PUT("/api/v1/spaces/{spaceId}/projects/{projectId}/members/{userId}")
    suspend fun updateMemberRole(
        @Path("spaceId") spaceId: Long,
        @Path("projectId") projectId: Long,
        @Path("userId") userId: Long,
        @Body body: ProjectMemberRoleUpdateRequestDto
    ): ApiResponse<ProjectMemberDto>

    @DELETE("/api/v1/spaces/{spaceId}/projects/{projectId}/members/{userId}")
    suspend fun removeMember(
        @Path("spaceId") spaceId: Long,
        @Path("projectId") projectId: Long,
        @Path("userId") userId: Long
    ): ApiResponse<EmptyResponse>

    @GET("/api/v1/spaces/{spaceId}/projects/{projectId}/tasks")
    suspend fun tasks(
        @Path("spaceId") spaceId: Long,
        @Path("projectId") projectId: Long,
        @Query("pageNum") pageNum: Long,
        @Query("pageSize") pageSize: Long,
        @Query("keyword") keyword: String?,
        @Query("status") status: String?,
        @Query("mode") mode: String?,
        @Query("assigneeId") assigneeId: Long?
    ): ApiResponse<PageResponse<TaskDto>>
}
