package com.notaskflow.domain.todo

import com.notaskflow.domain.model.Page
import com.notaskflow.domain.model.Todo
import com.notaskflow.domain.model.TodoQuery
import com.notaskflow.domain.model.TodoSave

interface TodoRepository {
    suspend fun page(spaceId: Long, query: TodoQuery): Result<Page<Todo>>
    suspend fun create(spaceId: Long, todo: TodoSave): Result<Todo>
    suspend fun update(spaceId: Long, id: Long, todo: TodoSave): Result<Todo>
    suspend fun delete(spaceId: Long, id: Long): Result<Unit>
    suspend fun complete(spaceId: Long, id: Long): Result<Todo>
    suspend fun uncomplete(spaceId: Long, id: Long): Result<Todo>
}
