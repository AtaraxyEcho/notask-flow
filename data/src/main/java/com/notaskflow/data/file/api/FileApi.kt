package com.notaskflow.data.file.api

import com.notaskflow.core.network.ApiResponse
import com.notaskflow.core.network.PageResponse
import com.notaskflow.data.common.dto.EmptyResponseDto
import com.notaskflow.data.file.dto.FileFolderDto
import com.notaskflow.data.file.dto.FileFolderSaveRequestDto
import com.notaskflow.data.file.dto.FilePreviewHtmlDto
import com.notaskflow.data.file.dto.FilePreviewTextDto
import com.notaskflow.data.file.dto.FileReferenceDto
import com.notaskflow.data.file.dto.ManagedFileUpdateRequestDto
import com.notaskflow.data.file.dto.ManagedFileDto
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.PUT
import retrofit2.http.Query

interface FileApi {
    @GET("/api/v1/spaces/{spaceId}/files")
    suspend fun page(
        @Path("spaceId") spaceId: Long,
        @Query("pageNum") pageNum: Long,
        @Query("pageSize") pageSize: Long,
        @Query("folderId") folderId: Long?,
        @Query("keyword") keyword: String?,
        @Query("mimeType") mimeType: String?,
        @Query("uploaderId") uploaderId: Long?,
        @Query("trashed") trashed: Boolean?
    ): ApiResponse<PageResponse<ManagedFileDto>>

    @GET("/api/v1/spaces/{spaceId}/files/tree")
    suspend fun folders(@Path("spaceId") spaceId: Long): ApiResponse<List<FileFolderDto>>

    @POST("/api/v1/spaces/{spaceId}/files/folders")
    suspend fun createFolder(
        @Path("spaceId") spaceId: Long,
        @Body request: FileFolderSaveRequestDto
    ): ApiResponse<FileFolderDto>

    @PUT("/api/v1/spaces/{spaceId}/files/folders/{folderId}")
    suspend fun updateFolder(
        @Path("spaceId") spaceId: Long,
        @Path("folderId") folderId: Long,
        @Body request: FileFolderSaveRequestDto
    ): ApiResponse<FileFolderDto>

    @DELETE("/api/v1/spaces/{spaceId}/files/folders/{folderId}")
    suspend fun deleteFolder(
        @Path("spaceId") spaceId: Long,
        @Path("folderId") folderId: Long
    ): ApiResponse<EmptyResponseDto>

    @GET("/api/v1/spaces/{spaceId}/files/{fileId}")
    suspend fun get(
        @Path("spaceId") spaceId: Long,
        @Path("fileId") fileId: Long
    ): ApiResponse<ManagedFileDto>

    @PUT("/api/v1/spaces/{spaceId}/files/{fileId}")
    suspend fun updateFile(
        @Path("spaceId") spaceId: Long,
        @Path("fileId") fileId: Long,
        @Body request: ManagedFileUpdateRequestDto
    ): ApiResponse<ManagedFileDto>

    @DELETE("/api/v1/spaces/{spaceId}/files/{fileId}")
    suspend fun delete(
        @Path("spaceId") spaceId: Long,
        @Path("fileId") fileId: Long
    ): ApiResponse<EmptyResponseDto>

    @POST("/api/v1/spaces/{spaceId}/files/{fileId}/restore")
    suspend fun restore(
        @Path("spaceId") spaceId: Long,
        @Path("fileId") fileId: Long
    ): ApiResponse<ManagedFileDto>

    @DELETE("/api/v1/spaces/{spaceId}/files/{fileId}/physical")
    suspend fun physicalDelete(
        @Path("spaceId") spaceId: Long,
        @Path("fileId") fileId: Long,
        @Query("force") force: Boolean
    ): ApiResponse<EmptyResponseDto>

    @GET("/api/v1/spaces/{spaceId}/files/{fileId}/preview-url")
    suspend fun previewUrl(
        @Path("spaceId") spaceId: Long,
        @Path("fileId") fileId: Long
    ): ApiResponse<ManagedFileDto>

    @GET("/api/v1/spaces/{spaceId}/files/{fileId}/download-url")
    suspend fun downloadUrl(
        @Path("spaceId") spaceId: Long,
        @Path("fileId") fileId: Long
    ): ApiResponse<ManagedFileDto>

    @GET("/api/v1/spaces/{spaceId}/files/{fileId}/references")
    suspend fun references(
        @Path("spaceId") spaceId: Long,
        @Path("fileId") fileId: Long
    ): ApiResponse<List<FileReferenceDto>>

    @GET("/api/v1/spaces/{spaceId}/files/{fileId}/preview-text")
    suspend fun previewText(
        @Path("spaceId") spaceId: Long,
        @Path("fileId") fileId: Long
    ): ApiResponse<FilePreviewTextDto>

    @GET("/api/v1/spaces/{spaceId}/files/{fileId}/preview-html")
    suspend fun previewHtml(
        @Path("spaceId") spaceId: Long,
        @Path("fileId") fileId: Long
    ): ApiResponse<FilePreviewHtmlDto>

    @Multipart
    @POST("/api/v1/spaces/{spaceId}/files/upload")
    suspend fun upload(
        @Path("spaceId") spaceId: Long,
        @Query("folderId") folderId: Long?,
        @Part file: MultipartBody.Part
    ): ApiResponse<ManagedFileDto>

    @Multipart
    @POST("/api/v1/spaces/{spaceId}/files/editor-upload")
    suspend fun editorUpload(
        @Path("spaceId") spaceId: Long,
        @Part file: MultipartBody.Part
    ): ApiResponse<ManagedFileDto>
}
