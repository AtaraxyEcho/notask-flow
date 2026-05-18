package com.notaskflow.feature.todo

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.notaskflow.core.common.formatDateTimeText
import com.notaskflow.domain.model.Todo
import com.notaskflow.feature.common.DateTimePickerField
import com.notaskflow.feature.common.NotaskFilledTextField
import com.notaskflow.feature.common.SwipeDeleteContainer

@Composable
fun TodoListRoute(
    modifier: Modifier = Modifier,
    spaceId: Long? = null,
    onCreateTodo: () -> Unit = {},
    viewModel: TodoListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingTodo by remember { mutableStateOf<Todo?>(null) }
    var pendingDeleteTodo by remember { mutableStateOf<Todo?>(null) }
    var newTodoTitle by remember { mutableStateOf("") }
    var todoDeadline by remember { mutableStateOf("") }
    LaunchedEffect(spaceId) {
        if (spaceId != null) {
            viewModel.load(spaceId)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            item {
                Text(
                    text = "待办",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "整理今天需要推进的小事",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(20.dp))
            }

            item {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::updateSearchQuery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    placeholder = { Text("搜索待办") },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                )
                Spacer(Modifier.height(14.dp))
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TodoFilter.entries.forEach { filter ->
                        FilterPill(
                            text = filter.label,
                            selected = uiState.selectedFilter == filter,
                            onClick = { viewModel.selectFilter(filter) }
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
            }

            if (spaceId == null) {
                item { StateText("请先选择空间") }
            } else if (uiState.isLoading && uiState.todos.isEmpty()) {
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
            } else if (uiState.errorMessage != null && uiState.todos.isEmpty()) {
                item { StateText(uiState.errorMessage ?: "加载失败") }
            } else if (uiState.todos.isEmpty()) {
                item { StateText("暂无待办") }
            } else {
                items(uiState.todos, key = { todo -> "todo-${todo.id}" }) { todo ->
                    SwipeDeleteContainer(
                        onDeleteRequest = { pendingDeleteTodo = todo },
                        cornerRadius = 20.dp
                    ) {
                        TodoCard(
                            todo = todo,
                            onToggleComplete = { viewModel.toggleComplete(todo) },
                            onEdit = {
                                editingTodo = todo
                                newTodoTitle = todo.title
                                todoDeadline = todo.deadline.orEmpty()
                            }
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }

        FloatingActionButton(
            onClick = {
                if (spaceId == null) {
                    onCreateTodo()
                } else {
                    showCreateDialog = true
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Filled.Add, "新建待办", tint = MaterialTheme.colorScheme.onPrimary)
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("新建待办") },
            text = {
                TodoEditFields(
                    title = newTodoTitle,
                    deadline = todoDeadline,
                    onTitleChange = { newTodoTitle = it },
                    onDeadlineChange = { todoDeadline = it }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.create(newTodoTitle, todoDeadline)
                        newTodoTitle = ""
                        todoDeadline = ""
                        showCreateDialog = false
                    }
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    editingTodo?.let { todo ->
        AlertDialog(
            onDismissRequest = { editingTodo = null },
            title = { Text("编辑待办") },
            text = {
                TodoEditFields(
                    title = newTodoTitle,
                    deadline = todoDeadline,
                    onTitleChange = { newTodoTitle = it },
                    onDeadlineChange = { todoDeadline = it }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.update(todo, newTodoTitle, todoDeadline)
                        newTodoTitle = ""
                        todoDeadline = ""
                        editingTodo = null
                    }
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingTodo = null }) {
                    Text("取消")
                }
            }
        )
    }

    pendingDeleteTodo?.let { todo ->
        AlertDialog(
            onDismissRequest = { pendingDeleteTodo = null },
            title = { Text("删除待办") },
            text = { Text("确定删除“${todo.title}”吗？") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.delete(todo)
                        pendingDeleteTodo = null
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteTodo = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun FilterPill(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceContainerHigh
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 9.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TodoCard(
    todo: Todo,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        if (todo.isCompleted) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    )
                    .clickable(onClick = onToggleComplete),
                contentAlignment = Alignment.Center
            ) {
                if (todo.isCompleted) {
                    Icon(
                        Icons.Filled.Done,
                        contentDescription = "标记未完成",
                        modifier = Modifier.size(17.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = "标记完成",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = todo.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (todo.isCompleted) FontWeight.Normal else FontWeight.Medium,
                    textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (todo.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                todo.deadline?.let { deadline ->
                    Spacer(Modifier.height(7.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = formatDateTimeText(deadline),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            TextButton(onClick = onEdit) {
                Text("编辑")
            }
        }
    }
}

@Composable
private fun TodoEditFields(
    title: String,
    deadline: String,
    onTitleChange: (String) -> Unit,
    onDeadlineChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        NotaskFilledTextField(
            value = title,
            onValueChange = onTitleChange,
            label = "标题",
            placeholder = "写下待办事项",
            singleLine = true
        )
        DateTimePickerField(
            value = deadline,
            onValueChange = onDeadlineChange,
            label = "截止时间",
            modifier = Modifier.fillMaxWidth(),
            placeholder = "可选"
        )
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
