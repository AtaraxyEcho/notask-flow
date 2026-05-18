package com.notaskflow.feature.navigation

object AppRoute {
    const val Login = "auth/login"
    const val Register = "auth/register"
    const val ForgotPassword = "auth/forgot-password"
    const val Home = "home"
    const val CreateTeamSpace = "space/create-team"
    const val JoinTeamSpace = "space/join-team"
    const val Settings = "settings"
    const val Search = "search"
    const val NoteEdit = "note/edit?noteId={noteId}"
    const val ProjectDetail = "project/{projectId}"
    const val TaskCreate = "task/create"
    const val TaskDetail = "task/{taskId}"
    const val FilePreview = "file/{fileId}"
    const val Notification = "notification"
    const val TodoList = "todo/list"
    const val Members = "members"
    const val HomeTab = "home/tab/{tab}"

    fun noteEdit(noteId: Long? = null): String {
        return if (noteId == null) {
            "note/edit"
        } else {
            "note/edit?noteId=$noteId"
        }
    }

    fun projectDetail(projectId: Long): String {
        return "project/$projectId"
    }

    fun taskDetail(taskId: Long): String {
        return "task/$taskId"
    }

    fun filePreview(fileId: Long): String {
        return "file/$fileId"
    }

    fun homeTab(tab: String): String {
        return "home/tab/$tab"
    }
}
