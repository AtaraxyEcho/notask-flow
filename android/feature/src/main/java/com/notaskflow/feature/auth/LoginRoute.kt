package com.notaskflow.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.notaskflow.core.ui.theme.SunriseColors

@Composable
fun LoginRoute(
    onLoginSuccess: () -> Unit,
    onRegisterClick: () -> Unit = {},
    onForgotPasswordClick: () -> Unit = {},
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                LoginEffect.LoginSuccess -> onLoginSuccess()
            }
        }
    }
    LoginScreen(
        uiState = uiState,
        onAccountChange = viewModel::onAccountChange,
        onPasswordChange = viewModel::onPasswordChange,
        onTogglePasswordVisibility = viewModel::togglePasswordVisibility,
        onToggleRememberMe = viewModel::toggleRememberMe,
        onLoginClick = viewModel::onLoginClick,
        onRegisterClick = onRegisterClick,
        onForgotPasswordClick = onForgotPasswordClick
    )
}

@Composable
fun LoginScreen(
    uiState: LoginUiState,
    onAccountChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onToggleRememberMe: () -> Unit,
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onForgotPasswordClick: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    AuthFormScaffold(
        title = "欢迎回来",
        subtitle = "登录后继续管理你的任务、笔记与团队协作",
        icon = Icons.Filled.Email,
        onBack = {},
        showBackButton = false
    ) {
        AuthTextField(
            value = uiState.account,
            onValueChange = onAccountChange,
            label = "账号或邮箱",
            placeholder = "输入用户名或邮箱",
            leadingIcon = Icons.Filled.Email,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )
        AuthTextField(
            value = uiState.password,
            onValueChange = onPasswordChange,
            label = "密码",
            placeholder = "输入登录密码",
            leadingIcon = Icons.Filled.Lock,
            visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    onLoginClick()
                }
            ),
            trailingContent = {
                IconButton(onClick = onTogglePasswordVisibility, modifier = Modifier.size(44.dp)) {
                    Icon(
                        imageVector = if (uiState.isPasswordVisible) {
                            Icons.Filled.Visibility
                        } else {
                            Icons.Filled.VisibilityOff
                        },
                        contentDescription = if (uiState.isPasswordVisible) "隐藏密码" else "显示密码"
                    )
                }
            }
        )
        uiState.errorMessage?.let { message ->
            AuthMessage(text = message, isError = true)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = uiState.rememberMe,
                    onCheckedChange = { onToggleRememberMe() },
                    colors = CheckboxDefaults.colors(checkedColor = SunriseColors.primary)
                )
                Text(
                    text = "记住我",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SunriseColors.onSurfaceVariant
                )
            }
            TextButton(onClick = onForgotPasswordClick) {
                Text(text = "忘记密码？", color = SunriseColors.primary)
            }
        }
        AuthPrimaryButton(
            text = "登录",
            loading = uiState.isLoading,
            enabled = true,
            onClick = onLoginClick
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "没有账号？",
                style = MaterialTheme.typography.bodyMedium,
                color = SunriseColors.onSurfaceVariant
            )
            TextButton(onClick = onRegisterClick) {
                Text(
                    text = "立即注册",
                    color = SunriseColors.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
