package com.notaskflow.feature.note

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.automirrored.filled.FormatIndentIncrease
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.notaskflow.core.common.parseLocalDateTime
import com.notaskflow.data.BuildConfig
import com.notaskflow.domain.model.ManagedFile
import com.notaskflow.domain.model.NoteAttachment
import com.notaskflow.domain.model.NoteExportFile
import com.notaskflow.domain.model.NoteExportFormat
import com.notaskflow.domain.model.NoteHistory
import com.notaskflow.domain.model.Notebook
import com.notaskflow.domain.model.UserProfile
import com.notaskflow.feature.common.DateTimePickerField
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditRoute(
    spaceId: Long? = null,
    noteId: Long? = null,
    currentUser: UserProfile? = null,
    onBack: () -> Unit,
    viewModel: NoteEditViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    var notebookMenuExpanded by remember { mutableStateOf(false) }
    var moreMenuExpanded by remember { mutableStateOf(false) }
    var shareLink by remember { mutableStateOf<String?>(null) }
    var showShareDialog by remember { mutableStateOf(false) }
    var shareExpireAt by remember { mutableStateOf("") }
    var showHistorySheet by remember { mutableStateOf(false) }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var showExitConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var exportedNoteFile by remember { mutableStateOf<NoteExportFile?>(null) }
    var showPaperSheet by remember { mutableStateOf(false) }
    var showReferenceSheet by remember { mutableStateOf(false) }
    var pendingExternalFile by remember { mutableStateOf<EditorExternalFile?>(null) }
    var paperTone by remember { mutableStateOf(PaperTone.DEFAULT) }
    var editorInteractionSignal by remember { mutableStateOf(0) }
    var activeAudioPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var activeAudioKey by remember { mutableStateOf<String?>(null) }
    var collabStatusMessage by remember { mutableStateOf("正在准备协作") }
    var collabPeers by remember { mutableStateOf<List<CollabPeer>>(emptyList()) }
    val latestAudioPlayer by rememberUpdatedState(activeAudioPlayer)
    val editorController = remember { RichTextEditorController() }
    LaunchedEffect(uiState.collabEnabled, noteId, spaceId) {
        if (uiState.collabEnabled) {
            collabStatusMessage = if (noteId == null || spaceId == null) {
                "协作文档参数不完整"
            } else {
                "正在加载协作编辑器"
            }
        }
    }
    val attachmentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri -> uri?.let(viewModel::uploadAttachment) }
    )
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri -> uri?.let(viewModel::uploadInlineImage) }
    )
    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri -> uri?.let { viewModel.uploadInlineFile(it, InlineFileKind.AUDIO) } }
    )
    val insertFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri -> uri?.let { viewModel.uploadInlineFile(it, InlineFileKind.ATTACHMENT) } }
    )
    val exportSaveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            val targetUri = result.data?.data
            val file = exportedNoteFile
            if (result.resultCode == Activity.RESULT_OK && targetUri != null && file != null) {
                writeExportedNoteFile(
                    context = context,
                    file = file,
                    targetUri = targetUri
                )
            }
        }
    )

    LaunchedEffect(spaceId, noteId) {
        if (spaceId != null) {
            viewModel.load(spaceId, noteId)
        }
    }

    BackHandler {
        if (uiState.hasUnsavedChanges) {
            showExitConfirmDialog = true
        } else {
            onBack()
        }
    }

    LaunchedEffect(uiState.savedMessage) {
        if (uiState.savedMessage == "已保存") {
            Toast.makeText(context, "已保存", Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            latestAudioPlayer?.releaseSafely()
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                NoteEditEffect.Saved -> onBack()
                NoteEditEffect.Deleted -> onBack()
                is NoteEditEffect.ShareReady -> {
                    val publicLink = buildPublicNoteUrl(effect.shareCode)
                    copyTextToClipboard(context, "Notask Flow 分享链接", publicLink)
                    Toast.makeText(context, "分享链接已复制", Toast.LENGTH_SHORT).show()
                    shareLink = publicLink
                }
                is NoteEditEffect.ExportReady -> {
                    exportedNoteFile = effect.file
                    Toast.makeText(context, "导出完成，可保存或分享", Toast.LENGTH_SHORT).show()
                }
                is NoteEditEffect.ImageReady -> {
                    editorController.execute(
                        RichTextCommand.InsertImage(
                            alt = effect.alt,
                            url = effect.url,
                            managedFileId = effect.managedFileId,
                            attachmentId = effect.attachmentId
                        )
                    )
                }
                is NoteEditEffect.FileReady -> {
                    when (effect.kind) {
                        InlineFileKind.AUDIO -> editorController.execute(
                            RichTextCommand.InsertAudio(
                                name = effect.name,
                                url = effect.url,
                                managedFileId = effect.managedFileId,
                                attachmentId = effect.attachmentId
                            )
                        )
                        InlineFileKind.ATTACHMENT,
                        InlineFileKind.REFERENCE -> editorController.execute(
                            RichTextCommand.InsertFile(
                                name = effect.name,
                                url = effect.url,
                                mimeType = effect.mimeType,
                                fileSize = effect.fileSize,
                                managedFileId = effect.managedFileId,
                                attachmentId = effect.attachmentId
                            )
                        )
                    }
                    showReferenceSheet = false
                }
                is NoteEditEffect.EditorContentReady -> {
                    editorController.execute(
                        RichTextCommand.SetContent(
                            effect.contentHtml ?: effect.content.toPlainDocumentHtml()
                        )
                    )
                }
                is NoteEditEffect.CollabTicketReady -> {
                    editorController.execute(
                        RichTextCommand.ReceiveCollabTicket(
                            requestId = effect.requestId,
                            ticket = effect.ticket,
                            errorMessage = null
                        )
                    )
                }
                is NoteEditEffect.CollabTicketFailed -> {
                    editorController.execute(
                        RichTextCommand.ReceiveCollabTicket(
                            requestId = effect.requestId,
                            ticket = "",
                            errorMessage = effect.message
                        )
                    )
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.title.ifBlank { "新建笔记" },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (uiState.hasUnsavedChanges) {
                                showExitConfirmDialog = true
                            } else {
                                onBack()
                            }
                        }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { editorController.execute(RichTextCommand.Undo) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "撤销",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { editorController.execute(RichTextCommand.Redo) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "重做",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = {
                            editorController.snapshot { snapshot ->
                                if (snapshot == null) {
                                    viewModel.save()
                                } else {
                                    viewModel.saveSnapshot(snapshot.text, snapshot.html)
                                }
                            }
                        },
                        enabled = !uiState.isSaving && spaceId != null
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.Check, contentDescription = "保存")
                        }
                    }
                    Box {
                        IconButton(onClick = { moreMenuExpanded = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "更多", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        DropdownMenu(
                            expanded = moreMenuExpanded,
                            onDismissRequest = { moreMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("分享") },
                                leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                                onClick = {
                                    moreMenuExpanded = false
                                    viewModel.share()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("版本历史") },
                                leadingIcon = { Icon(Icons.Filled.History, contentDescription = null) },
                                onClick = {
                                    moreMenuExpanded = false
                                    showHistorySheet = true
                                    viewModel.loadHistories()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("附件管理") },
                                leadingIcon = { Icon(Icons.Filled.AttachFile, contentDescription = null) },
                                onClick = {
                                    moreMenuExpanded = false
                                    showAttachmentSheet = true
                                    viewModel.loadAttachments()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("设置纸张底纹") },
                                leadingIcon = { Icon(Icons.Filled.Palette, contentDescription = null) },
                                onClick = {
                                    moreMenuExpanded = false
                                    showPaperSheet = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("导出") },
                                leadingIcon = { Icon(Icons.Filled.Download, contentDescription = null) },
                                enabled = !uiState.isExporting,
                                onClick = {
                                    moreMenuExpanded = false
                                    showExportDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    moreMenuExpanded = false
                                    showDeleteConfirmDialog = true
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = paperTone.color.copy(alpha = 0.96f)
                )
            )
        },
        containerColor = paperTone.color,
        bottomBar = {
            RichTextBottomControls(
                onCommand = editorController::execute,
                isImageUploading = uiState.isInlineImageUploading,
                collapseSignal = editorInteractionSignal,
                onRequestHideKeyboard = {
                    focusManager.clearFocus(force = true)
                    editorController.execute(RichTextCommand.Blur)
                    hideSoftwareKeyboard(context)
                },
                onRequestShowKeyboard = {
                    editorController.requestInput()
                },
                onPickImage = { imagePicker.launch(arrayOf(IMAGE_FILE_TYPES)) },
                onPickAudio = { audioPicker.launch(arrayOf(AUDIO_FILE_TYPES)) },
                onInsertTable = { rows, columns ->
                    editorController.execute(RichTextCommand.InsertTable(rows, columns))
                    editorController.requestInput()
                },
                onPickFile = { insertFilePicker.launch(arrayOf(ALL_FILE_TYPES)) },
                onReferenceFile = {
                    showReferenceSheet = true
                    viewModel.loadReferenceFiles()
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(paperTone.color)
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                NoteTitleField(
                    value = uiState.title,
                    onValueChange = viewModel::updateTitle
                )

                NoteMetadataRow(
                    documentDate = uiState.documentDate,
                    characterCount = uiState.content.trim().count { !it.isWhitespace() },
                    notebookName = uiState.notebooks
                        .flattenNotebookTree()
                        .firstOrNull { it.id == uiState.selectedNotebookId }
                        ?.name ?: "默认笔记本",
                    onNotebookClick = { notebookMenuExpanded = true }
                )
                DropdownMenu(
                    expanded = notebookMenuExpanded,
                    onDismissRequest = { notebookMenuExpanded = false }
                ) {
                    uiState.notebooks.flattenNotebookTree().forEach { notebook ->
                        DropdownMenuItem(
                            text = { Text(notebook.name) },
                            onClick = {
                                viewModel.selectNotebook(notebook.id)
                                notebookMenuExpanded = false
                            }
                        )
                    }
                }

                uiState.errorMessage?.let { message ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (uiState.collabEnabled) {
                    Spacer(Modifier.height(10.dp))
                    CollabPresenceBar(
                        statusMessage = collabStatusMessage,
                        currentUser = currentUser,
                        peers = collabPeers
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                RichDocumentEditor(
                    spaceId = spaceId,
                    noteId = noteId,
                    value = uiState.content,
                    contentHtml = uiState.contentHtml,
                    collaborationEnabled = uiState.collabEnabled,
                    authToken = uiState.authToken,
                    currentUser = currentUser,
                    controller = editorController,
                    onDocumentChange = { text, html ->
                        if (uiState.collabEnabled) {
                            viewModel.updateCollabContent(text, html)
                        } else {
                            viewModel.updateContent(text, html)
                        }
                    },
                    onEditorInteraction = { editorInteractionSignal += 1 },
                    onRequestCollabTicket = viewModel::requestCollabTicket,
                    onCollabStatus = { status, message ->
                        Log.d(COLLAB_LOG_TAG, "status=$status message=$message")
                        collabStatusMessage = message
                    },
                    onAwarenessChanged = { usersJson ->
                        Log.d(COLLAB_LOG_TAG, "awareness=$usersJson")
                        collabPeers = parseCollabPeers(usersJson)
                    },
                    onOpenExternalFile = { name, url, mimeType, managedFileId ->
                        pendingExternalFile = EditorExternalFile(name, url, mimeType, managedFileId)
                    },
                    onPlayAudio = { name, url, managedFileId ->
                        val audioSource = resolveEditorAudioUrl(
                            spaceId = spaceId,
                            managedFileId = managedFileId,
                            url = url
                        )
                        if (audioSource.isBlank()) {
                            Toast.makeText(context, "音频链接无效", Toast.LENGTH_SHORT).show()
                        } else if (activeAudioKey == audioSource) {
                            activeAudioPlayer?.releaseSafely()
                            activeAudioPlayer = null
                            activeAudioKey = null
                            editorController.execute(RichTextCommand.ResetAudioPlayback)
                            Toast.makeText(context, "已停止播放", Toast.LENGTH_SHORT).show()
                        } else {
                            activeAudioPlayer?.releaseSafely()
                            val player = MediaPlayer()
                            activeAudioPlayer = player
                            activeAudioKey = audioSource
                            playEditorAudio(
                                context = context,
                                player = player,
                                name = name,
                                source = audioSource,
                                authToken = uiState.authToken,
                                requiresAuth = managedFileId != null,
                                onStop = {
                                    activeAudioPlayer = null
                                    activeAudioKey = null
                                    editorController.execute(RichTextCommand.ResetAudioPlayback)
                                }
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                )
            }
        }
    }

    if (showShareDialog) {
        AlertDialog(
            onDismissRequest = { showShareDialog = false },
            title = { Text("生成分享链接") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "可选择分享有效期；留空则长期有效。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    DateTimePickerField(
                        value = shareExpireAt,
                        onValueChange = { shareExpireAt = it },
                        label = "有效期至",
                        placeholder = "长期有效",
                        supportingText = null
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.share(shareExpireAt.takeIf { it.isNotBlank() })
                        showShareDialog = false
                    }
                ) {
                    Text("生成链接")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        shareExpireAt = ""
                        viewModel.share()
                        showShareDialog = false
                    }
                ) {
                    Text("长期有效")
                }
            }
        )
    }

    shareLink?.let { link ->
        AlertDialog(
            onDismissRequest = { shareLink = null },
            title = { Text("分享链接已生成") },
            text = {
                Text(
                    text = link,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { shareLink = null }) {
                    Text("知道了")
                }
            }
        )
    }

    if (showExitConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showExitConfirmDialog = false },
            title = { Text("退出编辑？") },
            text = { Text("当前笔记还有未保存内容，退出后可能丢失最新修改。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitConfirmDialog = false
                        if (uiState.collabEnabled) {
                            viewModel.checkpointCollabContent(closeAfterSave = true)
                        } else {
                            onBack()
                        }
                    }
                ) {
                    Text("退出")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmDialog = false }) {
                    Text("继续编辑")
                }
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("删除笔记") },
            text = { Text("确定删除当前笔记吗？删除后可在列表重新加载确认状态。") },
            confirmButton = {
                Button(
                    enabled = !uiState.isSaving,
                    onClick = {
                        viewModel.deleteCurrentNote()
                        showDeleteConfirmDialog = false
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showExportDialog) {
        ExportTypeDialog(
            onDismiss = { showExportDialog = false },
            isExporting = uiState.isExporting,
            onExportPdf = {
                showExportDialog = false
                exportCurrentEditorSnapshot(editorController, viewModel, NoteExportFormat.Pdf)
            },
            onExportWord = {
                showExportDialog = false
                exportCurrentEditorSnapshot(editorController, viewModel, NoteExportFormat.Word)
            },
            onExportImage = {
                showExportDialog = false
                exportCurrentEditorSnapshot(editorController, viewModel, NoteExportFormat.Image)
            }
        )
    }

    exportedNoteFile?.let { file ->
        ExportResultDialog(
            file = file,
            onDismiss = { exportedNoteFile = null },
            onSave = {
                exportSaveLauncher.launch(createExportDocumentIntent(file))
            },
            onOpen = {
                openExportedNoteFile(context, file)
            },
            onShare = {
                shareExportedNoteFile(context, file)
            }
        )
    }

    pendingExternalFile?.let { file ->
        AlertDialog(
            onDismissRequest = { pendingExternalFile = null },
            title = { Text("打开附件") },
            text = { Text("是否打开“${file.name}”？") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            openEditorFile(
                                context = context,
                                file = file,
                                spaceId = spaceId,
                                authToken = uiState.authToken
                            )
                        }
                        pendingExternalFile = null
                    }
                ) {
                    Text("打开")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingExternalFile = null }) {
                    Text("取消")
                }
            }
        )
    }

    if (showPaperSheet) {
        PaperToneSheet(
            selectedTone = paperTone,
            onDismiss = { showPaperSheet = false },
            onSelect = {
                paperTone = it
                showPaperSheet = false
            }
        )
    }

    if (showReferenceSheet) {
        FileReferenceSheet(
            files = uiState.referenceFiles,
            isLoading = uiState.isReferenceLoading,
            onDismiss = { showReferenceSheet = false },
            onSelect = viewModel::insertReferenceFile
        )
    }

    if (showHistorySheet) {
        NoteHistorySheet(
            histories = uiState.histories,
            previewHistory = uiState.previewHistory,
            isLoading = uiState.isMetadataLoading,
            onDismiss = { showHistorySheet = false },
            onPreview = viewModel::loadHistory,
            onRestore = { version ->
                viewModel.restoreHistory(version)
                showHistorySheet = false
            }
        )
    }

    if (showAttachmentSheet) {
        NoteAttachmentSheet(
            attachments = uiState.attachments,
            isLoading = uiState.isMetadataLoading,
            isUploading = uiState.isAttachmentUploading,
            activeAttachmentId = uiState.activeAttachmentId,
            onUpload = { attachmentPicker.launch(arrayOf(ALL_FILE_TYPES)) },
            onRemove = viewModel::unbindAttachment,
            onDismiss = { showAttachmentSheet = false }
        )
    }
}

@Composable
private fun NoteTitleField(
    value: String,
    onValueChange: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        if (value.isBlank()) {
            Text(
                text = "请输入标题",
                style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.52f)
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            ),
            singleLine = true,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
private fun NoteMetadataRow(
    documentDate: String?,
    characterCount: Int,
    notebookName: String,
    onNotebookClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${documentDate.toEditorDateText()} | $characterCount 字 | ",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f)
        )
        Text(
            text = notebookName,
            modifier = Modifier.clickable(onClick = onNotebookClick),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            fontWeight = FontWeight.SemiBold
        )
    }
}

private data class CollabPeer(
    val clientId: Long,
    val userId: Long,
    val name: String,
    val color: String,
    val colorLight: String
)

@Composable
private fun CollabPresenceBar(
    statusMessage: String,
    currentUser: UserProfile?,
    peers: List<CollabPeer>
) {
    val localPeer = remember(currentUser) { currentUser.toLocalCollabPeer() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.82f))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primary)
        )
        Text(
            text = statusMessage,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        CollabPeerChip(
            peer = localPeer,
            self = true
        )
        peers.take(MAX_VISIBLE_COLLAB_PEERS).forEach { peer ->
            CollabPeerChip(peer = peer)
        }
        if (peers.size > MAX_VISIBLE_COLLAB_PEERS) {
            Text(
                text = "+${peers.size - MAX_VISIBLE_COLLAB_PEERS}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun CollabPeerChip(peer: CollabPeer, self: Boolean = false) {
    val foreground = runCatching { Color(AndroidColor.parseColor(peer.color)) }
        .getOrDefault(MaterialTheme.colorScheme.primary)
    val background = runCatching { Color(AndroidColor.parseColor(peer.colorLight)) }
        .getOrDefault(MaterialTheme.colorScheme.primaryContainer)
    val label = if (self) {
        peer.name.takeIf { it.isNotBlank() && it != DEFAULT_LOCAL_COLLAB_NAME }
            ?.let { name -> "$name (me)" }
            ?: "me"
    } else {
        peer.name
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(RoundedCornerShape(50))
                .background(foreground)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = foreground,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun UserProfile?.toLocalCollabPeer(): CollabPeer {
    val colors = presenceColors()
    return CollabPeer(
        clientId = 0L,
        userId = this?.id ?: 0L,
        name = this?.displayCollabName() ?: DEFAULT_LOCAL_COLLAB_NAME,
        color = colors.first,
        colorLight = colors.second
    )
}

private class RichTextEditorController {
    private var scriptHandler: ((String, ((String?) -> Unit)?) -> Unit)? = null
    private var inputView: WebView? = null

    fun bind(handler: (String, ((String?) -> Unit)?) -> Unit) {
        scriptHandler = handler
    }

    fun bindInputView(webView: WebView) {
        inputView = webView
    }

    fun execute(command: RichTextCommand) {
        evaluate(command.toJavascript())
    }

    fun snapshot(onResult: (EditorSnapshot?) -> Unit) {
        evaluate(EDITOR_SNAPSHOT_SCRIPT) { rawResult ->
            onResult(parseEditorSnapshot(rawResult))
        }
    }

    fun requestInput() {
        execute(RichTextCommand.Focus)
        inputView?.let { webView ->
            webView.requestFocus()
            val inputMethodManager = webView.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            inputMethodManager?.showSoftInput(webView, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun evaluate(script: String, callback: ((String?) -> Unit)? = null) {
        val handler = scriptHandler
        if (handler == null) {
            callback?.invoke(null)
            return
        }
        handler(script, callback)
    }
}

private sealed interface RichTextCommand {
    data object Undo : RichTextCommand
    data object Redo : RichTextCommand
    data object Focus : RichTextCommand
    data object Blur : RichTextCommand
    data object Paragraph : RichTextCommand
    data object HeadingOne : RichTextCommand
    data object HeadingTwo : RichTextCommand
    data object HeadingThree : RichTextCommand
    data object HeadingFour : RichTextCommand
    data object Bold : RichTextCommand
    data object Italic : RichTextCommand
    data object Underline : RichTextCommand
    data object BulletList : RichTextCommand
    data object NumberList : RichTextCommand
    data object AlphaList : RichTextCommand
    data object TodoList : RichTextCommand
    data object Quote : RichTextCommand
    data object Divider : RichTextCommand
    data object QuickLink : RichTextCommand
    data object RemoveLink : RichTextCommand
    data object FirstLineIndent : RichTextCommand
    data object ResetAudioPlayback : RichTextCommand
    data class FontSize(val sizePx: Int) : RichTextCommand
    data class FontColor(val colorHex: String) : RichTextCommand
    data class FontFamily(val family: String) : RichTextCommand
    data class LineHeight(val lineHeight: Float) : RichTextCommand
    data class Align(val alignment: TextAlignment) : RichTextCommand
    data class InsertLink(val text: String, val url: String) : RichTextCommand
    data class InsertImage(
        val alt: String,
        val url: String,
        val managedFileId: Long?,
        val attachmentId: Long?
    ) : RichTextCommand

    data class SetContent(val html: String) : RichTextCommand
    data class InsertTable(val rows: Int, val columns: Int) : RichTextCommand
    data class InsertAudio(
        val name: String,
        val url: String,
        val managedFileId: Long?,
        val attachmentId: Long?
    ) : RichTextCommand

    data class InsertFile(
        val name: String,
        val url: String,
        val mimeType: String?,
        val fileSize: Long,
        val managedFileId: Long?,
        val attachmentId: Long?
    ) : RichTextCommand

    data class ReceiveCollabTicket(
        val requestId: String,
        val ticket: String,
        val errorMessage: String?
    ) : RichTextCommand
}

private enum class TextAlignment {
    LEFT,
    CENTER,
    RIGHT
}

private enum class PaperTone(val label: String, val color: Color) {
    DEFAULT("默认浅色", Color(0xFFFFFBFF)),
    SOFT_GRAY("柔和灰", Color(0xFFF7F7F7)),
    WARM("暖白", Color(0xFFFFFBF4)),
    GREEN("护眼绿", Color(0xFFF2F8F1))
}

private data class EditorExternalFile(
    val name: String,
    val url: String,
    val mimeType: String?,
    val managedFileId: Long?
)

private data class EditorSnapshot(
    val text: String,
    val html: String?
)

private data class FontFamilyChoice(
    val label: String,
    val cssValue: String
)

private enum class EditorToolTab(val label: String) {
    FONT("Aa"),
    TODO("待办"),
    PARAGRAPH("段落"),
    QUOTE("引号"),
    DIVIDER("分割线"),
    LINK("链接"),
    INSERT("+"),
    TABLE_PICKER("表格")
}

private fun EditorToolTab.adaptivePanelHeight(keyboardHeight: Dp): Dp {
    val keyboardMatchedHeight = maxOf(keyboardHeight, DEFAULT_TOOLBAR_DRAWER_HEIGHT.dp) + TOOL_PANEL_KEYBOARD_BUFFER.dp
    val minimumHeight = when (this) {
        EditorToolTab.INSERT -> MIN_INSERT_TOOL_PANEL_HEIGHT.dp
        EditorToolTab.TABLE_PICKER -> TABLE_PICKER_PANEL_HEIGHT.dp
        else -> MIN_TOOLBAR_DRAWER_HEIGHT.dp
    }
    return maxOf(minimumHeight, keyboardMatchedHeight)
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun RichDocumentEditor(
    spaceId: Long?,
    noteId: Long?,
    value: String,
    contentHtml: String?,
    collaborationEnabled: Boolean,
    authToken: String?,
    currentUser: UserProfile?,
    controller: RichTextEditorController,
    onDocumentChange: (String, String?) -> Unit,
    onEditorInteraction: () -> Unit,
    onRequestCollabTicket: (String) -> Unit,
    onCollabStatus: (String, String) -> Unit,
    onAwarenessChanged: (String) -> Unit,
    onOpenExternalFile: (String, String, String?, Long?) -> Unit,
    onPlayAudio: (String, String, Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    val latestOnDocumentChange by rememberUpdatedState(onDocumentChange)
    val latestOnEditorInteraction by rememberUpdatedState(onEditorInteraction)
    val latestOnRequestCollabTicket by rememberUpdatedState(onRequestCollabTicket)
    val latestOnCollabStatus by rememberUpdatedState(onCollabStatus)
    val latestOnAwarenessChanged by rememberUpdatedState(onAwarenessChanged)
    val latestOnOpenExternalFile by rememberUpdatedState(onOpenExternalFile)
    val latestOnPlayAudio by rememberUpdatedState(onPlayAudio)
    val initialHtml = remember(contentHtml, value) {
        contentHtml?.takeIf { it.isNotBlank() } ?: value.toPlainDocumentHtml()
    }
    val collabSpaceId = spaceId.takeIf { collaborationEnabled && noteId != null }
    val collabNoteId = noteId.takeIf { collaborationEnabled && spaceId != null }
    val collabMode = collabSpaceId != null && collabNoteId != null
    val editorLoadKey = remember(collabMode, collabSpaceId, collabNoteId, spaceId) {
        if (collabMode) {
            "collab:$collabSpaceId:$collabNoteId"
        } else {
            "plain:${spaceId ?: 0L}"
        }
    }
    val collabConfigureScript = remember(collabMode, initialHtml, collabNoteId, collabSpaceId, currentUser) {
        if (collabSpaceId != null && collabNoteId != null) {
            collabConfigureScript(
                spaceId = collabSpaceId,
                noteId = collabNoteId,
                initialHtml = initialHtml,
                currentUser = currentUser
            )
        } else {
            null
        }
    }

    AndroidView(
        modifier = modifier.background(Color.Transparent),
        factory = { context ->
            WebView(context).apply {
                configureDocumentWebView(authToken)
                webViewClient = EditorAssetWebViewClient(
                    authToken = authToken,
                    onStatus = { status, message -> latestOnCollabStatus(status, message) },
                    onPageFinished = { webView ->
                        latestOnCollabStatus("page", "协作页面已加载，正在初始化脚本")
                        collabConfigureScript?.let { script ->
                            webView.evaluateJavascript(script, null)
                        }
                    }
                )
                val bridge = RichDocumentBridge(
                    documentChangedHandler = { text, html -> latestOnDocumentChange(text, html) },
                    editorInteractionHandler = { latestOnEditorInteraction() },
                    requestCollabTicketHandler = { requestId -> latestOnRequestCollabTicket(requestId) },
                    collabStatusHandler = { status, message -> latestOnCollabStatus(status, message) },
                    awarenessChangedHandler = { usersJson -> latestOnAwarenessChanged(usersJson) },
                    openExternalFileHandler = { name, url, mimeType, managedFileId ->
                        latestOnOpenExternalFile(name, url, mimeType, managedFileId)
                    },
                    playAudioHandler = { name, url, managedFileId -> latestOnPlayAudio(name, url, managedFileId) }
                )
                addJavascriptInterface(
                    bridge,
                    EDITOR_BRIDGE_NAME
                )
                addJavascriptInterface(bridge, COLLAB_BRIDGE_NAME)
                controller.bindInputView(this)
                loadEditorDocument(
                    collabMode = collabMode,
                    initialHtml = initialHtml,
                    spaceId = spaceId,
                    loadKey = editorLoadKey
                )
            }
        },
        update = { webView ->
            webView.webViewClient = EditorAssetWebViewClient(
                authToken = authToken,
                onStatus = { status, message -> latestOnCollabStatus(status, message) },
                onPageFinished = { page ->
                    latestOnCollabStatus("page", "协作页面已加载，正在初始化脚本")
                    collabConfigureScript?.let { script ->
                        page.evaluateJavascript(script, null)
                    }
                }
            )
            controller.bind { script, callback ->
                webView.evaluateJavascript(script, callback)
            }
            if (webView.tag != editorLoadKey) {
                Log.d(COLLAB_LOG_TAG, "reload editor document loadKey=$editorLoadKey")
                latestOnCollabStatus("page", "正在切换协作编辑器")
                webView.loadEditorDocument(
                    collabMode = collabMode,
                    initialHtml = initialHtml,
                    spaceId = spaceId,
                    loadKey = editorLoadKey
                )
            }
        }
    )
}

private fun WebView.loadEditorDocument(
    collabMode: Boolean,
    initialHtml: String,
    spaceId: Long?,
    loadKey: String
) {
    tag = loadKey
    if (collabMode) {
        loadDataWithBaseURL(
            collabEditorBaseUrl(),
            collabDocumentHtml(),
            HTML_MIME_TYPE,
            UTF8_ENCODING,
            null
        )
    } else {
        loadDataWithBaseURL(
            editorBaseUrl(),
            richDocumentHtml(initialHtml, spaceId),
            HTML_MIME_TYPE,
            UTF8_ENCODING,
            null
        )
    }
}

private class RichDocumentBridge(
    private val documentChangedHandler: (String, String) -> Unit,
    private val editorInteractionHandler: () -> Unit,
    private val requestCollabTicketHandler: (String) -> Unit,
    private val collabStatusHandler: (String, String) -> Unit,
    private val awarenessChangedHandler: (String) -> Unit,
    private val openExternalFileHandler: (String, String, String?, Long?) -> Unit,
    private val playAudioHandler: (String, String, Long?) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onDocumentChanged(text: String?, html: String?) {
        dispatchDocumentChanged(text, html)
    }

    @JavascriptInterface
    fun onContentChanged(text: String?, html: String?) {
        dispatchDocumentChanged(text, html)
    }

    @JavascriptInterface
    fun onEditorInteraction() {
        postToMain { editorInteractionHandler() }
    }

    @JavascriptInterface
    fun requestTicket(requestId: String?) {
        val safeRequestId = requestId?.takeIf { it.isNotBlank() } ?: return
        postToMain { requestCollabTicketHandler(safeRequestId) }
    }

    @JavascriptInterface
    fun onCollabStatus(status: String?, message: String?) {
        postToMain { collabStatusHandler(status.orEmpty(), message.orEmpty()) }
    }

    @JavascriptInterface
    fun onAwarenessChanged(usersJson: String?) {
        postToMain { awarenessChangedHandler(usersJson.orEmpty()) }
    }

    @JavascriptInterface
    fun onReady() {
        postToMain { collabStatusHandler("ready", "协作编辑器已加载") }
    }

    @JavascriptInterface
    fun onOpenExternalFile(name: String?, url: String?, mimeType: String?, managedFileId: String?) {
        postToMain {
            openExternalFileHandler(
                name?.takeIf { it.isNotBlank() } ?: "附件",
                url.orEmpty(),
                mimeType?.takeIf { it.isNotBlank() },
                managedFileId?.toLongOrNull()
            )
        }
    }

    @JavascriptInterface
    fun onPlayAudio(name: String?, url: String?, managedFileId: String?) {
        postToMain {
            playAudioHandler(
                name?.takeIf { it.isNotBlank() } ?: "音频文件",
                url.orEmpty(),
                managedFileId?.toLongOrNull()
            )
        }
    }

    private fun dispatchDocumentChanged(text: String?, html: String?) {
        val safeText = text.orEmpty()
        val safeHtml = html.orEmpty()
        postToMain {
            Log.d(
                COLLAB_LOG_TAG,
                "bridge document changed textLength=${safeText.length} htmlLength=${safeHtml.length}"
            )
            documentChangedHandler(safeText, safeHtml)
        }
    }

    private fun postToMain(action: () -> Unit) {
        mainHandler.post(action)
    }
}

@Suppress("DEPRECATION")
private fun WebView.configureDocumentWebView(authToken: String?) {
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    settings.allowFileAccess = false
    settings.allowContentAccess = false
    settings.allowFileAccessFromFileURLs = false
    settings.allowUniversalAccessFromFileURLs = false
    settings.cacheMode = WebSettings.LOAD_NO_CACHE
    settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
    settings.loadsImagesAutomatically = true
    settings.blockNetworkImage = false
    settings.mediaPlaybackRequiresUserGesture = false
    webViewClient = EditorAssetWebViewClient(authToken)
    webChromeClient = EditorWebChromeClient()
    setBackgroundColor(AndroidColor.TRANSPARENT)
    isVerticalScrollBarEnabled = false
    clearCache(false)
}

private class EditorAssetWebViewClient(
    private val authToken: String?,
    private val onStatus: ((String, String) -> Unit)? = null,
    private val onPageFinished: ((WebView) -> Unit)? = null
) : WebViewClient() {
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onPageFinished(view: WebView?, url: String?) {
        view?.let { webView ->
            postToMain { onPageFinished?.invoke(webView) }
        }
    }

    override fun onPageCommitVisible(view: WebView?, url: String?) {
        postToMain { onStatus?.invoke("page_visible", "协作页面正在渲染") }
    }

    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
        val uri = request?.url ?: return null
        if (!isEditorInterceptRequest(uri)) {
            return null
        }
        val segments = uri.pathSegments
        if (segments.size == COLLAB_KERNEL_SEGMENT_COUNT &&
            segments[0] == COLLAB_KERNEL_SEGMENT &&
            segments[1] == COLLAB_KERNEL_SCRIPT_NAME
        ) {
            return runCatching {
                val inputStream = view?.context?.assets?.open(COLLAB_KERNEL_ASSET_PATH)
                    ?: return@runCatching null
                WebResourceResponse(
                    JAVASCRIPT_MIME_TYPE,
                    UTF8_ENCODING,
                    inputStream
                )
            }.getOrNull()
        }
        if (segments.size < EDITOR_ASSET_SEGMENT_COUNT ||
            segments[0] != EDITOR_ASSET_MARKER ||
            segments[1] != EDITOR_ASSET_SPACES_SEGMENT ||
            segments[3] != EDITOR_ASSET_FILES_SEGMENT ||
            segments[5] != EDITOR_ASSET_PREVIEW_SEGMENT
        ) {
            return null
        }
        return runCatching {
            val previewUrl = "${BuildConfig.BASE_URL.trimEnd('/')}/api/v1/spaces/${segments[2]}/files/${segments[4]}/preview"
            val connection = URL(previewUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = EDITOR_ASSET_CONNECT_TIMEOUT_MS
            connection.readTimeout = EDITOR_ASSET_READ_TIMEOUT_MS
            request.requestHeaders[RANGE_HEADER]?.takeIf { it.isNotBlank() }?.let { range ->
                connection.setRequestProperty(RANGE_HEADER, range)
            }
            authToken?.takeIf { it.isNotBlank() }?.let { token ->
                connection.setRequestProperty(AUTHORIZATION_HEADER, "Bearer $token")
            }
            val responseCode = connection.responseCode
            if (responseCode !in EDITOR_ASSET_SUCCESS_CODES) {
                connection.disconnect()
                return@runCatching null
            }
            WebResourceResponse(
                connection.contentType?.substringBefore(";") ?: BINARY_MIME_TYPE,
                null,
                responseCode,
                connection.responseMessage?.takeIf { it.isNotBlank() } ?: OK_RESPONSE_REASON,
                connection.headerFields
                    .filterKeys { key -> key != null }
                    .mapKeys { entry -> entry.key.orEmpty() }
                    .mapValues { entry -> entry.value.joinToString(",") },
                connection.inputStream
            )
        }.getOrNull()
    }

    private fun postToMain(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            mainHandler.post(action)
        }
    }
}

private class EditorWebChromeClient : WebChromeClient() {
    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        val message = consoleMessage ?: return super.onConsoleMessage(consoleMessage)
        Log.d(
            COLLAB_LOG_TAG,
            "console ${message.messageLevel()} ${message.sourceId()}:${message.lineNumber()} ${message.message()}"
        )
        return true
    }
}

@Composable
private fun RichTextBottomControls(
    onCommand: (RichTextCommand) -> Unit,
    isImageUploading: Boolean,
    collapseSignal: Int,
    onRequestHideKeyboard: () -> Unit,
    onRequestShowKeyboard: () -> Unit,
    onPickImage: () -> Unit,
    onPickAudio: () -> Unit,
    onInsertTable: (Int, Int) -> Unit,
    onPickFile: () -> Unit,
    onReferenceFile: () -> Unit
) {
    var activeTool by remember { mutableStateOf<EditorToolTab?>(null) }
    var selectedFontSize by remember { mutableStateOf(DEFAULT_FONT_SIZE) }
    var selectedLineHeight by remember { mutableStateOf(DEFAULT_LINE_HEIGHT) }
    var selectedFontFamily by remember { mutableStateOf(FONT_FAMILY_OPTIONS.first()) }
    var selectedParagraph by remember { mutableStateOf("正文") }
    var selectedAlignment by remember { mutableStateOf(TextAlignment.LEFT) }
    var selectedList by remember { mutableStateOf<String?>(null) }
    var selectedColor by remember { mutableStateOf(DEFAULT_TEXT_COLOR) }
    var boldEnabled by remember { mutableStateOf(false) }
    var italicEnabled by remember { mutableStateOf(false) }
    var underlineEnabled by remember { mutableStateOf(false) }
    var todoEnabled by remember { mutableStateOf(false) }
    var quoteEnabled by remember { mutableStateOf(false) }
    var keyboardRestorePanelHeight by remember { mutableStateOf<Dp?>(null) }
    val density = LocalDensity.current
    val currentKeyboardHeight = with(density) { WindowInsets.ime.getBottom(this).toDp() }
    val isKeyboardVisible = currentKeyboardHeight > MIN_REMEMBERED_KEYBOARD_HEIGHT.dp
    var rememberedKeyboardHeight by remember { mutableStateOf(DEFAULT_TOOLBAR_DRAWER_HEIGHT.dp) }
    LaunchedEffect(currentKeyboardHeight) {
        if (currentKeyboardHeight > MIN_REMEMBERED_KEYBOARD_HEIGHT.dp) {
            rememberedKeyboardHeight = currentKeyboardHeight
        }
    }
    LaunchedEffect(isKeyboardVisible) {
        if (isKeyboardVisible) {
            delay(KEYBOARD_RESTORE_HOLD_MS)
            keyboardRestorePanelHeight = null
        }
    }
    LaunchedEffect(collapseSignal) {
        if (collapseSignal > 0) {
            activeTool = null
            keyboardRestorePanelHeight = null
        }
    }
    fun runPanelCommand(command: RichTextCommand) {
        onCommand(command)
    }
    fun runInputCommand(command: RichTextCommand) {
        activeTool = null
        keyboardRestorePanelHeight = null
        onCommand(command)
        onRequestShowKeyboard()
    }
    fun selectPanelTool(tool: EditorToolTab) {
        if (activeTool == tool) {
            keyboardRestorePanelHeight = tool.adaptivePanelHeight(rememberedKeyboardHeight)
            activeTool = null
            onRequestShowKeyboard()
        } else {
            keyboardRestorePanelHeight = null
            activeTool = tool
            onRequestHideKeyboard()
        }
    }
    fun openTool(tool: EditorToolTab) {
        when (tool) {
            EditorToolTab.INSERT -> selectPanelTool(tool)
            EditorToolTab.TODO -> {
                todoEnabled = !todoEnabled
                runInputCommand(RichTextCommand.TodoList)
            }
            EditorToolTab.QUOTE -> {
                quoteEnabled = !quoteEnabled
                runInputCommand(RichTextCommand.Quote)
            }
            EditorToolTab.DIVIDER -> {
                runInputCommand(RichTextCommand.Divider)
            }
            EditorToolTab.LINK -> {
                runInputCommand(RichTextCommand.QuickLink)
            }
            EditorToolTab.FONT,
            EditorToolTab.PARAGRAPH,
            EditorToolTab.TABLE_PICKER -> selectPanelTool(tool)
        }
    }

    val visibleTool = activeTool
    val restoringPanelHeight = keyboardRestorePanelHeight
    val targetSlotHeight = when {
        visibleTool != null -> visibleTool.adaptivePanelHeight(rememberedKeyboardHeight)
        restoringPanelHeight != null -> maxOf(restoringPanelHeight, currentKeyboardHeight)
        else -> currentKeyboardHeight
    }
    val slotHeight by animateDpAsState(
        targetValue = targetSlotHeight,
        animationSpec = tween(durationMillis = TOOL_SLOT_ANIMATION_MS),
        label = "richTextToolSlotHeight"
    )
    val needsNavigationPadding = currentKeyboardHeight <= MIN_REMEMBERED_KEYBOARD_HEIGHT.dp
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .then(
                if (needsNavigationPadding) {
                    Modifier.navigationBarsPadding()
                } else {
                    Modifier
                }
            )
    ) {
        ToolbarEntryBar(
            activeTool = visibleTool,
            isImageUploading = isImageUploading,
            onSelect = ::openTool
        )
        if (visibleTool == null) {
            Spacer(Modifier.height(slotHeight))
        } else {
            ToolDrawer(
                tool = visibleTool,
                drawerHeight = slotHeight,
                selectedFontSize = selectedFontSize,
                selectedLineHeight = selectedLineHeight,
                selectedFontFamily = selectedFontFamily,
                selectedParagraph = selectedParagraph,
                selectedAlignment = selectedAlignment,
                selectedList = selectedList,
                selectedColor = selectedColor,
                boldEnabled = boldEnabled,
                italicEnabled = italicEnabled,
                underlineEnabled = underlineEnabled,
                todoEnabled = todoEnabled,
                quoteEnabled = quoteEnabled,
                onParagraph = { label ->
                    selectedParagraph = label
                    runPanelCommand(label.toParagraphCommand())
                },
                onBold = {
                    boldEnabled = !boldEnabled
                    runPanelCommand(RichTextCommand.Bold)
                },
                onItalic = {
                    italicEnabled = !italicEnabled
                    runPanelCommand(RichTextCommand.Italic)
                },
                onUnderline = {
                    underlineEnabled = !underlineEnabled
                    runPanelCommand(RichTextCommand.Underline)
                },
                onFontSizeChange = { fontSize ->
                    selectedFontSize = fontSize
                    runPanelCommand(RichTextCommand.FontSize(fontSize))
                },
                onColor = { colorHex ->
                    selectedColor = colorHex
                    runPanelCommand(RichTextCommand.FontColor(colorHex))
                },
                onFirstLineIndent = {
                    runPanelCommand(RichTextCommand.FirstLineIndent)
                },
                onTodo = {
                    todoEnabled = !todoEnabled
                    runInputCommand(RichTextCommand.TodoList)
                },
                onAlign = { alignment ->
                    selectedAlignment = alignment
                    runPanelCommand(RichTextCommand.Align(alignment))
                },
                onList = { list ->
                    val isSameList = selectedList == list
                    selectedList = if (isSameList) null else list
                    when (list) {
                        "bullet" -> runPanelCommand(RichTextCommand.BulletList)
                        "number" -> runPanelCommand(RichTextCommand.NumberList)
                        "alpha" -> runPanelCommand(RichTextCommand.AlphaList)
                    }
                },
                onLineHeight = { lineHeight ->
                    selectedLineHeight = lineHeight
                    runPanelCommand(RichTextCommand.LineHeight(lineHeight))
                },
                onFontFamily = { fontFamily ->
                    selectedFontFamily = fontFamily
                    runPanelCommand(RichTextCommand.FontFamily(fontFamily.cssValue))
                },
                onQuote = {
                    quoteEnabled = !quoteEnabled
                    runInputCommand(RichTextCommand.Quote)
                },
                onDivider = { runInputCommand(RichTextCommand.Divider) },
                onLink = { runInputCommand(RichTextCommand.QuickLink) },
                isImageUploading = isImageUploading,
                onPickImage = {
                    activeTool = null
                    keyboardRestorePanelHeight = null
                    onPickImage()
                },
                onPickAudio = {
                    activeTool = null
                    keyboardRestorePanelHeight = null
                    onPickAudio()
                },
                onOpenTablePicker = {
                    activeTool = EditorToolTab.TABLE_PICKER
                },
                onInsertTable = { rows, columns ->
                    activeTool = null
                    keyboardRestorePanelHeight = EditorToolTab.TABLE_PICKER.adaptivePanelHeight(rememberedKeyboardHeight)
                    onInsertTable(rows, columns)
                },
                onPickFile = {
                    activeTool = null
                    keyboardRestorePanelHeight = null
                    onPickFile()
                },
                onReferenceFile = {
                    activeTool = null
                    keyboardRestorePanelHeight = null
                    onReferenceFile()
                }
            )
        }
    }
}

@Composable
private fun ToolDrawer(
    tool: EditorToolTab,
    drawerHeight: Dp,
    selectedFontSize: Int,
    selectedLineHeight: Float,
    selectedFontFamily: FontFamilyChoice,
    selectedParagraph: String,
    selectedAlignment: TextAlignment,
    selectedList: String?,
    selectedColor: String,
    boldEnabled: Boolean,
    italicEnabled: Boolean,
    underlineEnabled: Boolean,
    todoEnabled: Boolean,
    quoteEnabled: Boolean,
    onParagraph: (String) -> Unit,
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onUnderline: () -> Unit,
    onFontSizeChange: (Int) -> Unit,
    onColor: (String) -> Unit,
    onFirstLineIndent: () -> Unit,
    onTodo: () -> Unit,
    onAlign: (TextAlignment) -> Unit,
    onList: (String) -> Unit,
    onLineHeight: (Float) -> Unit,
    onFontFamily: (FontFamilyChoice) -> Unit,
    onQuote: () -> Unit,
    onDivider: () -> Unit,
    onLink: () -> Unit,
    isImageUploading: Boolean,
    onPickImage: () -> Unit,
    onPickAudio: () -> Unit,
    onOpenTablePicker: () -> Unit,
    onInsertTable: (Int, Int) -> Unit,
    onPickFile: () -> Unit,
    onReferenceFile: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(drawerHeight)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ToolbarCard {
            when (tool) {
                EditorToolTab.FONT -> {
                    ToolbarTextRow(
                        labels = FONT_FAMILY_OPTIONS.map { it.label },
                        active = selectedFontFamily.label
                    ) { label ->
                        FONT_FAMILY_OPTIONS.firstOrNull { it.label == label }?.let(onFontFamily)
                    }
                    ToolbarTextRow(
                        listOf("正文", "H1", "H2", "H3", "H4"),
                        active = selectedParagraph,
                        onClick = onParagraph
                    )
                    ToolbarIconRow {
                        ToolbarIconButton(
                            icon = Icons.Filled.FormatBold,
                            text = "B",
                            selected = boldEnabled,
                            onClick = onBold,
                            modifier = Modifier.weight(1f)
                        )
                        ToolbarIconButton(
                            icon = Icons.Filled.FormatItalic,
                            text = "I",
                            selected = italicEnabled,
                            onClick = onItalic,
                            modifier = Modifier.weight(1f)
                        )
                        ToolbarIconButton(
                            icon = Icons.Filled.FormatUnderlined,
                            text = "U",
                            selected = underlineEnabled,
                            onClick = onUnderline,
                            modifier = Modifier.weight(1f)
                        )
                        ToolbarTextButton(
                            text = "A-",
                            selected = false,
                            onClick = { onFontSizeChange((selectedFontSize - FONT_SIZE_STEP).coerceAtLeast(MIN_FONT_SIZE)) },
                            modifier = Modifier.weight(1f)
                        )
                        ToolbarTextButton(
                            text = "$selectedFontSize",
                            selected = true,
                            onClick = { onFontSizeChange(selectedFontSize) },
                            modifier = Modifier.weight(1f)
                        )
                        ToolbarTextButton(
                            text = "A+",
                            selected = false,
                            onClick = { onFontSizeChange((selectedFontSize + FONT_SIZE_STEP).coerceAtMost(MAX_FONT_SIZE)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    ToolbarColorRow(selectedColor = selectedColor, onSelectColor = onColor)
                    ToolbarIconButton(
                        icon = Icons.AutoMirrored.Filled.FormatIndentIncrease,
                        text = "首行缩进",
                        selected = false,
                        onClick = onFirstLineIndent,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                EditorToolTab.TODO -> {
                    ToolbarIconButton(
                        icon = Icons.Filled.CheckBox,
                        text = "待办事项",
                        selected = todoEnabled,
                        onClick = onTodo,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                EditorToolTab.PARAGRAPH -> {
                    ToolbarIconRow {
                        ToolbarIconButton(
                            icon = Icons.AutoMirrored.Filled.FormatAlignLeft,
                            text = "左",
                            selected = selectedAlignment == TextAlignment.LEFT,
                            onClick = { onAlign(TextAlignment.LEFT) },
                            modifier = Modifier.weight(1f)
                        )
                        ToolbarIconButton(
                            icon = Icons.Filled.FormatAlignCenter,
                            text = "中",
                            selected = selectedAlignment == TextAlignment.CENTER,
                            onClick = { onAlign(TextAlignment.CENTER) },
                            modifier = Modifier.weight(1f)
                        )
                        ToolbarIconButton(
                            icon = Icons.AutoMirrored.Filled.FormatAlignRight,
                            text = "右",
                            selected = selectedAlignment == TextAlignment.RIGHT,
                            onClick = { onAlign(TextAlignment.RIGHT) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    ToolbarIconRow {
                        ToolbarIconButton(
                            icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                            text = "圆点",
                            selected = selectedList == "bullet",
                            onClick = { onList("bullet") },
                            modifier = Modifier.weight(1f)
                        )
                        ToolbarIconButton(
                            icon = Icons.Filled.FormatListNumbered,
                            text = "数字",
                            selected = selectedList == "number",
                            onClick = { onList("number") },
                            modifier = Modifier.weight(1f)
                        )
                        ToolbarTextButton(
                            text = "字母",
                            selected = selectedList == "alpha",
                            onClick = { onList("alpha") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    ToolbarTextRow(
                        LINE_HEIGHT_OPTIONS.map { "行距${it.formatLineHeight()}" },
                        active = "行距${selectedLineHeight.formatLineHeight()}"
                    ) { label ->
                        onLineHeight(label.removePrefix("行距").toFloatOrNull() ?: DEFAULT_LINE_HEIGHT)
                    }
                }
                EditorToolTab.QUOTE -> {
                    ToolbarIconButton(
                        icon = Icons.Filled.FormatQuote,
                        text = "引号",
                        selected = quoteEnabled,
                        onClick = onQuote,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                EditorToolTab.DIVIDER -> {
                    ToolbarIconButton(
                        icon = Icons.Filled.HorizontalRule,
                        text = "插入分割线",
                        selected = false,
                        onClick = onDivider,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                EditorToolTab.LINK -> {
                    ToolbarIconButton(
                        icon = Icons.Filled.Link,
                        text = "插入链接",
                        selected = false,
                        onClick = onLink,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                EditorToolTab.INSERT -> {
                    ToolbarIconRow {
                        ToolbarIconOnlyButton(
                            icon = Icons.Filled.Image,
                            contentDescription = "从相册选择图片",
                            enabled = !isImageUploading,
                            showProgress = isImageUploading,
                            onClick = onPickImage,
                            modifier = Modifier.weight(1f)
                        )
                        ToolbarIconOnlyButton(
                            icon = Icons.Filled.Audiotrack,
                            contentDescription = "音频",
                            onClick = onPickAudio,
                            modifier = Modifier.weight(1f)
                        )
                        ToolbarIconOnlyButton(
                            icon = Icons.Filled.TableChart,
                            contentDescription = "表格",
                            onClick = onOpenTablePicker,
                            modifier = Modifier.weight(1f)
                        )
                        ToolbarIconOnlyButton(
                            icon = Icons.Filled.AttachFile,
                            contentDescription = "附件",
                            onClick = onPickFile,
                            modifier = Modifier.weight(1f)
                        )
                        ToolbarIconOnlyButton(
                            icon = Icons.AutoMirrored.Filled.InsertDriveFile,
                            contentDescription = "附件引用",
                            onClick = onReferenceFile,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                EditorToolTab.TABLE_PICKER -> {
                    TableGridPicker(onPick = onInsertTable)
                }
            }
        }
    }
}

@Composable
private fun ToolbarEntryBar(
    activeTool: EditorToolTab?,
    isImageUploading: Boolean,
    onSelect: (EditorToolTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToolbarEntryButton(
            tool = EditorToolTab.FONT,
            activeTool = activeTool,
            label = "Aa",
            enabled = true,
            onSelect = onSelect,
            modifier = Modifier.weight(1f)
        )
        ToolbarEntryButton(
            tool = EditorToolTab.TODO,
            activeTool = activeTool,
            label = "待办",
            icon = Icons.Filled.CheckBox,
            enabled = true,
            onSelect = onSelect,
            modifier = Modifier.weight(1f)
        )
        ToolbarEntryButton(
            tool = EditorToolTab.PARAGRAPH,
            activeTool = activeTool,
            label = "段落",
            icon = Icons.AutoMirrored.Filled.FormatAlignLeft,
            enabled = true,
            onSelect = onSelect,
            modifier = Modifier.weight(1f)
        )
        ToolbarEntryButton(
            tool = EditorToolTab.QUOTE,
            activeTool = activeTool,
            label = "引号",
            icon = Icons.Filled.FormatQuote,
            enabled = true,
            onSelect = onSelect,
            modifier = Modifier.weight(1f)
        )
        ToolbarEntryButton(
            tool = EditorToolTab.DIVIDER,
            activeTool = activeTool,
            label = "分割线",
            icon = Icons.Filled.HorizontalRule,
            enabled = true,
            onSelect = onSelect,
            modifier = Modifier.weight(1f)
        )
        ToolbarEntryButton(
            tool = EditorToolTab.LINK,
            activeTool = activeTool,
            label = "链接",
            icon = Icons.Filled.Link,
            enabled = true,
            onSelect = onSelect,
            modifier = Modifier.weight(1f)
        )
        ToolbarEntryButton(
            tool = EditorToolTab.INSERT,
            activeTool = activeTool,
            label = "插入",
            icon = Icons.Filled.Add,
            enabled = !isImageUploading,
            onSelect = onSelect,
            showProgress = isImageUploading,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ToolbarEntryButton(
    tool: EditorToolTab,
    activeTool: EditorToolTab?,
    label: String,
    icon: ImageVector? = null,
    enabled: Boolean,
    onSelect: (EditorToolTab) -> Unit,
    showProgress: Boolean = false,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (activeTool == tool) MaterialTheme.colorScheme.primaryContainer else TOOLBAR_CARD_COLOR,
        label = "toolbarEntryButtonColor"
    )
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .clickable(enabled = enabled, onClick = { onSelect(tool) })
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (showProgress) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(20.dp),
                tint = if (enabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                }
            )
        } else {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                },
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ToolbarCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(TOOLBAR_CARD_COLOR)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = content
    )
}

@Composable
private fun ToolbarTextRow(
    labels: List<String>,
    active: String?,
    onClick: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        labels.forEach { label ->
            ToolbarTextButton(
                text = label,
                selected = active == label,
                onClick = { onClick(label) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ToolbarIconRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
private fun ToolbarTextButton(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        label = "toolbarTextButtonColor"
    )
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            },
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ToolbarIconButton(
    icon: ImageVector,
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showProgress: Boolean = false,
    onClick: () -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        label = "toolbarIconButtonColor"
    )
    Row(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showProgress) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        } else {
            Icon(
                imageVector = icon,
                contentDescription = text,
                modifier = Modifier.size(17.dp),
                tint = if (enabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                }
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ToolbarIconOnlyButton(
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showProgress: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (showProgress) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        } else {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(22.dp),
                tint = if (enabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                }
            )
        }
    }
}

@Composable
private fun ToolbarColorRow(
    selectedColor: String,
    onSelectColor: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.FormatColorText,
            contentDescription = "字体颜色",
            tint = selectedColor.toComposeColor(),
            modifier = Modifier.size(22.dp)
        )
        FONT_COLOR_OPTIONS.forEach { colorHex ->
            val selected = selectedColor.equals(colorHex, ignoreCase = true)
            val borderColor = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                Color.Transparent
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colorHex.toComposeColor())
                    .clickable { onSelectColor(colorHex) }
                    .padding(if (selected) 2.dp else 0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(7.dp))
                        .background(Color.Transparent)
                        .then(Modifier.background(borderColor.copy(alpha = if (selected) 0.24f else 0f)))
                )
            }
        }
    }
}

@Composable
private fun ExportTypeDialog(
    onDismiss: () -> Unit,
    isExporting: Boolean,
    onExportPdf: () -> Unit,
    onExportWord: () -> Unit,
    onExportImage: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择导出类型") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExportTypeItem("PDF", isExporting, onExportPdf)
                ExportTypeItem("Word", isExporting, onExportWord)
                ExportTypeItem("图片", isExporting, onExportImage)
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
private fun ExportTypeItem(
    text: String,
    isExporting: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(TOOLBAR_CARD_COLOR)
            .clickable(enabled = !isExporting, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isExporting) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun exportCurrentEditorSnapshot(
    editorController: RichTextEditorController,
    viewModel: NoteEditViewModel,
    format: NoteExportFormat
) {
    editorController.snapshot { snapshot ->
        viewModel.exportCurrentNote(
            format = format,
            content = snapshot?.text,
            contentHtml = snapshot?.html
        )
    }
}

@Composable
private fun ExportResultDialog(
    file: NoteExportFile,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导出完成") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = file.fileName,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "可以保存到本机文档，也可以直接打开或分享。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ExportResultAction(
                    text = "保存到本机",
                    icon = Icons.Filled.Download,
                    onClick = onSave
                )
                ExportResultAction(
                    text = "打开文件",
                    icon = Icons.AutoMirrored.Filled.InsertDriveFile,
                    onClick = onOpen
                )
                ExportResultAction(
                    text = "分享文件",
                    icon = Icons.Filled.Share,
                    onClick = onShare
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("完成")
            }
        }
    )
}

@Composable
private fun ExportResultAction(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun TableGridPicker(
    onPick: (Int, Int) -> Unit
) {
    var selectedRows by remember { mutableStateOf(DEFAULT_TABLE_SIZE) }
    var selectedColumns by remember { mutableStateOf(DEFAULT_TABLE_SIZE) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "${selectedRows} x ${selectedColumns}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        (1..TABLE_PICKER_GRID_SIZE).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                (1..TABLE_PICKER_GRID_SIZE).forEach { column ->
                    val selected = row <= selectedRows && column <= selectedColumns
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(26.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            )
                            .clickable {
                                selectedRows = row
                                selectedColumns = column
                            }
                    )
                }
            }
        }
        Button(
            onClick = { onPick(selectedRows, selectedColumns) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Filled.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("插入表格")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaperToneSheet(
    selectedTone: PaperTone,
    onDismiss: () -> Unit,
    onSelect: (PaperTone) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PaperTone.entries.forEach { tone ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(tone.color)
                        .clickable { onSelect(tone) }
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(tone.label, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                    if (selectedTone == tone) {
                        Icon(Icons.Filled.Check, contentDescription = null)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileReferenceSheet(
    files: List<ManagedFile>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSelect: (ManagedFile) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = LocalConfiguration.current.screenHeightDp.dp * INSERT_SHEET_MAX_SCREEN_FRACTION)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("附件引用", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(96.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (files.isEmpty()) {
                Text("暂无可引用文件", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                files.forEach { file ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(TOOLBAR_CARD_COLOR)
                            .clickable { onSelect(file) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.AutoMirrored.Filled.InsertDriveFile, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(file.displayName, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${file.fileSize.readableFileSize()} · ${file.mimeType ?: "未知类型"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun RichTextCommand.toJavascript(): String {
    return when (this) {
        RichTextCommand.Undo -> editorCommand("undo")
        RichTextCommand.Redo -> editorCommand("redo")
        RichTextCommand.Focus -> "window.NotaskEditor.focusEditor();"
        RichTextCommand.Blur -> "window.NotaskEditor.blurEditor();"
        RichTextCommand.Paragraph -> editorCommand("formatBlock", "p")
        RichTextCommand.HeadingOne -> editorCommand("formatBlock", "h1")
        RichTextCommand.HeadingTwo -> editorCommand("formatBlock", "h2")
        RichTextCommand.HeadingThree -> editorCommand("formatBlock", "h3")
        RichTextCommand.HeadingFour -> editorCommand("formatBlock", "h4")
        RichTextCommand.Bold -> editorCommand("bold")
        RichTextCommand.Italic -> editorCommand("italic")
        RichTextCommand.Underline -> editorCommand("underline")
        RichTextCommand.BulletList -> editorCommand("insertUnorderedList")
        RichTextCommand.NumberList -> editorCommand("insertOrderedList")
        RichTextCommand.AlphaList -> "window.NotaskEditor.alphaList();"
        RichTextCommand.TodoList -> "window.NotaskEditor.toggleTodo();"
        RichTextCommand.Quote -> "window.NotaskEditor.toggleBlockquote();"
        RichTextCommand.Divider -> editorInsertHtml("<hr><p><br></p>")
        RichTextCommand.QuickLink -> "window.NotaskEditor.quickLink();"
        RichTextCommand.RemoveLink -> "window.NotaskEditor.removeLink();"
        RichTextCommand.FirstLineIndent -> "window.NotaskEditor.toggleFirstLineIndent();"
        RichTextCommand.ResetAudioPlayback -> "window.NotaskEditor.resetAudioPlayback();"
        is RichTextCommand.FontSize -> "window.NotaskEditor.applyFontSize(${sizePx.coerceIn(MIN_FONT_SIZE, MAX_FONT_SIZE)});"
        is RichTextCommand.FontColor -> "window.NotaskEditor.applyFontColor(${JSONObject.quote(colorHex)});"
        is RichTextCommand.FontFamily -> "window.NotaskEditor.applyFontFamily(${JSONObject.quote(family)});"
        is RichTextCommand.LineHeight -> "window.NotaskEditor.applyLineHeight(${lineHeight.coerceIn(MIN_LINE_HEIGHT, MAX_LINE_HEIGHT)});"
        is RichTextCommand.Align -> editorCommand(alignment.toEditorCommand())
        is RichTextCommand.InsertLink -> editorInsertLink(text, url)
        is RichTextCommand.InsertImage -> editorInsertHtml(
            "<figure><img src=\"${url.toAbsoluteAssetUrl().escapeHtmlAttribute()}\" alt=\"${alt.escapeHtmlAttribute()}\"" +
                managedFileId.toDataAttribute(DATA_MANAGED_FILE_ID) +
                attachmentId.toDataAttribute(DATA_ATTACHMENT_ID) +
                "></figure><p><br></p>"
        )
        is RichTextCommand.SetContent -> editorSetContent(html)
        is RichTextCommand.InsertTable -> editorInsertHtml(tableHtml(rows, columns))
        is RichTextCommand.InsertAudio -> editorInsertHtml(audioHtml(name, url, managedFileId, attachmentId))
        is RichTextCommand.InsertFile -> editorInsertHtml(
            fileHtml(
                name = name,
                url = url,
                mimeType = mimeType,
                fileSize = fileSize,
                managedFileId = managedFileId,
                attachmentId = attachmentId
            )
        )
        is RichTextCommand.ReceiveCollabTicket -> editorReceiveCollabTicket(requestId, ticket, errorMessage)
    }
}

private fun editorCommand(command: String, value: String? = null): String {
    val commandValue = value?.let(JSONObject::quote) ?: "null"
    return "window.NotaskEditor.command(${JSONObject.quote(command)}, $commandValue);"
}

private fun editorInsertHtml(html: String): String {
    return "window.NotaskEditor.insertHtml(${JSONObject.quote(html)});"
}

private fun editorInsertLink(text: String, url: String): String {
    return "window.NotaskEditor.insertLink(${JSONObject.quote(text)}, ${JSONObject.quote(url)});"
}

private fun editorSetContent(html: String): String {
    return "window.NotaskEditor.setContent(${JSONObject.quote(html.sanitizeEditorHtml())});"
}

private fun editorReceiveCollabTicket(requestId: String, ticket: String, errorMessage: String?): String {
    return "window.NotaskEditor.receiveTicket(" +
        "${JSONObject.quote(requestId)}, " +
        "${JSONObject.quote(ticket)}, " +
        "${errorMessage?.let(JSONObject::quote) ?: "null"}" +
        ");"
}

private fun Long?.toDataAttribute(name: String): String {
    return this?.let { value -> " $name=\"$value\"" }.orEmpty()
}

private fun TextAlignment.toEditorCommand(): String {
    return when (this) {
        TextAlignment.LEFT -> "justifyLeft"
        TextAlignment.CENTER -> "justifyCenter"
        TextAlignment.RIGHT -> "justifyRight"
    }
}

private fun String.toParagraphCommand(): RichTextCommand {
    return when (this) {
        "H1" -> RichTextCommand.HeadingOne
        "H2" -> RichTextCommand.HeadingTwo
        "H3" -> RichTextCommand.HeadingThree
        "H4" -> RichTextCommand.HeadingFour
        else -> RichTextCommand.Paragraph
    }
}

private fun tableHtml(rows: Int, columns: Int): String {
    val body = (1..rows.coerceIn(1, MAX_TABLE_SIZE)).joinToString("") {
        "<tr>" + (1..columns.coerceIn(1, MAX_TABLE_SIZE)).joinToString("") { "<td><br></td>" } + "</tr>"
    }
    return "<section class=\"table-card\"><div class=\"table-actions\" contenteditable=\"false\">" +
        "<button type=\"button\" data-table-action=\"add-row\">+行</button>" +
        "<button type=\"button\" data-table-action=\"add-column\">+列</button>" +
        "<button type=\"button\" data-table-action=\"delete-row\">-行</button>" +
        "<button type=\"button\" data-table-action=\"delete-column\">-列</button>" +
        "</div><div class=\"table-scroll\"><table><tbody>$body</tbody></table></div></section><p><br></p>"
}

private fun audioHtml(
    name: String,
    url: String,
    managedFileId: Long?,
    attachmentId: Long?
): String {
    val absoluteUrl = url.toAbsoluteAssetUrl()
    val previewHref = managedFilePreviewHref(absoluteUrl, managedFileId)
    return "<section class=\"file-card audio-card\" contenteditable=\"false\"" +
        managedFileId.toDataAttribute(DATA_MANAGED_FILE_ID) +
        attachmentId.toDataAttribute(DATA_ATTACHMENT_ID) +
        " data-name=\"${name.escapeHtmlAttribute()}\" data-url=\"${absoluteUrl.escapeHtmlAttribute()}\"" +
        " data-preview-href=\"${previewHref.escapeHtmlAttribute()}\" data-mime=\"audio/*\">" +
        "<button class=\"media-play\" type=\"button\" aria-label=\"播放\"><span class=\"play-shape\"></span></button>" +
        "<div class=\"file-main\"><strong>${name.escapeHtml()}</strong>" +
        "<span>音频文件</span><div class=\"wave\"><i></i><i></i><i></i><i></i><i></i></div>" +
        "<audio src=\"${absoluteUrl.escapeHtmlAttribute()}\" preload=\"metadata\"></audio></div></section><p><br></p>"
}

private fun fileHtml(
    name: String,
    url: String,
    mimeType: String?,
    fileSize: Long,
    managedFileId: Long?,
    attachmentId: Long?
): String {
    val absoluteUrl = url.toAbsoluteAssetUrl()
    val previewHref = managedFilePreviewHref(absoluteUrl, managedFileId)
    val fileLabel = fileKindLabel(mimeType, name)
    return "<section class=\"file-card attachment-card\" contenteditable=\"false\"" +
        managedFileId.toDataAttribute(DATA_MANAGED_FILE_ID) +
        attachmentId.toDataAttribute(DATA_ATTACHMENT_ID) +
        " data-name=\"${name.escapeHtmlAttribute()}\" data-url=\"${absoluteUrl.escapeHtmlAttribute()}\"" +
        " data-preview-href=\"${previewHref.escapeHtmlAttribute()}\"" +
        " data-mime=\"${mimeType.orEmpty().escapeHtmlAttribute()}\">" +
        "<div class=\"file-icon\">${fileIconText(mimeType, name).escapeHtml()}</div><div class=\"file-main\"><strong>${name.escapeHtml()}</strong>" +
        "<span>${fileLabel.escapeHtml()}</span></div>" +
        "<a class=\"file-open\" href=\"${previewHref.escapeHtmlAttribute()}\">打开</a></section><p><br></p>"
}

private fun managedFilePreviewHref(url: String, managedFileId: Long?): String {
    return managedFileId?.let { fileId -> "/app/files/preview/$fileId" } ?: url
}

private fun collabDocumentHtml(): String {
    return """
        <!doctype html>
        <html>
        <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
            <style>
                html, body {
                    margin: 0;
                    padding: 0;
                    background: transparent;
                }
            </style>
        </head>
        <body>
            <main id="editor"></main>
            <script src="/$COLLAB_KERNEL_SEGMENT/$COLLAB_KERNEL_SCRIPT_NAME"></script>
        </body>
        </html>
    """.trimIndent()
}

private fun collabConfigureScript(
    spaceId: Long,
    noteId: Long,
    initialHtml: String,
    currentUser: UserProfile?
): String {
    val presenceColors = currentUser.presenceColors()
    val user = JSONObject()
        .put("userId", currentUser?.id ?: 0L)
        .put("name", currentUser?.displayCollabName() ?: "Android")
        .put("avatarUrl", currentUser?.avatarUrl.orEmpty())
        .put("color", presenceColors.first)
        .put("colorLight", presenceColors.second)
    val payload = JSONObject()
        .put("baseUrl", "${BuildConfig.BASE_URL.trimEnd('/')}/")
        .put("collabWsUrl", resolveCollabWsUrl())
        .put("spaceId", spaceId)
        .put("noteId", noteId)
        .put("canEdit", true)
        .put("placeholder", "开始协作编辑")
        .put("initialHtml", initialHtml.sanitizeEditorHtml())
        .put("user", user)
    return """
        (function configureNotaskCollab(attempt) {
            if (window.NotaskEditor && window.NotaskEditor.configure) {
                window.NotaskEditor.configure($payload);
                return;
            }
            if (attempt < 40) {
                window.setTimeout(function() {
                    configureNotaskCollab(attempt + 1);
                }, 100);
            } else if (window.$COLLAB_BRIDGE_NAME && window.$COLLAB_BRIDGE_NAME.onCollabStatus) {
                window.$COLLAB_BRIDGE_NAME.onCollabStatus('error', '协作编辑器脚本加载失败');
            }
        })(0);
    """.trimIndent()
}

private fun resolveCollabWsUrl(): String {
    val explicitUrl = BuildConfig.COLLAB_WS_URL.trim()
    if (explicitUrl.isNotBlank()) {
        return explicitUrl
    }
    val baseUri = Uri.parse(BuildConfig.BASE_URL)
    val protocol = if (baseUri.scheme == "https") {
        "wss"
    } else {
        "ws"
    }
    val host = baseUri.host ?: "192.168.1.20"
    return "$protocol://$host:8081/ws"
}

private fun collabEditorBaseUrl(): String {
    val baseUri = Uri.parse(BuildConfig.BASE_URL)
    val host = baseUri.host ?: return editorBaseUrl()
    val port = baseUri.port.takeIf { it > 0 }?.let { port -> ":$port" }.orEmpty()
    val scheme = if (resolveCollabWsUrl().startsWith("wss://")) {
        EDITOR_HTTPS_SCHEME
    } else {
        EDITOR_HTTP_SCHEME
    }
    return "$scheme://$host$port/"
}

private fun editorBaseUrl(): String {
    return "${BuildConfig.BASE_URL.trimEnd('/')}/"
}

private fun isEditorInterceptRequest(uri: Uri): Boolean {
    val baseUri = Uri.parse(BuildConfig.BASE_URL)
    val host = baseUri.host ?: return false
    return uri.host == host && uri.scheme in EDITOR_INTERCEPT_SCHEMES
}

private fun richDocumentHtml(initialHtml: String, spaceId: Long?): String {
    val editorSpaceId = spaceId?.toString().orEmpty()
    return """
        <!doctype html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
            <style>
                html, body {
                    margin: 0;
                    padding: 0;
                    background: transparent;
                    color: #1d1b20;
                    font-family: sans-serif;
                    -webkit-text-size-adjust: 100%;
                }
                #editor {
                    min-height: 100vh;
                    box-sizing: border-box;
                    padding: 18px 0 32px;
                    outline: none;
                    font-size: 16px;
                    line-height: 1.5;
                    word-break: break-word;
                }
                #editor:empty:before {
                    content: attr(data-placeholder);
                    color: #8a858f;
                }
                h1, h2, h3, p, blockquote, ul, ol, figure {
                    margin: 0 0 14px;
                }
                h1 {
                    font-size: 28px;
                    line-height: 1.3;
                    font-weight: 700;
                }
                h2 {
                    font-size: 23px;
                    line-height: 1.35;
                    font-weight: 700;
                }
                h3 {
                    font-size: 19px;
                    line-height: 1.45;
                    font-weight: 700;
                }
                h4 {
                    font-size: 17px;
                    line-height: 1.45;
                    font-weight: 700;
                }
                blockquote {
                    border-left: 3px solid #6750a4;
                    padding: 10px 12px;
                    background: rgba(103, 80, 164, 0.08);
                    border-radius: 10px;
                    color: #5f5867;
                }
                ul, ol {
                    padding-left: 24px;
                }
                a {
                    color: #6750a4;
                }
                hr {
                    border: 0;
                    border-top: 1px solid rgba(121, 116, 126, 0.35);
                    margin: 18px 0;
                }
                figure {
                    margin: 12px 0 16px;
                }
                img {
                    max-width: 100%;
                    border-radius: 12px;
                    display: block;
                }
                figcaption {
                    margin-top: 6px;
                    font-size: 12px;
                    color: #79747e;
                    text-align: center;
                }
                .todo-card {
                    margin: 12px 0 16px;
                    padding: 12px 14px;
                    border-left: 3px solid #6750a4;
                    border-radius: 10px;
                    background: rgba(103, 80, 164, 0.08);
                    color: #4f465b;
                    display: flex;
                    align-items: flex-start;
                    gap: 10px;
                }
                .todo-check {
                    flex: 0 0 auto;
                    margin-top: 4px;
                }
                .todo-card.checked .todo-text {
                    color: #79747e;
                    text-decoration: line-through;
                }
                .todo-text {
                    flex: 1;
                    min-height: 24px;
                    outline: none;
                }
                .table-card {
                    max-width: 100%;
                    margin: 12px 0 16px;
                    padding: 10px;
                    border-radius: 12px;
                    background: rgba(121, 116, 126, 0.08);
                    box-sizing: border-box;
                }
                .table-actions {
                    display: flex;
                    flex-wrap: wrap;
                    gap: 6px;
                    margin-bottom: 8px;
                }
                .table-actions button {
                    border: 0;
                    border-radius: 999px;
                    padding: 6px 10px;
                    color: #6750a4;
                    background: rgba(103, 80, 164, 0.14);
                    font-weight: 700;
                }
                .table-scroll {
                    max-width: 100%;
                    overflow-x: auto;
                    -webkit-overflow-scrolling: touch;
                }
                table {
                    width: max-content;
                    min-width: 100%;
                    max-width: none;
                    border-collapse: collapse;
                    margin: 0;
                    table-layout: fixed;
                }
                td {
                    min-width: 84px;
                    max-width: 160px;
                    padding: 10px;
                    border: 1px solid rgba(121, 116, 126, 0.32);
                    word-break: break-word;
                }
                .file-card {
                    position: relative;
                    display: flex;
                    align-items: center;
                    gap: 10px;
                    width: 100%;
                    max-width: 100%;
                    box-sizing: border-box;
                    overflow: hidden;
                    padding: 10px 12px;
                    margin: 12px 0 16px;
                    border-radius: 12px;
                    background: rgba(121, 116, 126, 0.10);
                }
                .file-card:focus {
                    outline: 2px solid rgba(103, 80, 164, 0.22);
                }
                .audio-card {
                    align-items: center;
                    min-height: 70px;
                }
                .file-icon {
                    width: 36px;
                    height: 36px;
                    border-radius: 10px;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    font-weight: 700;
                    background: rgba(103, 80, 164, 0.14);
                    color: #6750a4;
                }
                .file-main {
                    flex: 1;
                    min-width: 0;
                    display: flex;
                    flex-direction: column;
                    gap: 4px;
                }
                .file-main strong {
                    display: block;
                    overflow: hidden;
                    text-overflow: ellipsis;
                    white-space: nowrap;
                    line-height: 1.35;
                }
                .file-main span {
                    font-size: 12px;
                    color: #79747e;
                    overflow: hidden;
                    text-overflow: ellipsis;
                    white-space: nowrap;
                }
                .wave {
                    display: flex;
                    align-items: center;
                    gap: 3px;
                    height: 18px;
                }
                .wave i {
                    width: 4px;
                    border-radius: 4px;
                    background: #8f7bc8;
                    transform-origin: center;
                }
                .wave i:nth-child(1) { height: 8px; }
                .wave i:nth-child(2) { height: 14px; }
                .wave i:nth-child(3) { height: 18px; }
                .wave i:nth-child(4) { height: 12px; }
                .wave i:nth-child(5) { height: 16px; }
                .audio-card.playing .wave i {
                    animation: wavePulse 760ms ease-in-out infinite;
                }
                .audio-card.playing .wave i:nth-child(2) {
                    animation-delay: 90ms;
                }
                .audio-card.playing .wave i:nth-child(3) {
                    animation-delay: 180ms;
                }
                .audio-card.playing .wave i:nth-child(4) {
                    animation-delay: 270ms;
                }
                .audio-card.playing .wave i:nth-child(5) {
                    animation-delay: 360ms;
                }
                @keyframes wavePulse {
                    0%, 100% { transform: scaleY(0.55); opacity: 0.58; }
                    50% { transform: scaleY(1.18); opacity: 1; }
                }
                .media-play {
                    width: 38px;
                    height: 38px;
                    border: 0;
                    border-radius: 50%;
                    background: linear-gradient(135deg, #6750a4, #8f7bc8);
                    color: #ffffff;
                    flex: 0 0 auto;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    box-shadow: 0 8px 18px rgba(103, 80, 164, 0.22);
                }
                .audio-card.playing .media-play {
                    background: #4f378b;
                }
                .play-shape {
                    width: 0;
                    height: 0;
                    margin-left: 3px;
                    border-top: 8px solid transparent;
                    border-bottom: 8px solid transparent;
                    border-left: 12px solid #ffffff;
                }
                .audio-card.playing .play-shape {
                    width: 14px;
                    height: 16px;
                    margin-left: 0;
                    border: 0;
                    position: relative;
                }
                .audio-card.playing .play-shape:before,
                .audio-card.playing .play-shape:after {
                    content: "";
                    position: absolute;
                    top: 0;
                    width: 4px;
                    height: 16px;
                    border-radius: 2px;
                    background: #ffffff;
                }
                .audio-card.playing .play-shape:before {
                    left: 1px;
                }
                .audio-card.playing .play-shape:after {
                    right: 1px;
                }
                .file-open {
                    border: 0;
                    border-radius: 999px;
                    padding: 7px 11px;
                    background: rgba(103, 80, 164, 0.14);
                    color: #6750a4;
                    font-weight: 700;
                    flex: 0 0 auto;
                }
                .media-delete {
                    display: none;
                }
                .file-card audio {
                    width: 100%;
                    height: 30px;
                }
            </style>
        </head>
        <body>
            <div id="editor" contenteditable="true" data-placeholder="开始编写文档...">${initialHtml.sanitizeEditorHtml()}</div>
            <script>
                (function () {
                    const editor = document.getElementById('editor');
                    const editorSpaceId = '$editorSpaceId';
                    let savedRange = null;
                    function notify() {
                        prepareManagedMedia();
                        if (window.$EDITOR_BRIDGE_NAME) {
                            window.$EDITOR_BRIDGE_NAME.onDocumentChanged(editor.innerText || '', serializeHtml());
                        }
                    }
                    window.NotaskEditor = {
                        command: function(command, value) {
                            focusEditorForCommand();
                            document.execCommand(command, false, value);
                            finishCommand();
                        },
                        applyFontSize: function(size) {
                            focusEditorForCommand();
                            applyInlineStyle('fontSize', Math.max(12, Math.min(28, Number(size) || 16)) + 'px');
                            finishCommand();
                        },
                        applyFontColor: function(color) {
                            focusEditorForCommand();
                            applyInlineStyle('color', color || '#1d1b20');
                            finishCommand();
                        },
                        applyFontFamily: function(fontFamily) {
                            focusEditorForCommand();
                            applyInlineStyle('fontFamily', fontFamily || 'system-ui, sans-serif');
                            finishCommand();
                        },
                        toggleFirstLineIndent: function() {
                            focusEditorForCommand();
                            const targets = currentIndentTargets();
                            const shouldEnable = targets.some(function(block) {
                                return block.getAttribute('data-first-line-indent') !== 'true';
                            });
                            setFirstLineIndent(targets, shouldEnable);
                            finishCommand();
                        },
                        applyLineHeight: function(lineHeight) {
                            focusEditorForCommand();
                            applyBlockStyle('lineHeight', String(Math.max(1, Math.min(2, Number(lineHeight) || 1.5))));
                            finishCommand();
                        },
                        alphaList: function() {
                            focusEditorForCommand();
                            document.execCommand('insertOrderedList', false, null);
                            const block = currentBlockElement();
                            const list = closestElement(block, 'OL');
                            if (list) {
                                list.style.listStyleType = 'lower-alpha';
                            }
                            finishCommand();
                        },
                        toggleTodo: function() {
                            focusEditorForCommand();
                            const block = currentBlockElement();
                            if (block && block.classList && block.classList.contains('todo-card')) {
                                const text = block.innerText || '';
                                block.outerHTML = '<p>' + escapeHtml(text) + '</p>';
                            } else {
                                document.execCommand('insertHTML', false, '$TODO_ITEM_HTML');
                                focusLastTodoText();
                            }
                            finishCommand();
                        },
                        toggleBlockquote: function() {
                            focusEditorForCommand();
                            const block = currentBlockElement();
                            if (block && block.tagName === 'BLOCKQUOTE') {
                                document.execCommand('formatBlock', false, 'p');
                            } else {
                                document.execCommand('formatBlock', false, 'blockquote');
                            }
                            finishCommand();
                        },
                        insertHtml: function(html) {
                            focusEditorForCommand();
                            document.execCommand('insertHTML', false, html);
                            placeCursorAfterLastInsertedBlock();
                            finishCommand();
                        },
                        insertLink: function(text, url) {
                            focusEditorForCommand();
                            const selection = window.getSelection();
                            if (selection && selection.toString()) {
                                document.execCommand('createLink', false, url);
                            } else {
                                document.execCommand('insertHTML', false, '<a href="' + escapeHtml(url) + '">' + escapeHtml(text || url) + '</a>');
                            }
                            finishCommand();
                        },
                        quickLink: function() {
                            focusEditorForCommand();
                            const selection = window.getSelection();
                            const selectedText = selection && selection.toString() ? selection.toString().trim() : '';
                            if (selectedText) {
                                document.execCommand('createLink', false, normalizeHref(selectedText));
                            } else {
                                document.execCommand('insertHTML', false, '<a href="https://">链接</a>');
                            }
                            finishCommand();
                        },
                        removeLink: function() {
                            focusEditorForCommand();
                            document.execCommand('unlink', false, null);
                            finishCommand();
                        },
                        setContent: function(html) {
                            editor.innerHTML = sanitizeHtml(html || '');
                            prepareManagedMedia();
                            notify();
                        },
                        focusEditor: function() {
                            focusEditorForInput();
                            notify();
                        },
                        blurEditor: function() {
                            rememberSelection();
                            editor.blur();
                            if (document.activeElement && document.activeElement.blur) {
                                document.activeElement.blur();
                            }
                            notify();
                        },
                        snapshotContent: function() {
                            prepareManagedMedia();
                            return JSON.stringify({
                                text: editor.innerText || '',
                                html: serializeHtml()
                            });
                        },
                        resetAudioPlayback: function() {
                            resetAudioPlaybackUi();
                        }
                    };
                    function focusEditorForCommand() {
                        restoreSelection();
                        ensureSelectionInEditor();
                    }
                    function focusEditorForInput() {
                        editor.focus();
                        restoreSelection();
                        ensureSelectionInEditor();
                    }
                    function finishCommand() {
                        prepareManagedMedia();
                        rememberSelection();
                        notify();
                    }
                    function localPreviewUrl(fileId) {
                        if (!editorSpaceId || !fileId) {
                            return '';
                        }
                        return '${editorBaseUrl()}$EDITOR_ASSET_MARKER/$EDITOR_ASSET_SPACES_SEGMENT/' + editorSpaceId + '/$EDITOR_ASSET_FILES_SEGMENT/' + fileId + '/$EDITOR_ASSET_PREVIEW_SEGMENT';
                    }
                    function prepareManagedMedia() {
                        normalizeWebAudioCards();
                        normalizeManagedFileLinks();
                        normalizeFileCards();
                        if (!editorSpaceId) {
                            return;
                        }
                        const mediaNodes = editor.querySelectorAll('img[data-managed-file-id], audio');
                        mediaNodes.forEach(function(node) {
                            const owner = node.getAttribute('data-managed-file-id') ? node : node.closest('[data-managed-file-id]');
                            const fileId = owner ? owner.getAttribute('data-managed-file-id') : '';
                            const nextSource = localPreviewUrl(fileId);
                            if (!nextSource) {
                                return;
                            }
                            const currentSource = node.getAttribute('src') || '';
                            if (!node.getAttribute('data-original-src')) {
                                node.setAttribute('data-original-src', currentSource);
                            }
                            node.setAttribute('data-android-src', nextSource);
                            if (currentSource !== nextSource) {
                                node.setAttribute('src', nextSource);
                            }
                        });
                    }
                    function normalizeWebAudioCards() {
                        editor.querySelectorAll('.notask-audio-card[data-managed-file-id]').forEach(function(card) {
                            if (card.classList.contains('file-card')) {
                                return;
                            }
                            const fileId = card.getAttribute('data-managed-file-id') || '';
                            const attachmentId = card.getAttribute('data-attachment-id') || '';
                            const name = card.getAttribute('data-name') || (card.querySelector('.notask-audio-name') || {}).textContent || '音频文件';
                            const mimeType = card.getAttribute('data-mime') || 'audio/*';
                            const previewHref = card.getAttribute('data-preview-href') || (fileId ? '/app/files/preview/' + fileId : '');
                            const assetUrl = fileId && editorSpaceId ? localPreviewUrl(fileId) : card.getAttribute('data-url') || previewHref;
                            const androidCard = document.createElement('section');
                            androidCard.className = 'file-card audio-card';
                            androidCard.setAttribute('contenteditable', 'false');
                            androidCard.setAttribute('data-type', 'audio');
                            if (fileId) {
                                androidCard.setAttribute('data-managed-file-id', fileId);
                            }
                            if (attachmentId) {
                                androidCard.setAttribute('data-attachment-id', attachmentId);
                            }
                            androidCard.setAttribute('data-name', name);
                            androidCard.setAttribute('data-url', assetUrl || previewHref);
                            androidCard.setAttribute('data-preview-href', previewHref || assetUrl);
                            androidCard.setAttribute('data-mime', mimeType);
                            androidCard.innerHTML =
                                '<button class="media-play" type="button" aria-label="播放"><span class="play-shape"></span></button>' +
                                '<div class="file-main"><strong></strong><span>音频文件</span>' +
                                '<div class="wave"><i></i><i></i><i></i><i></i><i></i></div>' +
                                '<audio preload="metadata"></audio></div>';
                            const title = androidCard.querySelector('.file-main strong');
                            if (title) {
                                title.textContent = name;
                            }
                            const audio = androidCard.querySelector('audio');
                            if (audio && assetUrl) {
                                audio.setAttribute('src', assetUrl);
                            }
                            card.replaceWith(androidCard);
                        });
                    }
                    function normalizeManagedFileLinks() {
                        editor.querySelectorAll('a[data-managed-file-id]').forEach(function(link) {
                            if (link.closest('.file-card')) {
                                return;
                            }
                            const fileId = link.getAttribute('data-managed-file-id') || '';
                            const attachmentId = link.getAttribute('data-attachment-id') || '';
                            const name = link.getAttribute('data-name') || (link.textContent || '').trim() || '附件';
                            const href = link.getAttribute('href') || link.getAttribute('data-url') || '';
                            const mimeType = link.getAttribute('data-mime') || inferMimeType(name, link.getAttribute('data-type') || '');
                            const previewHref = href || (fileId ? '/app/files/preview/' + fileId : '');
                            const assetUrl = fileId && editorSpaceId ? localPreviewUrl(fileId) : previewHref;
                            const card = document.createElement('section');
                            card.className = 'file-card ' + (isAudioFile(mimeType, name) ? 'audio-card' : 'attachment-card');
                            card.setAttribute('contenteditable', 'false');
                            if (fileId) {
                                card.setAttribute('data-managed-file-id', fileId);
                            }
                            if (attachmentId) {
                                card.setAttribute('data-attachment-id', attachmentId);
                            }
                            card.setAttribute('data-name', name);
                            card.setAttribute('data-url', assetUrl || previewHref);
                            card.setAttribute('data-preview-href', previewHref || assetUrl);
                            card.setAttribute('data-mime', mimeType);
                            if (isAudioFile(mimeType, name)) {
                                card.innerHTML =
                                    '<button class="media-play" type="button" aria-label="播放"><span class="play-shape"></span></button>' +
                                    '<div class="file-main"><strong></strong><span>音频文件</span>' +
                                    '<div class="wave"><i></i><i></i><i></i><i></i><i></i></div>' +
                                    '<audio preload="metadata"></audio></div>';
                                const audio = card.querySelector('audio');
                                if (audio && assetUrl) {
                                    audio.setAttribute('src', assetUrl);
                                }
                            } else {
                                card.innerHTML =
                                    '<div class="file-icon"></div><div class="file-main"><strong></strong><span></span></div>' +
                                    '<a class="file-open">打开</a>';
                                const openLink = card.querySelector('.file-open');
                                if (openLink) {
                                    openLink.setAttribute('href', previewHref || assetUrl || '#');
                                }
                            }
                            const title = card.querySelector('.file-main strong');
                            if (title) {
                                title.textContent = name;
                            }
                            link.replaceWith(card);
                        });
                    }
                    function normalizeFileCards() {
                        editor.querySelectorAll('.attachment-card').forEach(function(card) {
                            const name = card.getAttribute('data-name') || '';
                            const mimeType = card.getAttribute('data-mime') || '';
                            const label = fileKindLabel(mimeType, name);
                            const meta = card.querySelector('.file-main span');
                            const icon = card.querySelector('.file-icon');
                            if (meta) {
                                meta.textContent = label;
                            }
                            if (icon) {
                                icon.textContent = fileIconText(mimeType, name);
                            }
                        });
                    }
                    function inferMimeType(fileName, type) {
                        if (type === 'audio') {
                            return 'audio/*';
                        }
                        const extension = fileExtension(fileName);
                        if (extension === 'mp3' || extension === 'wav' || extension === 'm4a' || extension === 'aac' || extension === 'flac' || extension === 'ogg' || extension === 'opus') {
                            return 'audio/*';
                        }
                        return '';
                    }
                    function isAudioFile(mimeType, fileName) {
                        if (/^audio\//i.test(mimeType)) {
                            return true;
                        }
                        const extension = fileExtension(fileName);
                        return extension === 'mp3' || extension === 'wav' || extension === 'm4a' || extension === 'aac' || extension === 'flac' || extension === 'ogg' || extension === 'opus';
                    }
                    function fileKindLabel(mimeType, fileName) {
                        const extension = fileExtension(fileName);
                        if (extension === 'doc' || extension === 'docx') {
                            return 'Word 文档';
                        }
                        if (extension === 'xls' || extension === 'xlsx') {
                            return 'Excel 表格';
                        }
                        if (extension === 'ppt' || extension === 'pptx') {
                            return 'PowerPoint 演示文稿';
                        }
                        if (extension === 'pdf') {
                            return 'PDF 文档';
                        }
                        if (extension === 'txt') {
                            return '文本文件';
                        }
                        if (/^image\//i.test(mimeType)) {
                            return '图片文件';
                        }
                        if (/^audio\//i.test(mimeType)) {
                            return '音频文件';
                        }
                        if (/^video\//i.test(mimeType)) {
                            return '视频文件';
                        }
                        return '附件';
                    }
                    function fileIconText(mimeType, fileName) {
                        const extension = fileExtension(fileName);
                        if (extension === 'doc' || extension === 'docx') {
                            return 'W';
                        }
                        if (extension === 'xls' || extension === 'xlsx') {
                            return 'X';
                        }
                        if (extension === 'ppt' || extension === 'pptx') {
                            return 'P';
                        }
                        if (extension === 'pdf') {
                            return 'P';
                        }
                        if (extension === 'txt') {
                            return 'T';
                        }
                        if (/^image\//i.test(mimeType)) {
                            return 'I';
                        }
                        if (/^audio\//i.test(mimeType)) {
                            return 'A';
                        }
                        return 'F';
                    }
                    function fileExtension(fileName) {
                        const dotIndex = String(fileName || '').lastIndexOf('.');
                        if (dotIndex < 0) {
                            return '';
                        }
                        return String(fileName).substring(dotIndex + 1).toLowerCase();
                    }
                    function serializeHtml() {
                        const clone = editor.cloneNode(true);
                        clone.querySelectorAll('.file-card[data-managed-file-id]').forEach(function(card) {
                            const link = document.createElement('a');
                            const fileId = card.getAttribute('data-managed-file-id') || '';
                            const attachmentId = card.getAttribute('data-attachment-id') || '';
                            const name = card.getAttribute('data-name') || ((card.querySelector('.file-main strong') || {}).textContent || '').trim() || '附件';
                            const mimeType = card.getAttribute('data-mime') || '';
                            const href = card.getAttribute('data-preview-href') || (fileId ? '/app/files/preview/' + fileId : card.getAttribute('data-url') || '#');
                            link.setAttribute('href', href);
                            link.setAttribute('data-managed-file-id', fileId);
                            if (attachmentId) {
                                link.setAttribute('data-attachment-id', attachmentId);
                            }
                            if (name) {
                                link.setAttribute('data-name', name);
                            }
                            if (mimeType) {
                                link.setAttribute('data-mime', mimeType);
                            }
                            link.setAttribute('data-type', card.classList.contains('audio-card') ? 'audio' : 'file');
                            link.textContent = name;
                            card.replaceWith(link);
                        });
                        clone.querySelectorAll('[data-original-src]').forEach(function(node) {
                            const originalSource = node.getAttribute('data-original-src') || '';
                            node.setAttribute('src', originalSource);
                            node.removeAttribute('data-original-src');
                            node.removeAttribute('data-android-src');
                        });
                        return clone.innerHTML || '';
                    }
                    function focusLastTodoText() {
                        const todoTextNodes = editor.querySelectorAll('.todo-text');
                        const target = todoTextNodes[todoTextNodes.length - 1];
                        if (!target) {
                            return;
                        }
                        focusEditableNode(target);
                    }
                    function placeCursorAfterLastInsertedBlock() {
                        const trailingParagraphs = editor.querySelectorAll('p');
                        const target = trailingParagraphs[trailingParagraphs.length - 1];
                        if (target) {
                            focusEditableNode(target);
                        }
                    }
                    function focusEditableNode(node) {
                        const range = document.createRange();
                        range.selectNodeContents(node);
                        range.collapse(false);
                        const selection = window.getSelection();
                        if (selection) {
                            selection.removeAllRanges();
                            selection.addRange(range);
                        }
                        if (node.focus) {
                            node.focus();
                        } else {
                            editor.focus();
                        }
                        savedRange = range.cloneRange();
                    }
                    function rememberSelection() {
                        const selection = window.getSelection();
                        if (!selection || selection.rangeCount === 0) {
                            return;
                        }
                        const range = selection.getRangeAt(0);
                        if (isEditorNode(range.commonAncestorContainer)) {
                            savedRange = range.cloneRange();
                        }
                    }
                    function restoreSelection() {
                        if (!savedRange) {
                            return;
                        }
                        const selection = window.getSelection();
                        if (!selection) {
                            return;
                        }
                        selection.removeAllRanges();
                        selection.addRange(savedRange);
                    }
                    function ensureSelectionInEditor() {
                        const selection = window.getSelection();
                        if (selection && selection.rangeCount > 0 && isEditorNode(selection.getRangeAt(0).commonAncestorContainer)) {
                            return;
                        }
                        const range = document.createRange();
                        range.selectNodeContents(editor);
                        range.collapse(false);
                        if (selection) {
                            selection.removeAllRanges();
                            selection.addRange(range);
                        }
                    }
                    function applyInlineStyle(styleName, styleValue) {
                        const selection = window.getSelection();
                        if (!selection || selection.rangeCount === 0 || selection.isCollapsed) {
                            const block = currentBlockElement();
                            if (block) {
                                block.style[styleName] = styleValue;
                            }
                            return;
                        }
                        const range = selection.getRangeAt(0);
                        const span = document.createElement('span');
                        span.style[styleName] = styleValue;
                        try {
                            span.appendChild(range.extractContents());
                            range.insertNode(span);
                            selection.removeAllRanges();
                            const nextRange = document.createRange();
                            nextRange.selectNodeContents(span);
                            selection.addRange(nextRange);
                        } catch (error) {
                            const cssName = styleName === 'fontSize' ? 'font-size' : (styleName === 'fontFamily' ? 'font-family' : styleName);
                            document.execCommand('insertHTML', false, '<span style="' + cssName + ':' + escapeHtml(styleValue) + '">' + escapeHtml(selection.toString()) + '</span>');
                        }
                    }
                    function applyBlockStyle(styleName, styleValue) {
                        const blocks = selectedBlockElements();
                        if (blocks.length === 0) {
                            const block = currentBlockElement();
                            if (block) {
                                block.style[styleName] = styleValue;
                            }
                            return;
                        }
                        blocks.forEach(function(block) {
                            block.style[styleName] = styleValue;
                        });
                    }
                    function currentIndentTargets() {
                        const blocks = selectedBlockElements();
                        if (blocks.length > 0) {
                            return blocks.filter(function(block) {
                                return block && block !== editor;
                            });
                        }
                        const block = currentBlockElement();
                        return block && block !== editor ? [block] : [];
                    }
                    function setFirstLineIndent(targets, enabled) {
                        targets.forEach(function(block) {
                            if (enabled) {
                                block.setAttribute('data-first-line-indent', 'true');
                                block.style.textIndent = '2em';
                            } else {
                                block.removeAttribute('data-first-line-indent');
                                block.style.textIndent = '';
                            }
                        });
                    }
                    function handleTabIndent(event) {
                        const targets = currentIndentTargets();
                        if (!targets.length) {
                            return false;
                        }
                        event.preventDefault();
                        setFirstLineIndent(targets, !event.shiftKey);
                        rememberSelection();
                        notify();
                        return true;
                    }
                    function selectedBlockElements() {
                        const selection = window.getSelection();
                        if (!selection || selection.rangeCount === 0) {
                            return [];
                        }
                        const range = selection.getRangeAt(0);
                        const walker = document.createTreeWalker(editor, NodeFilter.SHOW_ELEMENT);
                        const blocks = [];
                        let node = currentBlockElement(range.startContainer);
                        if (node) {
                            blocks.push(node);
                        }
                        while (walker.nextNode()) {
                            const element = walker.currentNode;
                            if (!isBlockElement(element) || blocks.indexOf(element) >= 0) {
                                continue;
                            }
                            try {
                                if (range.intersectsNode(element)) {
                                    blocks.push(element);
                                }
                            } catch (error) {
                            }
                        }
                        return blocks;
                    }
                    function currentBlockElement(source) {
                        const selection = window.getSelection();
                        let node = source || (selection ? selection.anchorNode : null) || editor;
                        if (node.nodeType === Node.TEXT_NODE) {
                            node = node.parentElement;
                        }
                        while (node && node !== editor) {
                            if (isBlockElement(node)) {
                                return node;
                            }
                            node = node.parentElement;
                        }
                        return editor;
                    }
                    function closestElement(element, tagName) {
                        let node = element;
                        while (node && node !== editor) {
                            if (node.tagName === tagName) {
                                return node;
                            }
                            node = node.parentElement;
                        }
                        return null;
                    }
                    function isBlockElement(element) {
                        return ['P', 'H1', 'H2', 'H3', 'H4', 'BLOCKQUOTE', 'LI', 'FIGCAPTION', 'TD', 'SECTION'].indexOf(element.tagName) >= 0;
                    }
                    function isEditorNode(node) {
                        if (!node) {
                            return false;
                        }
                        if (node === editor) {
                            return true;
                        }
                        if (node.nodeType === Node.TEXT_NODE) {
                            return editor.contains(node.parentElement);
                        }
                        return editor.contains(node);
                    }
                    function normalizeHref(value) {
                        const trimmed = String(value || '').trim();
                        if (/^(https?:|mailto:|tel:|\/)/i.test(trimmed)) {
                            return trimmed;
                        }
                        return 'https://' + trimmed;
                    }
                    function sanitizeHtml(value) {
                        return String(value).replace(/<script[\s\S]*?<\/script>/gi, '');
                    }
                    function escapeHtml(value) {
                        return String(value)
                            .replace(/&/g, '&amp;')
                            .replace(/</g, '&lt;')
                            .replace(/>/g, '&gt;')
                            .replace(/"/g, '&quot;');
                    }
                    function isBlankEditableText(element) {
                        return !String(element ? element.innerText || element.textContent || '' : '').trim();
                    }
                    function insertParagraphAfter(element) {
                        const paragraph = document.createElement('p');
                        paragraph.innerHTML = '<br>';
                        element.insertAdjacentElement('afterend', paragraph);
                        focusEditableNode(paragraph);
                        return paragraph;
                    }
                    function createTodoCardAfter(card) {
                        const wrapper = document.createElement('section');
                        wrapper.className = 'todo-card';
                        wrapper.innerHTML = '<input class="todo-check" type="checkbox" contenteditable="false"><span class="todo-text" contenteditable="true"><br></span>';
                        card.insertAdjacentElement('afterend', wrapper);
                        const target = wrapper.querySelector('.todo-text');
                        if (target) {
                            focusEditableNode(target);
                        }
                    }
                    function activeTodoCard(target) {
                        if (target) {
                            const targetCard = target.closest('.todo-card');
                            if (targetCard) {
                                return targetCard;
                            }
                        }
                        const block = currentBlockElement();
                        return block && block.closest ? block.closest('.todo-card') : null;
                    }
                    function activeBlockquote(target) {
                        if (target) {
                            const targetBlockquote = target.closest('blockquote');
                            if (targetBlockquote) {
                                return targetBlockquote;
                            }
                        }
                        const block = currentBlockElement();
                        return block && block.closest ? block.closest('blockquote') : null;
                    }
                    function replaceElementWithParagraph(element) {
                        const paragraph = document.createElement('p');
                        paragraph.innerHTML = '<br>';
                        element.replaceWith(paragraph);
                        focusEditableNode(paragraph);
                        return paragraph;
                    }
                    function handleTodoEnter(card, event) {
                        const textNode = card.querySelector('.todo-text');
                        event.preventDefault();
                        if (!textNode || isBlankEditableText(textNode)) {
                            replaceElementWithParagraph(card);
                        } else {
                            createTodoCardAfter(card);
                        }
                        notify();
                    }
                    function handleBlockquoteEnter(block, event) {
                        event.preventDefault();
                        if (isBlankEditableText(block)) {
                            replaceElementWithParagraph(block);
                        } else {
                            const nextQuote = document.createElement('blockquote');
                            nextQuote.innerHTML = '<br>';
                            block.insertAdjacentElement('afterend', nextQuote);
                            focusEditableNode(nextQuote);
                        }
                        notify();
                    }
                    function handleEmptyTodoBackspace(card, event) {
                        const textNode = card.querySelector('.todo-text');
                        if (!textNode || !isBlankEditableText(textNode)) {
                            return;
                        }
                        event.preventDefault();
                        replaceElementWithParagraph(card);
                        notify();
                    }
                    function handleTableAction(actionButton) {
                        const card = actionButton.closest('.table-card');
                        const table = card ? card.querySelector('table') : null;
                        const body = table ? table.querySelector('tbody') : null;
                        if (!table || !body) {
                            return;
                        }
                        const action = actionButton.getAttribute('data-table-action') || '';
                        const rows = Array.from(body.querySelectorAll('tr'));
                        const columnCount = rows[0] ? rows[0].children.length : 0;
                        if (action === 'add-row') {
                            const row = document.createElement('tr');
                            for (let index = 0; index < Math.max(1, columnCount); index += 1) {
                                const cell = document.createElement('td');
                                cell.innerHTML = '<br>';
                                row.appendChild(cell);
                            }
                            body.appendChild(row);
                        } else if (action === 'add-column') {
                            rows.forEach(function(row) {
                                const cell = document.createElement('td');
                                cell.innerHTML = '<br>';
                                row.appendChild(cell);
                            });
                        } else if (action === 'delete-row' && rows.length > 1) {
                            rows[rows.length - 1].remove();
                        } else if (action === 'delete-column' && columnCount > 1) {
                            rows.forEach(function(row) {
                                if (row.lastElementChild) {
                                    row.lastElementChild.remove();
                                }
                            });
                        }
                        notify();
                    }
                    editor.addEventListener('keydown', function(event) {
                        const target = event.target instanceof Element ? event.target : null;
                        const todoCard = activeTodoCard(target);
                        const blockquote = activeBlockquote(target);
                        if (event.key === 'Tab' && handleTabIndent(event)) {
                            return;
                        }
                        if (event.key === 'Enter' && todoCard) {
                            handleTodoEnter(todoCard, event);
                            return;
                        }
                        if (event.key === 'Enter' && blockquote) {
                            handleBlockquoteEnter(blockquote, event);
                            return;
                        }
                        if (event.key === 'Backspace' && todoCard) {
                            handleEmptyTodoBackspace(todoCard, event);
                        }
                    });
                    editor.addEventListener('input', function() {
                        rememberSelection();
                        notify();
                    });
                    editor.addEventListener('change', function(event) {
                        const target = event.target;
                        if (target && target.classList && target.classList.contains('todo-check')) {
                            const todoCard = target.closest('.todo-card');
                            if (todoCard) {
                                todoCard.classList.toggle('checked', target.checked);
                            }
                            notify();
                        }
                    });
                    editor.addEventListener('click', function(event) {
                        const target = event.target instanceof Element ? event.target : null;
                        if (!target) {
                            return;
                        }
                        const deleteButton = target.closest('.media-delete');
                        if (deleteButton) {
                            const card = deleteButton.closest('.file-card');
                            if (card) {
                                card.remove();
                                notify();
                            }
                            event.preventDefault();
                            event.stopPropagation();
                            return;
                        }
                        const playButton = target.closest('.media-play');
                        if (playButton) {
                            const card = playButton.closest('.audio-card');
                            const audio = card ? card.querySelector('audio') : null;
                            if (audio && window.$EDITOR_BRIDGE_NAME) {
                                const wasPlaying = card.classList.contains('playing');
                                resetAudioPlaybackUi();
                                if (!wasPlaying) {
                                    card.classList.add('playing');
                                }
                                const originalSource = audio.getAttribute('data-original-src')
                                    || audio.getAttribute('src')
                                    || (card ? card.getAttribute('data-url') || '' : '');
                                const managedFileId = card ? card.getAttribute('data-managed-file-id') || '' : '';
                                const name = card ? card.getAttribute('data-name') || '音频文件' : '音频文件';
                                window.$EDITOR_BRIDGE_NAME.onPlayAudio(name, originalSource, managedFileId);
                            }
                            event.preventDefault();
                            event.stopPropagation();
                            return;
                        }
                        const openButton = target.closest('.file-open');
                        if (openButton) {
                            const card = openButton.closest('.file-card');
                            const name = card ? card.getAttribute('data-name') || '附件' : '附件';
                            const url = card ? card.getAttribute('data-url') || '' : '';
                            const mimeType = card ? card.getAttribute('data-mime') || '' : '';
                            const managedFileId = card ? card.getAttribute('data-managed-file-id') || '' : '';
                            if (url && window.$EDITOR_BRIDGE_NAME) {
                                window.$EDITOR_BRIDGE_NAME.onOpenExternalFile(name, url, mimeType, managedFileId);
                            }
                            event.preventDefault();
                            event.stopPropagation();
                        }
                        const tableActionButton = target.closest('[data-table-action]');
                        if (tableActionButton) {
                            handleTableAction(tableActionButton);
                            event.preventDefault();
                            event.stopPropagation();
                        }
                    });
                    editor.addEventListener('focus', function() {
                        if (window.$EDITOR_BRIDGE_NAME) {
                            window.$EDITOR_BRIDGE_NAME.onEditorInteraction();
                        }
                    });
                    editor.addEventListener('keyup', rememberSelection);
                    editor.addEventListener('mouseup', rememberSelection);
                    editor.addEventListener('touchend', rememberSelection);
                    editor.addEventListener('blur', function() {
                        rememberSelection();
                        notify();
                    });
                    document.addEventListener('selectionchange', rememberSelection);
                    prepareManagedMedia();
                    setTimeout(notify, 80);
                    function resetAudioPlaybackUi() {
                        editor.querySelectorAll('.audio-card.playing').forEach(function(card) {
                            card.classList.remove('playing');
                        });
                    }
                })();
            </script>
        </body>
        </html>
    """.trimIndent()
}

private fun String.toPlainDocumentHtml(): String {
    return lines().joinToString("") { line ->
        if (line.isBlank()) {
            "<p><br></p>"
        } else {
            "<p>${line.escapeHtml()}</p>"
        }
    }
}

private fun String.sanitizeEditorHtml(): String {
    return replace(SCRIPT_TAG_REGEX, "")
}

private fun String.escapeHtml(): String {
    return replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
}

private fun String.escapeHtmlAttribute(): String {
    return escapeHtml().replace("\"", "&quot;")
}

private fun String.toComposeColor(): Color {
    val parsedColor = AndroidColor.parseColor(this)
    return Color(
        red = AndroidColor.red(parsedColor),
        green = AndroidColor.green(parsedColor),
        blue = AndroidColor.blue(parsedColor),
        alpha = AndroidColor.alpha(parsedColor)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteHistorySheet(
    histories: List<NoteHistory>,
    previewHistory: NoteHistory?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onPreview: (Int) -> Unit,
    onRestore: (Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .heightIn(max = 620.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("版本历史", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (histories.isNotEmpty()) {
                    Text(
                        text = "${histories.size} 个版本，可点选预览",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (histories.isEmpty()) {
                Text("暂无历史版本", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                histories.forEach { history ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (previewHistory?.version == history.version) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerLow
                                }
                            )
                            .clickable { onPreview(history.version) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "版本 ${history.version} · ${history.title}",
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                            Text(
                                text = history.changeSummary ?: history.saveType ?: "保存于 ${history.gmtCreate.orEmpty()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${history.content.orEmpty().count { !it.isWhitespace() }} 字",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(onClick = { onRestore(history.version) }) {
                            Text("恢复")
                        }
                    }
                }
                previewHistory?.let { history ->
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "预览版本 ${history.version}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = history.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = history.content.orEmpty().ifBlank { "这个版本没有正文内容" },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 180.dp)
                                .verticalScroll(rememberScrollState()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteAttachmentSheet(
    attachments: List<NoteAttachment>,
    isLoading: Boolean,
    isUploading: Boolean,
    activeAttachmentId: Long?,
    onUpload: () -> Unit,
    onRemove: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("附件管理", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                TextButton(
                    onClick = onUpload,
                    enabled = !isUploading && activeAttachmentId == null
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(if (isUploading) "上传中" else "上传")
                }
            }
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (attachments.isEmpty()) {
                Text("当前笔记暂无附件", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                attachments.forEach { attachment ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(attachment.fileName, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "${attachment.fileSize.readableFileSize()} · ${attachment.mimeType ?: "未知类型"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (activeAttachmentId == attachment.id) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            IconButton(
                                onClick = { onRemove(attachment.id) },
                                enabled = !isUploading && activeAttachmentId == null
                            ) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "移除附件",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun List<Notebook>.flattenNotebookTree(): List<Notebook> {
    return flatMap { notebook ->
        listOf(notebook) + notebook.children.flattenNotebookTree()
    }
}

private fun Long.readableFileSize(): String {
    if (this < FILE_SIZE_KB) {
        return "$this B"
    }
    val kb = this.toDouble() / FILE_SIZE_KB
    if (kb < FILE_SIZE_KB) {
        return "%.1f KB".format(kb)
    }
    val mb = kb / FILE_SIZE_KB
    return "%.1f MB".format(mb)
}

private fun String.toAbsoluteAssetUrl(): String {
    val trimmedUrl = trim()
    if (trimmedUrl.isBlank() || ASSET_SCHEME_REGEX.containsMatchIn(trimmedUrl)) {
        return trimmedUrl
    }
    val baseUrl = BuildConfig.BASE_URL.trimEnd('/')
    return if (trimmedUrl.startsWith("/")) {
        "$baseUrl$trimmedUrl"
    } else {
        "$baseUrl/$trimmedUrl"
    }
}

private fun String?.toEditorDateText(): String {
    return parseLocalDateTime(orEmpty())
        ?.format(DOCUMENT_DATE_FORMATTER)
        ?: LocalDateTime.now().format(DOCUMENT_DATE_FORMATTER)
}

private fun Float.formatLineHeight(): String {
    return when (this) {
        1.0f -> "1.0"
        1.5f -> "1.5"
        2.0f -> "2.0"
        else -> toString()
    }
}

private fun buildPublicNoteUrl(shareCode: String): String {
    return "${BuildConfig.BASE_URL.trimEnd('/')}/public/notes/$shareCode"
}

private fun parseEditorSnapshot(rawResult: String?): EditorSnapshot? {
    val raw = rawResult?.takeIf { it.isNotBlank() && it != "null" } ?: return null
    val jsonText = runCatching {
        when (val value = JSONTokener(raw).nextValue()) {
            is String -> value
            is JSONObject -> value.toString()
            else -> raw
        }
    }.getOrElse { raw }
    return runCatching {
        val json = JSONObject(jsonText)
        EditorSnapshot(
            text = json.optString("text"),
            html = json.optString("html").takeIf { it.isNotBlank() }
        )
    }.getOrNull()
}

private fun parseCollabPeers(rawJson: String): List<CollabPeer> {
    return runCatching {
        val array = JSONArray(rawJson)
        (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            val userId = item.optLong("userId", 0L)
            val clientId = item.optLong("clientId", 0L)
            val name = item.optString("name").takeIf { it.isNotBlank() } ?: "协作者"
            CollabPeer(
                clientId = clientId,
                userId = userId,
                name = name,
                color = item.optString("color").takeIf { it.isNotBlank() } ?: DEFAULT_COLLAB_COLOR,
                colorLight = item.optString("colorLight").takeIf { it.isNotBlank() } ?: DEFAULT_COLLAB_LIGHT_COLOR
            )
        }.distinctBy { peer ->
            peer.clientId.takeIf { it > 0 }?.toString()
                ?: peer.userId.takeIf { it > 0 }?.toString()
                ?: peer.name
        }
    }.getOrDefault(emptyList())
}

private fun UserProfile?.presenceColors(): Pair<String, String> {
    val userId = this?.id ?: 0L
    val paletteIndex = (abs(userId) % COLLAB_COLOR_PALETTE.size).toInt()
    return COLLAB_COLOR_PALETTE[paletteIndex]
}

private fun UserProfile.displayCollabName(): String {
    return nickname?.takeIf { it.isNotBlank() }
        ?: username.takeIf { it.isNotBlank() }
        ?: "Android"
}

private fun resolveEditorAudioUrl(
    spaceId: Long?,
    managedFileId: Long?,
    url: String
): String {
    if (spaceId != null && managedFileId != null) {
        return "${BuildConfig.BASE_URL.trimEnd('/')}/api/v1/spaces/$spaceId/files/$managedFileId/preview"
    }
    return url.toAbsoluteAssetUrl()
}

private suspend fun openEditorFile(
    context: Context,
    file: EditorExternalFile,
    spaceId: Long?,
    authToken: String?
) {
    val localUri = if (spaceId != null && file.managedFileId != null) {
        runCatching {
            withContext(Dispatchers.IO) {
                downloadEditorFileToCache(
                    context = context,
                    file = file,
                    spaceId = spaceId,
                    authToken = authToken
                )
            }
        }.getOrNull()
    } else {
        null
    }
    if (localUri == null) {
        openExternalUrl(context, file.url)
        return
    }
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(localUri, mimeTypeForFile(file.mimeType, file.name))
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
        .onFailure {
            Toast.makeText(context, "没有可打开该文件的应用", Toast.LENGTH_SHORT).show()
        }
}

private fun openExportedNoteFile(context: Context, file: NoteExportFile) {
    runCatching {
        val uri = cacheExportedNoteFile(context, file)
        val mimeType = mimeTypeForFile(file.contentType, file.fileName)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, mimeType)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.clipData = ClipData.newUri(context.contentResolver, file.fileName, uri)
        val chooser = Intent.createChooser(intent, "打开导出文件")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }.onFailure {
        Toast.makeText(context, "导出完成，但没有可打开该文件的应用", Toast.LENGTH_SHORT).show()
    }
}

private fun createExportDocumentIntent(file: NoteExportFile): Intent {
    return Intent(Intent.ACTION_CREATE_DOCUMENT)
        .addCategory(Intent.CATEGORY_OPENABLE)
        .setType(mimeTypeForFile(file.contentType, file.fileName))
        .putExtra(Intent.EXTRA_TITLE, file.fileName.toSafeFileName())
}

private fun writeExportedNoteFile(
    context: Context,
    file: NoteExportFile,
    targetUri: Uri
) {
    runCatching {
        val outputStream = context.contentResolver.openOutputStream(targetUri)
            ?: throw IllegalStateException("无法写入导出文件")
        outputStream.use { stream ->
            stream.write(file.bytes)
        }
    }.onSuccess {
        Toast.makeText(context, "已保存到本机", Toast.LENGTH_SHORT).show()
    }.onFailure {
        Toast.makeText(context, "保存导出文件失败", Toast.LENGTH_SHORT).show()
    }
}

private fun shareExportedNoteFile(context: Context, file: NoteExportFile) {
    runCatching {
        val uri = cacheExportedNoteFile(context, file)
        val mimeType = mimeTypeForFile(file.contentType, file.fileName)
        val intent = Intent(Intent.ACTION_SEND)
            .setType(mimeType)
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_SUBJECT, file.fileName)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.clipData = ClipData.newUri(context.contentResolver, file.fileName, uri)
        val chooser = Intent.createChooser(intent, "分享导出文件")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }.onFailure {
        Toast.makeText(context, "无法分享导出文件", Toast.LENGTH_SHORT).show()
    }
}

private fun cacheExportedNoteFile(context: Context, file: NoteExportFile): Uri {
    val cacheDir = File(context.cacheDir, NOTE_EXPORT_CACHE_DIR).apply { mkdirs() }
    val cacheFile = File(cacheDir, file.fileName.toSafeFileName())
    cacheFile.writeBytes(file.bytes)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cacheFile)
}

private fun downloadEditorFileToCache(
    context: Context,
    file: EditorExternalFile,
    spaceId: Long,
    authToken: String?
): Uri {
    val source = resolveEditorAudioUrl(spaceId, file.managedFileId, file.url)
    val connection = URL(source).openConnection() as HttpURLConnection
    return try {
        connection.connectTimeout = EDITOR_ASSET_CONNECT_TIMEOUT_MS
        connection.readTimeout = EDITOR_ASSET_READ_TIMEOUT_MS
        authToken?.takeIf { it.isNotBlank() }?.let { token ->
            connection.setRequestProperty(AUTHORIZATION_HEADER, "Bearer $token")
        }
        val responseCode = connection.responseCode
        if (responseCode !in EDITOR_ASSET_SUCCESS_CODES) {
            throw IllegalStateException("文件下载失败")
        }
        val cacheDir = File(context.cacheDir, EDITOR_FILE_CACHE_DIR).apply { mkdirs() }
        val cacheFile = File(cacheDir, file.name.toSafeFileName())
        connection.inputStream.use { inputStream ->
            cacheFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cacheFile)
    } finally {
        connection.disconnect()
    }
}

private fun playEditorAudio(
    context: Context,
    player: MediaPlayer,
    name: String,
    source: String,
    authToken: String?,
    requiresAuth: Boolean,
    onStop: () -> Unit
) {
    val headers = if (requiresAuth && !authToken.isNullOrBlank()) {
        mapOf(AUTHORIZATION_HEADER to "Bearer $authToken")
    } else {
        emptyMap()
    }
    player.setOnPreparedListener { preparedPlayer ->
        preparedPlayer.start()
        Toast.makeText(context, "正在播放 $name", Toast.LENGTH_SHORT).show()
    }
    player.setOnCompletionListener { completedPlayer ->
        completedPlayer.releaseSafely()
        onStop()
    }
    player.setOnErrorListener { failedPlayer, _, _ ->
        failedPlayer.releaseSafely()
        onStop()
        Toast.makeText(context, "音频播放失败", Toast.LENGTH_SHORT).show()
        true
    }
    runCatching {
        player.setDataSource(context, Uri.parse(source), headers)
        player.prepareAsync()
    }.onFailure {
        player.releaseSafely()
        onStop()
        Toast.makeText(context, "音频播放失败", Toast.LENGTH_SHORT).show()
    }
}

private fun MediaPlayer.releaseSafely() {
    runCatching { stop() }
    runCatching { reset() }
    runCatching { release() }
}

private fun fileKindLabel(mimeType: String?, fileName: String): String {
    return when (fileName.extension()) {
        "doc", "docx" -> "Word 文档"
        "xls", "xlsx" -> "Excel 表格"
        "ppt", "pptx" -> "PowerPoint 演示文稿"
        "pdf" -> "PDF 文档"
        "txt" -> "文本文件"
        "zip", "rar", "7z" -> "压缩文件"
        else -> when {
            mimeType?.startsWith(IMAGE_MIME_PREFIX, ignoreCase = true) == true -> "图片文件"
            mimeType?.startsWith(AUDIO_MIME_PREFIX, ignoreCase = true) == true -> "音频文件"
            mimeType?.startsWith("video/", ignoreCase = true) == true -> "视频文件"
            else -> "附件"
        }
    }
}

private fun fileIconText(mimeType: String?, fileName: String): String {
    return when (fileName.extension()) {
        "doc", "docx" -> "W"
        "xls", "xlsx" -> "X"
        "ppt", "pptx" -> "P"
        "pdf" -> "P"
        "txt" -> "T"
        else -> when {
            mimeType?.startsWith(IMAGE_MIME_PREFIX, ignoreCase = true) == true -> "I"
            mimeType?.startsWith(AUDIO_MIME_PREFIX, ignoreCase = true) == true -> "A"
            else -> "F"
        }
    }
}

private fun mimeTypeForFile(mimeType: String?, fileName: String): String {
    return mimeType?.takeIf { it.isNotBlank() && it != "audio/*" } ?: when (fileName.extension()) {
        "doc" -> "application/msword"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "xls" -> "application/vnd.ms-excel"
        "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        "ppt" -> "application/vnd.ms-powerpoint"
        "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        "pdf" -> "application/pdf"
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "webp" -> "image/webp"
        "txt" -> "text/plain"
        else -> BINARY_MIME_TYPE
    }
}

private fun String.toSafeFileName(): String {
    val sanitized = replace(UNSAFE_FILE_NAME_REGEX, "_").trim('_').takeIf { it.isNotBlank() }
    return sanitized ?: DEFAULT_EDITOR_FILE_NAME
}

private fun String.extension(): String {
    return substringAfterLast('.', "").lowercase()
}

private fun copyTextToClipboard(context: Context, label: String, text: String) {
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboardManager?.setPrimaryClip(
        ClipData.newPlainText(label, text)
    )
}

private fun hideSoftwareKeyboard(context: Context) {
    val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    val activity = context.findActivity()
    val view = activity?.currentFocus ?: activity?.window?.decorView
    view?.windowToken?.let { token ->
        inputMethodManager?.hideSoftInputFromWindow(token, 0)
    }
}

private fun openExternalUrl(context: Context, url: String) {
    val uri = Uri.parse(url)
    val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
        .onFailure {
            Toast.makeText(context, "无法打开该附件", Toast.LENGTH_SHORT).show()
        }
}

private fun Context.findActivity(): Activity? {
    var currentContext: Context? = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return currentContext as? Activity
}

private const val FILE_SIZE_KB = 1024
private const val ALL_FILE_TYPES = "*/*"
private const val IMAGE_FILE_TYPES = "image/*"
private const val AUDIO_FILE_TYPES = "audio/*"
private const val EDITOR_BRIDGE_NAME = "NotaskRichText"
private const val COLLAB_BRIDGE_NAME = "NativeCollab"
private const val COLLAB_LOG_TAG = "NotaskCollab"
private const val EDITOR_HTTPS_SCHEME = "https"
private const val EDITOR_HTTP_SCHEME = "http"
private const val COLLAB_KERNEL_SEGMENT = "__collab__"
private const val COLLAB_KERNEL_SCRIPT_NAME = "editor.js"
private const val COLLAB_KERNEL_SEGMENT_COUNT = 2
private const val COLLAB_KERNEL_ASSET_PATH = "notask_collab/editor.js"
private const val EDITOR_ASSET_MARKER = "__asset__"
private const val EDITOR_ASSET_SPACES_SEGMENT = "spaces"
private const val EDITOR_ASSET_FILES_SEGMENT = "files"
private const val EDITOR_ASSET_PREVIEW_SEGMENT = "preview"
private const val EDITOR_ASSET_SEGMENT_COUNT = 6
private const val EDITOR_ASSET_CONNECT_TIMEOUT_MS = 10_000
private const val EDITOR_ASSET_READ_TIMEOUT_MS = 30_000
private val EDITOR_ASSET_SUCCESS_CODES = 200..299
private const val HTML_MIME_TYPE = "text/html"
private const val JAVASCRIPT_MIME_TYPE = "application/javascript"
private const val UTF8_ENCODING = "UTF-8"
private const val BINARY_MIME_TYPE = "application/octet-stream"
private const val IMAGE_MIME_PREFIX = "image/"
private const val AUDIO_MIME_PREFIX = "audio/"
private const val RANGE_HEADER = "Range"
private const val AUTHORIZATION_HEADER = "Authorization"
private const val OK_RESPONSE_REASON = "OK"
private const val EDITOR_FILE_CACHE_DIR = "editor-files"
private const val NOTE_EXPORT_CACHE_DIR = "note-exports"
private const val DEFAULT_EDITOR_FILE_NAME = "notask-file"
private const val TODO_ITEM_HTML = "<section class=\"todo-card\"><input class=\"todo-check\" type=\"checkbox\" contenteditable=\"false\"><span class=\"todo-text\" contenteditable=\"true\"><br></span></section><p><br></p>"
private const val EDITOR_SNAPSHOT_SCRIPT = "window.NotaskEditor && window.NotaskEditor.snapshotContent ? window.NotaskEditor.snapshotContent() : null;"
private const val DEFAULT_FONT_SIZE = 16
private const val DEFAULT_LINE_HEIGHT = 1.5f
private const val MIN_FONT_SIZE = 12
private const val MAX_FONT_SIZE = 28
private const val FONT_SIZE_STEP = 2
private const val MIN_LINE_HEIGHT = 1.0f
private const val MAX_LINE_HEIGHT = 2.0f
private const val DEFAULT_TEXT_COLOR = "#1D1B20"
private const val DEFAULT_COLLAB_COLOR = "#3C79D0"
private const val DEFAULT_COLLAB_LIGHT_COLOR = "#D4E4FB"
private const val DEFAULT_LOCAL_COLLAB_NAME = "Android"
private const val MAX_VISIBLE_COLLAB_PEERS = 2
private val COLLAB_COLOR_PALETTE = listOf(
    "#C96B4A" to "#F9D5C9",
    "#2F8F83" to "#CDEFE8",
    "#7A58C1" to "#E0D7F7",
    "#B86B2B" to "#F7DFC8",
    "#3C79D0" to "#D4E4FB",
    "#BE4D72" to "#F8D2DE"
)
private const val DATA_MANAGED_FILE_ID = "data-managed-file-id"
private const val DATA_ATTACHMENT_ID = "data-attachment-id"
private const val DEFAULT_TOOLBAR_DRAWER_HEIGHT = 280
private const val MIN_TOOLBAR_DRAWER_HEIGHT = 180
private const val MIN_INSERT_TOOL_PANEL_HEIGHT = 68
private const val TOOL_PANEL_KEYBOARD_BUFFER = 20
private const val MIN_REMEMBERED_KEYBOARD_HEIGHT = 120
private const val KEYBOARD_RESTORE_HOLD_MS = 180L
private const val TOOL_SLOT_ANIMATION_MS = 180
private const val INSERT_SHEET_MAX_SCREEN_FRACTION = 0.5f
private const val DEFAULT_TABLE_SIZE = 3
private const val MAX_TABLE_SIZE = 9
private const val TABLE_PICKER_GRID_SIZE = 6
private const val TABLE_PICKER_PANEL_HEIGHT = 320
private val LINE_HEIGHT_OPTIONS = listOf(1.0f, 1.5f, 2.0f)
private val EDITOR_INTERCEPT_SCHEMES = setOf(EDITOR_HTTPS_SCHEME, EDITOR_HTTP_SCHEME)
private val FONT_FAMILY_OPTIONS = listOf(
    FontFamilyChoice("系统", "system-ui, -apple-system, BlinkMacSystemFont, sans-serif"),
    FontFamilyChoice("思源黑体", "'Noto Sans SC', 'Source Han Sans SC', sans-serif"),
    FontFamilyChoice("思源宋体", "'Noto Serif SC', 'Source Han Serif SC', serif"),
    FontFamilyChoice("霞鹜文楷", "'LXGW WenKai', 'KaiTi', serif"),
    FontFamilyChoice("阿里普惠", "'Alibaba PuHuiTi', 'MiSans', sans-serif")
)
private val FONT_COLOR_OPTIONS = listOf(
    "#1D1B20",
    "#D32F2F",
    "#F57C00",
    "#FBC02D",
    "#388E3C",
    "#1976D2",
    "#7B1FA2",
    "#607D8B"
)
private val ASSET_SCHEME_REGEX = Regex("^(https?:|content:|file:|blob:|data:)", RegexOption.IGNORE_CASE)
private val UNSAFE_FILE_NAME_REGEX = Regex("[\\\\/:*?\"<>|\\r\\n]+")
private val DOCUMENT_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
private val TOOLBAR_CARD_COLOR = Color(0xFFF5F5F5)
private val SCRIPT_TAG_REGEX = Regex("<script[\\s\\S]*?</script>", RegexOption.IGNORE_CASE)
