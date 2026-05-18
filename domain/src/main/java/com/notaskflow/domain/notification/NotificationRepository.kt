package com.notaskflow.domain.notification

import com.notaskflow.domain.model.Notification
import com.notaskflow.domain.model.NotificationQuery
import com.notaskflow.domain.model.Page

interface NotificationRepository {
    suspend fun unreadCount(): Result<Long>

    suspend fun page(query: NotificationQuery): Result<Page<Notification>>

    suspend fun markRead(id: Long): Result<Unit>

    suspend fun delete(id: Long): Result<Unit>

    suspend fun markAllRead(): Result<Unit>

    suspend fun clearRead(): Result<Unit>
}
