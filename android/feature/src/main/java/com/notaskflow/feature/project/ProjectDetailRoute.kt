package com.notaskflow.feature.project

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.notaskflow.core.common.formatDateTimeText
import com.notaskflow.domain.model.Note
import com.notaskflow.domain.model.Project
import com.notaskflow.domain.model.ProjectMember
import com.notaskflow.domain.model.ProjectMemberRole
import com.notaskflow.domain.model.SpaceMember
import com.notaskflow.domain.model.Task

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailRoute(
    spaceId: Long? = null,
    projectId: Long,
    onBack: () -> Unit,
    onTaskClick: (Long) -> Unit,
    onNoteClick: (Long) -> Unit,
    viewModel: ProjectDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedSection by remember { mutableStateOf(ProjectSection.TASKS) }
    var showMemberPicker by remember { mutableStateOf(false) }
    var selectedProjectMember by remember { mutableStateOf<ProjectMember?>(null) }

    LaunchedEffect(spaceId, projectId) {
        if (spaceId != null) {
            viewModel.load(spaceId, projectId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.project?.name ?: "项目详情",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    uiState.project?.let { project ->
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(Icons.Filled.Edit, contentDescription = "编辑项目")
                        }
                        IconButton(
                            enabled = !uiState.isSaving,
                            onClick = {
                                val effectiveSpaceId = spaceId
                                if (effectiveSpaceId != null) {
                                    viewModel.archive(effectiveSpaceId, projectId, !project.archived)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (project.archived) {
                                    Icons.Filled.Unarchive
                                } else {
                                    Icons.Filled.Archive
                                },
                                contentDescription = if (project.archived) {
                                    "取消归档"
                                } else {
                                    "归档项目"
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (spaceId == null) {
            StateText("请先选择空间", modifier = Modifier.padding(padding))
            return@Scaffold
        }
        if (uiState.isLoading && uiState.project == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            uiState.project?.let { project ->
                item { ProjectOverview(project) }
                item {
                    ProjectSectionSelector(
                        selectedSection = selectedSection,
                        project = project,
                        onSectionSelected = { selectedSection = it }
                    )
                }
                when (selectedSection) {
                    ProjectSection.TASKS -> {
                        item {
                            Text("项目任务", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                        if (uiState.tasks.isEmpty()) {
                            item { StateText("暂无项目任务") }
                        } else {
                            items(uiState.tasks, key = { task -> "project-task-${task.id}" }) { task ->
                                ProjectTaskRow(task = task, onClick = { onTaskClick(task.id) })
                            }
                        }
                    }
                    ProjectSection.NOTES -> {
                        item {
                            Text("项目文档", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                        if (uiState.notes.isEmpty()) {
                            item { StateText("暂无项目文档") }
                        } else {
                            items(uiState.notes, key = { note -> "project-note-${note.id}" }) { note ->
                                ProjectNoteRow(note = note, onClick = { onNoteClick(note.id) })
                            }
                        }
                    }
                    ProjectSection.MEMBERS -> {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("项目成员", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                if (uiState.availableMembers.isNotEmpty()) {
                                    TextButton(
                                        onClick = { showMemberPicker = true },
                                        enabled = !uiState.isMemberMutating
                                    ) {
                                        Text("添加成员")
                                    }
                                }
                            }
                        }
                        if (uiState.members.isEmpty()) {
                            item { StateText("暂无项目成员") }
                        } else {
                            items(uiState.members, key = { member -> "project-member-${member.userId}" }) { member ->
                                ProjectMemberRow(
                                    member = member,
                                    onClick = { selectedProjectMember = member }
                                )
                            }
                        }
                    }
                }
            }
            uiState.errorMessage?.let { message ->
                item {
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (showEditDialog) {
        uiState.project?.let { project ->
            ProjectFormDialog(
                title = "编辑项目",
                project = project,
                isSaving = uiState.isSaving,
                onDismiss = { showEditDialog = false },
                onConfirm = { save ->
                    val effectiveSpaceId = spaceId
                    if (effectiveSpaceId != null) {
                        viewModel.update(effectiveSpaceId, projectId, save)
                    }
                    showEditDialog = false
                }
            )
        }
    }

    if (showMemberPicker) {
        AddProjectMemberDialog(
            members = uiState.availableMembers,
            isSaving = uiState.isMemberMutating,
            onAdd = { userId ->
                viewModel.addMember(userId)
                showMemberPicker = false
            },
            onDismiss = { showMemberPicker = false }
        )
    }

    selectedProjectMember?.let { member ->
        ProjectMemberActionDialog(
            member = member,
            isSaving = uiState.isMemberMutating,
            onRemove = {
                viewModel.removeMember(member)
                selectedProjectMember = null
            },
            onDismiss = { selectedProjectMember = null }
        )
    }
}

@Composable
private fun ProjectSectionSelector(
    selectedSection: ProjectSection,
    project: Project,
    onSectionSelected: (ProjectSection) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ProjectSection.entries.forEach { section ->
            val selected = section == selectedSection
            Text(
                text = section.label(project),
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        }
                    )
                    .clickable { onSectionSelected(section) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun ProjectOverview(project: Project) {
    val coverColor = project.coverColor.toProjectColor()
    val progress = project.completionRate.coerceIn(0, 100) / 100f
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(coverColor),
                contentAlignment = Alignment.BottomStart
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = project.name,
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (project.archived) {
                            "${project.members.size} 名成员 · 已归档"
                        } else {
                            "${project.members.size} 名成员"
                        },
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = project.description.orEmpty().ifBlank { "暂无项目说明" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = coverColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ProjectMetric("任务", project.taskCount.toString(), Icons.AutoMirrored.Filled.Assignment)
                    ProjectMetric("文档", project.documentCount.toString(), Icons.Filled.Description)
                    ProjectMetric("逾期", project.overdueTaskCount.toString(), Icons.Filled.Person)
                }
            }
        }
    }
}

@Composable
private fun ProjectMetric(label: String, value: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Column(Modifier.padding(start = 6.dp)) {
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ProjectTaskRow(task: Task, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f)
            ) {
                Text(task.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(
                    text = "${task.status.name} · ${task.priority.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ProjectNoteRow(note: Note, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = note.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
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
                text = formatDateTimeText(note.gmtModified).ifBlank { "未记录更新时间" },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProjectMemberRow(
    member: ProjectMember,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = member.displayName().firstOrNull()?.toString().orEmpty(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(min = 0.dp)
            ) {
                Text(
                    text = member.displayName(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = member.email ?: member.username,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = member.role.label(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun AddProjectMemberDialog(
    members: List<SpaceMember>,
    isSaving: Boolean,
    onAdd: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedUserId by remember(members) { mutableStateOf<Long?>(members.firstOrNull()?.userId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加项目成员") },
        text = {
            if (members.isEmpty()) {
                Text(
                    text = "团队成员都已经在当前项目中了。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "从团队成员中选择要加入项目的人。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    members.forEach { member ->
                        val selected = member.userId == selectedUserId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selected) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerLow
                                    }
                                )
                                .clickable { selectedUserId = member.userId }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = member.displayName().firstOrNull()?.toString().orEmpty(),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = member.displayName(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = member.email ?: member.username,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isSaving && selectedUserId != null && members.isNotEmpty(),
                onClick = { selectedUserId?.let(onAdd) }
            ) {
                Text("加入项目")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun ProjectMemberActionDialog(
    member: ProjectMember,
    isSaving: Boolean,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    val isOwner = member.role == ProjectMemberRole.OWNER

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(member.displayName(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = member.email ?: member.username,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "角色：${member.role.label()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isOwner) {
                    Text(
                        text = "当前负责人暂不支持在移动端移出。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            if (!isOwner) {
                Button(
                    enabled = !isSaving,
                    onClick = onRemove
                ) {
                    Text("移出项目")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("关闭")
            }
        }
    )
}

@Composable
private fun StateText(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun String?.toProjectColor(): Color {
    val normalized = this?.takeIf { it.startsWith("#") } ?: return Color(0xFF2D6A4F)
    return runCatching {
        Color(AndroidColor.parseColor(normalized))
    }.getOrDefault(Color(0xFF2D6A4F))
}

private fun ProjectMember.displayName(): String {
    return nickname?.takeIf { it.isNotBlank() } ?: username.ifBlank { "成员" }
}

private fun ProjectMemberRole.label(): String {
    return when (this) {
        ProjectMemberRole.OWNER -> "负责人"
        ProjectMemberRole.MEMBER -> "成员"
    }
}

private fun SpaceMember.displayName(): String {
    return nickname?.takeIf { it.isNotBlank() } ?: username.ifBlank { "成员" }
}

private enum class ProjectSection {
    TASKS,
    NOTES,
    MEMBERS;

    fun label(project: Project): String {
        return when (this) {
            TASKS -> "任务 ${project.taskCount}"
            NOTES -> "文档 ${project.documentCount}"
            MEMBERS -> "成员 ${project.members.size}"
        }
    }
}
