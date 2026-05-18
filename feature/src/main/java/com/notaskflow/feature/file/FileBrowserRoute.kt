package com.notaskflow.feature.file
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.notaskflow.domain.model.FileFolder
import com.notaskflow.domain.model.ManagedFile
import com.notaskflow.feature.common.NotaskFilledTextField
import com.notaskflow.feature.common.SwipeDeleteContainer
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileBrowserRoute(
    spaceId: Long,
    modifier: Modifier = Modifier,
    onFileClick: (Long) -> Unit = {},
    viewModel: FileBrowserViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var folderNameInput by remember { mutableStateOf("") }
    var renamingFolder by remember { mutableStateOf<FileFolder?>(null) }
    var pendingDeleteFolder by remember { mutableStateOf<FileFolder?>(null) }
    var pendingDeleteFile by remember { mutableStateOf<Pair<ManagedFile, Int>?>(null) }
    var movingFolder by remember { mutableStateOf<FileFolder?>(null) }
    var renamingFile by remember { mutableStateOf<ManagedFile?>(null) }
    var pendingRestoreFile by remember { mutableStateOf<ManagedFile?>(null) }
    var pendingPhysicalDeleteFile by remember { mutableStateOf<ManagedFile?>(null) }
    var fileNameInput by remember { mutableStateOf("") }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri -> uri?.let(viewModel::upload) }
    )

    LaunchedEffect(spaceId) {
        viewModel.load(spaceId)
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is FileBrowserEffect.ConfirmDelete -> {
                    pendingDeleteFile = effect.file to effect.referenceCount
                }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerShape = RectangleShape) {
                val content = uiState as? FileBrowserUiState.Content
                FileTreeDrawerContent(
                    state = content,
                    onAllClick = {
                        viewModel.selectFolder(null)
                        scope.launch { drawerState.close() }
                    },
                    onFolderClick = { folder ->
                        viewModel.selectFolder(folder)
                        scope.launch { drawerState.close() }
                    },
                    onFolderMoveToParent = { folder, parentId ->
                        viewModel.moveFolder(folder, parentId)
                    },
                    onTrashClick = {
                        viewModel.selectTrash()
                        scope.launch { drawerState.close() }
                    },
                    onFolderDropToTrash = { folder -> pendingDeleteFolder = folder },
                    onCreateFolder = {
                        folderNameInput = ""
                        showCreateFolderDialog = true
                    }
                )
            }
        }
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            when (val state = uiState) {
                FileBrowserUiState.Loading -> FileLoading()
                is FileBrowserUiState.Error -> FileError(message = state.message, onRetry = { viewModel.load(spaceId) })
                is FileBrowserUiState.Content -> FileBrowserContent(
                    state = state,
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onFileClick = onFileClick,
                    onDeleteFile = viewModel::prepareDelete,
                    onRestoreFile = { pendingRestoreFile = it },
                    onPhysicalDeleteFile = { pendingPhysicalDeleteFile = it },
                    onCreateFolder = {
                        folderNameInput = ""
                        showCreateFolderDialog = true
                    },
                    onRenameFolder = { folder ->
                        folderNameInput = folder.name
                        renamingFolder = folder
                    },
                    onDeleteFolder = { pendingDeleteFolder = it },
                    onMoveFolder = { movingFolder = it },
                    onRenameFile = { file ->
                        fileNameInput = file.displayName
                        renamingFile = file
                    },
                    onRetry = viewModel::refresh
                )
            }

            FloatingActionButton(
                onClick = { filePicker.launch(arrayOf(ALL_FILE_TYPES)) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(16.dp)
            ) {
                if ((uiState as? FileBrowserUiState.Content)?.isUploading == true) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Filled.Add, "上传文件", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }

    pendingRestoreFile?.let { file ->
        AlertDialog(
            onDismissRequest = { pendingRestoreFile = null },
            title = { Text("恢复文件") },
            text = { Text("确定恢复“${file.displayName}”吗？") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restore(file)
                        pendingRestoreFile = null
                    }
                ) {
                    Text("恢复")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreFile = null }) {
                    Text("取消")
                }
            }
        )
    }

    pendingPhysicalDeleteFile?.let { file ->
        AlertDialog(
            onDismissRequest = { pendingPhysicalDeleteFile = null },
            title = { Text("彻底删除文件") },
            text = { Text("彻底删除“${file.displayName}”后无法恢复，确定继续吗？") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.physicalDelete(file)
                        pendingPhysicalDeleteFile = null
                    }
                ) {
                    Text("彻底删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingPhysicalDeleteFile = null }) {
                    Text("取消")
                }
            }
        )
    }

    pendingDeleteFile?.let { pending ->
        val file = pending.first
        val referenceCount = pending.second
        AlertDialog(
            onDismissRequest = { pendingDeleteFile = null },
            title = { Text("删除文件") },
            text = {
                Text(
                    if (referenceCount > 0) {
                        "“${file.displayName}”当前被 $referenceCount 处内容引用。删除后引用可能无法打开，确定继续移入回收站吗？"
                    } else {
                        "确定将“${file.displayName}”移入回收站吗？"
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.delete(file)
                        pendingDeleteFile = null
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteFile = null }) {
                    Text("取消")
                }
            }
        )
    }

    if (showCreateFolderDialog) {
        FileTextInputDialog(
            title = "新建文件夹",
            label = "文件夹名称",
            value = folderNameInput,
            onValueChange = { folderNameInput = it },
            onDismiss = { showCreateFolderDialog = false },
            onConfirm = {
                viewModel.createFolder(folderNameInput)
                showCreateFolderDialog = false
            }
        )
    }

    renamingFolder?.let { folder ->
        FileTextInputDialog(
            title = "重命名文件夹",
            label = "文件夹名称",
            value = folderNameInput,
            onValueChange = { folderNameInput = it },
            onDismiss = { renamingFolder = null },
            onConfirm = {
                viewModel.renameFolder(folder, folderNameInput)
                renamingFolder = null
            }
        )
    }

    pendingDeleteFolder?.let { folder ->
        AlertDialog(
            onDismissRequest = { pendingDeleteFolder = null },
            title = { Text("删除文件夹") },
            text = { Text("确定删除“${folder.name}”吗？文件夹内仍有内容时，后端可能会拒绝删除。") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteFolder(folder)
                        pendingDeleteFolder = null
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteFolder = null }) {
                    Text("取消")
                }
            }
        )
    }

    renamingFile?.let { file ->
        FileTextInputDialog(
            title = "重命名文件",
            label = "文件名",
            value = fileNameInput,
            onValueChange = { fileNameInput = it },
            onDismiss = { renamingFile = null },
            onConfirm = {
                viewModel.renameFile(file, fileNameInput)
                renamingFile = null
            }
        )
    }

    movingFolder?.let { folder ->
        FolderMoveDialog(
            folder = folder,
            folders = (uiState as? FileBrowserUiState.Content)?.folders.orEmpty(),
            onMove = { parentId ->
                viewModel.moveFolder(folder, parentId)
                movingFolder = null
            },
            onDismiss = { movingFolder = null }
        )
    }
}

