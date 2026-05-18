package com.notaskflow.data.notification.dto

import com.notaskflow.domain.model.Notification
import com.notaskflow.domain.model.NotificationBusinessType
import com.notaskflow.domain.model.NotificationType
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NotificationDto(
    @param:Json(name = "id") val id: Long,
    @param:Json(name = "userId") val userId: Long,
    @param:Json(name = "spaceId") val spaceId: Long?,
    @param:Json(name = "type") val type: String?,
    @param:Json(name = "businessType") val businessType: String?,
    @param:Json(name = "businessId") val businessId: Long?,
    @param:Json(name = "title") val title: String?,
    @param:Json(name = "content") val content: String?,
    @param:Json(name = "isRead") val isRead: Boolean?,
    @param:Json(name = "gmtCreate") val gmtCreate: String?
)

fun NotificationDto.toDomain(): Notification {
    return Notification(
        id = id,
        userId = userId,
        spaceId = spaceId,
        type = parseNotificationType(type),
        businessType = parseBusinessType(businessType),
        businessId = businessId,
        title = title.orEmpty(),
        content = content.orEmpty(),
        isRead = isRead == true,
        gmtCreate = gmtCreate
    )
}

private fun parseNotificationType(value: String?): NotificationType {
    return runCatching {
        NotificationType.valueOf(value.orEmpty())
    }.getOrDefault(NotificationType.TASK_CREATED)
}

private fun parseBusinessType(value: String?): NotificationBusinessType? {
    return runCatching {
        NotificationBusinessType.valueOf(value.orEmpty())
    }.getOrNull()
}
