package com.notaskflow.feature.task

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.ViewKanban
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notaskflow.core.model.TaskPriority

// 任务项
data class TaskCardItem(
    val id: Long, val title: String, val priority: TaskPriority,
    val progress: Float = 0f, val deadline: String = "", val updatedAt: String = ""
)

private enum class TaskViewMode { LIST, KANBAN }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListRoute(
    modifier: Modifier = Modifier,
    onTaskClick: (Long) -> Unit = {},
    onCreateTask: () -> Unit = {}
) {
    var viewMode by remember { mutableStateOf(TaskViewMode.LIST) }
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Pending", "In Progress", "Completed", "Cancelled")

    val tasks = remember {
        listOf(
            TaskCardItem(1, "Deep Work: Journaling System", TaskPriority.HIGH, 0.75f, "Due: Oct 24", "2h ago"),
            TaskCardItem(2, "Weekly Meal Reflection", TaskPriority.MEDIUM, 0.3f, "Due: Oct 26", "5h ago"),
            TaskCardItem(3, "Plant Care & Propagation", TaskPriority.LOW, 1f, "Due: Oct 30", "1d ago"),
            TaskCardItem(4, "Book Summary: Essentialism", TaskPriority.MEDIUM, 0.5f, "Due: Oct 28", "10m ago"),
            TaskCardItem(5, "Morning Routine Redesign", TaskPriority.HIGH, 0.15f, "Due: Oct 23", "1h ago"),
            TaskCardItem(6, "API Documentation Update", TaskPriority.LOW, 0.9f, "Due: Nov 1", "3h ago")
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // 标题 + 视图切换段控制器
            item {
                Text("My Tasks", style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))

                // 段控件：List / Kanban
                Row(
                    Modifier.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    SegmentedButton("List", Icons.Filled.ViewList, viewMode == TaskViewMode.LIST) {
                        viewMode = TaskViewMode.LIST
                    }
                    SegmentedButton("Kanban", Icons.Filled.ViewKanban, viewMode == TaskViewMode.KANBAN) {
                        viewMode = TaskViewMode.KANBAN
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            // 筛选芯片
            item {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    filters.forEach { f ->
                        val sel = selectedFilter == f
                        Box(
                            Modifier.clip(RoundedCornerShape(50))
                                .background(if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow)
                                .clickable { selectedFilter = f }
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            Text(f, style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                color = if (sel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            // 任务卡片（Bento 风格不对称布局）
            items(tasks) { task ->
                TaskBentoCard(task) { onTaskClick(task.id) }
                Spacer(Modifier.height(12.dp))
            }

            item { Spacer(Modifier.height(80.dp)) }
        }

        FloatingActionButton(
            onClick = onCreateTask,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Filled.Add, "新建任务", tint = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

@Composable
private fun SegmentedButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        Modifier.clip(RoundedCornerShape(10.dp)).background(bg).clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(16.dp), tint = fg)
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = fg, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun TaskBentoCard(task: TaskCardItem, onClick: () -> Unit) {
    // 根据优先级决定卡片样式
    val (badgeBg, badgeFg) = when (task.priority) {
        TaskPriority.HIGH -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        TaskPriority.MEDIUM -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        TaskPriority.LOW -> MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurfaceVariant
    }
    val cardBg = when (task.priority) {
        TaskPriority.HIGH -> MaterialTheme.colorScheme.surfaceContainerHighest
        TaskPriority.MEDIUM -> MaterialTheme.colorScheme.surfaceContainerLow
        TaskPriority.LOW -> MaterialTheme.colorScheme.surfaceBright
    }

    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = if (task.priority == TaskPriority.HIGH) 3.dp else 1.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            // 头部：优先级标签 + 更多按钮
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.clip(RoundedCornerShape(50)).background(badgeBg).padding(horizontal = 12.dp, vertical = 4.dp)) {
                    Text(
                        text = when (task.priority) { TaskPriority.HIGH -> "High"; TaskPriority.MEDIUM -> "Medium"; TaskPriority.LOW -> "Low" },
                        style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = badgeFg
                    )
                }
                Icon(Icons.Filled.MoreVert, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(12.dp))

            // 标题
            Text(task.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)

            Spacer(Modifier.height(16.dp))

            // 进度条
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Progress", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${(task.progress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { task.progress }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = when (task.priority) {
                    TaskPriority.HIGH -> MaterialTheme.colorScheme.error
                    TaskPriority.MEDIUM -> MaterialTheme.colorScheme.primary
                    TaskPriority.LOW -> MaterialTheme.colorScheme.tertiary
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            // 底部信息
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CalendarToday, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text(task.deadline, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Update, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text(task.updatedAt, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
