package com.notaskflow.data.todo.dto

import com.notaskflow.domain.model.Todo
import com.notaskflow.domain.model.TodoSave
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TodoDto(
    @param:Json(name = "id") val id: Long,
    @param:Json(name = "spaceId") val spaceId: Long,
    @param:Json(name = "taskId") val taskId: Long?,
    @param:Json(name = "title") val title: String,
    @param:Json(name = "isCompleted") val isCompleted: Boolean,
    @param:Json(name = "deadline") val deadline: String?,
    @param:Json(name = "completedAt") val completedAt: String?,
    @param:Json(name = "gmtCreate") val gmtCreate: String?
)

@JsonClass(generateAdapter = true)
data class TodoSaveRequestDto(
    @param:Json(name = "title") val title: String,
    @param:Json(name = "deadline") val deadline: String?
)

fun TodoDto.toDomain(): Todo {
    return Todo(
        id = id,
        spaceId = spaceId,
        taskId = taskId,
        title = title,
        isCompleted = isCompleted,
        deadline = deadline,
        completedAt = completedAt,
        gmtCreate = gmtCreate
    )
}

fun TodoSave.toDto(): TodoSaveRequestDto {
    return TodoSaveRequestDto(
        title = title,
        deadline = deadline
    )
}