@Composable
private fun FileTreeDrawerContent(
    state: FileBrowserUiState.Content?,
    onAllClick: () -> Unit,
    onFolderClick: (FileFolder) -> Unit,
    onFolderMoveToParent: (FileFolder, Long?) -> Unit,
    onTrashClick: () -> Unit,
    onFolderDropToTrash: (FileFolder) -> Unit,
    onCreateFolder: () -> Unit
) {
    var trashBounds by remember { mutableStateOf<Rect?>(null) }
    var rootBounds by remember { mutableStateOf<Rect?>(null) }
    var draggingFolderId by remember { mutableStateOf<Long?>(null) }
    val folderBounds = remember { mutableStateMapOf<Long, Rect>() }
    LaunchedEffect(state?.folders) {
        folderBounds.clear()
    }
    val isTrashMode = state?.isTrashMode == true
    val selectedFolderId = state?.selectedFolderId
    val rootFolderCount = state?.folders?.size ?: 0
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "文件夹",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))
        FileTreeRow(
            name = "全部文件",
            selected = !isTrashMode && selectedFolderId == null,
            icon = Icons.Filled.FolderOpen,
            subtitle = if (rootFolderCount == 0) "暂无子文件夹" else "$rootFolderCount 个子文件夹",
            onClick = onAllClick,
            onPositioned = { rootBounds = it }
        )
        state?.folders.orEmpty().forEach { folder ->
            FileTreeItem(
                folder = folder,
                selectedFolderId = selectedFolderId,
                isTrashMode = isTrashMode,
                trashBounds = trashBounds,
                rootBounds = rootBounds,
                folderBounds = folderBounds,
                draggingFolderId = draggingFolderId,
                onDragStateChange = { id -> draggingFolderId = id },
                onFolderClick = onFolderClick,
                onFolderDropToTrash = onFolderDropToTrash,
                onFolderMoveToParent = onFolderMoveToParent
            )
        }
        Spacer(Modifier.weight(1f))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))
        FileTrashDropRow(
            selected = isTrashMode,
            active = draggingFolderId != null,
            onClick = onTrashClick,
            onPositioned = { trashBounds = it }
        )
        Spacer(Modifier.height(6.dp))
        TextButton(onClick = onCreateFolder, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("新建文件夹")
        }
    }
}

