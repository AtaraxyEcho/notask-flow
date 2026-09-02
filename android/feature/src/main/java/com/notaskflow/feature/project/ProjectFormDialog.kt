package com.notaskflow.feature.project

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.notaskflow.domain.model.Project
import com.notaskflow.domain.model.ProjectSave
import com.notaskflow.feature.common.NotaskFilledTextField

@Composable
fun ProjectFormDialog(
    title: String,
    project: Project? = null,
    isSaving: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (ProjectSave) -> Unit
) {
    var name by remember(project?.id) { mutableStateOf(project?.name.orEmpty()) }
    var description by remember(project?.id) { mutableStateOf(project?.description.orEmpty()) }
    var coverColor by remember(project?.id) { mutableStateOf(project?.coverColor ?: PROJECT_COLORS.first()) }
    var coverImageUrl by remember(project?.id) { mutableStateOf(project?.coverImageUrl.orEmpty()) }
    val normalizedName = name.trim()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                NotaskFilledTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "项目名称",
                    placeholder = "输入项目名称",
                    singleLine = true
                )
                NotaskFilledTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = "项目说明",
                    placeholder = "补充项目目标和说明",
                    singleLine = false,
                    minHeight = 120.dp,
                    minLines = 3,
                    maxLines = 6
                )
                NotaskFilledTextField(
                    value = coverImageUrl,
                    onValueChange = { coverImageUrl = it },
                    label = "封面图片地址",
                    placeholder = "可选",
                    singleLine = true
                )
                Column {
                    Text(
                        text = "封面颜色",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PROJECT_COLORS.forEach { color ->
                            ColorSwatch(
                                value = color,
                                selected = coverColor == color,
                                onClick = { coverColor = color }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = normalizedName.isNotBlank() && !isSaving,
                onClick = {
                    onConfirm(
                        ProjectSave(
                            name = normalizedName,
                            description = description.trim().ifBlank { null },
                            coverColor = coverColor,
                            coverImageUrl = coverImageUrl.trim().ifBlank { null },
                            ownerUserId = project?.ownerUserId
                        )
                    )
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun ColorSwatch(value: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(value.toColor())
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Color.White
            )
        }
    }
}

private fun String.toColor(): Color {
    return runCatching {
        Color(AndroidColor.parseColor(this))
    }.getOrDefault(Color(0xFF2D6A4F))
}

private val PROJECT_COLORS = listOf(
    "#2D6A4F",
    "#1565C0",
    "#6A1B9A",
    "#C62828",
    "#F57C00"
)
