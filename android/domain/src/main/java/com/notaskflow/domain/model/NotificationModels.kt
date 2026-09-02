package com.notaskflow.domain.model

enum class NotificationType {
    TASK_CREATED,
    TASK_CLAIMED,
    TASK_MEMBER_COMPLETED,
    TASK_COMPLETED,
    TODO_CREATED,
    COMMENT_MENTIONED,
    SPACE_JOIN_APPLIED,
    SPACE_JOIN_APPROVED,
    SPACE_JOIN_REJECTED
}

enum class NotificationBusinessType {
    NOTE,
    TASK,
    TODO,
    SPACE_JOIN_REQUEST
}

data class Notification(
    val id: Long,
    val userId: Long,
    val spaceId: Long?,
    val type: NotificationType,
    val businessType: NotificationBusinessType?,
    val businessId: Long?,
    val title: String,
    val content: String,
    val isRead: Boolean,
    val gmtCreate: String?
)

data class NotificationQuery(
    val pageNum: Long = 1,
    val pageSize: Long = 20,
    val isRead: Boolean? = null
)
