package com.notaskflow.feature.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.notaskflow.core.ui.theme.SunriseColors

@Composable
fun ForgotPasswordRoute(
    onBack: () -> Unit,
    viewModel: ForgotPasswordViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                ForgotPasswordEffect.ResetDone -> onBack()
            }
        }
    }

    AuthFormScaffold(
        title = "重置密码",
        subtitle = "先验证邮箱验证码，再设置新的登录密码",
        icon = Icons.Filled.VerifiedUser,
        onBack = onBack
    ) {
        AuthTextField(
            value = uiState.email,
            onValueChange = viewModel::updateEmail,
            label = "邮箱",
            placeholder = "输入注册邮箱",
            leadingIcon = Icons.Filled.Email,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        AuthCodeTextField(
            value = uiState.code,
            onValueChange = viewModel::updateCode,
            label = "验证码",
            placeholder = "输入邮箱验证码",
            buttonText = "发送",
            loading = uiState.isSubmitting && uiState.resetToken == null,
            onSendClick = viewModel::sendCode,
            leadingIcon = Icons.Filled.Check
        )
        OutlinedButton(
            onClick = viewModel::verifyCode,
            enabled = !uiState.isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = MaterialTheme.shapes.large,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = SunriseColors.primary
            )
        ) {
            if (uiState.isSubmitting && uiState.resetToken == null) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .size(18.dp),
                    color = SunriseColors.primary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(if (uiState.resetToken == null) "校验验证码" else "验证码已通过")
            }
        }
        AuthTextField(
            value = uiState.newPassword,
            onValueChange = viewModel::updateNewPassword,
            label = "新密码",
            placeholder = "设置新的登录密码",
            leadingIcon = Icons.Filled.Lock,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        AuthPasswordStrength(password = uiState.newPassword)
        AuthTextField(
            value = uiState.confirmPassword,
            onValueChange = viewModel::updateConfirmPassword,
            label = "确认新密码",
            placeholder = "再次输入新密码",
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
        AuthPrimaryButton(
            text = "重置密码",
            loading = uiState.isSubmitting && uiState.resetToken != null,
            enabled = !uiState.isSubmitting,
            onClick = viewModel::resetPassword
        )
        Text(
            text = "重置完成后会自动返回登录页",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            color = SunriseColors.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
