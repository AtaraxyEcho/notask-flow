package com.notaskflow.data.task.api

import com.notaskflow.core.network.ApiResponse
import com.notaskflow.core.network.EmptyResponse
import com.notaskflow.core.network.PageResponse
import com.notaskflow.data.attachment.dto.AttachmentBindRequestDto
import com.notaskflow.data.attachment.dto.AttachmentDto
import com.notaskflow.data.attachment.dto.AttachmentUnbindRequestDto
import com.notaskflow.data.task.dto.TaskClaimRequestDto
import com.notaskflow.data.task.dto.TaskCommentCreateRequestDto
import com.notaskflow.data.task.dto.TaskCommentDto
import com.notaskflow.data.task.dto.TaskCreateRequestDto
import com.notaskflow.data.task.dto.TaskDto
import com.notaskflow.data.task.dto.TaskMemberCompleteRequestDto
import com.notaskflow.data.task.dto.TaskMemberDto
import com.notaskflow.data.task.dto.TaskStatusUpdateRequestDto
import com.notaskflow.data.task.dto.TaskUpdateRequestDto
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.Part
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface TaskApi {
    @GET("/api/v1/spaces/{spaceId}/tasks")
    suspend fun page(
        @Path("spaceId") spaceId: Long,
        @Query("pageNum") pageNum: Long,
        @Query("pageSize") pageSize: Long,
        @Query("keyword") keyword: String?,
        @Query("status") status: String?,
        @Query("mode") mode: String?,
        @Query("assigneeId") assigneeId: Long?,
        @Query("projectId") projectId: Long?
    ): ApiResponse<PageResponse<TaskDto>>

    @POST("/api/v1/spaces/{spaceId}/tasks")
    suspend fun create(
        @Path("spaceId") spaceId: Long,
        @Body request: TaskCreateRequestDto
    ): ApiResponse<TaskDto>

    @GET("/api/v1/spaces/{spaceId}/tasks/{id}")
    suspend fun get(
        @Path("spaceId") spaceId: Long,
        @Path("id") id: Long
    ): ApiResponse<TaskDto>

    @PUT("/api/v1/spaces/{spaceId}/tasks/{id}")
    suspend fun update(
        @Path("spaceId") spaceId: Long,
        @Path("id") id: Long,
        @Body request: TaskUpdateRequestDto
    ): ApiResponse<TaskDto>

    @DELETE("/api/v1/spaces/{spaceId}/tasks/{id}")
    suspend fun delete(
        @Path("spaceId") spaceId: Long,
        @Path("id") id: Long
    ): ApiResponse<EmptyResponse>

    @PATCH("/api/v1/spaces/{spaceId}/tasks/{id}/status")
    suspend fun updateStatus(
        @Path("spaceId") spaceId: Long,
        @Path("id") id: Long,
        @Body request: TaskStatusUpdateRequestDto
    ): ApiResponse<TaskDto>

    @POST("/api/v1/spaces/{spaceId}/tasks/{taskId}/members/{memberId}/start")
    suspend fun startMember(
        @Path("spaceId") spaceId: Long,
        @Path("taskId") taskId: Long,
        @Path("memberId") memberId: Long
    ): ApiResponse<TaskMemberDto>

    @POST("/api/v1/spaces/{spaceId}/tasks/{taskId}/members/{memberId}/complete")
    suspend fun completeMember(
        @Path("spaceId") spaceId: Long,
        @Path("taskId") taskId: Long,
        @Path("memberId") memberId: Long,
        @Body request: TaskMemberCompleteRequestDto
    ): ApiResponse<TaskMemberDto>

    @POST("/api/v1/spaces/{spaceId}/tasks/{taskId}/claim")
    suspend fun claim(
        @Path("spaceId") spaceId: Long,
        @Path("taskId") taskId: Long,
        @Body request: TaskClaimRequestDto
    ): ApiResponse<TaskMemberDto>

    @GET("/api/v1/spaces/{spaceId}/tasks/{id}/comments")
    suspend fun comments(
        @Path("spaceId") spaceId: Long,
        @Path("id") id: Long
    ): ApiResponse<List<TaskCommentDto>>

    @POST("/api/v1/spaces/{spaceId}/tasks/{id}/comments")
    suspend fun addComment(
        @Path("spaceId") spaceId: Long,
        @Path("id") id: Long,
        @Body request: TaskCommentCreateRequestDto
    ): ApiResponse<TaskCommentDto>

    @GET("/api/v1/spaces/{spaceId}/tasks/{id}/attachments")
    suspend fun attachments(
        @Path("spaceId") spaceId: Long,
        @Path("id") id: Long
    ): ApiResponse<List<AttachmentDto>>

    @Multipart
    @POST("/api/v1/spaces/{spaceId}/attachments")
    suspend fun uploadAttachment(
        @Path("spaceId") spaceId: Long,
        @Part file: MultipartBody.Part
    ): ApiResponse<AttachmentDto>

    @POST("/api/v1/spaces/{spaceId}/attachments/bind")
    suspend fun bindAttachment(
        @Path("spaceId") spaceId: Long,
        @Body request: AttachmentBindRequestDto
    ): ApiResponse<EmptyResponse>

    @HTTP(method = "DELETE", path = "/api/v1/spaces/{spaceId}/attachments/{attachmentId}/unbind", hasBody = true)
    suspend fun unbindAttachment(
        @Path("spaceId") spaceId: Long,
        @Path("attachmentId") attachmentId: Long,
        @Body request: AttachmentUnbindRequestDto
    ): ApiResponse<EmptyResponse>

    @DELETE("/api/v1/spaces/{spaceId}/attachments/{attachmentId}")
    suspend fun deleteAttachment(
        @Path("spaceId") spaceId: Long,
        @Path("attachmentId") attachmentId: Long
    ): ApiResponse<EmptyResponse>
}
