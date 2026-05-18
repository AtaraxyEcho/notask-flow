package com.notaskflow.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface NoteDao {
    @Query(
        """
        SELECT * FROM notes
        WHERE space_id = :spaceId
        AND (:notebookId IS NULL OR notebook_id = :notebookId)
        AND (:projectId IS NULL OR project_id = :projectId)
        AND (:keyword IS NULL OR title LIKE '%' || :keyword || '%' OR content LIKE '%' || :keyword || '%')
        ORDER BY gmt_modified DESC, cached_at DESC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun queryNotes(
        spaceId: Long,
        notebookId: Long?,
        projectId: Long?,
        keyword: String?,
        limit: Long,
        offset: Long
    ): List<NoteEntity>

    @Query(
        """
        SELECT COUNT(*) FROM notes
        WHERE space_id = :spaceId
        AND (:notebookId IS NULL OR notebook_id = :notebookId)
        AND (:projectId IS NULL OR project_id = :projectId)
        AND (:keyword IS NULL OR title LIKE '%' || :keyword || '%' OR content LIKE '%' || :keyword || '%')
        """
    )
    suspend fun countNotes(
        spaceId: Long,
        notebookId: Long?,
        projectId: Long?,
        keyword: String?
    ): Long

    @Query("SELECT * FROM notes WHERE space_id = :spaceId AND id = :id LIMIT 1")
    suspend fun findNote(spaceId: Long, id: Long): NoteEntity?

    @Query("DELETE FROM notes WHERE space_id = :spaceId AND id = :id")
    suspend fun deleteNote(spaceId: Long, id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNote(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNotes(notes: List<NoteEntity>)

    @Query("SELECT * FROM notebooks WHERE space_id = :spaceId ORDER BY parent_id ASC, sort_order ASC, id ASC")
    suspend fun queryNotebooks(spaceId: Long): List<NotebookEntity>

    @Query("DELETE FROM notebooks WHERE space_id = :spaceId")
    suspend fun deleteNotebooks(spaceId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNotebooks(notebooks: List<NotebookEntity>)
}
