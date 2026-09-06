package com.notaskflow.core.ui.theme.authpure

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Auth Pure 设计令牌（Rigorous Functional Minimalism）。
 * 与 Web 端 frontend/src/styles/auth.css 的令牌一一对应，认证页固定浅色，
 * 不响应系统暗色与应用主题切换。
 */
object AuthPureColors {
    val Canvas = Color(0xFFF8FAFC)
    val Surface = Color(0xFFFFFFFF)
    val Ink = Color(0xFF0F172A)
    val InkHover = Color(0xFF1E293B)
    val InkActive = Color(0xFF334155)
    val Secondary = Color(0xFF334155)
    val Muted = Color(0xFF64748B)
    val Placeholder = Color(0xFF94A3B8)
    val Border = Color(0xFFE2E8F0)
    val BorderHover = Color(0xFFCBD5E1)
    val DisabledBg = Color(0xFFF1F5F9)
    val Error = Color(0xFFEF4444)
    val ErrorSurface = Color(0xFFFEF2F2)
    val ErrorBorder = Color(0xFFFCA5A5)
    val ErrorText = Color(0xFF991B1B)
    val Success = Color(0xFF10B981)
    val SuccessSurface = Color(0xFFECFDF5)
    val SuccessBorder = Color(0xFFA7F3D0)
    val SuccessText = Color(0xFF065F46)
    val Strength1 = Color(0xFFEF4444)
    val Strength2 = Color(0xFFF59E0B)
    val Strength3 = Color(0xFF10B981)
    val Strength4 = Color(0xFF059669)
}

/**
 * 认证页专用主题：把 Auth Pure 令牌映射进 Material 3 全部角色，
 * 标准 M3 组件（按钮/底部弹层/勾选框）无需定制即可获得正确配色。
 */
@Composable
fun AuthPureTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = authPureColorScheme(),
        content = content
    )
}

private fun authPureColorScheme(): ColorScheme = lightColorScheme(
    primary = AuthPureColors.Ink,
    onPrimary = Color.White,
    primaryContainer = AuthPureColors.InkActive,
    onPrimaryContainer = Color.White,
    inversePrimary = AuthPureColors.InkHover,
    secondary = AuthPureColors.Secondary,
    onSecondary = Color.White,
    secondaryContainer = AuthPureColors.DisabledBg,
    onSecondaryContainer = AuthPureColors.Ink,
    tertiary = AuthPureColors.InkActive,
    onTertiary = Color.White,
    tertiaryContainer = AuthPureColors.DisabledBg,
    onTertiaryContainer = AuthPureColors.Ink,
    background = AuthPureColors.Canvas,
    onBackground = AuthPureColors.Ink,
    surface = AuthPureColors.Surface,
    onSurface = AuthPureColors.Ink,
    surfaceVariant = AuthPureColors.DisabledBg,
    onSurfaceVariant = AuthPureColors.Muted,
    outline = AuthPureColors.Muted,
    outlineVariant = AuthPureColors.Border,
    error = AuthPureColors.Error,
    onError = Color.White,
    errorContainer = AuthPureColors.ErrorSurface,
    onErrorContainer = AuthPureColors.ErrorText,
    inverseSurface = AuthPureColors.Ink,
    inverseOnSurface = AuthPureColors.Canvas,
    surfaceTint = Color.Transparent,
    scrim = Color(0x5210172A),
    surfaceDim = AuthPureColors.Canvas,
    surfaceBright = AuthPureColors.Surface,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = AuthPureColors.Surface,
    surfaceContainer = AuthPureColors.Surface,
    surfaceContainerHigh = AuthPureColors.Surface,
    surfaceContainerHighest = AuthPureColors.DisabledBg
)
