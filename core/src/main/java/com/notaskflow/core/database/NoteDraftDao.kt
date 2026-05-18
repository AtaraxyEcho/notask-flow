package com.notaskflow.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface NoteDraftDao {
    @Query("SELECT * FROM note_drafts WHERE draft_key = :draftKey LIMIT 1")
    suspend fun findDraft(draftKey: String): NoteDraftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDraft(draft: NoteDraftEntity)

    @Query("DELETE FROM note_drafts WHERE draft_key = :draftKey")
    suspend fun deleteDraft(draftKey: String)
}
