package com.notaskflow.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        NoteEntity::class,
        NoteDraftEntity::class,
        NotebookEntity::class,
        TaskEntity::class,
        TodoEntity::class,
        OfflineQueueEntry::class
    ],
    version = 3,
    exportSchema = false
)
abstract class NotaskFlowDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    abstract fun noteDraftDao(): NoteDraftDao

    abstract fun todoDao(): TodoDao

    abstract fun offlineQueueDao(): OfflineQueueDao

    companion object {
        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS note_drafts (
                        draft_key TEXT NOT NULL PRIMARY KEY,
                        space_id INTEGER NOT NULL,
                        note_id INTEGER,
                        notebook_id INTEGER,
                        title TEXT NOT NULL,
                        content TEXT NOT NULL,
                        updated_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
