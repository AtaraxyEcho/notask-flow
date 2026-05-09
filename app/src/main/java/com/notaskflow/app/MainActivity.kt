package com.notaskflow.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.notaskflow.core.ui.theme.NotaskFlowTheme
import com.notaskflow.feature.auth.LoginRoute
import com.notaskflow.feature.home.HomeRoute
import com.notaskflow.feature.note.NoteEditRoute
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
private fun NotaskFlowApp() {
    val navController = rememberNavController()
    NotaskFlowTheme {
        NavHost(
            navController = navController,
            startDestination = "auth/login"
        ) {
            composable("auth/login") {
                LoginRoute(
                    onLoginSuccess = {
                        navController.navigate("home") {
                            popUpTo("auth/login") { inclusive = true }
                        }
                    }
                )
            }
            composable("home") {
                HomeRoute(
                    onNavigateToNoteEdit = { navController.navigate("note/edit") },
                    onNavigateToTaskCreate = { },
                    onNavigateToTodoCreate = { }
                )
            }
            composable("note/edit?noteId={noteId}",
                arguments = listOf(navArgument("noteId") {
                    type = NavType.LongType; defaultValue = -1L
                })
            ) {
                NoteEditRoute(onBack = { navController.popBackStack() })
            }
        }
    }
}
