package com.notaskflow.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.notaskflow.core.model.SpaceType as UiSpaceType
import com.notaskflow.core.ui.components.BottomNavTab
import com.notaskflow.core.ui.components.SpaceItem
import com.notaskflow.core.ui.theme.NotaskFlowTheme
import com.notaskflow.domain.model.Space
import com.notaskflow.domain.model.SpaceType
import com.notaskflow.domain.model.NotificationBusinessType
import com.notaskflow.feature.auth.ForgotPasswordRoute
import com.notaskflow.feature.auth.LoginRoute
import com.notaskflow.feature.auth.RegisterRoute
import com.notaskflow.feature.file.FilePreviewRoute
import com.notaskflow.feature.home.HomeRoute
import com.notaskflow.feature.members.MembersRoute
import com.notaskflow.feature.navigation.AppRoute
import com.notaskflow.feature.note.NoteEditRoute
import com.notaskflow.feature.notification.NotificationRoute
import com.notaskflow.feature.project.ProjectDetailRoute
import com.notaskflow.feature.search.SearchRoute
import com.notaskflow.feature.settings.SettingsRoute
import com.notaskflow.feature.space.CreateTeamSpaceRoute
import com.notaskflow.feature.space.JoinTeamSpaceRoute
import com.notaskflow.feature.task.TaskCreateRoute
import com.notaskflow.feature.task.TaskDetailRoute
import com.notaskflow.feature.todo.TodoListRoute
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { NotaskFlowApp() }
    }
}

