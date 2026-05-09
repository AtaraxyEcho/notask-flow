package com.notaskflow.feature.file

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

data class FileItem(
    val id: Long, val name: String, val isFolder: Boolean,
    val mimeType: String? = null, val size: String? = null,
    val updatedAt: String? = null, val uploaderName: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileBrowserRoute(modifier: Modifier = Modifier, onUploadClick: () -> Unit = {}) {
    val recentFiles = remember {
        listOf(
            FileItem(1, "Tax_Return_2023.pdf", false, "application/pdf", "1.2 MB", "2h ago", "Alex"),
            FileItem(2, "Cabin_Morning.jpg", false, "image/jpeg", "3.5 MB", "5h ago", "Alex"),
            FileItem(3, "Q2_Report.docx", false, "application/msword", "890 KB", "1d ago", "Sarah"),
            FileItem(4, "Meeting_Notes.md", false, "text/markdown", "12 KB", "2d ago", "Alex")
        )
    }
    val folderFiles = remember {
        listOf(
            FileItem(5, "项目文档", true, updatedAt = "更新于 2026-05-08"),
            FileItem(6, "设计稿", true, updatedAt = "更新于 2026-05-07"),
            FileItem(7, "API文档.md", false, "text/markdown", "32 KB", "2026-05-05", "王五"),
            FileItem(8, "产品截图.png", false, "image/png", "856 KB", "2026-05-08", "李四")
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
        ) {
            // 最近文件 — 横向滚动卡片
            item {
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Recent Files", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = { }) { Text("See all", color = MaterialTheme.colorScheme.primary) }
                }
                Spacer(Modifier.height(12.dp))
            }
            item {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    recentFiles.forEach { f -> FileCard(f) }
                }
                Spacer(Modifier.height(32.dp))
            }

            // 文件夹 + 所有文件列表
            item {
                Text("All Files", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
            }
            items(folderFiles) { f -> FileListItem(f) }

            item { Spacer(Modifier.height(80.dp)) }
        }

        FloatingActionButton(
            onClick = onUploadClick,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Filled.Add, "上传文件", tint = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

@Composable
private fun FileCard(file: FileItem) {
    Card(
        Modifier.width(180.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            // 预览区
            Box(Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = fileTypeIcon(file), contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = fileTypeColor(file).copy(alpha = 0.5f)
                )
            }
            Column(Modifier.padding(12.dp)) {
                Text(file.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                file.updatedAt?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

@Composable
private fun FileListItem(file: FileItem) {
    Column {
        Row(Modifier.fillMaxWidth().clickable { }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(fileTypeIcon(file), null, Modifier.size(24.dp), tint = fileTypeColor(file))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(file.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!file.isFolder) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        file.size?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        file.updatedAt?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        file.uploaderName?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
                    }
                } else {
                    file.updatedAt?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            Icon(Icons.Filled.ChevronRight, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    }
}

private fun fileTypeIcon(file: FileItem): ImageVector = when {
    file.isFolder -> Icons.Filled.Folder
    file.mimeType?.startsWith("image/") == true -> Icons.Filled.Image
    file.mimeType == "application/pdf" -> Icons.Filled.PictureAsPdf
    else -> Icons.Filled.InsertDriveFile
}

@Composable
private fun fileTypeColor(file: FileItem) = when {
    file.isFolder -> MaterialTheme.colorScheme.primary
    file.mimeType?.startsWith("image/") == true -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
    file.mimeType == "application/pdf" -> androidx.compose.ui.graphics.Color(0xFFF44336)
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}
