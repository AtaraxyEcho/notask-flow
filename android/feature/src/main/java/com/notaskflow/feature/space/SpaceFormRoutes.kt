package com.notaskflow.feature.space

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun CreateTeamSpaceRoute(
    onBack: () -> Unit,
    onDone: () -> Unit,
    viewModel: SpaceFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    SpaceFormEffectHandler(viewModel = viewModel, onDone = onDone)
    SpaceFormScaffold(
        title = "创建团队空间",
        buttonText = "创建",
        isSubmitting = uiState.isSubmitting,
        errorMessage = uiState.errorMessage,
        onBack = onBack,
        onSubmit = viewModel::createTeamSpace
    ) {
        SpaceFormTextField(
            value = uiState.teamName,
            onValueChange = viewModel::updateTeamName,
            label = "团队空间名称",
            placeholder = "例如：产品协作组"
        )
        Text(
            text = "创建后你会成为该团队空间管理员，可以在 Web 端继续配置角色和邀请策略。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun JoinTeamSpaceRoute(
    onBack: () -> Unit,
    onDone: () -> Unit,
    viewModel: SpaceFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    SpaceFormEffectHandler(viewModel = viewModel, onDone = onDone)
    LaunchedEffect(Unit) {
        viewModel.loadMineApplications()
    }
    SpaceFormScaffold(
        title = "加入团队空间",
        buttonText = "邀请码加入",
        isSubmitting = uiState.isSubmitting,
        errorMessage = uiState.errorMessage,
        onBack = onBack,
        onSubmit = viewModel::joinByInviteCode
    ) {
        SpaceFormTextField(
            value = uiState.inviteCode,
            onValueChange = viewModel::updateInviteCode,
            label = "邀请码",
            placeholder = "输入团队管理员分享的邀请码"
        )
        Text(
            text = "也可以向上级账号发起加入申请，由管理员审批后进入团队。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SpaceFormTextField(
            value = uiState.supervisorAccount,
            onValueChange = viewModel::updateSupervisorAccount,
            label = "上级账号",
            placeholder = "用户名或邮箱"
        )
        SpaceFormTextField(
            value = uiState.applicationTeamName,
            onValueChange = viewModel::updateApplicationTeamName,
            label = "团队名称",
            placeholder = "选填，便于管理员识别"
        )
        SpaceFormTextField(
            value = uiState.applicationRemark,
            onValueChange = viewModel::updateApplicationRemark,
            label = "申请说明",
            placeholder = "选填，说明身份或加入目的"
        )
        Button(
            onClick = viewModel::applyJoinTeam,
            enabled = !uiState.isSubmitting,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Text("提交加入申请")
        }
        if (uiState.mineApplications.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "我的申请",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                uiState.mineApplications.take(3).forEach { application ->
                    Text(
                        text = listOf(
                            application.teamName ?: application.targetSpaceName ?: "团队",
                            application.status.name,
                            application.rejectReason
                        ).filterNot { it.isNullOrBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SpaceFormEffectHandler(
    viewModel: SpaceFormViewModel,
    onDone: () -> Unit
) {
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                SpaceFormEffect.Done -> onDone()
            }
        }
    }
}

@Composable
private fun SpaceFormScaffold(
    title: String,
    buttonText: String,
    isSubmitting: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(horizontal = 14.dp)
            .padding(bottom = 18.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 24.dp, bottomEnd = 24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.16f)
                            .height(4.dp)
                            .background(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(50)
                            )
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                content()
                errorMessage?.let { message ->
                    Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(2.dp))
                Button(
                    onClick = onSubmit,
                    enabled = !isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(buttonText)
                    }
                }
                TextButton(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Text("取消")
                }
            }
        }
    }
}

@Composable
private fun SpaceFormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    )
}
