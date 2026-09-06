package com.notaskflow.feature.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.notaskflow.core.ui.theme.authpure.AuthPureColors
import com.notaskflow.feature.R

private const val FORGOT_STEP_EMAIL = 1
private const val FORGOT_STEP_CODE = 2
private const val FORGOT_STEP_PASSWORD = 3

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
    val colors = AuthPureColors
    val step = when {
        uiState.resetToken != null -> FORGOT_STEP_PASSWORD
        uiState.hasSentCode -> FORGOT_STEP_CODE
        else -> FORGOT_STEP_EMAIL
    }

    AuthPureScaffold(
        title = stringResource(R.string.auth_forgot_title),
        subtitle = stringResource(R.string.auth_forgot_subtitle),
        onBack = onBack,
        steps = {
            AuthSteps(
                currentStep = step,
                labels = listOf(
                    stringResource(R.string.auth_step_email),
                    stringResource(R.string.auth_step_code),
                    stringResource(R.string.auth_step_password)
                )
            )
        },
        errorBanner = uiState.errorMessage,
        successBanner = uiState.message,
        bottomContent = {
            Text(
                text = stringResource(R.string.auth_forgot_footer),
                modifier = Modifier.fillMaxWidth(),
                fontSize = 13.sp,
                color = colors.Muted
            )
        }
    ) {
        when (step) {
            FORGOT_STEP_EMAIL -> {
                AuthTextField(
                    value = uiState.email,
                    onValueChange = viewModel::updateEmail,
                    label = stringResource(R.string.auth_forgot_email_label),
                    placeholder = stringResource(R.string.auth_forgot_email_placeholder),
                    required = true,
                    leadingIcon = Icons.Filled.Mail,
                    keyboardType = KeyboardType.Email,
                    keyboardActions = KeyboardActions(onDone = { viewModel.sendCode() })
                )
                AuthPrimaryButton(
                    text = stringResource(R.string.auth_send_code),
                    loading = uiState.isSubmitting,
                    enabled = true,
                    onClick = viewModel::sendCode
                )
                AuthSwitchLine {
                    Text(
                        text = stringResource(R.string.auth_remember_password),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = onBack,
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.auth_back_to_login),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            FORGOT_STEP_CODE -> {
                Column {
                    AuthFieldLabel(label = stringResource(R.string.auth_forgot_code_label), required = true)
                    Spacer(Modifier.height(6.dp))
                    AuthOtpRow(
                        code = uiState.code,
                        onCodeChange = viewModel::updateCode
                    )
                }
                AuthPrimaryButton(
                    text = stringResource(R.string.auth_verify_code),
                    loading = uiState.isSubmitting,
                    enabled = true,
                    onClick = viewModel::verifyCode
                )
                AuthSwitchLine {
                    Text(
                        text = stringResource(R.string.auth_no_code),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = viewModel::sendCode,
                        enabled = uiState.resendCountdown == 0 && !uiState.isSubmitting,
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text(
                            text = if (uiState.resendCountdown > 0) {
                                stringResource(R.string.auth_resend_in, uiState.resendCountdown)
                            } else {
                                stringResource(R.string.auth_resend_code)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (uiState.resendCountdown > 0) {
                                colors.Placeholder
                            } else {
                                colors.Ink
                            }
                        )
                    }
                    TextButton(
                        onClick = viewModel::editEmail,
                        enabled = !uiState.isSubmitting,
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.auth_edit_email),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            else -> {
                AuthTextField(
                    value = uiState.newPassword,
                    onValueChange = viewModel::updateNewPassword,
                    label = stringResource(R.string.auth_new_password_label),
                    placeholder = stringResource(R.string.auth_new_password_placeholder),
                    required = true,
                    visualTransformation = if (uiState.isPasswordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardType = KeyboardType.Password,
                    trailingContent = {
                        IconButton(onClick = viewModel::togglePasswordVisibility, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = if (uiState.isPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = if (uiState.isPasswordVisible) {
                                    stringResource(R.string.auth_hide_password)
                                } else {
                                    stringResource(R.string.auth_show_password)
                                },
                                tint = colors.Muted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                )
                AuthStrengthBar(password = uiState.newPassword)
                AuthTextField(
                    value = uiState.confirmPassword,
                    onValueChange = viewModel::updateConfirmPassword,
                    label = stringResource(R.string.auth_confirm_new_password_label),
                    placeholder = stringResource(R.string.auth_confirm_new_password_placeholder),
                    required = true,
                    visualTransformation = if (uiState.isConfirmPasswordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardType = KeyboardType.Password,
                    trailingContent = {
                        IconButton(
                            onClick = viewModel::toggleConfirmPasswordVisibility,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (uiState.isConfirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = if (uiState.isConfirmPasswordVisible) {
                                    stringResource(R.string.auth_hide_password)
                                } else {
                                    stringResource(R.string.auth_show_password)
                                },
                                tint = colors.Muted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                )
                AuthPrimaryButton(
                    text = stringResource(R.string.auth_reset_password),
                    loading = uiState.isSubmitting,
                    enabled = true,
                    onClick = viewModel::resetPassword
                )
            }
        }
    }
}
