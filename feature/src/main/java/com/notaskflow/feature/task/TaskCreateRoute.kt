package com.notaskflow.feature.task

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.notaskflow.domain.model.Project
import com.notaskflow.domain.model.SpaceMember
import com.notaskflow.domain.model.TaskMode
import com.notaskflow.domain.model.TaskPriority
import com.notaskflow.feature.common.DateTimePickerField
import com.notaskflow.feature.common.NotaskFilledTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCreateRoute(
    spaceId: Long?,
    isPersonalSpace: Boolean = false,
    currentUserId: Long? = null,
    onBack: () -> Unit,
    onCreated: (Long) -> Unit,
    viewModel: TaskCreateViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(spaceId, isPersonalSpace, currentUserId) {
        viewModel.bindSpace(
            spaceId = spaceId,
            isPersonalSpace = isPersonalSpace,
            currentUserId = currentUserId
        )
    }
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is TaskCreateEffect.Created -> onCreated(effect.taskId)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("新建任务", fontWeight = FontWeight.SemiBold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::save, enabled = !uiState.isSaving && spaceId != null) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.padding(10.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.Check, contentDescription = "保存")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            NotaskFilledTextField(
                value = uiState.title,
                onValueChange = viewModel::updateTitle,
                label = "任务标题",
                placeholder = "写下这件事要达成的结果",
                singleLine = true
            )
            NotaskFilledTextField(
                value = uiState.description,
                onValueChange = viewModel::updateDescription,
                label = "任务说明",
                placeholder = "补充背景、验收标准或下一步动作",
                minHeight = 142.dp,
                singleLine = false
            )
            if (uiState.isPersonalSpace) {
                PersonalTaskHintCard()
            } else {
                ChoiceSection(title = "任务模式") {
                    TaskMode.entries.forEach { mode ->
                        TaskChoiceChip(
                            selected = uiState.mode == mode,
                            onClick = { viewModel.updateMode(mode) },
                            label = mode.label()
                        )
                    }
                }
                if (uiState.mode == TaskMode.ASSIGNED) {
                    AssigneeSection(
                        members = uiState.members,
                        selectedAssigneeIds = uiState.selectedAssigneeIds,
                        onToggleAssignee = viewModel::toggleAssignee
                    )
                }
            }
            ChoiceSection(title = "优先级") {
                TaskPriority.entries.forEach { priority ->
                    TaskChoiceChip(
                        selected = uiState.priority == priority,
                        onClick = { viewModel.updatePriority(priority) },
                        label = priority.label()
                    )
                }
            }
            DateTimePickerField(
                value = uiState.deadline,
                onValueChange = viewModel::updateDeadline,
                modifier = Modifier.fillMaxWidth(),
                label = "截止时间",
                placeholder = "不选择则默认无截止时间"
            )
            if (!uiState.isPersonalSpace) {
                ProjectPicker(
                    projects = uiState.projects,
                    selectedProjectId = uiState.selectedProjectId,
                    onSelectProject = viewModel::selectProject
                )
            }
            uiState.errorMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PersonalTaskHintCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.48f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "个人任务",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "个人空间任务会自动分配给当前账号，不再需要选择任务模式。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ProjectPicker(
    projects: List<Project>,
    selectedProjectId: Long?,
    onSelectProject: (Long?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedProject = projects.firstOrNull { project -> project.id == selectedProjectId }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("归属项目", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .clickable { expanded = true }
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedProject?.name ?: "不关联项目",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = selectedProject?.description?.takeIf { it.isNotBlank() } ?: "可选，任务将显示在对应项目下",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "选择项目")
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("不关联项目") },
                    onClick = {
                        onSelectProject(null)
                        expanded = false
                    }
                )
                projects.forEach { project ->
                    DropdownMenuItem(
                        text = { Text(project.name) },
                        onClick = {
                            onSelectProject(project.id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AssigneeSection(
    members: List<SpaceMember>,
    selectedAssigneeIds: Set<Long>,
    onToggleAssignee: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("指派成员", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        if (members.isEmpty()) {
            Text(
                text = "当前空间暂无可选成员",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            members.chunked(2).forEach { rowMembers ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowMembers.forEach { member ->
                        FilterChip(
                            selected = selectedAssigneeIds.contains(member.userId),
                            onClick = { onToggleAssignee(member.userId) },
                            label = { Text(member.displayName()) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChoiceSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            content()
        }
    }
}

@Composable
private fun TaskChoiceChip(selected: Boolean, onClick: () -> Unit, label: String) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            )
        },
        shape = RoundedCornerShape(50),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
            selectedBorderColor = Color.Transparent
        )
    )
}

private fun SpaceMember.displayName(): String {
    return nickname?.takeIf { it.isNotBlank() } ?: username
}

private fun TaskMode.label(): String {
    return when (this) {
        TaskMode.ASSIGNED -> "指派"
        TaskMode.OPEN -> "开放认领"
    }
}

private fun TaskPriority.label(): String {
    return when (this) {
        TaskPriority.LOW -> "低"
        TaskPriority.MEDIUM -> "中"
        TaskPriority.HIGH -> "高"
    }
}
