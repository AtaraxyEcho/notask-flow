package com.notaskflow.feature.file

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.webkit.WebView
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.notaskflow.data.BuildConfig
import com.notaskflow.domain.model.FileFolder
import com.notaskflow.domain.model.ManagedFile
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilePreviewRoute(
    spaceId: Long?,
    fileId: Long,
    onBack: () -> Unit,
    viewModel: FilePreviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showMoveDialog by remember { mutableStateOf(false) }

    LaunchedEffect(spaceId, fileId) {
        if (spaceId != null) {
            viewModel.load(spaceId, fileId)
        }
    }

    LaunchedEffect(uiState) {
        val downloadUrl = (uiState as? FilePreviewUiState.Content)?.file?.downloadUrl
        if (!downloadUrl.isNullOrBlank()) {
            openDownloadUrl(context, downloadUrl)
            viewModel.consumeDownloadUrl()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = (uiState as? FilePreviewUiState.Content)?.file?.displayName ?: "文件预览",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    val content = uiState as? FilePreviewUiState.Content
                    if (content != null) {
                        IconButton(
                            onClick = viewModel::download,
                            enabled = !content.isSaving
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = "下载文件")
                        }
                        IconButton(
                            onClick = { showMoveDialog = true },
                            enabled = !content.isSaving
                        ) {
                            Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = "移动文件")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (spaceId == null) {
                PreviewError(message = "请先选择空间")
                return@Box
            }
            when (val state = uiState) {
                FilePreviewUiState.Loading -> PreviewLoading()
                is FilePreviewUiState.Error -> PreviewError(message = state.message)
                is FilePreviewUiState.Content -> PreviewContent(state = state)
            }
        }
    }

    val content = uiState as? FilePreviewUiState.Content
    if (showMoveDialog && content != null) {
        FileMoveDialog(
            file = content.file,
            folders = content.folders,
            onMove = { folderId ->
                viewModel.move(folderId)
                showMoveDialog = false
            },
            onDismiss = { showMoveDialog = false }
        )
    }
}

