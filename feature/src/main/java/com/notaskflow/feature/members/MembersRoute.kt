package com.notaskflow.feature.members

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.notaskflow.core.common.formatDateTimeText
import com.notaskflow.core.ui.theme.TeamColors
import com.notaskflow.domain.model.SpaceInvite
import com.notaskflow.domain.model.SpaceJoinApplication
import com.notaskflow.domain.model.SpaceMember

private data class RoleChoice(
    val code: String,
    val label: String
)

@Composable
fun MembersRoute(
    modifier: Modifier = Modifier,
    spaceId: Long? = null,
    onInviteMember: () -> Unit = {},
    viewModel: MembersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showInviteDialog by remember { mutableStateOf(false) }
    var selectedMember by remember { mutableStateOf<SpaceMember?>(null) }
    var pendingRemoveMember by remember { mutableStateOf<SpaceMember?>(null) }
    var approvingApplication by remember { mutableStateOf<SpaceJoinApplication?>(null) }
    var rejectingApplication by remember { mutableStateOf<SpaceJoinApplication?>(null) }
    var rejectReason by remember { mutableStateOf("") }

    LaunchedEffect(spaceId) {
        if (spaceId != null) {
            viewModel.load(spaceId)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            item {
                Spacer(Modifier.height(12.dp))
                Text(
                    "团队成员",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TeamColors.primary
                )
                Text(
                    "${uiState.members.size} 名成员",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                uiState.errorMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                Spacer(Modifier.height(20.dp))
            }
            if (uiState.pendingApplications.isNotEmpty()) {
                item {
                    Text(
                        "待审核申请",
                        style = MaterialTheme.typography.labelLarge,
                        color = TeamColors.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(10.dp))
                }
                items(uiState.pendingApplications, key = { application -> "join-application-${application.id}" }) { application ->
                    JoinApplicationCard(
                        application = application,
                        onApprove = { approvingApplication = application },
                        onReject = {
                            rejectReason = ""
                            rejectingApplication = application
                        }
                    )
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
            if (spaceId == null) {
                item { StateText("请先选择空间") }
            } else if (uiState.isLoading && uiState.members.isEmpty()) {
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
            } else if (uiState.errorMessage != null && uiState.members.isEmpty()) {
                item { StateText(uiState.errorMessage ?: "成员加载失败") }
            } else if (uiState.members.isEmpty()) {
                item { StateText("暂无成员") }
            } else {
                uiState.members.groupBy { it.roleName.ifBlank { it.roleCode.ifBlank { "成员" } } }
                    .forEach { (role, members) ->
                        item {
                            Text(
                                role,
                                style = MaterialTheme.typography.labelLarge,
                                color = TeamColors.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(10.dp))
                        }
                        items(members, key = { member -> "space-member-${member.userId}" }) { member ->
                            MemberCard(member = member, onClick = { selectedMember = member })
                        }
                        item { Spacer(Modifier.height(10.dp)) }
                    }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }

        if (uiState.isMutating) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(24.dp),
                strokeWidth = 2.dp
            )
        }

        FloatingActionButton(
            onClick = {
                onInviteMember()
                showInviteDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = TeamColors.primary,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Filled.Add, "邀请成员", tint = TeamColors.onPrimary)
        }
    }

    if (showInviteDialog) {
        InviteMemberDialog(
            invite = uiState.invite,
            isSaving = uiState.isMutating,
            onCreateInvite = viewModel::createInvite,
            onDismiss = {
                viewModel.clearInvite()
                showInviteDialog = false
            }
        )
    }

    selectedMember?.let { member ->
        MemberActionDialog(
            member = member,
            isSaving = uiState.isMutating,
            onUpdateRole = { roleCode ->
                viewModel.updateRole(member, roleCode)
                selectedMember = null
            },
            onRemove = {
                pendingRemoveMember = member
                selectedMember = null
            },
            onDismiss = { selectedMember = null }
        )
    }

    pendingRemoveMember?.let { member ->
        AlertDialog(
            onDismissRequest = { pendingRemoveMember = null },
            title = { Text("移除成员") },
            text = { Text("确定将 ${member.displayName()} 移出当前团队空间吗？该成员将无法继续访问团队内容。") },
            confirmButton = {
                Button(
                    enabled = !uiState.isMutating,
                    onClick = {
                        viewModel.removeMember(member)
                        pendingRemoveMember = null
                    }
                ) {
                    Text("移除")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoveMember = null }, enabled = !uiState.isMutating) {
                    Text("取消")
                }
            }
        )
    }

    approvingApplication?.let { application ->
        AlertDialog(
            onDismissRequest = { approvingApplication = null },
            title = { Text("通过申请") },
            text = { Text("确认通过 ${application.applicantUsername.ifBlank { "该用户" }} 的加入申请吗？") },
            confirmButton = {
                Button(
                    enabled = !uiState.isMutating,
                    onClick = {
                        viewModel.approveApplication(application)
                        approvingApplication = null
                    }
                ) {
                    Text("通过")
                }
            },
            dismissButton = {
                TextButton(onClick = { approvingApplication = null }, enabled = !uiState.isMutating) {
                    Text("取消")
                }
            }
        )
    }

    rejectingApplication?.let { application ->
        AlertDialog(
            onDismissRequest = { rejectingApplication = null },
            title = { Text("拒绝申请") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("确认拒绝 ${application.applicantUsername.ifBlank { "该用户" }} 的加入申请吗？")
                    OutlinedTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it },
                        label = { Text("拒绝原因（选填）") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = !uiState.isMutating,
                    onClick = {
                        viewModel.rejectApplication(application, rejectReason.trim().takeIf { it.isNotBlank() })
                        rejectingApplication = null
                    }
                ) {
                    Text("拒绝")
                }
            },
            dismissButton = {
                TextButton(onClick = { rejectingApplication = null }, enabled = !uiState.isMutating) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun MemberCard(member: SpaceMember, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = TeamColors.glassBackground)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(member.avatarColor()),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = member.displayName().firstOrNull()?.toString().orEmpty(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (member.online) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50))
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    member.displayName(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    member.email ?: member.username,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                member.roleName.ifBlank { member.roleCode.roleLabel() },
                style = MaterialTheme.typography.labelSmall,
                color = TeamColors.primary
            )
        }
    }
}

