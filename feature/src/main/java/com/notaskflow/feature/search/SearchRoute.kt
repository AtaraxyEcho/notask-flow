package com.notaskflow.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Task
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.notaskflow.core.common.formatDateTimeText
import com.notaskflow.domain.model.ManagedFile
import com.notaskflow.domain.model.Note
import com.notaskflow.domain.model.Project
import com.notaskflow.domain.model.Task
import com.notaskflow.domain.model.TaskStatus
import com.notaskflow.domain.model.Todo

private enum class SearchScope(val label: String) {
    ALL("全部"),
    NOTES("笔记"),
    TASKS("任务"),
    TODOS("待办"),
    PROJECTS("项目"),
    FILES("文件")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchRoute(
    spaceId: Long?,
    isTeamSpace: Boolean = false,
    onBack: () -> Unit,
    onNoteClick: (Long) -> Unit,
    onTaskClick: (Long) -> Unit,
    onTodoClick: () -> Unit,
    onProjectClick: (Long) -> Unit,
    onFileClick: (Long) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedScope by remember { mutableStateOf(SearchScope.ALL) }
    val availableScopes = remember(isTeamSpace) {
        if (isTeamSpace) SearchScope.entries else SearchScope.entries.filterNot { it == SearchScope.PROJECTS }
    }

    LaunchedEffect(spaceId, isTeamSpace) {
        viewModel.loadDefault(spaceId, isTeamSpace)
        selectedScope = SearchScope.ALL
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("全局搜索", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CompactSearchField(
                                value = uiState.query,
                                onValueChange = { value ->
                                    viewModel.updateQuery(value)
                                    if (value.isBlank()) {
                                        viewModel.loadDefault(spaceId, isTeamSpace)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                placeholder = if (isTeamSpace) "搜索文档、任务、待办" else "搜索笔记、任务、待办"
                            )
                            Button(
                                onClick = { viewModel.search(spaceId, isTeamSpace) },
                                enabled = !uiState.isLoading,
                                modifier = Modifier.height(38.dp),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Text("搜索", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        SearchScopeTabs(
                            isTeamSpace = isTeamSpace,
                            selectedScope = selectedScope,
                            scopes = availableScopes,
                            onScopeChange = { scope -> selectedScope = scope }
                        )
                        Text(
                            text = if (uiState.query.isBlank()) {
                                buildSearchSummary(
                                    isTeamSpace = isTeamSpace,
                                    noteCount = uiState.notes.size,
                                    taskCount = uiState.tasks.size,
                                    todoCount = uiState.todos.size,
                                    projectCount = uiState.projects.size,
                                    fileCount = uiState.files.size
                                )
                            } else {
                                buildSearchResultSummary(
                                    isTeamSpace = isTeamSpace,
                                    noteCount = uiState.notes.size,
                                    taskCount = uiState.tasks.size,
                                    todoCount = uiState.todos.size,
                                    projectCount = uiState.projects.size,
                                    fileCount = uiState.files.size
                                )
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            uiState.errorMessage?.let { message ->
                item {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                if (selectedScope == SearchScope.ALL || selectedScope == SearchScope.NOTES) {
                    item {
                        SearchSectionTitle(title = if (isTeamSpace) "文档" else "笔记", count = uiState.notes.size)
                    }
                    if (uiState.notes.isEmpty()) {
                        item { SearchStateText(if (uiState.query.isBlank()) if (isTeamSpace) "暂无文档" else "暂无笔记" else if (isTeamSpace) "暂无匹配文档" else "暂无匹配笔记") }
                    } else {
                        items(uiState.notes, key = { note -> "search-note-${note.id}" }) { note ->
                            SearchNoteCard(note = note, onClick = { onNoteClick(note.id) })
                        }
                    }
                }

                if (selectedScope == SearchScope.ALL || selectedScope == SearchScope.TASKS) {
                    item {
                        SearchSectionTitle(title = "任务", count = uiState.tasks.size)
                    }
                    if (uiState.tasks.isEmpty()) {
                        item { SearchStateText(if (uiState.query.isBlank()) "暂无任务" else "暂无匹配任务") }
                    } else {
                        items(uiState.tasks, key = { task -> "search-task-${task.id}" }) { task ->
                            SearchTaskCard(task = task, onClick = { onTaskClick(task.id) })
                        }
                    }
                }

                if (selectedScope == SearchScope.ALL || selectedScope == SearchScope.TODOS) {
                    item {
                        SearchSectionTitle(title = "待办", count = uiState.todos.size)
                    }
                    if (uiState.todos.isEmpty()) {
                        item { SearchStateText(if (uiState.query.isBlank()) "暂无待办" else "暂无匹配待办") }
                    } else {
                        items(uiState.todos, key = { todo -> "search-todo-${todo.id}" }) { todo ->
                            SearchTodoCard(todo = todo, onClick = onTodoClick)
                        }
                    }
                }

                if (isTeamSpace && (selectedScope == SearchScope.ALL || selectedScope == SearchScope.PROJECTS)) {
                    item {
                        SearchSectionTitle(title = "项目", count = uiState.projects.size)
                    }
                    if (uiState.projects.isEmpty()) {
                        item { SearchStateText(if (uiState.query.isBlank()) "暂无项目" else "暂无匹配项目") }
                    } else {
                        items(uiState.projects, key = { project -> "search-project-${project.id}" }) { project ->
                            SearchProjectCard(project = project, onClick = { onProjectClick(project.id) })
                        }
                    }
                }

                if (selectedScope == SearchScope.ALL || selectedScope == SearchScope.FILES) {
                    item {
                        SearchSectionTitle(title = "文件", count = uiState.files.size)
                    }
                    if (uiState.files.isEmpty()) {
                        item { SearchStateText(if (uiState.query.isBlank()) "暂无文件" else "暂无匹配文件") }
                    } else {
                        items(uiState.files, key = { file -> "search-file-${file.id}" }) { file ->
                            SearchFileCard(file = file, onClick = { onFileClick(file.id) })
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(72.dp)) }
        }
    }
}

@Composable
private fun SearchScopeTabs(
    isTeamSpace: Boolean,
    scopes: List<SearchScope>,
    selectedScope: SearchScope,
    onScopeChange: (SearchScope) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        scopes.forEach { scope ->
            val selected = selectedScope == scope
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainer
                    )
                    .clickable { onScopeChange(scope) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = scope.displayLabel(isTeamSpace),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CompactSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(8.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (value.isBlank()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun SearchScope.displayLabel(isTeamSpace: Boolean): String {
    return when (this) {
        SearchScope.ALL -> "全部"
        SearchScope.NOTES -> if (isTeamSpace) "文档" else "笔记"
        SearchScope.TASKS -> "任务"
        SearchScope.TODOS -> "待办"
        SearchScope.PROJECTS -> "项目"
        SearchScope.FILES -> "文件"
    }
}

private fun buildSearchSummary(
    isTeamSpace: Boolean,
    noteCount: Int,
    taskCount: Int,
    todoCount: Int,
    projectCount: Int,
    fileCount: Int
): String {
    return if (isTeamSpace) {
        "$noteCount 篇文档，$taskCount 条任务，$todoCount 条待办，$projectCount 个项目，$fileCount 个文件"
    } else {
        "$noteCount 条笔记，$taskCount 条任务，$todoCount 条待办，$fileCount 个文件"
    }
}

private fun buildSearchResultSummary(
    isTeamSpace: Boolean,
    noteCount: Int,
    taskCount: Int,
    todoCount: Int,
    projectCount: Int,
    fileCount: Int
): String {
    return if (isTeamSpace) {
        "找到 $noteCount 篇文档，$taskCount 条任务，$todoCount 条待办，$projectCount 个项目，$fileCount 个文件"
    } else {
        "找到 $noteCount 条笔记，$taskCount 条任务，$todoCount 条待办，$fileCount 个文件"
    }
}

@Composable
private fun SearchSectionTitle(title: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        ) {
            Text(
                text = "$count 条",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun SearchNoteCard(note: Note, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            ResultIcon(icon = Icons.Filled.Description, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                note.content?.takeIf { it.isNotBlank() }?.let { content ->
                    Text(
                        text = content,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = formatDateTimeText(note.gmtModified).ifBlank { "暂无更新时间" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SearchTaskCard(task: Task, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            ResultIcon(icon = Icons.Filled.Task, tint = MaterialTheme.colorScheme.tertiary)
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = listOf(task.status.label(), task.projectName, formatDateTimeText(task.deadline))
                        .filterNot { it.isNullOrBlank() }
                        .joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SearchTodoCard(todo: Todo, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            ResultIcon(icon = Icons.Filled.CheckCircle, tint = Color(0xFF2196F3))
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = todo.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = listOf(
                        if (todo.isCompleted) "已完成" else "未完成",
                        formatDateTimeText(todo.deadline)
                    ).filterNot { it.isBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SearchProjectCard(project: Project, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            ResultIcon(icon = Icons.Filled.Work, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = listOf(
                        "${project.completedTaskCount}/${project.taskCount} 已完成",
                        "${project.documentCount} 篇文档",
                        formatDateTimeText(project.gmtModified)
                    ).filterNot { it.isBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SearchFileCard(file: ManagedFile, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            val icon = if (file.mimeType?.startsWith("image/") == true) {
                Icons.Filled.Image
            } else {
                Icons.AutoMirrored.Filled.InsertDriveFile
            }
            ResultIcon(icon = icon, tint = MaterialTheme.colorScheme.tertiary)
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = file.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = listOf(
                        formatSearchFileSize(file.fileSize),
                        file.mimeType.orEmpty(),
                        formatDateTimeText(file.gmtCreate)
                    ).filterNot { it.isBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ResultIcon(icon: ImageVector, tint: Color) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(tint.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(21.dp),
            tint = tint
        )
    }
}

@Composable
private fun SearchStateText(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatSearchFileSize(bytes: Long): String {
    if (bytes < 1024L) {
        return "$bytes B"
    }
    val kilobytes = bytes / 1024.0
    if (kilobytes < 1024.0) {
        return String.format("%.1f KB", kilobytes)
    }
    val megabytes = kilobytes / 1024.0
    return String.format("%.1f MB", megabytes)
}

private fun TaskStatus.label(): String {
    return when (this) {
        TaskStatus.PENDING -> "待开始"
        TaskStatus.OPEN -> "待开始"
        TaskStatus.IN_PROGRESS -> "进行中"
        TaskStatus.COMPLETED -> "已完成"
        TaskStatus.CANCELLED -> "已取消"
    }
}
