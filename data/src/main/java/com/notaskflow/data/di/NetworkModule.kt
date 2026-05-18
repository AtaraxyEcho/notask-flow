package com.notaskflow.data.di

import android.content.Context
import com.notaskflow.core.datastore.TokenManager
import com.notaskflow.core.network.AuthInvalidationInterceptor
import com.notaskflow.core.network.AuthInterceptor
import com.notaskflow.core.network.LocalDateTimeJsonAdapter
import com.notaskflow.data.BuildConfig
import com.notaskflow.data.auth.TokenRefreshAuthenticator
import com.notaskflow.data.auth.api.AuthApi
import com.notaskflow.data.file.api.FileApi
import com.notaskflow.data.note.api.NoteApi
import com.notaskflow.data.notification.api.NotificationApi
import com.notaskflow.data.project.api.ProjectApi
import com.notaskflow.data.space.api.SpaceApi
import com.notaskflow.data.stats.api.StatsApi
import com.notaskflow.data.task.api.TaskApi
import com.notaskflow.data.team.api.TeamApplicationApi
import com.notaskflow.data.todo.api.TodoApi
import com.notaskflow.data.user.api.UserApi
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Authenticator
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideTokenManager(
        @ApplicationContext context: Context
    ): TokenManager = TokenManager(context)

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(LocalDateTimeJsonAdapter())
            .build()
    }

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        val level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BASIC
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
        return HttpLoggingInterceptor()
            .apply { redactHeader(AUTHORIZATION_HEADER) }
            .setLevel(level)
    }

    @Provides
    @Singleton
    @Named("refreshOkHttpClient")
    fun provideRefreshOkHttpClient(
        tokenManager: TokenManager,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenManager))
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    @Named("refreshRetrofit")
    fun provideRefreshRetrofit(
        @Named("refreshOkHttpClient") okHttpClient: OkHttpClient,
        moshi: Moshi
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    @Named("refreshAuthApi")
    fun provideRefreshAuthApi(
        @Named("refreshRetrofit") retrofit: Retrofit
    ): AuthApi {
        return retrofit.create(AuthApi::class.java)
    }

    @Provides
    @Singleton
    fun provideAuthenticator(
        @Named("refreshAuthApi") refreshAuthApi: AuthApi,
        tokenManager: TokenManager
    ): Authenticator {
        return TokenRefreshAuthenticator(refreshAuthApi, tokenManager)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        tokenManager: TokenManager,
        loggingInterceptor: HttpLoggingInterceptor,
        authenticator: Authenticator
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenManager))
            .addInterceptor(loggingInterceptor)
            .addInterceptor(AuthInvalidationInterceptor(tokenManager))
            .authenticator(authenticator)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        moshi: Moshi
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi {
        return retrofit.create(AuthApi::class.java)
    }

    @Provides
    @Singleton
    fun provideUserApi(retrofit: Retrofit): UserApi {
        return retrofit.create(UserApi::class.java)
    }

    @Provides
    @Singleton
    fun provideSpaceApi(retrofit: Retrofit): SpaceApi {
        return retrofit.create(SpaceApi::class.java)
    }

    @Provides
    @Singleton
    fun provideNotificationApi(retrofit: Retrofit): NotificationApi {
        return retrofit.create(NotificationApi::class.java)
    }

    @Provides
    @Singleton
    fun provideTodoApi(retrofit: Retrofit): TodoApi {
        return retrofit.create(TodoApi::class.java)
    }

    @Provides
    @Singleton
    fun provideNoteApi(retrofit: Retrofit): NoteApi {
        return retrofit.create(NoteApi::class.java)
    }

    @Provides
    @Singleton
    fun provideFileApi(retrofit: Retrofit): FileApi {
        return retrofit.create(FileApi::class.java)
    }

    @Provides
    @Singleton
    fun provideProjectApi(retrofit: Retrofit): ProjectApi {
        return retrofit.create(ProjectApi::class.java)
    }

    @Provides
    @Singleton
    fun provideStatsApi(retrofit: Retrofit): StatsApi {
        return retrofit.create(StatsApi::class.java)
    }

    @Provides
    @Singleton
    fun provideTaskApi(retrofit: Retrofit): TaskApi {
        return retrofit.create(TaskApi::class.java)
    }

    @Provides
    @Singleton
    fun provideTeamApplicationApi(retrofit: Retrofit): TeamApplicationApi {
        return retrofit.create(TeamApplicationApi::class.java)
    }

    private const val AUTHORIZATION_HEADER = "Authorization"
}
