package com.notaskflow.data.todo

import com.notaskflow.core.database.TodoEntity
import com.notaskflow.domain.model.Todo

fun Todo.toTodoEntity(cachedAt: Long): TodoEntity {
    return TodoEntity(
        id = id,
        spaceId = spaceId,
        taskId = taskId,
        title = title,
        isCompleted = isCompleted,
        deadline = deadline,
        completedAt = completedAt,
        gmtCreate = gmtCreate,
        cachedAt = cachedAt
    )
}

fun TodoEntity.toCachedTodo(): Todo {
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
