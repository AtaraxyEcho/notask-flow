package com.notaskflow.feature.note

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.notaskflow.core.common.formatDateTimeText
import com.notaskflow.domain.model.Note
import com.notaskflow.domain.model.Notebook
import com.notaskflow.feature.common.NotaskFilledTextField
import com.notaskflow.feature.common.SwipeDeleteContainer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NoteListRoute(
    modifier: Modifier = Modifier,
    spaceId: Long? = null,
    title: String = "笔记",
    subtitle: String = "最近更新的内容",
    onNoteClick: (Long) -> Unit = {},
    onCreateNote: () -> Unit = {},
    viewModel: NoteListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showCreateNotebookDialog by remember { mutableStateOf(false) }
    var pendingDeleteNote by remember { mutableStateOf<Note?>(null) }
    var pendingDeleteNotebook by remember { mutableStateOf<Notebook?>(null) }
    var notebookName by remember { mutableStateOf("") }

    LaunchedEffect(spaceId) {
        if (spaceId != null) {
            viewModel.load(spaceId)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerShape = RectangleShape) {
                NotebookDrawerContent(
                    notebooks = uiState.notebooks,
                    selectedNotebookId = uiState.selectedNotebookId,
                    onNotebookClick = { notebookId ->
                        viewModel.selectNotebook(notebookId)
                        scope.launch { drawerState.close() }
                    },
                    onNotebookMoveToParent = { notebook, parentId ->
                        viewModel.moveNotebook(notebook, parentId)
                    },
                    onNotebookDropToTrash = { notebook -> pendingDeleteNotebook = notebook },
                    onCreateNotebook = {
                        showCreateNotebookDialog = true
                    }
                )
            }
        }
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "打开笔记本")
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                }

                if (spaceId == null) {
                    item { StateText("请先选择空间") }
                } else if (uiState.isLoading && uiState.notes.isEmpty()) {
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
                } else if (uiState.errorMessage != null && uiState.notes.isEmpty()) {
                    item { StateText(uiState.errorMessage ?: "加载失败") }
                } else if (uiState.notes.isEmpty()) {
                    item { StateText("暂无笔记") }
                } else {
                    items(uiState.notes, key = { note -> "note-${note.id}" }) { note ->
                        SwipeDeleteContainer(
                            onDeleteRequest = { pendingDeleteNote = note }
                        ) {
                            NoteCard(note = note, onClick = { onNoteClick(note.id) })
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }

                item { Spacer(Modifier.height(80.dp)) }
            }

            FloatingActionButton(
                onClick = onCreateNote,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.Add, "新建笔记", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }

    if (showCreateNotebookDialog) {
        AlertDialog(
            onDismissRequest = { showCreateNotebookDialog = false },
            title = { Text("新建笔记本") },
            text = {
                NotaskFilledTextField(
                    value = notebookName,
                    onValueChange = { notebookName = it },
                    label = "名称",
                    placeholder = "输入笔记本名称",
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createNotebook(notebookName)
                        notebookName = ""
                        showCreateNotebookDialog = false
                    }
                ) {
                    Text("创建")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateNotebookDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    pendingDeleteNote?.let { note ->
        AlertDialog(
            onDismissRequest = { pendingDeleteNote = null },
            title = { Text("删除笔记") },
            text = { Text("确定删除“${note.title}”吗？删除后将无法在当前列表中继续访问。") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.delete(note)
                        pendingDeleteNote = null
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteNote = null }) {
                    Text("取消")
                }
            }
        )
    }

    pendingDeleteNotebook?.let { notebook ->
        AlertDialog(
            onDismissRequest = { pendingDeleteNotebook = null },
            title = { Text("删除笔记本") },
            text = { Text("确定删除“${notebook.name}”吗？如果笔记本内仍有内容，后端可能会拒绝删除。") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteNotebook(notebook)
                        pendingDeleteNotebook = null
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteNotebook = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun NoteCard(note: Note, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.24f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Description,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatDateTimeText(note.gmtModified).ifBlank { "未记录更新时间" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            note.content?.takeIf { it.isNotBlank() }?.let { content ->
                Spacer(Modifier.height(10.dp))
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun NotebookDrawerContent(
    notebooks: List<Notebook>,
    selectedNotebookId: Long?,
    onNotebookClick: (Long) -> Unit,
    onNotebookMoveToParent: (Notebook, Long?) -> Unit,
    onNotebookDropToTrash: (Notebook) -> Unit,
    onCreateNotebook: () -> Unit
) {
    var trashBounds by remember { mutableStateOf<Rect?>(null) }
    var draggingNotebookId by remember { mutableStateOf<Long?>(null) }
    val notebookBounds = remember { mutableStateMapOf<Long, Rect>() }
    LaunchedEffect(notebooks) {
        notebookBounds.clear()
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "笔记本",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))
        notebooks.forEach { notebook ->
            NotebookTreeItem(
                node = notebook,
                selectedNotebookId = selectedNotebookId,
                onClick = onNotebookClick,
                onDropToTrash = onNotebookDropToTrash,
                onMoveToParent = onNotebookMoveToParent,
                trashBounds = trashBounds,
                notebookBounds = notebookBounds,
                draggingNotebookId = draggingNotebookId,
                onDragStateChange = { id -> draggingNotebookId = id }
            )
        }
        Spacer(Modifier.weight(1f))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))
        TrashDropRow(
            active = draggingNotebookId != null,
            onPositioned = { trashBounds = it }
        )
        Spacer(Modifier.height(6.dp))
        TextButton(onClick = onCreateNotebook, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("新建笔记本")
        }
    }
}

@Composable
private fun NotebookTreeItem(
    node: Notebook,
    selectedNotebookId: Long?,
    onClick: (Long) -> Unit,
    onDropToTrash: (Notebook) -> Unit,
    onMoveToParent: (Notebook, Long?) -> Unit,
    trashBounds: Rect?,
    notebookBounds: MutableMap<Long, Rect>,
    draggingNotebookId: Long?,
    onDragStateChange: (Long?) -> Unit,
    depth: Int = 0
) {
    DraggableNotebookRow(
        notebook = node,
        name = node.name,
        selected = selectedNotebookId == node.id,
        depth = depth,
        onClick = { onClick(node.id) },
        trashBounds = trashBounds,
        dropTargets = notebookBounds,
        isDragging = draggingNotebookId == node.id,
        onDragStateChange = onDragStateChange,
        onPositioned = { rect -> notebookBounds[node.id] = rect },
        onDropToTrash = { onDropToTrash(node) },
        onDropToParent = { parentId -> onMoveToParent(node, parentId) }
    )
    node.children.forEach { child ->
        NotebookTreeItem(
            node = child,
            selectedNotebookId = selectedNotebookId,
            onClick = onClick,
            onDropToTrash = onDropToTrash,
            onMoveToParent = onMoveToParent,
            trashBounds = trashBounds,
            notebookBounds = notebookBounds,
            draggingNotebookId = draggingNotebookId,
            onDragStateChange = onDragStateChange,
            depth = depth + 1
        )
    }
}

@Composable
private fun TrashDropRow(active: Boolean, onPositioned: (Rect) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates -> onPositioned(coordinates.boundsInWindow()) }
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (active) MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.surfaceContainerLow
            )
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.Delete,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = if (active) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (active) "松手移入回收流程" else "拖拽笔记本到这里删除",
            style = MaterialTheme.typography.bodyMedium,
            color = if (active) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DraggableNotebookRow(
    notebook: Notebook,
    name: String,
    selected: Boolean,
    depth: Int,
    onClick: () -> Unit,
    trashBounds: Rect?,
    dropTargets: Map<Long, Rect>,
    isDragging: Boolean,
    onDragStateChange: (Long?) -> Unit,
    onPositioned: (Rect) -> Unit,
    onDropToTrash: () -> Unit,
    onDropToParent: (Long?) -> Unit
) {
    var rowCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var dragOrigin by remember { mutableStateOf(Offset.Zero) }
    var dragDelta by remember { mutableStateOf(Offset.Zero) }
    NotebookRow(
        name = name,
        subtitle = if (notebook.children.isNotEmpty()) "${notebook.children.size} 个子文件夹" else "无子文件夹",
        selected = selected,
        depth = depth,
        onClick = onClick,
        modifier = Modifier
            .zIndex(if (isDragging) 1f else 0f)
            .onGloballyPositioned { coordinates -> rowCoordinates = coordinates }
            .graphicsLayer {
                translationX = if (isDragging) dragDelta.x else 0f
                translationY = if (isDragging) dragDelta.y else 0f
                alpha = if (isDragging) 0.82f else 1f
            }
            .pointerInput(notebook.id, trashBounds) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        dragOrigin = offset
                        dragDelta = Offset.Zero
                        onDragStateChange(notebook.id)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragDelta += dragAmount
                    },
                    onDragEnd = {
                        val dropPoint = rowCoordinates?.localToWindow(dragOrigin + dragDelta)
                        if (dropPoint != null && trashBounds?.contains(dropPoint) == true) {
                            onDropToTrash()
                        } else if (dropPoint != null) {
                            val targetId = dropTargets.entries.firstOrNull { entry ->
                                entry.key != notebook.id &&
                                    !notebook.children.containsNotebook(entry.key) &&
                                    entry.value.contains(dropPoint)
                            }?.key
                            if (targetId != null) {
                                onDropToParent(targetId)
                            }
                        }
                        dragDelta = Offset.Zero
                        onDragStateChange(null)
                    },
                    onDragCancel = {
                        dragDelta = Offset.Zero
                        onDragStateChange(null)
                    }
                )
            }
            .onGloballyPositioned { coordinates -> onPositioned(coordinates.boundsInWindow()) }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NotebookRow(
    name: String,
    subtitle: String?,
    selected: Boolean,
    depth: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onPositioned: ((Rect) -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = (depth * 10).dp, top = 4.dp, bottom = 4.dp)
            .onGloballyPositioned { coordinates -> onPositioned?.invoke(coordinates.boundsInWindow()) },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.30f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        ),
        border = if (selected) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.30f))
        } else {
            null
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun List<Notebook>.containsNotebook(notebookId: Long?): Boolean {
    if (notebookId == null) {
        return false
    }
    return any { notebook -> notebook.id == notebookId || notebook.children.containsNotebook(notebookId) }
}

@Composable
private fun NotebookNavigatorPanel(
    notebooks: List<Notebook>,
    selectedNotebookId: Long?,
    onRootClick: () -> Unit,
    onNotebookClick: (Long) -> Unit
) {
    val selectedNotebook = notebooks.findNotebook(selectedNotebookId)
    val path = selectedNotebookId?.let { notebooks.findNotebookPath(it) }.orEmpty()
    val children = selectedNotebook?.children ?: notebooks
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "目录",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "${children.size} 个子文件夹",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NotebookScopeChip(
                text = "根目录",
                selected = selectedNotebookId == null,
                onClick = onRootClick
            )
            path.forEachIndexed { index, notebook ->
                if (index > 0) {
                    Text(
                        text = "›",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                NotebookScopeChip(
                    text = notebook.name,
                    selected = selectedNotebookId == notebook.id,
                    onClick = { onNotebookClick(notebook.id) }
                )
            }
        }
        if (children.isEmpty()) {
            Text(
                text = "当前目录没有子文件夹",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                children.forEach { child ->
                    NotebookScopeChip(
                        text = child.name,
                        selected = selectedNotebookId == child.id,
                        onClick = { onNotebookClick(child.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NotebookScopeChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surface
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun List<Notebook>.findNotebook(notebookId: Long?): Notebook? {
    if (notebookId == null) {
        return null
    }
    forEach { notebook ->
        if (notebook.id == notebookId) {
            return notebook
        }
        val child = notebook.children.findNotebook(notebookId)
        if (child != null) {
            return child
        }
    }
    return null
}

private fun List<Notebook>.findNotebookPath(notebookId: Long): List<Notebook> {
    forEach { notebook ->
        if (notebook.id == notebookId) {
            return listOf(notebook)
        }
        val childPath = notebook.children.findNotebookPath(notebookId)
        if (childPath.isNotEmpty()) {
            return listOf(notebook) + childPath
        }
    }
    return emptyList()
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
