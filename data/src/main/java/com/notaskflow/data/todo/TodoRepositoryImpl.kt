package com.notaskflow.data.todo

import com.notaskflow.core.database.TodoDao
import com.notaskflow.data.common.toDomain
import com.notaskflow.data.todo.api.TodoApi
import com.notaskflow.data.todo.dto.toDomain
import com.notaskflow.data.todo.dto.toDto
import com.notaskflow.domain.model.Page
import com.notaskflow.domain.model.Todo
import com.notaskflow.domain.model.TodoQuery
import com.notaskflow.domain.model.TodoSave
import com.notaskflow.domain.todo.TodoRepository
import javax.inject.Inject

class TodoRepositoryImpl @Inject constructor(
    private val todoApi: TodoApi,
    private val todoDao: TodoDao
) : TodoRepository {
    override suspend fun page(spaceId: Long, query: TodoQuery): Result<Page<Todo>> {
        val networkResult = runCatching {
            todoApi.page(
                spaceId = spaceId,
                pageNum = query.pageNum,
                pageSize = query.pageSize,
                keyword = query.keyword,
                isCompleted = query.isCompleted
            ).getOrThrow().toDomain { it.toDomain() }
                .also { page ->
                    todoDao.upsertTodos(page.list.map { it.toTodoEntity(System.currentTimeMillis()) })
                }
        }
        if (networkResult.isSuccess) {
            return networkResult
        }
        return cachedPage(spaceId, query, networkResult.exceptionOrNull())
    }

    private suspend fun cachedPage(
        spaceId: Long,
        query: TodoQuery,
        failure: Throwable?
    ): Result<Page<Todo>> {
        return runCatching {
            val offset = ((query.pageNum - 1).coerceAtLeast(0)) * query.pageSize
            val keyword = query.keyword?.takeIf { it.isNotBlank() }
            val cachedTodos = todoDao.queryTodos(
                spaceId = spaceId,
                keyword = keyword,
                isCompleted = query.isCompleted,
                limit = query.pageSize,
                offset = offset
            )
            val total = todoDao.countTodos(
                spaceId = spaceId,
                keyword = keyword,
                isCompleted = query.isCompleted
            )
            if (total == 0L && cachedTodos.isEmpty()) {
                throw failure ?: IllegalStateException("本地暂无待办缓存")
            }
            Page(
                total = total,
                pageNum = query.pageNum,
                pageSize = query.pageSize,
                list = cachedTodos.map { it.toCachedTodo() }
            )
        }
    }

    override suspend fun create(spaceId: Long, todo: TodoSave): Result<Todo> {
        return runCatching {
            todoApi.create(spaceId, todo.toDto()).getOrThrow().toDomain()
                .also { created -> todoDao.upsertTodo(created.toTodoEntity(System.currentTimeMillis())) }
        }
    }

    override suspend fun update(spaceId: Long, id: Long, todo: TodoSave): Result<Todo> {
        return runCatching {
            todoApi.update(spaceId, id, todo.toDto()).getOrThrow().toDomain()
                .also { updated -> todoDao.upsertTodo(updated.toTodoEntity(System.currentTimeMillis())) }
        }
    }

    override suspend fun delete(spaceId: Long, id: Long): Result<Unit> {
        return runCatching {
            todoApi.delete(spaceId, id).requireSuccess()
            todoDao.deleteTodo(spaceId, id)
        }
    }

    override suspend fun complete(spaceId: Long, id: Long): Result<Todo> {
        return runCatching {
            todoApi.complete(spaceId, id).getOrThrow().toDomain()
                .also { todo -> todoDao.upsertTodo(todo.toTodoEntity(System.currentTimeMillis())) }
        }
    }

    override suspend fun uncomplete(spaceId: Long, id: Long): Result<Todo> {
        return runCatching {
            todoApi.uncomplete(spaceId, id).getOrThrow().toDomain()
                .also { todo -> todoDao.upsertTodo(todo.toTodoEntity(System.currentTimeMillis())) }
        }
    }
}
