package com.notaskflow.data.di

import android.content.Context
import androidx.room.Room
import com.notaskflow.core.database.NoteDao
import com.notaskflow.core.database.NoteDraftDao
import com.notaskflow.core.database.NotaskFlowDatabase
import com.notaskflow.core.database.TodoDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): NotaskFlowDatabase {
        return Room.databaseBuilder(
            context,
            NotaskFlowDatabase::class.java,
            DATABASE_NAME
        ).addMigrations(NotaskFlowDatabase.MIGRATION_2_3)
            .build()
    }

    @Provides
    @Singleton
    fun provideNoteDao(database: NotaskFlowDatabase): NoteDao {
        return database.noteDao()
    }

    @Provides
    @Singleton
    fun provideNoteDraftDao(database: NotaskFlowDatabase): NoteDraftDao {
        return database.noteDraftDao()
    }

    @Provides
    @Singleton
    fun provideTodoDao(database: NotaskFlowDatabase): TodoDao {
        return database.todoDao()
    }

    private const val DATABASE_NAME = "notask_flow.db"
}
