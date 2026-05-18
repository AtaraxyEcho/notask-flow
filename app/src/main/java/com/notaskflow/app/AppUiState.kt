package com.notaskflow.app

import com.notaskflow.core.ui.theme.ThemeConfig
import com.notaskflow.domain.model.Space
import com.notaskflow.domain.model.UserProfile

data class AppUiState(
    val isInitializing: Boolean = true,
    val isLoggedIn: Boolean = false,
    val currentUser: UserProfile? = null,
    val spaces: List<Space> = emptyList(),
    val currentSpace: Space? = null,
    val permissions: Set<String> = emptySet(),
    val unreadNotificationCount: Long = 0,
    val themeConfig: ThemeConfig = ThemeConfig(),
    val errorMessage: String? = null
)
