package com.notaskflow.data.note.api

import com.notaskflow.core.network.ApiResponse
import com.notaskflow.core.network.EmptyResponse
import com.notaskflow.core.network.PageResponse
import com.notaskflow.data.attachment.dto.AttachmentBindRequestDto
import com.notaskflow.data.attachment.dto.AttachmentDto
import com.notaskflow.data.attachment.dto.AttachmentUnbindRequestDto
import com.notaskflow.data.note.dto.CollabContentSaveRequestDto
import com.notaskflow.data.note.dto.CollabTicketDto
import com.notaskflow.data.note.dto.NoteAttachmentDto
import com.notaskflow.data.note.dto.NoteDto
import com.notaskflow.data.note.dto.NoteHistoryDto
import com.notaskflow.data.note.dto.NoteShareRequestDto
import com.notaskflow.data.note.dto.NoteSaveRequestDto
import com.notaskflow.data.note.dto.NotebookDto
import com.notaskflow.data.note.dto.NotebookSaveRequestDto
import com.notaskflow.data.note.dto.TagDto
import retrofit2.http.GET
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.HTTP
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Part
import retrofit2.http.Query
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response

interface NoteApi {
    @GET("/api/v1/spaces/{spaceId}/notes")
    suspend fun page(
        @Path("spaceId") spaceId: Long,
        @Query("pageNum") pageNum: Long,
        @Query("pageSize") pageSize: Long,
        @Query("notebookId") notebookId: Long?,
        @Query("tagId") tagId: Long?,
        @Query("keyword") keyword: String?,
        @Query("projectId") projectId: Long?
    ): ApiResponse<PageResponse<NoteDto>>

    @POST("/api/v1/spaces/{spaceId}/notes")
    suspend fun create(
        @Path("spaceId") spaceId: Long,
        @Body request: NoteSaveRequestDto
    ): ApiResponse<NoteDto>

    @GET("/api/v1/spaces/{spaceId}/notes/{id}")
    suspend fun get(
        @Path("spaceId") spaceId: Long,
        @Path("id") id: Long
    ): ApiResponse<NoteDto>

    @PUT("/api/v1/spaces/{spaceId}/notes/{id}")
    suspend fun update(
        @Path("spaceId") spaceId: Long,
        @Path("id") id: Long,
        @Body request: NoteSaveRequestDto
    ): ApiResponse<NoteDto>

    @DELETE("/api/v1/spaces/{spaceId}/notes/{id}")
    suspend fun delete(
        @Path("spaceId") spaceId: Long,
        @Path("id") id: Long
    ): ApiResponse<EmptyResponse>

    @POST("/api/v1/spaces/{spaceId}/notes/{id}/share")
    suspend fun share(
        @Path("spaceId") spaceId: Long,
        @Path("id") id: Long,
        @Body request: NoteShareRequestDto
    ): ApiResponse<NoteDto>

    @POST("/api/v1/spaces/{spaceId}/notes/{id}/collab-ticket")
    suspend fun createCollabTicket(
        @Path("spaceId") spaceId: Long,
        @Path("id") id: Long
    ): ApiResponse<CollabTicketDto>

    @PUT("/api/v1/spaces/{spaceId}/notes/{id}/collab-content")
    suspend fun saveCollabContent(
        @Path("spaceId") spaceId: Long,
        @Path("id") id: Long,
        @Body request: CollabContentSaveRequestDto
    ): ApiResponse<NoteDto>

    @POST("/api/v1/spaces/{spaceId}/notes/{id}/checkpoints")
    suspend fun createCheckpoint(
        @Path("spaceId") spaceId: Long,
        @Path("id") id: Long,
        @Body request: CollabContentSaveRequestDto
    ): ApiResponse<NoteDto>

    @GET("/api/v1/spaces/{spaceId}/notes/{id}/export")
    suspend fun export(
        @Path("spaceId") spaceId: Long,
        @Path("id") id: Long,
        @Query("format") format: String
    ): Response<ResponseBody>

    @GET("/api/v1/spaces/{spaceId}/notes/{id}/history")
    suspend fun histories(
        @Path("spaceId") spaceId: Long,
        @Path("id") id: Long
    ): ApiResponse<List<NoteHistoryDto>>

    @GET("/api/v1/spaces/{spaceId}/notes/{id}/history/{version}")
    suspend fun history(
        @Path("spaceId") spaceId: Long,
        @Path("id") id: Long,
        @Path("version") version: Int
    ): ApiResponse<NoteHistoryDto>

    @POST("/api/v1/spaces/{spaceId}/notes/{id}/history/{version}/restore")
    suspend fun restore(
        @Path("spaceId") spaceId: Long,
        @Path("id") id: Long,
        @Path("version") version: Int
    ): ApiResponse<NoteDto>

    @GET("/api/v1/spaces/{spaceId}/notes/{id}/attachments")
    suspend fun attachments(
        @Path("spaceId") spaceId: Long,
        @Path("id") id: Long
    ): ApiResponse<List<NoteAttachmentDto>>

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

    @GET("/api/v1/spaces/{spaceId}/notebooks")
    suspend fun notebooks(@Path("spaceId") spaceId: Long): ApiResponse<List<NotebookDto>>

    @POST("/api/v1/spaces/{spaceId}/notebooks")
    suspend fun createNotebook(
        @Path("spaceId") spaceId: Long,
        @Body request: NotebookSaveRequestDto
    ): ApiResponse<NotebookDto>

    @PUT("/api/v1/spaces/{spaceId}/notebooks/{id}")
    suspend fun updateNotebook(
        @Path("spaceId") spaceId: Long,
        @Path("id") id: Long,
        @Body request: NotebookSaveRequestDto
    ): ApiResponse<NotebookDto>

    @DELETE("/api/v1/spaces/{spaceId}/notebooks/{id}")
    suspend fun deleteNotebook(
        @Path("spaceId") spaceId: Long,
        @Path("id") id: Long
    ): ApiResponse<EmptyResponse>

    @GET("/api/v1/spaces/{spaceId}/tags")
    suspend fun tags(@Path("spaceId") spaceId: Long): ApiResponse<List<TagDto>>
}
