package com.notaskflow.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        NoteEntity::class,
        TaskEntity::class,
        TodoEntity::class,
        OfflineQueueEntry::class
    ],
    version = 1,
    exportSchema = false
)
abstract class NotaskFlowDatabase : RoomDatabase() {
    abstract fun offlineQueueDao(): OfflineQueueDao
}
