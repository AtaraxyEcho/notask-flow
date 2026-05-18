package com.notaskflow.feature.auth

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.notaskflow.core.ui.theme.SunriseColors

@Composable
fun RegisterRoute(
    onBack: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                RegisterEffect.Registered -> onBack()
            }
        }
    }

    AuthFormScaffold(
        title = "注册账号",
        subtitle = "创建你的个人工作流账号，之后可加入团队空间协作",
        icon = Icons.Filled.Person,
        onBack = onBack
    ) {
        AuthTextField(
            value = uiState.username,
            onValueChange = viewModel::updateUsername,
            label = "用户名",
            placeholder = "用于登录的唯一账号",
            leadingIcon = Icons.Filled.Person
        )
        AuthTextField(
            value = uiState.nickname,
            onValueChange = viewModel::updateNickname,
            label = "昵称",
            placeholder = "大家在空间里怎么称呼你",
            leadingIcon = Icons.Filled.Person
        )
        AuthTextField(
            value = uiState.email,
            onValueChange = viewModel::updateEmail,
            label = "邮箱",
            placeholder = "name@example.com",
            leadingIcon = Icons.Filled.Email,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        AuthCodeTextField(
            value = uiState.emailCode,
            onValueChange = viewModel::updateEmailCode,
            label = "邮箱验证码",
            placeholder = "输入收到的 6 位验证码",
            buttonText = "发送",
            loading = uiState.isSendingCode,
            onSendClick = viewModel::sendEmailCode,
            leadingIcon = Icons.Filled.Check
        )
        AuthTextField(
            value = uiState.password,
            onValueChange = viewModel::updatePassword,
            label = "密码",
            placeholder = "至少 8 位，建议包含字母和数字",
            leadingIcon = Icons.Filled.Lock,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        AuthPasswordStrength(password = uiState.password)
        AuthTextField(
            value = uiState.confirmPassword,
            onValueChange = viewModel::updateConfirmPassword,
            label = "确认密码",
            placeholder = "再次输入密码",
            leadingIcon = Icons.Filled.Lock,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        uiState.message?.let { message ->
            AuthMessage(text = message, isError = false)
        }
        uiState.errorMessage?.let { message ->
            AuthMessage(text = message, isError = true)
        }
        Spacer(Modifier.height(4.dp))
        AuthPrimaryButton(
            text = "创建账号",
            loading = uiState.isRegistering,
            enabled = true,
            onClick = viewModel::register
        )
        Text(
            text = "密码至少 8 位；验证码可按后端策略决定是否必填",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            color = SunriseColors.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("已有账号？返回登录", color = SunriseColors.primary)
        }
        Spacer(Modifier.height(12.dp))
    }
}