@Composable
private fun FileTreeItem(
    folder: FileFolder,
    selectedFolderId: Long?,
    isTrashMode: Boolean,
    trashBounds: Rect?,
    rootBounds: Rect?,
    folderBounds: MutableMap<Long, Rect>,
    draggingFolderId: Long?,
    onDragStateChange: (Long?) -> Unit,
    onFolderClick: (FileFolder) -> Unit,
    onFolderDropToTrash: (FileFolder) -> Unit,
    onFolderMoveToParent: (FileFolder, Long?) -> Unit,
    depth: Int = 0
) {
    DraggableFileFolderRow(
        folder = folder,
        name = folder.name,
        selected = !isTrashMode && selectedFolderId == folder.id,
        depth = depth,
        trashBounds = trashBounds,
        rootBounds = rootBounds,
        dropTargets = folderBounds,
        isDragging = draggingFolderId == folder.id,
        onDragStateChange = onDragStateChange,
        onPositioned = { rect -> folderBounds[folder.id] = rect },
        onClick = { onFolderClick(folder) },
        onDropToTrash = { onFolderDropToTrash(folder) },
        onDropToParent = { parentId -> onFolderMoveToParent(folder, parentId) }
    )
    folder.children.forEach { child ->
        FileTreeItem(
            folder = child,
            selectedFolderId = selectedFolderId,
            isTrashMode = isTrashMode,
            trashBounds = trashBounds,
            rootBounds = rootBounds,
            folderBounds = folderBounds,
            draggingFolderId = draggingFolderId,
            onDragStateChange = onDragStateChange,
            onFolderClick = onFolderClick,
            onFolderDropToTrash = onFolderDropToTrash,
            onFolderMoveToParent = onFolderMoveToParent,
            depth = depth + 1
        )
    }
}

