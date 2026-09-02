package com.notaskflow.data.notification.api

import com.notaskflow.core.network.ApiResponse
import com.notaskflow.core.network.PageResponse
import com.notaskflow.data.common.dto.EmptyResponseDto
import com.notaskflow.data.notification.dto.NotificationDto
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface NotificationApi {
    @GET("/api/v1/notifications")
    suspend fun page(
        @Query("pageNum") pageNum: Long,
        @Query("pageSize") pageSize: Long,
        @Query("isRead") isRead: Boolean?
    ): ApiResponse<PageResponse<NotificationDto>>

    @GET("/api/v1/notifications/unread-count")
    suspend fun unreadCount(): ApiResponse<Long>

    @PUT("/api/v1/notifications/{id}/read")
    suspend fun markRead(@Path("id") id: Long): ApiResponse<NotificationDto>

    @DELETE("/api/v1/notifications/{id}")
    suspend fun delete(@Path("id") id: Long): ApiResponse<EmptyResponseDto>

    @PUT("/api/v1/notifications/read-all")
    suspend fun markAllRead(): ApiResponse<EmptyResponseDto>

    @DELETE("/api/v1/notifications/read")
    suspend fun clearRead(): ApiResponse<EmptyResponseDto>
}
