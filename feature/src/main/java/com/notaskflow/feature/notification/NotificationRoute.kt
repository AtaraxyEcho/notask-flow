package com.notaskflow.feature.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Task
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

data class NotificationItem(
    val id: Long,
    val type: NotificationType,
    val title: String,
    val content: String,
    val time: String,
    val isRead: Boolean = false
)

enum class NotificationType { TASK, NOTE, SYSTEM }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationRoute(
    modifier: Modifier = Modifier,
    onNotificationClick: (Long) -> Unit = {},
    onMarkAllRead: () -> Unit = {},
    onClearRead: () -> Unit = {}
) {
    val notifications = remember {
        listOf(
            NotificationItem(1, NotificationType.TASK, "新任务已分配", "你被分配到「修复登录页样式问题」", "10 分钟前"),
            NotificationItem(2, NotificationType.NOTE, "笔记已分享", "张三与你分享了「Sprint 回顾」", "1 小时前"),
            NotificationItem(3, NotificationType.SYSTEM, "团队邀请", "你已被邀请加入「产品开发团队」", "3 小时前", isRead = true),
            NotificationItem(4, NotificationType.TASK, "任务已完成", "李四完成了「API 文档更新」", "5 小时前", isRead = true),
            NotificationItem(5, NotificationType.NOTE, "评论回复", "王五回复了你的笔记", "1 天前", isRead = true)
        )
    }

    val unreadCount = notifications.count { !it.isRead }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("通知")
                        if (unreadCount > 0) {
                            Text(
                                text = " ($unreadCount)",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                actions = {
                    TextButton(onClick = onMarkAllRead) { Text("全部已读") }
                    TextButton(onClick = onClearRead) { Text("清除") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(notifications) { notification ->
                NotificationListItem(
                    notification = notification,
                    onClick = { onNotificationClick(notification.id) }
                )
            }
        }
    }
}

@Composable
private fun NotificationListItem(
    notification: NotificationItem,
    onClick: () -> Unit
) {
    val bgColor = if (!notification.isRead)
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
    else MaterialTheme.colorScheme.surface

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // 类型图标
            val icon = notificationTypeIcon(notification.type)
            val iconTint = notificationTypeColor(notification.type)
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = iconTint
            )
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = notification.time,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = notification.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 未读指示点
            if (!notification.isRead) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error)
                )
            }
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            modifier = Modifier.padding(horizontal = 56.dp)
        )
    }
}

private fun notificationTypeIcon(type: NotificationType): ImageVector = when (type) {
    NotificationType.TASK -> Icons.Filled.CheckCircle
    NotificationType.NOTE -> Icons.Filled.Description
    NotificationType.SYSTEM -> Icons.Filled.Notifications
}

private fun notificationTypeColor(type: NotificationType): androidx.compose.ui.graphics.Color = when (type) {
    NotificationType.TASK -> androidx.compose.ui.graphics.Color(0xFFFF9800)
    NotificationType.NOTE -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
    NotificationType.SYSTEM -> androidx.compose.ui.graphics.Color(0xFF2196F3)
}