@Composable
private fun FileTrashDropRow(
    selected: Boolean,
    active: Boolean,
    onClick: () -> Unit,
    onPositioned: (Rect) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates -> onPositioned(coordinates.boundsInWindow()) }
            .clip(RoundedCornerShape(14.dp))
            .background(
                when {
                    active -> MaterialTheme.colorScheme.errorContainer
                    selected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
                    else -> MaterialTheme.colorScheme.surfaceContainerLow
                }
            )
            .clickable(onClick = onClick)
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
            text = if (active) "松手删除文件夹" else "回收站",
            style = MaterialTheme.typography.bodyMedium,
            color = if (active) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun DraggableFileFolderRow(
    folder: FileFolder,
    name: String,
    selected: Boolean,
    depth: Int,
    trashBounds: Rect?,
    rootBounds: Rect?,
    dropTargets: Map<Long, Rect>,
    isDragging: Boolean,
    onDragStateChange: (Long?) -> Unit,
    onPositioned: (Rect) -> Unit,
    onClick: () -> Unit,
    onDropToTrash: () -> Unit,
    onDropToParent: (Long?) -> Unit
) {
    var rowCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var dragOrigin by remember { mutableStateOf(Offset.Zero) }
    var dragDelta by remember { mutableStateOf(Offset.Zero) }
    FileTreeRow(
        name = name,
        subtitle = if (folder.children.isNotEmpty()) "${folder.children.size} 个子文件夹" else "无子文件夹",
        selected = selected,
        icon = Icons.Filled.Folder,
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
            .pointerInput(folder.id, trashBounds) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        dragOrigin = offset
                        dragDelta = Offset.Zero
                        onDragStateChange(folder.id)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragDelta += dragAmount
                    },
                    onDragEnd = {
                        val dropPoint = rowCoordinates?.localToWindow(dragOrigin + dragDelta)
                        if (dropPoint != null && trashBounds?.contains(dropPoint) == true) {
                            onDropToTrash()
                        } else if (dropPoint != null && rootBounds?.contains(dropPoint) == true) {
                            onDropToParent(null)
                        } else if (dropPoint != null) {
                            val targetId = dropTargets.entries.firstOrNull { entry ->
                                entry.key != folder.id &&
                                    !folder.children.containsFolder(entry.key) &&
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

@Composable
private fun FileTreeRow(
    name: String,
    subtitle: String?,
    selected: Boolean,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    depth: Int = 0,
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
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.34f)
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
                    icon,
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

@Composable
private fun FileBrowserContent(
    state: FileBrowserUiState.Content,
    onOpenDrawer: () -> Unit,
    onFileClick: (Long) -> Unit,
    onDeleteFile: (ManagedFile) -> Unit,
    onRestoreFile: (ManagedFile) -> Unit,
    onPhysicalDeleteFile: (ManagedFile) -> Unit,
    onCreateFolder: () -> Unit,
    onRenameFolder: (FileFolder) -> Unit,
    onDeleteFolder: (FileFolder) -> Unit,
    onMoveFolder: (FileFolder) -> Unit,
    onRenameFile: (ManagedFile) -> Unit,
    onRetry: () -> Unit
) {
    val flattenedFolders = state.folders.flattenFolders()
    val selectedFolder = flattenedFolders.firstOrNull { folder -> folder.id == state.selectedFolderId }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onOpenDrawer) {
                    Icon(Icons.Filled.Menu, contentDescription = "打开文件夹")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.selectedFolderName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (state.isTrashMode) {
                            "${state.files.size} 个回收站文件"
                        } else {
                            "${state.files.size} 个文件"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (state.isRefreshing) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Text(
                        text = "刷新",
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onRetry)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            if (!state.isTrashMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "新建文件夹",
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onCreateFolder)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge
                    )
                    selectedFolder?.let { folder ->
                        Text(
                            text = "移动文件夹",
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onMoveFolder(folder) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = "重命名",
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onRenameFolder(folder) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = "删除文件夹",
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onDeleteFolder(folder) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
            state.errorMessage?.let { message ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(Modifier.height(12.dp))
        }
        item {
            Text(
                text = if (state.isTrashMode) "回收站文件" else "最近文件",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(10.dp))
        }
        if (state.files.isNotEmpty()) {
            if (!state.isTrashMode) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        state.files.take(RECENT_FILE_LIMIT).forEach { file ->
                            FileCard(file = file, onClick = { onFileClick(file.id) })
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                }
            }
            item {
                Text(
                    text = if (state.isTrashMode) "可恢复文件" else "全部文件",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
            }
            items(state.files, key = { file -> "file-${file.id}" }) { file ->
                if (state.isTrashMode) {
                    FileListItem(
                        file = file,
                        onClick = { onFileClick(file.id) },
                        onRename = { },
                        onRestore = { onRestoreFile(file) },
                        onPhysicalDelete = { onPhysicalDeleteFile(file) },
                        trashMode = true
                    )
                } else {
                    SwipeDeleteContainer(
                        onDeleteRequest = { onDeleteFile(file) },
                        cornerRadius = 14.dp,
                        deleteFromStartToEnd = false
                    )
                    {
                        FileListItem(
                            file = file,
                            onClick = { onFileClick(file.id) },
                            onRename = { onRenameFile(file) }
                        )
                    }
                }
            }
        } else {
            item {
                EmptyFiles()
            }
        }
        item { Spacer(Modifier.height(88.dp)) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderPill(
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    depth: Int = 0
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                }
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(start = (12 + depth * 10).dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            text = name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FolderMoveDialog(
    folder: FileFolder,
    folders: List<FileFolder>,
    onMove: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("移动“${folder.name}”") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "长按文件夹可调整父子结构。选择目标父级后，当前文件夹会移动到对应位置。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = { onMove(null) }, modifier = Modifier.fillMaxWidth()) {
                    Text("移动到根目录")
                }
                folders.flattenFolders()
                    .filterNot { candidate -> candidate.id == folder.id || folder.children.containsFolder(candidate.id) }
                    .forEach { candidate ->
                        TextButton(onClick = { onMove(candidate.id) }, modifier = Modifier.fillMaxWidth()) {
                            Text("移动到 ${candidate.name}")
                        }
                    }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun FileMoveDialog(
    file: ManagedFile,
    folders: List<FileFolder>,
    onMove: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("移动“${file.displayName}”") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "选择文件要移动到的目标文件夹。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = { onMove(null) }, modifier = Modifier.fillMaxWidth()) {
                    Text("移动到全部文件")
                }
                folders.flattenFolders().forEach { folder ->
                    TextButton(onClick = { onMove(folder.id) }, modifier = Modifier.fillMaxWidth()) {
                        Text("移动到 ${folder.name}")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun FileTextInputDialog(
    title: String,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            NotaskFilledTextField(
                value = value,
                onValueChange = onValueChange,
                label = label,
                placeholder = label,
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("确认")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun FileLoading() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun FileError(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = message, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(12.dp))
            Text(
                text = "重试",
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onRetry)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun EmptyFiles() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
        contentAlignment = Alignment.Center
    ) {
        Text("暂无文件", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FileCard(file: ManagedFile, onClick: () -> Unit) {
    val accentColor = fileTypeColor(file)
    Card(
        modifier = Modifier
            .width(220.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = fileTypeIcon(file),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = accentColor
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = file.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = displayFileType(file),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = formatSize(file.fileSize),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                file.gmtCreate?.let { createdAt ->
                    Text(
                        formatDateTimeText(createdAt),
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

@Composable
private fun FileListItem(
    file: ManagedFile,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onRestore: () -> Unit = {},
    onPhysicalDelete: () -> Unit = {},
    trashMode: Boolean = false
) {
    val accentColor = fileTypeColor(file)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = if (trashMode) 1.dp else 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(fileTypeIcon(file), null, Modifier.size(24.dp), tint = accentColor)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = file.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = displayFileType(file),
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(formatSize(file.fileSize), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    file.gmtCreate?.let { createdAt ->
                        Text(
                            formatDateTimeText(createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (trashMode) {
                TextButton(onClick = onRestore) {
                    Text("恢复")
                }
                TextButton(onClick = onPhysicalDelete) {
                    Text("彻底删除", color = MaterialTheme.colorScheme.error)
                }
            } else {
                IconButton(onClick = onRename) {
                    Icon(Icons.Filled.Edit, contentDescription = "重命名文件", Modifier.size(18.dp))
                }
                Icon(Icons.Filled.ChevronRight, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun fileTypeIcon(file: ManagedFile): ImageVector = when {
    file.mimeType?.startsWith("image/") == true -> Icons.Filled.Image
    file.mimeType?.startsWith("audio/") == true -> Icons.Filled.MusicNote
    file.mimeType == PDF_MIME_TYPE || file.fileExtension() == "pdf" -> Icons.Filled.PictureAsPdf
    file.mimeType?.startsWith("text/") == true || file.fileExtension() in DOCUMENT_EXTENSIONS -> Icons.Filled.Description
    else -> Icons.AutoMirrored.Filled.InsertDriveFile
}

@Composable
private fun fileTypeColor(file: ManagedFile): Color = when {
    file.mimeType?.startsWith("image/") == true -> MaterialTheme.colorScheme.tertiary
    file.mimeType?.startsWith("audio/") == true -> MaterialTheme.colorScheme.secondary
    file.mimeType == PDF_MIME_TYPE || file.fileExtension() == "pdf" -> MaterialTheme.colorScheme.error
    file.mimeType?.startsWith("text/") == true -> MaterialTheme.colorScheme.secondary
    file.fileExtension() in DOCUMENT_EXTENSIONS -> MaterialTheme.colorScheme.primary
    else -> MaterialTheme.colorScheme.outline
}

private fun displayFileType(file: ManagedFile): String {
    return when (file.fileExtension()) {
        "doc", "docx" -> "Word 文档"
        "xls", "xlsx" -> "Excel 表格"
        "ppt", "pptx" -> "演示文稿"
        "pdf" -> "PDF 文档"
        "txt", "md" -> "文本文件"
        "json", "xml", "yaml", "yml" -> "结构化文本"
        else -> when {
            file.mimeType?.startsWith("image/") == true -> "图片文件"
            file.mimeType?.startsWith("audio/") == true -> "音频文件"
            file.mimeType?.startsWith("video/") == true -> "视频文件"
            file.mimeType?.isNotBlank() == true -> "文件"
            else -> "未知类型"
        }
    }
}

private fun ManagedFile.fileExtension(): String {
    return displayName.substringAfterLast('.', fileName.substringAfterLast('.', "")).lowercase()
}

private fun formatSize(bytes: Long): String {
    if (bytes < 1024L) {
        return "$bytes B"
    }
    val kilobytes = bytes / 1024.0
    if (kilobytes < 1024.0) {
        return String.format("%.1f KB", kilobytes)
    }
    val megabytes = kilobytes / 1024.0
    if (megabytes < 1024.0) {
        return String.format("%.1f MB", megabytes)
    }
    return String.format("%.1f GB", megabytes / 1024.0)
}

private fun List<FileFolder>.flattenFolders(): List<FileFolder> {
    return flatMap { folder -> listOf(folder) + folder.children.flattenFolders() }
}

private fun List<FileFolder>.containsFolder(folderId: Long?): Boolean {
    if (folderId == null) {
        return false
    }
    return any { folder -> folder.id == folderId || folder.children.containsFolder(folderId) }
}

private fun FileFolder.depthIn(folders: List<FileFolder>): Int {
    return folders.depthOf(id)
}

private fun List<FileFolder>.depthOf(folderId: Long, depth: Int = 0): Int {
    forEach { folder ->
        if (folder.id == folderId) {
            return depth
        }
        val childDepth = folder.children.depthOf(folderId, depth + 1)
        if (childDepth >= 0) {
            return childDepth
        }
    }
    return -1
}

private const val ALL_FILE_TYPES = "*/*"
private const val PDF_MIME_TYPE = "application/pdf"
private const val RECENT_FILE_LIMIT = 6
private val DOCUMENT_EXTENSIONS = setOf("doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md")
