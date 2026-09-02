package com.notaskflow.data.di

import com.notaskflow.data.auth.AuthRepositoryImpl
import com.notaskflow.data.file.FileRepositoryImpl
import com.notaskflow.data.note.NoteDraftRepositoryImpl
import com.notaskflow.data.note.NoteRepositoryImpl
import com.notaskflow.data.notification.NotificationRepositoryImpl
import com.notaskflow.data.project.ProjectRepositoryImpl
import com.notaskflow.data.space.SpaceRepositoryImpl
import com.notaskflow.data.stats.StatsRepositoryImpl
import com.notaskflow.data.task.TaskRepositoryImpl
import com.notaskflow.data.team.TeamApplicationRepositoryImpl
import com.notaskflow.data.todo.TodoRepositoryImpl
import com.notaskflow.data.user.UserRepositoryImpl
import com.notaskflow.domain.auth.AuthRepository
import com.notaskflow.domain.file.FileRepository
import com.notaskflow.domain.note.NoteDraftRepository
import com.notaskflow.domain.note.NoteRepository
import com.notaskflow.domain.notification.NotificationRepository
import com.notaskflow.domain.project.ProjectRepository
import com.notaskflow.domain.space.SpaceRepository
import com.notaskflow.domain.stats.StatsRepository
import com.notaskflow.domain.task.TaskRepository
import com.notaskflow.domain.team.TeamApplicationRepository
import com.notaskflow.domain.todo.TodoRepository
import com.notaskflow.domain.user.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        repository: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        repository: UserRepositoryImpl
    ): UserRepository

    @Binds
    @Singleton
    abstract fun bindSpaceRepository(
        repository: SpaceRepositoryImpl
    ): SpaceRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(
        repository: NotificationRepositoryImpl
    ): NotificationRepository

    @Binds
    @Singleton
    abstract fun bindTodoRepository(
        repository: TodoRepositoryImpl
    ): TodoRepository

    @Binds
    @Singleton
    abstract fun bindNoteRepository(
        repository: NoteRepositoryImpl
    ): NoteRepository

    @Binds
    @Singleton
    abstract fun bindNoteDraftRepository(
        repository: NoteDraftRepositoryImpl
    ): NoteDraftRepository

    @Binds
    @Singleton
    abstract fun bindFileRepository(
        repository: FileRepositoryImpl
    ): FileRepository

    @Binds
    @Singleton
    abstract fun bindProjectRepository(
        repository: ProjectRepositoryImpl
    ): ProjectRepository

    @Binds
    @Singleton
    abstract fun bindStatsRepository(
        repository: StatsRepositoryImpl
    ): StatsRepository

    @Binds
    @Singleton
    abstract fun bindTaskRepository(
        repository: TaskRepositoryImpl
    ): TaskRepository

    @Binds
    @Singleton
    abstract fun bindTeamApplicationRepository(
        repository: TeamApplicationRepositoryImpl
    ): TeamApplicationRepository
}