@Composable
private fun NotaskFlowApp(
    appViewModel: AppViewModel = hiltViewModel()
) {
    val appUiState by appViewModel.uiState.collectAsState()
    NotaskFlowTheme(config = appUiState.themeConfig) {
        if (appUiState.isInitializing) {
            SplashScreen()
            return@NotaskFlowTheme
        }
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route
        LaunchedEffect(appUiState.isLoggedIn, currentRoute) {
            if (!appUiState.isLoggedIn && currentRoute !in authRoutes) {
                navController.navigate(AppRoute.Login) {
                    popUpTo(navController.graph.startDestinationId) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }
        }
        NavHost(
            navController = navController,
            startDestination = if (appUiState.isLoggedIn) AppRoute.Home else AppRoute.Login
        ) {
            composable(AppRoute.Login) {
                LoginRoute(
                    onLoginSuccess = {
                        appViewModel.refreshSession(showInitializing = true)
                        navController.navigate(AppRoute.Home) {
                            popUpTo(AppRoute.Login) { inclusive = true }
                        }
                    },
                    onRegisterClick = { navController.navigate(AppRoute.Register) },
                    onForgotPasswordClick = { navController.navigate(AppRoute.ForgotPassword) }
                )
            }
            composable(AppRoute.Register) {
                RegisterRoute(onBack = { navController.popBackStack() })
            }
            composable(AppRoute.ForgotPassword) {
                ForgotPasswordRoute(onBack = { navController.popBackStack() })
            }
            composable(AppRoute.Home) {
                HomeRoute(
                    currentSpace = appUiState.currentSpace?.toSpaceItem(),
                    spaces = appUiState.spaces.map { it.toSpaceItem() },
                    unreadNotificationCount = appUiState.unreadNotificationCount
                        .coerceAtMost(MAX_BADGE_COUNT)
                        .toInt(),
                    userAvatarUrl = appUiState.currentUser?.avatarUrl,
                    onSpaceSelected = { space -> appViewModel.selectSpace(space.id) },
                    onNavigateToCreateTeam = { navController.navigate(AppRoute.CreateTeamSpace) },
                    onNavigateToJoinTeam = { navController.navigate(AppRoute.JoinTeamSpace) },
                    onNavigateToSettings = { navController.navigate(AppRoute.Settings) },
                    onNavigateToSearch = { navController.navigate(AppRoute.Search) },
                    onNavigateToNoteEdit = { noteId -> navController.navigate(AppRoute.noteEdit(noteId)) },
                    onNavigateToProject = { projectId -> navController.navigate(AppRoute.projectDetail(projectId)) },
                    onNavigateToTaskDetail = { taskId -> navController.navigate(AppRoute.taskDetail(taskId)) },
                    onNavigateToFilePreview = { fileId -> navController.navigate(AppRoute.filePreview(fileId)) },
                    onNavigateToNotifications = { navController.navigate(AppRoute.Notification) },
                    onNavigateToTaskCreate = { navController.navigate(AppRoute.TaskCreate) },
                    onNavigateToTodoCreate = { }
                )
            }
            composable(AppRoute.HomeTab,
                arguments = listOf(navArgument("tab") {
                    type = NavType.StringType
                })
            ) { backStackEntry ->
                val initialTab = backStackEntry.arguments
                    ?.getString("tab")
                    ?.toBottomNavTab()
                HomeRoute(
                    currentSpace = appUiState.currentSpace?.toSpaceItem(),
                    spaces = appUiState.spaces.map { it.toSpaceItem() },
                    initialTab = initialTab,
                    unreadNotificationCount = appUiState.unreadNotificationCount
                        .coerceAtMost(MAX_BADGE_COUNT)
                        .toInt(),
                    userAvatarUrl = appUiState.currentUser?.avatarUrl,
                    onSpaceSelected = { space -> appViewModel.selectSpace(space.id) },
                    onNavigateToCreateTeam = { navController.navigate(AppRoute.CreateTeamSpace) },
                    onNavigateToJoinTeam = { navController.navigate(AppRoute.JoinTeamSpace) },
                    onNavigateToSettings = { navController.navigate(AppRoute.Settings) },
                    onNavigateToSearch = { navController.navigate(AppRoute.Search) },
                    onNavigateToNoteEdit = { noteId -> navController.navigate(AppRoute.noteEdit(noteId)) },
                    onNavigateToProject = { projectId -> navController.navigate(AppRoute.projectDetail(projectId)) },
                    onNavigateToTaskDetail = { taskId -> navController.navigate(AppRoute.taskDetail(taskId)) },
                    onNavigateToFilePreview = { fileId -> navController.navigate(AppRoute.filePreview(fileId)) },
                    onNavigateToNotifications = { navController.navigate(AppRoute.Notification) },
                    onNavigateToTaskCreate = { navController.navigate(AppRoute.TaskCreate) },
                    onNavigateToTodoCreate = { }
                )
            }
            composable(AppRoute.Settings) {
                SettingsRoute(
                    onSettingsChanged = { appViewModel.refreshSession(showInitializing = false) },
                    onLogout = {
                        appViewModel.logout()
                        navController.navigate(AppRoute.Login) {
                            popUpTo(AppRoute.Home) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(AppRoute.Search) {
                SearchRoute(
                    spaceId = appUiState.currentSpace?.id,
                    isTeamSpace = appUiState.currentSpace?.type == SpaceType.TEAM,
                    onBack = { navController.popBackStack() },
                    onNoteClick = { noteId -> navController.navigate(AppRoute.noteEdit(noteId)) },
                    onTaskClick = { taskId -> navController.navigate(AppRoute.taskDetail(taskId)) },
                    onTodoClick = { navController.navigate(AppRoute.TodoList) },
                    onProjectClick = { projectId -> navController.navigate(AppRoute.projectDetail(projectId)) },
                    onFileClick = { fileId -> navController.navigate(AppRoute.filePreview(fileId)) }
                )
            }
            dialog(AppRoute.CreateTeamSpace) {
                CreateTeamSpaceRoute(
                    onBack = { navController.popBackStack() },
                    onDone = {
                        appViewModel.refreshSession(showInitializing = false)
                        navController.popBackStack()
                    }
                )
            }
            dialog(AppRoute.JoinTeamSpace) {
                JoinTeamSpaceRoute(
                    onBack = { navController.popBackStack() },
                    onDone = {
                        appViewModel.refreshSession(showInitializing = false)
                        navController.popBackStack()
                    }
                )
            }
            composable(AppRoute.NoteEdit,
                arguments = listOf(navArgument("noteId") {
                    type = NavType.LongType; defaultValue = -1L
                })
            ) { backStackEntry ->
                val noteId = backStackEntry.arguments
                    ?.getLong("noteId")
                    ?.takeIf { it > 0 }
                NoteEditRoute(
                    spaceId = appUiState.currentSpace?.id,
                    noteId = noteId,
                    currentUser = appUiState.currentUser,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(AppRoute.ProjectDetail,
                arguments = listOf(navArgument("projectId") {
                    type = NavType.LongType
                })
            ) { backStackEntry ->
                val projectId = backStackEntry.arguments?.getLong("projectId") ?: return@composable
                ProjectDetailRoute(
                    spaceId = appUiState.currentSpace?.id,
                    projectId = projectId,
                    onBack = { navController.popBackStack() },
                    onTaskClick = { taskId -> navController.navigate(AppRoute.taskDetail(taskId)) },
                    onNoteClick = { noteId -> navController.navigate(AppRoute.noteEdit(noteId)) }
                )
            }
            composable(AppRoute.TaskCreate) {
                TaskCreateRoute(
                    spaceId = appUiState.currentSpace?.id,
                    isPersonalSpace = appUiState.currentSpace?.type == SpaceType.PERSONAL,
                    currentUserId = appUiState.currentUser?.id,
                    onBack = { navController.popBackStack() },
                    onCreated = { taskId ->
                        navController.navigate(AppRoute.taskDetail(taskId)) {
                            popUpTo(AppRoute.TaskCreate) { inclusive = true }
                        }
                    }
                )
            }
            composable(AppRoute.TaskDetail,
                arguments = listOf(navArgument("taskId") {
                    type = NavType.LongType
                })
            ) { backStackEntry ->
                val taskId = backStackEntry.arguments?.getLong("taskId") ?: return@composable
                TaskDetailRoute(
                    spaceId = appUiState.currentSpace?.id,
                    taskId = taskId,
                    isTeamSpace = appUiState.currentSpace?.type == SpaceType.TEAM,
                    currentUserId = appUiState.currentUser?.id,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(AppRoute.FilePreview,
                arguments = listOf(navArgument("fileId") {
                    type = NavType.LongType
                })
            ) { backStackEntry ->
                val fileId = backStackEntry.arguments?.getLong("fileId") ?: return@composable
                FilePreviewRoute(
                    spaceId = appUiState.currentSpace?.id,
                    fileId = fileId,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(AppRoute.Notification) {
                NotificationRoute(
                    onBack = {
                        appViewModel.refreshSession(showInitializing = false)
                        navController.popBackStack()
                    },
                    onNotificationClick = { notification ->
                        appViewModel.refreshSession(showInitializing = false)
                        when (notification.businessType) {
                            NotificationBusinessType.TASK -> {
                                navController.navigateHomeTab(BottomNavTab.TASK.name)
                            }
                            NotificationBusinessType.NOTE -> {
                                val tab = if (appUiState.currentSpace?.type == SpaceType.TEAM) {
                                    BottomNavTab.DOCUMENT
                                } else {
                                    BottomNavTab.NOTE
                                }
                                navController.navigateHomeTab(tab.name)
                            }
                            NotificationBusinessType.TODO -> {
                                navController.navigateHomeTab(BottomNavTab.TODO.name)
                            }
                            NotificationBusinessType.SPACE_JOIN_REQUEST -> {
                                navController.navigateHomeTab(BottomNavTab.MEMBERS.name)
                            }
                            null -> Unit
                        }
                    }
                )
            }
            composable(AppRoute.TodoList) {
                TodoListRoute(
                    modifier = Modifier.fillMaxSize(),
                    spaceId = appUiState.currentSpace?.id,
                    onCreateTodo = { navController.popBackStack(AppRoute.Home, inclusive = false) }
                )
            }
            composable(AppRoute.Members) {
                MembersRoute(
                    modifier = Modifier.fillMaxSize(),
                    spaceId = appUiState.currentSpace?.id
                )
            }
        }
    }
}

@Composable
private fun SplashScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

private fun Space.toSpaceItem(): SpaceItem {
    return SpaceItem(
        id = id,
        name = name,
        type = when (type) {
            SpaceType.PERSONAL -> UiSpaceType.PERSONAL
            SpaceType.TEAM -> UiSpaceType.TEAM
        },
        memberCount = memberCount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
        hasUnread = unreadCount > 0
    )
}

private fun String.toBottomNavTab(): BottomNavTab? {
    return runCatching { BottomNavTab.valueOf(this) }.getOrNull()
}

private fun NavHostController.navigateHomeTab(tab: String) {
    navigate(AppRoute.homeTab(tab)) {
        popUpTo(AppRoute.Notification) {
            inclusive = true
        }
        launchSingleTop = true
    }
}

private const val MAX_BADGE_COUNT = 99L
private val authRoutes = setOf(
    AppRoute.Login,
    AppRoute.Register,
    AppRoute.ForgotPassword
)
