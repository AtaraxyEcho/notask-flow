package com.notaskflow.data.todo.api

import com.notaskflow.core.network.ApiResponse
import com.notaskflow.core.network.EmptyResponse
import com.notaskflow.core.network.PageResponse
import com.notaskflow.data.todo.dto.TodoDto
import com.notaskflow.data.todo.dto.TodoSaveRequestDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface TodoApi {
    @GET("/api/v1/spaces/{spaceId}/todos")
    suspend fun page(
        @Path("spaceId") spaceId: Long,
        @Query("pageNum") pageNum: Long,
        @Query("pageSize") pageSize: Long,
        @Query("keyword") keyword: String?,
        @Query("isCompleted") isCompleted: Boolean?
    ): ApiResponse<PageResponse<TodoDto>>

    @POST("/api/v1/spaces/{spaceId}/todos")
    suspend fun create(
        @Path("spaceId") spaceId: Long,
        @Body request: TodoSaveRequestDto
    ): ApiResponse<TodoDto>

    @PUT("/api/v1/spaces/{spaceId}/todos/{id}")
    suspend fun update(
        @Path("spaceId") spaceId: Long,
        @Path("id") id: Long,
        @Body request: TodoSaveRequestDto
    ): ApiResponse<TodoDto>

    @DELETE("/api/v1/spaces/{spaceId}/todos/{id}")
    suspend fun delete(
        @Path("spaceId") spaceId: Long,
        @Path("id") id: Long
    ): ApiResponse<EmptyResponse>

    @PUT("/api/v1/spaces/{spaceId}/todos/{id}/complete")
    suspend fun complete(
        @Path("spaceId") spaceId: Long,
        @Path("id") id: Long
    ): ApiResponse<TodoDto>

    @PUT("/api/v1/spaces/{spaceId}/todos/{id}/uncomplete")
    suspend fun uncomplete(
        @Path("spaceId") spaceId: Long,
        @Path("id") id: Long
    ): ApiResponse<TodoDto>
}
