package com.notaskflow.feature.notification

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Task
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.notaskflow.core.common.formatDateTimeText
import com.notaskflow.domain.model.Notification
import com.notaskflow.domain.model.NotificationBusinessType
import com.notaskflow.domain.model.NotificationType
import com.notaskflow.feature.common.SwipeDeleteContainer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NotificationRoute(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onNotificationClick: (Notification) -> Unit = {},
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedNotificationIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var dismissingNotificationIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("站内通知")
                        if (uiState.unreadCount > 0) {
                            Text(
                                text = " (${uiState.unreadCount.coerceAtMost(99)})",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        val visibleNotifications = uiState.visibleNotifications
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
            val selectedNotifications = visibleNotifications.filter { notification ->
                notification.id in selectedNotificationIds
            }
            item {
                NotificationHeader(
                    unreadCount = uiState.unreadCount,
                    isMutating = uiState.isMutating,
                    actionMessage = uiState.actionMessage,
                    errorMessage = uiState.errorMessage,
                    onMarkAllRead = viewModel::markAllRead,
                    onClearRead = viewModel::clearRead
                )
                NotificationFilterRow(
                    selectedFilter = uiState.selectedFilter,
                    onFilterClick = viewModel::selectFilter
                )
                if (selectedNotificationIds.isNotEmpty()) {
                    NotificationSelectionBar(
                        selectedCount = selectedNotificationIds.size,
                        onMarkRead = {
                            selectedNotifications.forEach(viewModel::markRead)
                            selectedNotificationIds = emptySet()
                        },
                        onDelete = {
                            selectedNotifications.forEach(viewModel::delete)
                            selectedNotificationIds = emptySet()
                        },
                        onClear = { selectedNotificationIds = emptySet() }
                    )
                }
            }

            if (uiState.isLoading && visibleNotifications.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (uiState.errorMessage != null && visibleNotifications.isEmpty()) {
                item { StateText(uiState.errorMessage ?: "加载失败") }
            } else if (visibleNotifications.isEmpty()) {
                item { StateText("暂无通知") }
            } else {
                items(visibleNotifications, key = { notification -> "notification-${notification.id}" }) { notification ->
                    AnimatedVisibility(
                        visible = notification.id !in dismissingNotificationIds,
                        enter = fadeIn(tween(NOTIFICATION_ENTER_MS)) +
                            slideInVertically(tween(NOTIFICATION_ENTER_MS)) { -it / 3 },
                        exit = fadeOut(tween(NOTIFICATION_ENTER_MS)) +
                            shrinkVertically(tween(NOTIFICATION_ENTER_MS))
                    ) {
                        SwipeDeleteContainer(
                            onDeleteRequest = {
                                if (notification.id !in dismissingNotificationIds) {
                                    dismissingNotificationIds = dismissingNotificationIds + notification.id
                                    coroutineScope.launch {
                                        delay(NOTIFICATION_ENTER_MS.toLong())
                                        viewModel.delete(notification)
                                        dismissingNotificationIds = dismissingNotificationIds - notification.id
                                    }
                                }
                            },
                            cornerRadius = 26.dp,
                            deleteFromStartToEnd = false
                        ) {
                            NotificationListItem(
                                notification = notification,
                                selected = notification.id in selectedNotificationIds,
                                selectionMode = selectedNotificationIds.isNotEmpty(),
                                onClick = {
                                    if (selectedNotificationIds.isNotEmpty()) {
                                        selectedNotificationIds = selectedNotificationIds.toggle(notification.id)
                                    } else {
                                        viewModel.markRead(notification)
                                        onNotificationClick(notification)
                                    }
                                },
                                onLongClick = {
                                    selectedNotificationIds = selectedNotificationIds.toggle(notification.id)
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }
            }
        }
    }
}

@Composable
private fun NotificationHeader(
    unreadCount: Long,
    isMutating: Boolean,
    actionMessage: String?,
    errorMessage: String?,
    onMarkAllRead: () -> Unit,
    onClearRead: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (unreadCount > 0) "${unreadCount.coerceAtMost(99)} 条未读" else "暂无未读",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = actionMessage ?: errorMessage ?: "点击通知卡片可跳转到对应模块",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (errorMessage == null) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            TextButton(
                onClick = onMarkAllRead,
                enabled = !isMutating,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Text("全部已读")
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onClearRead,
                enabled = !isMutating,
                shape = RoundedCornerShape(50)
            ) {
                Text("清空已读")
            }
            if (isMutating) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .size(18.dp),
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

@Composable
private fun NotificationFilterRow(
    selectedFilter: NotificationFilter,
    onFilterClick: (NotificationFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        NotificationFilter.entries.forEach { filter ->
            FilterPill(
                text = filter.label,
                selected = selectedFilter == filter,
                onClick = { onFilterClick(filter) }
            )
        }
    }
}

@Composable
private fun NotificationSelectionBar(
    selectedCount: Int,
    onMarkRead: () -> Unit,
    onDelete: () -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "已选 $selectedCount 条",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onMarkRead) {
            Text("已读")
        }
        TextButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("删除")
        }
        IconButton(onClick = onClear, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.Close, contentDescription = "退出多选", modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun FilterPill(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceContainerLow
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NotificationListItem(
    notification: Notification,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val accentColor = notification.iconColor()
    val cardPaddingHorizontal = 20.dp
    val cardPaddingVertical = 18.dp
    val cardCornerRadius = 26.dp
    val iconSize = 48.dp
    val titleMaxLines = 1
    val contentMaxLines = 2
    val containerColor = if (notification.isRead) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surfaceContainerLowest
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .animateContentSize(),
        shape = RoundedCornerShape(cardCornerRadius),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.62f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (notification.isRead) 0.30f else 0.22f)
            }
        ),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (notification.isRead) 0.dp else 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .padding(
                    horizontal = cardPaddingHorizontal,
                    vertical = cardPaddingVertical
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(iconSize)
                    .clip(CircleShape)
                    .background(
                        if (notification.isRead) {
                            MaterialTheme.colorScheme.surfaceContainer
                        } else {
                            accentColor.copy(alpha = 0.16f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = notification.icon(),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = if (notification.isRead) MaterialTheme.colorScheme.onSurfaceVariant else accentColor
                )
                if (!notification.isRead) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 2.dp, end = 2.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = notification.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.SemiBold,
                            textDecoration = if (notification.isRead) TextDecoration.LineThrough else TextDecoration.None,
                            color = if (notification.isRead) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            maxLines = titleMaxLines,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = notification.content,
                            style = MaterialTheme.typography.bodySmall,
                            textDecoration = if (notification.isRead) TextDecoration.LineThrough else TextDecoration.None,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = if (notification.isRead) 0.82f else 1f
                            ),
                            maxLines = contentMaxLines,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = formatDateTimeText(notification.gmtCreate),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NotificationChip(notification.businessType?.displayLabel() ?: "系统")
                    NotificationChip(notification.type.displayLabel())
                    if (!notification.isRead) {
                        NotificationChip(
                            text = "未读",
                            accentColor = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (selectionMode && selected) {
                        NotificationChip(
                            text = "已选",
                            accentColor = accentColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationChip(
    text: String,
    accentColor: Color? = null
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = accentColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 9.dp, vertical = 4.dp)
    )
}

@Composable
private fun StateText(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun Notification.icon(): ImageVector {
    return when (businessType) {
        NotificationBusinessType.TASK -> Icons.Filled.Task
        NotificationBusinessType.NOTE -> Icons.AutoMirrored.Filled.Comment
        NotificationBusinessType.TODO -> Icons.Filled.CheckCircle
        NotificationBusinessType.SPACE_JOIN_REQUEST -> Icons.Filled.Notifications
        null -> when (type) {
            NotificationType.COMMENT_MENTIONED -> Icons.AutoMirrored.Filled.Comment
            else -> Icons.Filled.Notifications
        }
    }
}

@Composable
private fun Notification.iconColor(): Color {
    val colorScheme = MaterialTheme.colorScheme
    return when (businessType) {
        NotificationBusinessType.TASK -> colorScheme.tertiary
        NotificationBusinessType.NOTE -> colorScheme.primary
        NotificationBusinessType.TODO -> colorScheme.secondary
        NotificationBusinessType.SPACE_JOIN_REQUEST -> colorScheme.error
        null -> colorScheme.outline
    }
}

private fun NotificationBusinessType.displayLabel(): String {
    return when (this) {
        NotificationBusinessType.NOTE -> "笔记"
        NotificationBusinessType.TASK -> "任务"
        NotificationBusinessType.TODO -> "待办"
        NotificationBusinessType.SPACE_JOIN_REQUEST -> "团队"
    }
}

private fun NotificationType.displayLabel(): String {
    return when (this) {
        NotificationType.TASK_CREATED -> "任务创建"
        NotificationType.TASK_CLAIMED -> "任务认领"
        NotificationType.TASK_MEMBER_COMPLETED -> "成员完成"
        NotificationType.TASK_COMPLETED -> "任务完成"
        NotificationType.TODO_CREATED -> "待办"
        NotificationType.COMMENT_MENTIONED -> "@我"
        NotificationType.SPACE_JOIN_APPLIED -> "加入申请"
        NotificationType.SPACE_JOIN_APPROVED -> "申请通过"
        NotificationType.SPACE_JOIN_REJECTED -> "申请拒绝"
    }
}

private fun Set<Long>.toggle(id: Long): Set<Long> {
    return if (id in this) {
        this - id
    } else {
        this + id
    }
}

private const val NOTIFICATION_ENTER_MS = 200
