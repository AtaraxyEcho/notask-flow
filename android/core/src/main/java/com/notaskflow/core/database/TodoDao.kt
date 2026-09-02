package com.notaskflow.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TodoDao {
    @Query(
        """
        SELECT * FROM todos
        WHERE space_id = :spaceId
        AND (:isCompleted IS NULL OR is_completed = :isCompleted)
        AND (:keyword IS NULL OR title LIKE '%' || :keyword || '%')
        ORDER BY is_completed ASC, deadline ASC, cached_at DESC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun queryTodos(
        spaceId: Long,
        keyword: String?,
        isCompleted: Boolean?,
        limit: Long,
        offset: Long
    ): List<TodoEntity>

    @Query(
        """
        SELECT COUNT(*) FROM todos
        WHERE space_id = :spaceId
        AND (:isCompleted IS NULL OR is_completed = :isCompleted)
        AND (:keyword IS NULL OR title LIKE '%' || :keyword || '%')
        """
    )
    suspend fun countTodos(
        spaceId: Long,
        keyword: String?,
        isCompleted: Boolean?
    ): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTodo(todo: TodoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTodos(todos: List<TodoEntity>)

    @Query("DELETE FROM todos WHERE space_id = :spaceId AND id = :id")
    suspend fun deleteTodo(spaceId: Long, id: Long)
}