@Composable
private fun PreviewLoading() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun PreviewError(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = message, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun PreviewContent(state: FilePreviewUiState.Content) {
    Column(modifier = Modifier.fillMaxSize()) {
        state.errorMessage?.let { message ->
            Text(
                text = message,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        when (val payload = state.payload) {
            is FilePreviewPayload.Image -> ImagePreview(url = payload.url, authToken = state.authToken)
            is FilePreviewPayload.Pdf -> PdfPreview(url = payload.url, authToken = state.authToken)
            is FilePreviewPayload.Text -> TextPreview(content = payload.content)
            is FilePreviewPayload.Html -> HtmlPreview(content = payload.content)
            is FilePreviewPayload.Link -> LinkPreview(file = state.file, url = payload.url, authToken = state.authToken)
        }
    }
}

private sealed interface ImageRenderState {
    data object Loading : ImageRenderState
    data class Rendered(val bitmap: Bitmap) : ImageRenderState
    data class Error(val message: String) : ImageRenderState
}

private sealed interface PdfRenderState {
    data object Loading : PdfRenderState
    data class Rendered(val bitmap: Bitmap) : PdfRenderState
    data class Error(val message: String) : PdfRenderState
}

@Composable
private fun ImagePreview(url: String, authToken: String?) {
    val renderState by produceState<ImageRenderState>(initialValue = ImageRenderState.Loading, key1 = url, key2 = authToken) {
        value = loadImageBitmap(url = url, authToken = authToken)
    }
    when (val state = renderState) {
        ImageRenderState.Loading -> PreviewLoading()
        is ImageRenderState.Error -> PreviewError(message = state.message)
        is ImageRenderState.Rendered -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Image(
                    bitmap = state.bitmap.asImageBitmap(),
                    contentDescription = "图片预览",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Composable
private fun TextPreview(content: String) {
    SelectionContainer {
        Text(
            text = content,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(16.dp),
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun PdfPreview(url: String?, authToken: String?) {
    if (url.isNullOrBlank()) {
        PreviewError(message = "PDF 预览地址暂不可用")
        return
    }
    val context = LocalContext.current
    val renderState by produceState<PdfRenderState>(initialValue = PdfRenderState.Loading, key1 = url, key2 = authToken) {
        value = renderPdfFirstPage(context = context, url = url, authToken = authToken)
    }
    when (val state = renderState) {
        PdfRenderState.Loading -> PreviewLoading()
        is PdfRenderState.Error -> PreviewError(message = state.message)
        is PdfRenderState.Rendered -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(12.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Image(
                    bitmap = state.bitmap.asImageBitmap(),
                    contentDescription = "PDF 第一页预览",
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.FillWidth
                )
            }
        }
    }
}

@Composable
private fun HtmlPreview(content: String) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                configureSecurePreviewWebView(this)
                loadDataWithBaseURL(null, content, HTML_MIME_TYPE, UTF_8, null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(null, content, HTML_MIME_TYPE, UTF_8, null)
        }
    )
}

@Composable
private fun LinkPreview(file: ManagedFile, url: String?, authToken: String?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = file.displayName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = file.displayTypeLabel(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(18.dp))
        Text(
            text = "当前文件类型暂不支持内嵌预览，可下载到临时缓存后使用系统应用打开。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = {
                if (url.isNullOrBlank()) {
                    return@Button
                }
                scope.launch {
                    openPreviewFile(context = context, file = file, url = url, authToken = authToken)
                }
            },
            enabled = !url.isNullOrBlank()
        ) {
            Text("打开文件")
        }
    }
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
            Column {
                Text(
                    text = "选择文件要移动到的目标文件夹。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { onMove(null) }, modifier = Modifier.fillMaxWidth()) {
                    Text("移动到全部文件")
                }
                folders.flattenFolders().forEach { folder ->
                    TextButton(onClick = { onMove(folder.id) }, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Spacer(Modifier.width((folder.depthIn(folders) * 14).dp))
                            Text("移动到 ${folder.name}")
                        }
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

private suspend fun loadImageBitmap(url: String, authToken: String?): ImageRenderState {
    return withContext(Dispatchers.IO) {
        runCatching {
            openAuthorizedConnection(url, authToken).use { connection ->
                connection.inputStream.use { input ->
                    BitmapFactory.decodeStream(input)
                } ?: throw IllegalStateException("图片解码失败")
            }
        }.fold(
            onSuccess = { bitmap -> ImageRenderState.Rendered(bitmap) },
            onFailure = { throwable -> ImageRenderState.Error(throwable.message ?: "图片预览失败") }
        )
    }
}

private suspend fun renderPdfFirstPage(context: Context, url: String, authToken: String?): PdfRenderState {
    return withContext(Dispatchers.IO) {
        runCatching {
            val file = File(context.cacheDir, "notask-preview-${url.hashCode()}.pdf")
            openAuthorizedConnection(url, authToken).use { connection ->
                connection.inputStream.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                PdfRenderer(descriptor).use { renderer ->
                    if (renderer.pageCount == 0) {
                        return@runCatching PdfRenderState.Error("PDF 暂无可预览页面")
                    }
                    renderer.openPage(0).use { page ->
                        val width = page.width.coerceAtLeast(MIN_PDF_PAGE_WIDTH)
                        val height = page.height.coerceAtLeast(MIN_PDF_PAGE_HEIGHT)
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(AndroidColor.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        PdfRenderState.Rendered(bitmap)
                    }
                }
            }
        }.getOrElse { throwable ->
            PdfRenderState.Error(throwable.message ?: "PDF 原生预览失败，使用下载地址")
        }
    }
}

private suspend fun openPreviewFile(
    context: Context,
    file: ManagedFile,
    url: String,
    authToken: String?
) {
    val localUri = runCatching {
        withContext(Dispatchers.IO) {
            downloadPreviewFile(context = context, file = file, url = url, authToken = authToken)
        }
    }.getOrElse {
        Toast.makeText(context, "文件打开失败，请稍后重试", Toast.LENGTH_SHORT).show()
        return
    }
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(localUri, file.mimeTypeForOpen())
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
        .onFailure {
            Toast.makeText(context, "没有可打开该文件的应用", Toast.LENGTH_SHORT).show()
        }
}

private fun downloadPreviewFile(
    context: Context,
    file: ManagedFile,
    url: String,
    authToken: String?
): Uri {
    val cacheDir = File(context.cacheDir, PREVIEW_CACHE_DIR).apply { mkdirs() }
    val cacheFile = File(cacheDir, file.displayName.toSafeFileName())
    openAuthorizedConnection(url, authToken).use { connection ->
        connection.inputStream.use { input ->
            cacheFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cacheFile)
}

private fun openAuthorizedConnection(url: String, authToken: String?): HttpURLConnection {
    val connection = URL(url).openConnection() as HttpURLConnection
    connection.connectTimeout = NETWORK_TIMEOUT_MS
    connection.readTimeout = NETWORK_TIMEOUT_MS
    authToken?.takeIf { it.isNotBlank() }?.let { token ->
        connection.setRequestProperty(AUTHORIZATION_HEADER, "Bearer $token")
    }
    val responseCode = connection.responseCode
    if (responseCode !in SUCCESS_HTTP_CODES) {
        connection.disconnect()
        throw IllegalStateException("文件请求失败")
    }
    return connection
}

private fun openDownloadUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url.toAbsoluteUrl()))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching {
        context.startActivity(intent)
    }.onFailure {
        Toast.makeText(context, "无法打开下载链接", Toast.LENGTH_SHORT).show()
    }
}

private inline fun <T> HttpURLConnection.use(block: (HttpURLConnection) -> T): T {
    return try {
        block(this)
    } finally {
        disconnect()
    }
}

private fun ManagedFile.displayTypeLabel(): String {
    return when (displayName.extension()) {
        "doc", "docx" -> "Word 文档"
        "xls", "xlsx" -> "Excel 表格"
        "ppt", "pptx" -> "PowerPoint 演示文稿"
        "pdf" -> "PDF 文档"
        "txt" -> "文本文件"
        else -> mimeType?.takeIf { it.isNotBlank() } ?: "附件"
    }
}

private fun ManagedFile.mimeTypeForOpen(): String {
    return mimeType?.takeIf { it.isNotBlank() } ?: when (displayName.extension()) {
        "doc" -> "application/msword"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "xls" -> "application/vnd.ms-excel"
        "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        "ppt" -> "application/vnd.ms-powerpoint"
        "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        "pdf" -> "application/pdf"
        "txt" -> "text/plain"
        else -> "application/octet-stream"
    }
}

private fun String.toSafeFileName(): String {
    return replace(UNSAFE_FILE_NAME_REGEX, "_").trim('_').takeIf { it.isNotBlank() } ?: DEFAULT_FILE_NAME
}

private fun String.extension(): String {
    return substringAfterLast('.', "").lowercase()
}

private fun String.toAbsoluteUrl(): String {
    val value = trim()
    if (value.startsWith("http://") || value.startsWith("https://")) {
        return value
    }
    val baseUrl = BuildConfig.BASE_URL.trimEnd('/')
    return if (value.startsWith("/")) {
        "$baseUrl$value"
    } else {
        "$baseUrl/$value"
    }
}

private fun List<FileFolder>.flattenFolders(): List<FileFolder> {
    return flatMap { folder -> listOf(folder) + folder.children.flattenFolders() }
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

@Suppress("DEPRECATION")
private fun configureSecurePreviewWebView(webView: WebView) {
    webView.settings.javaScriptEnabled = false
    webView.settings.domStorageEnabled = false
    webView.settings.allowFileAccess = false
    webView.settings.allowContentAccess = false
    webView.settings.allowFileAccessFromFileURLs = false
    webView.settings.allowUniversalAccessFromFileURLs = false
    webView.clearCache(false)
}

private const val HTML_MIME_TYPE = "text/html"
private const val UTF_8 = "UTF-8"
private const val MIN_PDF_PAGE_WIDTH = 1
private const val MIN_PDF_PAGE_HEIGHT = 1
private const val PREVIEW_CACHE_DIR = "file-preview"
private const val DEFAULT_FILE_NAME = "notask-file"
private const val AUTHORIZATION_HEADER = "Authorization"
private const val NETWORK_TIMEOUT_MS = 30_000
private val SUCCESS_HTTP_CODES = 200..299
private val UNSAFE_FILE_NAME_REGEX = Regex("[\\\\/:*?\"<>|\\r\\n]+")
