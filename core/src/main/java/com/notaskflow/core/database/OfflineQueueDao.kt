package com.notaskflow.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineQueueDao {
    @Query("SELECT * FROM offline_queue WHERE status = :status ORDER BY createdAt ASC")
    fun observeByStatus(status: String): Flow<List<OfflineQueueEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: OfflineQueueEntry)
}
