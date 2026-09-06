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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.notaskflow.core.common.LegalDocumentType
import com.notaskflow.core.ui.theme.authpure.AuthPureColors
import com.notaskflow.feature.R

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
    val focusManager = LocalFocusManager.current
    val colors = AuthPureColors

    AuthPureScaffold(
        title = stringResource(R.string.auth_register_title),
        subtitle = stringResource(R.string.auth_register_subtitle),
        onBack = onBack,
        errorBanner = uiState.errorMessage,
        successBanner = uiState.message,
        bottomContent = {
            AuthLinkLine(
                prefix = stringResource(R.string.auth_has_account),
                links = listOf(
                    AuthLink(label = stringResource(R.string.auth_back_to_login), onClick = onBack)
                )
            )
        }
    ) {
        AuthTextField(
            value = uiState.username,
            onValueChange = viewModel::updateUsername,
            label = stringResource(R.string.auth_username_label),
            placeholder = stringResource(R.string.auth_username_placeholder),
            required = true,
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )
        AuthTextField(
            value = uiState.nickname,
            onValueChange = viewModel::updateNickname,
            label = stringResource(R.string.auth_nickname_label),
            placeholder = stringResource(R.string.auth_nickname_placeholder),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )
        AuthTextField(
            value = uiState.email,
            onValueChange = viewModel::updateEmail,
            label = stringResource(R.string.auth_email_label),
            placeholder = "name@example.com",
            errorMessage = uiState.emailError,
            required = true,
            keyboardType = KeyboardType.Email,
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )
        Column {
            AuthFieldLabel(
                label = stringResource(R.string.auth_email_code_label),
                required = true,
                trailing = {
                    when {
                        uiState.isSendingCode -> CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = colors.Ink
                        )
                        uiState.resendCountdown > 0 -> Text(
                            text = stringResource(R.string.auth_resend_in, uiState.resendCountdown),
                            fontSize = 13.sp,
                            color = colors.Placeholder
                        )
                        else -> Text(
                            text = stringResource(R.string.auth_get_code),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.Ink,
                            modifier = Modifier.clickable { viewModel.sendEmailCode() }
                        )
                    }
                }
            )
            Spacer(Modifier.height(6.dp))
            AuthOtpRow(
                code = uiState.emailCode,
                onCodeChange = viewModel::updateEmailCode
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.auth_email_code_hint),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = colors.Muted
            )
        }
        AuthTextField(
            value = uiState.password,
            onValueChange = viewModel::updatePassword,
            label = stringResource(R.string.auth_password_create_label),
            placeholder = stringResource(R.string.auth_password_create_placeholder),
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
            },
            labelTrailing = {
                val level = passwordStrengthLevel(uiState.password)
                val levelText = when (level) {
                    0 -> stringResource(R.string.auth_strength_empty)
                    1 -> stringResource(R.string.auth_strength_weak)
                    2 -> stringResource(R.string.auth_strength_fair)
                    3 -> stringResource(R.string.auth_strength_good)
                    else -> stringResource(R.string.auth_strength_strong)
                }
                Text(
                    text = stringResource(R.string.auth_strength_value, levelText),
                    fontSize = 12.sp,
                    color = colors.Muted
                )
            }
        )
        AuthStrengthBar(password = uiState.password)
        AuthTextField(
            value = uiState.confirmPassword,
            onValueChange = viewModel::updateConfirmPassword,
            label = stringResource(R.string.auth_confirm_password_label),
            placeholder = stringResource(R.string.auth_confirm_password_placeholder),
            required = true,
            visualTransformation = if (uiState.isConfirmPasswordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            keyboardType = KeyboardType.Password,
            trailingContent = {
                IconButton(onClick = viewModel::toggleConfirmPasswordVisibility, modifier = Modifier.size(32.dp)) {
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
        AuthTextField(
            value = uiState.inviteCode,
            onValueChange = viewModel::updateInviteCode,
            label = stringResource(R.string.auth_invite_code_label),
            placeholder = stringResource(R.string.auth_invite_code_placeholder),
            labelTrailing = {
                Text(
                    text = stringResource(R.string.auth_optional),
                    fontSize = 12.sp,
                    color = colors.Muted
                )
            }
        )
        AuthCheckboxRow(
            checked = uiState.acceptedTerms,
            onCheckedChange = { viewModel.toggleAcceptedTerms() }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.auth_terms_prefix),
                    fontSize = 13.sp,
                    color = colors.Secondary
                )
                Text(
                    text = stringResource(R.string.auth_terms_title),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.Ink,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier
                        .clickable { AuthLegalController.open(LegalDocumentType.TERMS) }
                        .padding(horizontal = 2.dp)
                )
                Text(
                    text = stringResource(R.string.auth_and),
                    fontSize = 13.sp,
                    color = colors.Secondary
                )
                Text(
                    text = stringResource(R.string.auth_privacy_title),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.Ink,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier
                        .clickable { AuthLegalController.open(LegalDocumentType.PRIVACY) }
                        .padding(horizontal = 2.dp)
                )
            }
        }
        AuthPrimaryButton(
            text = if (uiState.isRegistering) {
                stringResource(R.string.auth_registering)
            } else {
                stringResource(R.string.auth_create_account)
            },
            loading = uiState.isRegistering,
            enabled = true,
            onClick = {
                focusManager.clearFocus()
                viewModel.register()
            }
        )
    }
}