@Composable
private fun JoinApplicationCard(
    application: SpaceJoinApplication,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = application.applicantUsername.ifBlank { "申请人" },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = listOf(
                            application.applicantEmail,
                            application.teamName ?: application.targetSpaceName,
                            formatDateTimeText(application.gmtCreate)
                        ).filterNot { it.isNullOrBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Text(
                text = application.remark?.takeIf { it.isNotBlank() } ?: "未填写申请说明",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onApprove, modifier = Modifier.weight(1f)) {
                    Text("通过")
                }
                TextButton(onClick = onReject, modifier = Modifier.weight(1f)) {
                    Text("拒绝", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InviteMemberDialog(
    invite: SpaceInvite?,
    isSaving: Boolean,
    onCreateInvite: (String, Int?) -> Unit,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var selectedRole by remember { mutableStateOf(DEFAULT_INVITE_ROLE) }
    var expireMinutes by remember { mutableStateOf("30") }
    val parsedExpireMinutes = expireMinutes.trim().toIntOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (invite == null) "生成邀请码" else "邀请码已生成") },
        text = {
            if (invite == null) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("加入角色", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        INVITE_ROLE_CHOICES.forEach { role ->
                            RoleChoicePill(
                                selected = selectedRole == role.code,
                                onClick = { selectedRole = role.code },
                                text = role.label
                            )
                        }
                    }
                    OutlinedTextField(
                        value = expireMinutes,
                        onValueChange = { value -> expireMinutes = value.filter(Char::isDigit).take(4) },
                        label = { Text("有效期（分钟）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = invite.code,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = TeamColors.primary
                    )
                    Text(
                        text = "${invite.roleCode.roleLabel()} · ${formatDateTimeText(invite.expiresAt).ifBlank { "有效期内" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { clipboardManager.setText(AnnotatedString(invite.code)) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("复制邀请码")
                    }
                }
            }
        },
        confirmButton = {
            if (invite == null) {
                Button(
                    enabled = !isSaving && parsedExpireMinutes != null,
                    onClick = { onCreateInvite(selectedRole, parsedExpireMinutes) }
                ) {
                    Text("生成")
                }
            } else {
                Button(onClick = onDismiss) {
                    Text("完成")
                }
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
private fun RoleChoicePill(
    selected: Boolean,
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) {
                    TeamColors.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                }
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(TeamColors.onPrimary)
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = when {
                selected -> TeamColors.onPrimary
                enabled -> MaterialTheme.colorScheme.onSurfaceVariant
                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MemberActionDialog(
    member: SpaceMember,
    isSaving: Boolean,
    onUpdateRole: (String) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedRole by remember(member.userId) {
        mutableStateOf(member.roleCode.takeIf { it.isNotBlank() } ?: DEFAULT_INVITE_ROLE)
    }
    val isOwner = member.roleCode == ROLE_OWNER

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(member.displayName(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = member.email ?: member.username,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text("成员角色", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MEMBER_ROLE_CHOICES.forEach { role ->
                        RoleChoicePill(
                            enabled = !isOwner,
                            selected = selectedRole == role.code,
                            onClick = { selectedRole = role.code },
                            text = role.label
                        )
                    }
                }
                if (!isOwner) {
                    TextButton(
                        onClick = onRemove,
                        enabled = !isSaving
                    ) {
                        Text("移除成员", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isSaving && !isOwner && selectedRole != member.roleCode,
                onClick = { onUpdateRole(selectedRole) }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("关闭")
            }
        }
    )
}

@Composable
private fun StateText(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun SpaceMember.displayName(): String {
    return nickname?.takeIf { it.isNotBlank() } ?: username.ifBlank { "成员" }
}

private fun SpaceMember.avatarColor(): Color {
    val colors = listOf(
        TeamColors.primary,
        Color(0xFF2E7D32),
        Color(0xFF1565C0),
        Color(0xFF6A1B9A),
        Color(0xFFC62828)
    )
    val index = (userId % colors.size).toInt().coerceAtLeast(0)
    return colors[index]
}

private fun String.roleLabel(): String {
    return when (this) {
        ROLE_OWNER -> "空间所有者"
        ROLE_ADMIN -> "空间管理员"
        ROLE_MEMBER -> "空间成员"
        ROLE_GUEST -> "只读访客"
        else -> this.ifBlank { "成员" }
    }
}

private const val ROLE_OWNER = "SPACE_OWNER"
private const val ROLE_ADMIN = "SPACE_ADMIN"
private const val ROLE_MEMBER = "SPACE_MEMBER"
private const val ROLE_GUEST = "SPACE_GUEST"
private const val DEFAULT_INVITE_ROLE = ROLE_MEMBER

private val INVITE_ROLE_CHOICES = listOf(
    RoleChoice(ROLE_ADMIN, "管理员"),
    RoleChoice(ROLE_MEMBER, "成员"),
    RoleChoice(ROLE_GUEST, "访客")
)

private val MEMBER_ROLE_CHOICES = listOf(
    RoleChoice(ROLE_ADMIN, "管理员"),
    RoleChoice(ROLE_MEMBER, "成员"),
    RoleChoice(ROLE_GUEST, "访客")
)
