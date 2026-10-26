package com.notaskflow.feature.task

import com.notaskflow.feature.R
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.rememberCoroutineScope
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.notaskflow.core.common.formatDateTimeText
import com.notaskflow.data.BuildConfig
import com.notaskflow.domain.model.Task
import com.notaskflow.domain.model.TaskAttachment
import com.notaskflow.domain.model.TaskComment
import com.notaskflow.domain.model.TaskMember
import com.notaskflow.domain.model.TaskMemberStatus
import com.notaskflow.domain.model.TaskMode
import com.notaskflow.domain.model.TaskStatus
import com.notaskflow.domain.model.SpaceMember
import com.notaskflow.feature.common.NotaskFilledTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailRoute(
    spaceId: Long? = null,
    taskId: Long,
    isTeamSpace: Boolean = false,
    currentUserId: Long? = null,
    onBack: () -> Unit,
    viewModel: TaskDetailViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    var completingMemberId by remember { mutableStateOf<Long?>(null) }
    var completionRemark by remember { mutableStateOf("") }
    var claimResponsibility by remember { mutableStateOf("") }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var showMentionSheet by remember { mutableStateOf(false) }
    var showDeleteTaskDialog by remember { mutableStateOf(false) }
    var showEditTaskDialog by remember { mutableStateOf(false) }
    var editTitle by remember { mutableStateOf("") }
    var editDescription by remember { mutableStateOf("") }
    val attachmentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri -> uri?.let(viewModel::uploadAttachment) }
    )

    LaunchedEffect(spaceId, taskId, isTeamSpace) {
        if (spaceId != null) {
            viewModel.load(spaceId, taskId, isTeamSpace)
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                TaskDetailEffect.Deleted -> onBack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.task?.title ?: "任务详情",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val task = uiState.task
                            editTitle = task?.title.orEmpty()
                            editDescription = task?.description.orEmpty()
                            showEditTaskDialog = true
                        },
                        enabled = uiState.task != null && !uiState.isSubmitting
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = "编辑任务")
                    }
                    IconButton(
                        onClick = { showDeleteTaskDialog = true },
                        enabled = uiState.task != null && !uiState.isSubmitting
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "删除任务",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        if (spaceId == null) {
            StateText(stringResource(R.string.common_select_space_first), modifier = Modifier.padding(padding))
            return@Scaffold
        }
        if (uiState.isLoading && uiState.task == null) {
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

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            uiState.task?.let { task ->
                item { TaskOverviewCard(task = task) }
                item {
                    StatusActionRow(
                        targets = uiState.availableStatusTargets,
                        enabled = !uiState.isSubmitting,
                        onTargetClick = viewModel::updateStatus
                    )
                }
                if (task.mode == TaskMode.OPEN && (task.status == TaskStatus.PENDING || task.status == TaskStatus.OPEN)) {
                    item {
                        ClaimCard(
                            value = claimResponsibility,
                            enabled = !uiState.isSubmitting,
                            onValueChange = { claimResponsibility = it },
                            onSubmit = {
                                viewModel.claim(claimResponsibility)
                                claimResponsibility = ""
                            }
                        )
                    }
                }
                if (task.members.isNotEmpty()) {
                    item {
                        Text(stringResource(R.string.task_member_duties), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    items(task.members, key = { member -> "task-member-${member.id}" }) { member ->
                        TaskMemberRow(
                            member = member,
                            enabled = !uiState.isSubmitting,
                            onStart = { viewModel.startMember(member.id) },
                            onComplete = {
                                completingMemberId = member.id
                                completionRemark = ""
                            }
                        )
                    }
                }
                item {
                    TaskAttachmentSummaryCard(
                        attachmentCount = uiState.attachments.size,
                        isUploading = uiState.isAttachmentUploading,
                        onClick = {
                            showAttachmentSheet = true
                            viewModel.loadAttachments()
                        }
                    )
                }
            }

            uiState.errorMessage?.let { message ->
                item {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            val interactionTitle = if (uiState.isTeamSpace) "评论" else "记录"
            item {
                Text(interactionTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            if (uiState.comments.isEmpty()) {
                item { StateText(if (uiState.isTeamSpace) "暂无评论" else "暂无记录") }
            } else {
                items(uiState.comments, key = { comment -> "task-comment-${comment.id}" }) { comment ->
                    CommentCard(
                        comment = comment,
                        isTeamSpace = uiState.isTeamSpace,
                        members = uiState.members
                    )
                }
            }
            item {
                CommentInput(
                    value = uiState.commentInput,
                    isTeamSpace = uiState.isTeamSpace,
                    members = uiState.members,
                    currentUserId = currentUserId,
                    showMentionPicker = showMentionSheet,
                    enabled = !uiState.isSubmitting,
                    onValueChange = viewModel::updateCommentInput,
                    onAtTriggered = { showMentionSheet = true },
                    onMentionDismiss = { showMentionSheet = false },
                    onMentionSelected = { member ->
                        viewModel.toggleMention(member)
                        showMentionSheet = false
                    },
                    onSubmit = viewModel::addComment
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    completingMemberId?.let { memberId ->
        AlertDialog(
            onDismissRequest = { completingMemberId = null },
            title = { Text(stringResource(R.string.task_complete_duty)) },
            text = {
                NotaskFilledTextField(
                    value = completionRemark,
                    onValueChange = { completionRemark = it },
                    label = if (uiState.isTeamSpace) "完成说明" else "完成记录",
                    placeholder = if (uiState.isTeamSpace) "补充说明，可选" else "记录一下完成情况，可选",
                    minHeight = 120.dp,
                    singleLine = false,
                    minLines = 2,
                    maxLines = 5
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.completeMember(memberId, completionRemark)
                        completingMemberId = null
                    },
                    enabled = !uiState.isSubmitting
                ) {
                    Text(stringResource(R.string.common_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { completingMemberId = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    if (showAttachmentSheet) {
        TaskAttachmentSheet(
            attachments = uiState.attachments,
            isLoading = uiState.isAttachmentLoading,
            isUploading = uiState.isAttachmentUploading,
            activeAttachmentId = uiState.activeAttachmentId,
            onUpload = { attachmentPicker.launch(arrayOf(ALL_FILE_TYPES)) },
            onOpen = { attachment ->
                scope.launch {
                    openTaskAttachment(context, attachment, uiState.authToken)
                }
            },
            onRemove = viewModel::unbindAttachment,
            onDismiss = { showAttachmentSheet = false }
        )
    }

    if (showEditTaskDialog) {
        AlertDialog(
            onDismissRequest = { showEditTaskDialog = false },
            title = { Text(stringResource(R.string.task_edit)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    NotaskFilledTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = "任务标题",
                        placeholder = "填写任务标题",
                        singleLine = true
                    )
                    NotaskFilledTextField(
                        value = editDescription,
                        onValueChange = { editDescription = it },
                        label = "任务说明",
                        placeholder = "填写任务说明",
                        minHeight = 120.dp,
                        singleLine = false,
                        minLines = 3,
                        maxLines = 6
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateTask(editTitle, editDescription)
                        showEditTaskDialog = false
                    },
                    enabled = !uiState.isSubmitting
                ) {
                    Text(stringResource(R.string.common_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditTaskDialog = false }, enabled = !uiState.isSubmitting) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    if (showDeleteTaskDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteTaskDialog = false },
            title = { Text(stringResource(R.string.task_delete_title)) },
            text = { Text(stringResource(R.string.task_delete_confirm)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTask()
                        showDeleteTaskDialog = false
                    },
                    enabled = !uiState.isSubmitting
                ) {
                    Text(stringResource(R.string.common_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteTaskDialog = false }, enabled = !uiState.isSubmitting) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}

@Composable
private fun TaskOverviewCard(task: Task) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(task.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(
                task.description.orEmpty().ifBlank { "暂无任务说明" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { }, label = { Text(task.status.label()) })
                AssistChip(onClick = { }, label = { Text(task.priority.name) })
            }
            Text(
                text = task.projectName ?: "未归属项目",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatDateTimeText(task.deadline).ifBlank { "无截止时间" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatusActionRow(
    targets: List<TaskStatus>,
    enabled: Boolean,
    onTargetClick: (TaskStatus) -> Unit
) {
    if (targets.isEmpty()) {
        Text(
            text = "当前任务暂无可执行状态动作",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        targets.forEach { target ->
            Button(
                onClick = { onTargetClick(target) },
                enabled = enabled
            ) {
                Text(target.actionLabel())
            }
        }
    }
}

@Composable
private fun ClaimCard(
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.task_claim_open), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            NotaskFilledTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                label = "我的职责",
                placeholder = "填写你要负责的内容",
                minHeight = 120.dp,
                singleLine = false,
                minLines = 2,
                maxLines = 4
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(onClick = onSubmit, enabled = enabled) {
                    Text(stringResource(R.string.task_claim))
                }
            }
        }
    }
}

@Composable
private fun TaskMemberRow(
    member: TaskMember,
    enabled: Boolean,
    onStart: () -> Unit,
    onComplete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(member.username, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(
                    text = member.responsibility.orEmpty().ifBlank { "未填写职责" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = member.status.name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            when (member.status) {
                TaskMemberStatus.PENDING -> {
                    TextButton(onClick = onStart, enabled = enabled) {
                        Text(stringResource(R.string.task_start))
                    }
                }
                TaskMemberStatus.IN_PROGRESS -> {
                    TextButton(onClick = onComplete, enabled = enabled) {
                        Text(stringResource(R.string.task_complete))
                    }
                }
                TaskMemberStatus.COMPLETED -> Unit
            }
        }
    }
}

@Composable
private fun CommentCard(
    comment: TaskComment,
    isTeamSpace: Boolean,
    members: List<SpaceMember>
) {
    val mentionedNames = comment.mentionUserIds.mapNotNull { userId ->
        members.firstOrNull { member -> member.userId == userId }?.displayName()
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = if (isTeamSpace) comment.username else "记录",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = formatDateTimeText(comment.gmtCreate),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(comment.content, style = MaterialTheme.typography.bodyMedium)
            if (mentionedNames.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "提醒 ${mentionedNames.joinToString("、")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun CommentInput(
    value: String,
    isTeamSpace: Boolean,
    members: List<SpaceMember>,
    currentUserId: Long?,
    showMentionPicker: Boolean,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    onAtTriggered: () -> Unit,
    onMentionDismiss: () -> Unit,
    onMentionSelected: (SpaceMember) -> Unit,
    onSubmit: () -> Unit
) {
    val mentionMembers = members.filter { member -> currentUserId == null || member.userId != currentUserId }
    var fieldValue by remember { mutableStateOf(TextFieldValue(value, selection = TextRange(value.length))) }
    LaunchedEffect(value) {
        if (fieldValue.text != value) {
            fieldValue = TextFieldValue(value, selection = TextRange(value.length))
        }
    }
    Box {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            NotaskFilledTextField(
                value = fieldValue,
                onValueChange = { nextValue ->
                    val previousText = fieldValue.text
                    fieldValue = nextValue
                    onValueChange(nextValue.text)
                    when {
                        isTeamSpace && enabled && nextValue.text.length > previousText.length && nextValue.text.endsWith("@") -> {
                            onAtTriggered()
                        }
                        showMentionPicker && !nextValue.text.endsWith("@") -> onMentionDismiss()
                    }
                },
                enabled = enabled,
                label = if (isTeamSpace) "添加评论" else "添加记录",
                placeholder = if (isTeamSpace) "输入评论，输入 @ 选择成员" else "记录进展、想法或处理结果",
                minHeight = 120.dp,
                singleLine = false,
                minLines = 2,
                maxLines = 5
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onSubmit, enabled = enabled) {
                    Text(if (isTeamSpace) "发送" else "记录")
                }
            }
        }
        if (showMentionPicker && isTeamSpace && enabled) {
            MentionPickerBubble(
                members = mentionMembers,
                onSelect = onMentionSelected,
                onDismiss = onMentionDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(y = 30.dp)
                    .zIndex(2f)
            )
        }
    }
}

@Composable
private fun MentionPickerBubble(
    members: List<SpaceMember>,
    onSelect: (SpaceMember) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.widthIn(min = 180.dp, max = 260.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 10.dp
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.task_select_member), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.common_close))
                }
            }
            if (members.isEmpty()) {
                Text(
                    text = "没有可提醒的成员",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 220.dp)) {
                    items(members, key = { member -> "mention-${member.userId}" }) { member ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelect(member) }
                                .padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = member.displayName().take(1),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = member.displayName(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = member.roleName,
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
        }
    }
}

@Composable
private fun TaskAttachmentSummaryCard(
    attachmentCount: Int,
    isUploading: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.AttachFile,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.task_attachments), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    text = if (isUploading) "附件上传中" else "${attachmentCount} 个附件",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onClick) {
                Text(stringResource(R.string.task_manage))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskAttachmentSheet(
    attachments: List<TaskAttachment>,
    isLoading: Boolean,
    isUploading: Boolean,
    activeAttachmentId: Long?,
    onUpload: () -> Unit,
    onOpen: (TaskAttachment) -> Unit,
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
                Text(stringResource(R.string.task_attachments), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
                Text(stringResource(R.string.task_no_attachments), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            TextButton(onClick = { onOpen(attachment) }) {
                                Text(stringResource(R.string.task_open))
                            }
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

@Composable
private fun StateText(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

private fun TaskStatus.actionLabel(): String {
    return when (this) {
        TaskStatus.PENDING -> "设为待开始"
        TaskStatus.OPEN -> "设为待开始"
        TaskStatus.IN_PROGRESS -> "开始处理"
        TaskStatus.COMPLETED -> "标记完成"
        TaskStatus.CANCELLED -> "取消任务"
    }
}

private fun SpaceMember.displayName(): String {
    return nickname?.takeIf { it.isNotBlank() } ?: username
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

/**
 * 下载任务附件到本地缓存后通过 FileProvider 打开。
 *
 * 附件下载接口受 {@code Authorization} 保护，直接拉起外部浏览器会 401，
 * 因此统一走带鉴权头的下载链路。
 */
private suspend fun openTaskAttachment(
    context: Context,
    attachment: TaskAttachment,
    authToken: String?
) {
    val url = attachment.downloadUrl?.takeIf { it.isNotBlank() }?.toAbsoluteUrl()
    if (url == null) {
        Toast.makeText(context, "当前附件暂无可打开链接", Toast.LENGTH_SHORT).show()
        return
    }
    val localUri = runCatching {
        withContext(Dispatchers.IO) {
            downloadAttachmentToCache(context, attachment, url, authToken)
        }
    }.getOrElse {
        Toast.makeText(context, "附件下载失败，请稍后重试", Toast.LENGTH_SHORT).show()
        return
    }
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(localUri, attachment.mimeTypeOrDefault())
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
        .onFailure {
            Toast.makeText(context, "没有可打开该附件的应用", Toast.LENGTH_SHORT).show()
        }
}

/**
 * 将任务附件下载到应用缓存目录。
 *
 * @param context 上下文
 * @param attachment 附件信息
 * @param url 下载地址
 * @param authToken 鉴权令牌
 * @return FileProvider 可访问的 Uri
 */
private fun downloadAttachmentToCache(
    context: Context,
    attachment: TaskAttachment,
    url: String,
    authToken: String?
): android.net.Uri {
    val cacheDir = File(context.cacheDir, ATTACHMENT_CACHE_DIR).apply { mkdirs() }
    val safeName = attachment.fileName.ifBlank { "attachment-${attachment.id}" }.let { raw ->
        raw.replace(Regex("[\\/:*?\"<>|\\r\\n]"), "_")
    }
    val cacheFile = File(cacheDir, "${attachment.id}-$safeName")
    val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
    connection.connectTimeout = 15000
    connection.readTimeout = 30000
    authToken?.takeIf { it.isNotBlank() }?.let { token ->
        connection.setRequestProperty("Authorization", "Bearer $token")
    }
    if (connection.responseCode !in 200..299) {
        connection.disconnect()
        throw IllegalStateException("附件请求失败")
    }
    connection.inputStream.use { input ->
        cacheFile.outputStream().use { output -> input.copyTo(output) }
    }
    connection.disconnect()
    return androidx.core.content.FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        cacheFile
    )
}

/**
 * 解析附件 MIME 类型，缺失时回退为通用二进制流。
 *
 * @return MIME 类型
 */
private fun TaskAttachment.mimeTypeOrDefault(): String {
    return mimeType?.takeIf { it.isNotBlank() } ?: "application/octet-stream"
}

private fun String.toAbsoluteUrl(): String {
    val value = trim()
    if (value.startsWith("http://", ignoreCase = true) || value.startsWith("https://", ignoreCase = true)) {
        return value
    }
    val baseUrl = BuildConfig.BASE_URL.trimEnd('/')
    return if (value.startsWith("/")) {
        "$baseUrl$value"
    } else {
        "$baseUrl/$value"
    }
}

private const val FILE_SIZE_KB = 1024
private const val ALL_FILE_TYPES = "*/*"

private const val ATTACHMENT_CACHE_DIR = "task-attachments"
