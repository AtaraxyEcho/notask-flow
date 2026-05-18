package com.notaskflow.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notaskflow.core.datastore.TokenManager
import com.notaskflow.core.model.SpaceType as UiSpaceType
import com.notaskflow.core.ui.theme.PersonalPreset
import com.notaskflow.core.ui.theme.ThemeConfig
import com.notaskflow.core.ui.theme.ThemeMode
import com.notaskflow.domain.auth.AuthRepository
import com.notaskflow.domain.model.NotificationSettings
import com.notaskflow.domain.model.Space
import com.notaskflow.domain.model.SpaceType
import com.notaskflow.domain.notification.NotificationRepository
import com.notaskflow.domain.space.SpaceRepository
import com.notaskflow.domain.user.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@HiltViewModel
class AppViewModel @Inject constructor(
    private val tokenManager: TokenManager,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val spaceRepository: SpaceRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = mutableUiState
    private var tokenRefreshJob: Job? = null

    init {
        observeTokenInvalidation()
        refreshSession()
    }

    fun refreshSession(showInitializing: Boolean = true) {
        viewModelScope.launch {
            if (showInitializing) {
                mutableUiState.update { it.copy(isInitializing = true, errorMessage = null) }
            } else {
                mutableUiState.update { it.copy(errorMessage = null) }
            }
            val token = tokenManager.currentToken()
            if (token.isNullOrBlank()) {
                tokenRefreshJob?.cancel()
                mutableUiState.update {
                    AppUiState(isInitializing = false, themeConfig = it.themeConfig)
                }
                return@launch
            }
            val refreshResult = refreshTokenIfNeeded()
            if (refreshResult.isFailure) {
                tokenRefreshJob?.cancel()
                tokenManager.clear()
                mutableUiState.update {
                    AppUiState(isInitializing = false, themeConfig = it.themeConfig)
                }
                return@launch
            }
            loadAuthenticatedState()
        }
    }

    fun selectSpace(spaceId: Long) {
        val space = mutableUiState.value.spaces.firstOrNull { it.id == spaceId } ?: return
        viewModelScope.launch {
            val permissions = spaceRepository.permissions(space.id).getOrDefault(emptySet())
            mutableUiState.update {
                it.copy(
                    currentSpace = space,
                    permissions = permissions,
                    themeConfig = it.themeConfig.copy(spaceType = space.toUiSpaceType())
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            tokenRefreshJob?.cancel()
            authRepository.logout()
            mutableUiState.update {
                AppUiState(
                    isInitializing = false,
                    themeConfig = it.themeConfig
                )
            }
        }
    }

    private suspend fun loadAuthenticatedState() {
        val userResult = userRepository.profile()
        val spacesResult = spaceRepository.listSpaces()
        if (userResult.isFailure || spacesResult.isFailure) {
            mutableUiState.update {
                it.copy(
                    isInitializing = false,
                    isLoggedIn = false,
                    errorMessage = userResult.exceptionOrNull()?.message
                        ?: spacesResult.exceptionOrNull()?.message
                        ?: "初始化登录状态失败"
                )
            }
            return
        }
        val spaces = spacesResult.getOrDefault(emptyList())
        val currentSpace = chooseInitialSpace(spaces)
        val permissions = currentSpace?.let {
            spaceRepository.permissions(it.id).getOrDefault(emptySet())
        } ?: emptySet()
        val notificationSettings = userRepository.notificationSettings().getOrNull()
        val unreadCount = notificationRepository.unreadCount().getOrDefault(0L)
        scheduleTokenRefresh()
        mutableUiState.update {
            it.copy(
                isInitializing = false,
                isLoggedIn = true,
                currentUser = userResult.getOrNull(),
                spaces = spaces,
                currentSpace = currentSpace,
                permissions = permissions,
                unreadNotificationCount = unreadCount,
                themeConfig = notificationSettings.toThemeConfig(
                    fallback = it.themeConfig,
                    spaceType = currentSpace?.toUiSpaceType() ?: UiSpaceType.PERSONAL
                ),
                errorMessage = null
            )
        }
    }

    private fun observeTokenInvalidation() {
        viewModelScope.launch {
            tokenManager.tokenFlow
                .distinctUntilChanged()
                .collect { token ->
                    if (token.isNullOrBlank() && mutableUiState.value.isLoggedIn) {
                        tokenRefreshJob?.cancel()
                        mutableUiState.update {
                            AppUiState(
                                isInitializing = false,
                                themeConfig = it.themeConfig
                            )
                        }
                    }
                }
        }
    }

    private suspend fun refreshTokenIfNeeded(force: Boolean = false): Result<Unit> {
        val expireAt = tokenManager.expireTime()?.toEpochMillis() ?: return Result.success(Unit)
        val shouldRefresh = force || expireAt - System.currentTimeMillis() <= TOKEN_REFRESH_WINDOW_MS
        if (!shouldRefresh) {
            return Result.success(Unit)
        }
        return authRepository.refresh().map { }
    }

    private fun scheduleTokenRefresh() {
        tokenRefreshJob?.cancel()
        tokenRefreshJob = viewModelScope.launch {
            val expireAt = tokenManager.expireTime()?.toEpochMillis() ?: return@launch
            val delayMs = (expireAt - System.currentTimeMillis() - TOKEN_REFRESH_WINDOW_MS)
                .coerceAtLeast(MIN_REFRESH_DELAY_MS)
            delay(delayMs)
            refreshTokenIfNeeded(force = true)
                .onSuccess { scheduleTokenRefresh() }
                .onFailure {
                    tokenManager.clear()
                }
        }
    }

    private fun chooseInitialSpace(spaces: List<Space>): Space? {
        return spaces.firstOrNull { it.type == SpaceType.PERSONAL } ?: spaces.firstOrNull()
    }

    private fun Space.toUiSpaceType(): UiSpaceType {
        return when (type) {
            SpaceType.PERSONAL -> UiSpaceType.PERSONAL
            SpaceType.TEAM -> UiSpaceType.TEAM
        }
    }

    private fun NotificationSettings?.toThemeConfig(fallback: ThemeConfig, spaceType: UiSpaceType): ThemeConfig {
        if (this == null) {
            return fallback.copy(spaceType = spaceType)
        }
        return fallback.copy(
            mode = themeMode.toThemeMode(),
            personalPreset = personalThemePreset.toPersonalPreset(),
            spaceType = spaceType
        )
    }

    private fun String.toThemeMode(): ThemeMode {
        return when (lowercase()) {
            "dark" -> ThemeMode.DARK
            "system" -> ThemeMode.SYSTEM
            else -> ThemeMode.LIGHT
        }
    }

    private fun String.toPersonalPreset(): PersonalPreset {
        return when (lowercase()) {
            "forest" -> PersonalPreset.FOREST
            "ocean" -> PersonalPreset.OCEAN
            "midnight" -> PersonalPreset.MIDNIGHT
            else -> PersonalPreset.SUNRISE
        }
    }

    private fun Long.toEpochMillis(): Long {
        return if (this < EPOCH_MILLIS_THRESHOLD) {
            this * MILLIS_PER_SECOND
        } else {
            this
        }
    }

    private companion object {
        const val TOKEN_REFRESH_WINDOW_MS = 5L * 60L * 1000L
        const val MIN_REFRESH_DELAY_MS = 30L * 1000L
        const val MILLIS_PER_SECOND = 1000L
        const val EPOCH_MILLIS_THRESHOLD = 1_000_000_000_000L
    }
}
