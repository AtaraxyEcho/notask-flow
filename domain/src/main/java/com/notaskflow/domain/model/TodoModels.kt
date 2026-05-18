package com.notaskflow.domain.model

data class Todo(
    val id: Long,
    val spaceId: Long,
    val taskId: Long?,
    val title: String,
    val isCompleted: Boolean,
    val deadline: String?,
    val completedAt: String?,
    val gmtCreate: String?
)

data class TodoQuery(
    val pageNum: Long = 1,
    val pageSize: Long = 20,
    val keyword: String? = null,
    val isCompleted: Boolean? = null
)

data class TodoSave(
    val title: String,
    val deadline: String? = null
)
