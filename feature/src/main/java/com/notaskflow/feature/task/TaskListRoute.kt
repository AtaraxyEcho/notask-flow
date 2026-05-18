package com.notaskflow.feature.task

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.ViewKanban
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.notaskflow.core.common.formatDateTimeText
import com.notaskflow.domain.model.Task
import com.notaskflow.domain.model.TaskMemberStatus
import com.notaskflow.domain.model.TaskPriority
import com.notaskflow.domain.model.TaskStatus
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private sealed interface TaskViewMode {
    data object List : TaskViewMode

    data object Kanban : TaskViewMode
}

private val kanbanFilters = listOf(
    TaskFilter.PENDING,
    TaskFilter.IN_PROGRESS,
    TaskFilter.COMPLETED,
    TaskFilter.CANCELLED
)

@Composable
fun TaskListRoute(
    modifier: Modifier = Modifier,
    spaceId: Long? = null,
    onTaskClick: (Long) -> Unit = {},
    onCreateTask: () -> Unit = {},
    viewModel: TaskListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var viewMode by remember { mutableStateOf<TaskViewMode>(TaskViewMode.List) }

    LaunchedEffect(spaceId) {
        if (spaceId != null) {
            viewModel.load(spaceId)
        }
    }
    LaunchedEffect(viewMode) {
        viewModel.setKanbanMode(viewMode == TaskViewMode.Kanban)
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            item {
                Text(
                    text = "任务",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "查看并推进团队任务",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                TaskViewModeSwitch(
                    selectedMode = viewMode,
                    onModeChange = { mode -> viewMode = mode }
                )
                Spacer(Modifier.height(16.dp))
            }

            item {
                TaskFilterBar(
                    filters = if (viewMode == TaskViewMode.Kanban) kanbanFilters else TaskFilter.entries,
                    selectedFilter = uiState.selectedFilter,
                    onFilterClick = viewModel::selectFilter
                )
                Spacer(Modifier.height(18.dp))
            }

            if (spaceId == null) {
                item { StateText("请先选择空间") }
            } else if (uiState.isLoading && uiState.tasks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (uiState.errorMessage != null && uiState.tasks.isEmpty()) {
                item { StateText(uiState.errorMessage ?: "加载失败") }
            } else if (uiState.tasks.isEmpty() && viewMode == TaskViewMode.List) {
                item { StateText("暂无任务") }
            } else {
                item {
                    AnimatedContent(
                        targetState = viewMode,
                        transitionSpec = {
                            fadeIn(tween(180)).togetherWith(fadeOut(tween(120)))
                        },
                        label = "taskViewMode"
                    ) { mode ->
                        when (mode) {
                            TaskViewMode.List -> {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    uiState.tasks.forEach { task ->
                                        TaskCard(task = task, onClick = { onTaskClick(task.id) })
                                    }
                                }
                            }
                            TaskViewMode.Kanban -> {
                                TaskKanbanBoard(
                                    tasks = uiState.tasks,
                                    selectedFilter = uiState.selectedFilter,
                                    onFilterClick = viewModel::selectFilter,
                                    onTaskClick = onTaskClick
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }

        FloatingActionButton(
            onClick = onCreateTask,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "新建任务", tint = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

@Composable
private fun TaskViewModeSwitch(selectedMode: TaskViewMode, onModeChange: (TaskViewMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        SegmentedButton(
            label = "列表",
            icon = Icons.AutoMirrored.Filled.ViewList,
            selected = selectedMode == TaskViewMode.List,
            onClick = { onModeChange(TaskViewMode.List) },
            modifier = Modifier.weight(1f)
        )
        SegmentedButton(
            label = "看板",
            icon = Icons.Filled.ViewKanban,
            selected = selectedMode == TaskViewMode.Kanban,
            onClick = { onModeChange(TaskViewMode.Kanban) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SegmentedButton(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val background by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "taskSegmentBackground"
    )
    val foreground by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "taskSegmentForeground"
    )
    Row(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(17.dp), tint = foreground)
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = foreground,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun TaskFilterBar(
    filters: List<TaskFilter>,
    selectedFilter: TaskFilter,
    onFilterClick: (TaskFilter) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val compact = maxWidth < 360.dp
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 8.dp)
        ) {
            filters.forEach { filter ->
                FilterPill(
                    text = filter.label,
                    selected = selectedFilter == filter,
                    onClick = { onFilterClick(filter) },
                    compact = compact,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun FilterPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(if (compact) 34.dp else 38.dp)
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerLow
            )
            .clickable(onClick = onClick)
            .padding(horizontal = if (compact) 4.dp else 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TaskCard(task: Task, onClick: () -> Unit) {
    val progress = task.progress()
    val (priorityBackground, priorityForeground) = task.priority.priorityColors()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(priorityBackground)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = task.priority.label(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = priorityForeground
                    )
                }
                StatusBadge(status = task.status)
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            task.description?.takeIf { it.isNotBlank() }?.let { description ->
                Spacer(Modifier.height(6.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("进度", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CalendarToday, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = formatDateTimeText(task.deadline).ifBlank { "无截止时间" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Group, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${task.members.size} 人",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            task.gmtModified?.let { modified ->
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Update, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = formatDateTimeText(modified),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskKanbanBoard(
    tasks: List<Task>,
    selectedFilter: TaskFilter,
    onFilterClick: (TaskFilter) -> Unit,
    onTaskClick: (Long) -> Unit
) {
    val selectedPage = kanbanFilters.indexOf(selectedFilter).takeIf { it >= 0 } ?: 0
    val pagerState = rememberPagerState(initialPage = selectedPage) { kanbanFilters.size }
    val coroutineScope = rememberCoroutineScope()
    val latestSelectedFilter by rememberUpdatedState(selectedFilter)
    val activePage by remember {
        derivedStateOf { pagerState.targetPage.coerceIn(0, kanbanFilters.lastIndex) }
    }

    LaunchedEffect(selectedPage) {
        if (pagerState.targetPage != selectedPage) {
            pagerState.animateScrollToPage(selectedPage)
        }
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.targetPage.coerceIn(0, kanbanFilters.lastIndex) }
            .distinctUntilChanged()
            .collect { page ->
                val filter = kanbanFilters[page]
                if (filter != latestSelectedFilter) {
                    onFilterClick(filter)
                }
            }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            pageSpacing = 12.dp,
            beyondViewportPageCount = 1
        ) { page ->
            val filter = kanbanFilters[page]
            filter.status?.let { status ->
                TaskKanbanColumn(
                    status = status,
                    tasks = tasks.filter { it.status.kanbanStatus() == status },
                    onTaskClick = onTaskClick,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            kanbanFilters.forEachIndexed { index, filter ->
                val selected = activePage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(width = if (selected) 18.dp else 7.dp, height = 7.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                        .clickable {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                                onFilterClick(filter)
                            }
                        }
                )
            }
        }
    }
}

@Composable
private fun TaskKanbanColumn(
    status: TaskStatus,
    tasks: List<Task>,
    onTaskClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val (statusBackground, statusForeground) = status.statusColors()
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        color = statusBackground.copy(alpha = 0.28f),
        border = BorderStroke(1.dp, statusBackground.copy(alpha = 0.75f))
    ) {
        Column(
            modifier = Modifier
                .heightIn(min = 360.dp)
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = statusBackground
                ) {
                    Text(
                        text = status.label(),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = statusForeground,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = tasks.size.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(10.dp))
            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .heightIn(min = 220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "当前状态暂无任务",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                tasks.forEach { task ->
                    TaskKanbanCard(task = task, onClick = { onTaskClick(task.id) })
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun TaskKanbanCard(task: Task, onClick: () -> Unit) {
    val progress = task.progress()
    val (priorityBackground, priorityForeground) = task.priority.priorityColors()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(priorityBackground)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = task.priority.label(),
                        style = MaterialTheme.typography.labelSmall,
                        color = priorityForeground,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            task.deadline?.let { deadline ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CalendarToday, null, Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = formatDateTimeText(deadline),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun StateText(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TaskPriority.priorityColors(): Pair<Color, Color> {
    return when (this) {
        TaskPriority.HIGH -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        TaskPriority.MEDIUM -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        TaskPriority.LOW -> MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurfaceVariant
    }
}

private fun Task.progress(): Float {
    if (status == TaskStatus.COMPLETED) {
        return 1f
    }
    if (status == TaskStatus.CANCELLED) {
        return 0f
    }
    if (members.isEmpty()) {
        return when (status) {
            TaskStatus.PENDING -> 0.05f
            TaskStatus.OPEN -> 0.05f
            TaskStatus.IN_PROGRESS -> 0.5f
            TaskStatus.COMPLETED -> 1f
            TaskStatus.CANCELLED -> 0f
        }
    }
    return members.count { it.status == TaskMemberStatus.COMPLETED }.toFloat() / members.size
}

private fun TaskPriority.label(): String {
    return when (this) {
        TaskPriority.HIGH -> "高优先级"
        TaskPriority.MEDIUM -> "中优先级"
        TaskPriority.LOW -> "低优先级"
    }
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

private fun TaskStatus.kanbanStatus(): TaskStatus {
    return when (this) {
        TaskStatus.OPEN -> TaskStatus.PENDING
        else -> this
    }
}

@Composable
private fun StatusBadge(status: TaskStatus) {
    val (background, foreground) = status.statusColors()
    Surface(
        shape = RoundedCornerShape(50),
        color = background
    ) {
        Text(
            text = status.label(),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = foreground,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun TaskStatus.statusColors(): Pair<Color, Color> {
    return when (kanbanStatus()) {
        TaskStatus.PENDING -> MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurfaceVariant
        TaskStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        TaskStatus.COMPLETED -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        TaskStatus.CANCELLED -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        TaskStatus.OPEN -> MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurfaceVariant
    }
}
