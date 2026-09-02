package com.notaskflow.feature.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notaskflow.domain.model.Notification
import com.notaskflow.domain.model.NotificationBusinessType
import com.notaskflow.domain.model.NotificationQuery
import com.notaskflow.domain.model.NotificationType
import com.notaskflow.domain.notification.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationUiState(
    val isLoading: Boolean = false,
    val notifications: List<Notification> = emptyList(),
    val selectedFilter: NotificationFilter = NotificationFilter.ALL,
    val selectedCategory: NotificationCategory = NotificationCategory.ALL,
    val unreadCount: Long = 0,
    val isMutating: Boolean = false,
    val actionMessage: String? = null,
    val errorMessage: String? = null
) {
    val visibleNotifications: List<Notification>
        get() = notifications.filter { notification ->
            selectedFilter.matches(notification) && selectedCategory.matches(notification)
        }
}

enum class NotificationFilter(val label: String, val isRead: Boolean?) {
    ALL("全部", null),
    UNREAD("未读", false),
    READ("已读", true);

    fun matches(notification: Notification): Boolean {
        return isRead?.let { notification.isRead == it } ?: true
    }
}

enum class NotificationCategory(val label: String) {
    ALL("全部类型"),
    TASK("任务"),
    NOTE("笔记"),
    TODO("待办"),
    TEAM("团队"),
    MENTION("@我");

    fun matches(notification: Notification): Boolean {
        return when (this) {
            ALL -> true
            TASK -> notification.businessType == NotificationBusinessType.TASK
            NOTE -> notification.businessType == NotificationBusinessType.NOTE
            TODO -> notification.businessType == NotificationBusinessType.TODO
            TEAM -> notification.businessType == NotificationBusinessType.SPACE_JOIN_REQUEST
                || notification.type == NotificationType.SPACE_JOIN_APPLIED
                || notification.type == NotificationType.SPACE_JOIN_APPROVED
                || notification.type == NotificationType.SPACE_JOIN_REJECTED
            MENTION -> notification.type == NotificationType.COMMENT_MENTIONED
        }
    }
}

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = mutableUiState

    fun load() {
        refresh()
    }

    fun selectFilter(filter: NotificationFilter) {
        mutableUiState.update { it.copy(selectedFilter = filter) }
        refresh(showLoading = false)
    }

    fun selectCategory(category: NotificationCategory) {
        mutableUiState.update { it.copy(selectedCategory = category) }
    }

    fun markRead(notification: Notification) {
        if (notification.isRead) {
            return
        }
        val previousState = mutableUiState.value
        mutableUiState.update { state ->
            state.copy(
                notifications = state.notifications.map { item ->
                    if (item.id == notification.id) {
                        item.copy(isRead = true)
                    } else {
                        item
                    }
                },
                unreadCount = (state.unreadCount - 1).coerceAtLeast(0),
                isMutating = true,
                actionMessage = null,
                errorMessage = null
            )
        }
        viewModelScope.launch {
            notificationRepository.markRead(notification.id)
                .onSuccess {
                    mutableUiState.update { it.copy(isMutating = false, actionMessage = "已标记为已读") }
                    refresh(showLoading = false)
                }
                .onFailure { throwable ->
                    mutableUiState.update {
                        previousState.copy(
                            isMutating = false,
                            errorMessage = throwable.message ?: "标记已读失败"
                        )
                    }
                }
        }
    }

    fun markAllRead() {
        val previousState = mutableUiState.value
        mutableUiState.update { state ->
            state.copy(
                notifications = state.notifications.map { it.copy(isRead = true) },
                unreadCount = 0,
                isMutating = true,
                actionMessage = null,
                errorMessage = null
            )
        }
        viewModelScope.launch {
            notificationRepository.markAllRead()
                .onSuccess {
                    mutableUiState.update { it.copy(isMutating = false, actionMessage = "已全部标记为已读") }
                    refresh(showLoading = false)
                }
                .onFailure { throwable ->
                    mutableUiState.update {
                        previousState.copy(
                            isMutating = false,
                            errorMessage = throwable.message ?: "全部已读失败"
                        )
                    }
                }
        }
    }

    fun clearRead() {
        val previousState = mutableUiState.value
        mutableUiState.update { state ->
            state.copy(
                notifications = state.notifications.filterNot { it.isRead },
                unreadCount = state.unreadCount,
                isMutating = true,
                actionMessage = null,
                errorMessage = null
            )
        }
        viewModelScope.launch {
            notificationRepository.clearRead()
                .onSuccess {
                    mutableUiState.update { it.copy(isMutating = false, actionMessage = "已清空已读通知") }
                    refresh(showLoading = false)
                }
                .onFailure { throwable ->
                    mutableUiState.update {
                        previousState.copy(
                            isMutating = false,
                            errorMessage = throwable.message ?: "清除已读失败"
                        )
                    }
                }
        }
    }

    fun delete(notification: Notification) {
        val previousState = mutableUiState.value
        mutableUiState.update { state ->
            state.copy(
                notifications = state.notifications.filterNot { it.id == notification.id },
                unreadCount = if (notification.isRead) {
                    state.unreadCount
                } else {
                    (state.unreadCount - 1).coerceAtLeast(0)
                },
                isMutating = true,
                actionMessage = null,
                errorMessage = null
            )
        }
        viewModelScope.launch {
            notificationRepository.delete(notification.id)
                .onSuccess {
                    mutableUiState.update { it.copy(isMutating = false, actionMessage = "通知已删除") }
                    refresh(showLoading = false)
                }
                .onFailure { throwable ->
                    mutableUiState.update {
                        previousState.copy(
                            isMutating = false,
                            errorMessage = throwable.message ?: "删除通知失败"
                        )
                    }
                }
        }
    }

    private fun refresh(showLoading: Boolean = true) {
        val filter = mutableUiState.value.selectedFilter
        viewModelScope.launch {
            mutableUiState.update { it.copy(isLoading = showLoading, errorMessage = null) }
            val pageResult = notificationRepository.page(NotificationQuery(isRead = filter.isRead))
            val unreadResult = notificationRepository.unreadCount()
            mutableUiState.update { state ->
                state.copy(
                    isLoading = false,
                    isMutating = false,
                    notifications = pageResult.getOrNull()?.list ?: state.notifications,
                    unreadCount = unreadResult.getOrNull() ?: state.unreadCount,
                    errorMessage = pageResult.exceptionOrNull()?.message
                        ?: unreadResult.exceptionOrNull()?.message
                )
            }
        }
    }
}
