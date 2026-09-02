package com.notaskflow.data.notification

import com.notaskflow.data.common.toDomain
import com.notaskflow.data.notification.api.NotificationApi
import com.notaskflow.data.notification.dto.toDomain
import com.notaskflow.domain.model.Notification
import com.notaskflow.domain.model.NotificationQuery
import com.notaskflow.domain.model.Page
import com.notaskflow.domain.notification.NotificationRepository
import javax.inject.Inject

class NotificationRepositoryImpl @Inject constructor(
    private val notificationApi: NotificationApi
) : NotificationRepository {
    override suspend fun unreadCount(): Result<Long> {
        return runCatching {
            notificationApi.unreadCount().getOrThrow()
        }
    }

    override suspend fun page(query: NotificationQuery): Result<Page<Notification>> {
        return runCatching {
            notificationApi.page(
                pageNum = query.pageNum,
                pageSize = query.pageSize,
                isRead = query.isRead
            ).getOrThrow().toDomain { it.toDomain() }
        }
    }

    override suspend fun markRead(id: Long): Result<Unit> {
        return runCatching {
            notificationApi.markRead(id).getOrThrow()
            Unit
        }
    }

    override suspend fun delete(id: Long): Result<Unit> {
        return runCatching {
            notificationApi.delete(id).requireSuccess()
        }
    }

    override suspend fun markAllRead(): Result<Unit> {
        return runCatching {
            notificationApi.markAllRead().requireSuccess()
        }
    }

    override suspend fun clearRead(): Result<Unit> {
        return runCatching {
            notificationApi.clearRead().requireSuccess()
        }
    }
}
