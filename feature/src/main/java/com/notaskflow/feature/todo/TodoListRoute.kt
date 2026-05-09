package com.notaskflow.feature.todo

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

data class TodoItem(
    val id: Long, val title: String, val isCompleted: Boolean = false,
    val deadline: String = "", val isOverdue: Boolean = false,
    val category: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoListRoute(
    modifier: Modifier = Modifier,
    onCreateTodo: () -> Unit = {}
) {
    var selectedFilter by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    val filters = listOf("All", "Today", "Upcoming", "Overdue")

    val todos = remember {
        listOf(
            TodoItem(1, "Update the annual gratitude journal entries", false, "Overdue: Oct 24", true),
            TodoItem(2, "Water the monstera and fiddle leaf fig", false, "Today, 4:00 PM"),
            TodoItem(3, "Morning meditation and tea", true, "Completed"),
            TodoItem(4, "Book weekend pottery class", false, "Tomorrow"),
            TodoItem(5, "Read 20 pages of Newsreader philosophy", false, "Today", category = "Self-Care"),
            TodoItem(6, "Plan weekly meal prep for the family", false, "Oct 28")
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // 问候语
            item {
                Text("Good morning, Alex.",
                    style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary)
                Text("What's on your heart today?",
                    style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = FontStyle.Italic)
                Spacer(Modifier.height(24.dp))
            }

            // 搜索栏
            item {
                OutlinedTextField(
                    value = searchQuery, onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    placeholder = { Text("Search your todos...") },
                    leadingIcon = { Icon(Icons.Filled.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                )
                Spacer(Modifier.height(16.dp))
            }

            // 筛选芯片
            item {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    filters.forEach { f ->
                        val sel = selectedFilter == f
                        Box(Modifier.clip(RoundedCornerShape(50))
                            .background(if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable { selectedFilter = f }.padding(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            Text(f, style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            // 灵感图片卡片
            item {
                Card(
                    Modifier.fillMaxWidth().height(180.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Box(Modifier.fillMaxSize()) {
                        Image(
                            painter = painterResource(com.notaskflow.core.R.drawable.personal_todo),
                            contentDescription = "Inspiration",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0x99000000)).padding(20.dp), contentAlignment = Alignment.BottomStart) {
                            Column {
                                Text("Inspiration", style = MaterialTheme.typography.labelMedium, color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                Text("\"The secret of getting ahead is getting started.\"",
                                    style = MaterialTheme.typography.titleMedium, color = androidx.compose.ui.graphics.Color.White)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            // 待办卡片
            items(todos) { todo ->
                TodoBentoCard(todo)
                Spacer(Modifier.height(12.dp))
            }

            item { Spacer(Modifier.height(80.dp)) }
        }

        FloatingActionButton(
            onClick = onCreateTodo,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Filled.Add, "新建待办", tint = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

@Composable
private fun TodoBentoCard(todo: TodoItem) {
    // 逾期项特殊样式
    val borderMod = if (todo.isOverdue) Modifier.background(MaterialTheme.colorScheme.error.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                    else Modifier

    Card(
        Modifier.fillMaxWidth().then(borderMod),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.Top) {
            // 复选框
            val borderColor = when {
                todo.isOverdue -> MaterialTheme.colorScheme.error
                todo.isCompleted -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.primary
            }
            Box(
                Modifier.size(24.dp).clip(CircleShape).background(
                    if (todo.isCompleted) MaterialTheme.colorScheme.primary else Color.Transparent
                ),
                contentAlignment = Alignment.Center
            ) {
                if (todo.isCompleted) {
                    Icon(Icons.Filled.Done, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimary)
                } else {
                    // 空心圆 — 绘制边框
                    Box(Modifier.size(24.dp).clip(CircleShape).background(Color.Transparent))
                    androidx.compose.foundation.Canvas(Modifier.size(24.dp)) {
                        drawCircle(color = borderColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))
                    }
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    todo.category?.let { cat ->
                        Box(Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)).padding(horizontal = 12.dp, vertical = 3.dp)) {
                            Text(cat, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Icon(Icons.Filled.MoreVert, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(Modifier.height(8.dp))
                Text(todo.title, style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (todo.isCompleted) FontWeight.Normal else FontWeight.Medium,
                    textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (todo.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)

                if (todo.deadline.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val icon = when {
                            todo.isOverdue -> Icons.Filled.EventBusy
                            todo.isCompleted -> Icons.Filled.Done
                            else -> Icons.Filled.CalendarToday
                        }
                        val color = when {
                            todo.isOverdue -> MaterialTheme.colorScheme.error
                            todo.isCompleted -> MaterialTheme.colorScheme.onSurfaceVariant
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Icon(icon, null, Modifier.size(14.dp), tint = color)
                        Spacer(Modifier.width(4.dp))
                        Text(todo.deadline, style = MaterialTheme.typography.labelSmall, color = color)
                    }
                }
            }
        }
    }
}
