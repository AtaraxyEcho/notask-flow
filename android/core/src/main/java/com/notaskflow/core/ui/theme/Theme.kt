package com.notaskflow.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.notaskflow.core.model.SpaceType

// 主题模式
enum class ThemeMode { LIGHT, DARK, SYSTEM }

// 个人空间预设主题
enum class PersonalPreset { SUNRISE, FOREST, OCEAN, MIDNIGHT }

// 主题配置（由 UiStore 驱动，持久化至 DataStore）
data class ThemeConfig(
    val mode: ThemeMode = ThemeMode.LIGHT,
    val personalPreset: PersonalPreset = PersonalPreset.SUNRISE,
    val spaceType: SpaceType = SpaceType.PERSONAL
)

// 判断当前是否为暗色模式
@Composable
private fun ThemeConfig.isDark(): Boolean = when (mode) {
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
}

// 根据配置生成对应的 ColorScheme
@Composable
private fun ThemeConfig.toColorScheme(): ColorScheme {
    val dark = isDark()
    @Suppress("REDUNDANT_ELSE_IN_WHEN")
    return when {
        spaceType == SpaceType.TEAM -> teamColorScheme(dark)
        else -> when (personalPreset) {
            PersonalPreset.SUNRISE -> sunriseColorScheme(dark)
            PersonalPreset.FOREST -> forestColorScheme(dark)
            PersonalPreset.OCEAN -> oceanColorScheme(dark)
            PersonalPreset.MIDNIGHT -> midnightColorScheme()
            else -> sunriseColorScheme(dark)
        }
    }
}

private fun ThemeConfig.toShapes() = when (spaceType) {
    SpaceType.TEAM -> TeamShapes
    SpaceType.PERSONAL -> PersonalShapes
    else -> PersonalShapes
}

private fun ThemeConfig.toTypography() = when (spaceType) {
    SpaceType.TEAM -> TeamTypography
    SpaceType.PERSONAL -> PersonalTypography
    else -> PersonalTypography
}

// ============================================================
// ColorScheme 构造器
// ============================================================

private fun sunriseColorScheme(dark: Boolean): ColorScheme {
    if (dark) return midnightColorScheme()
    val c = SunriseColors
    return lightColorScheme(
        primary = c.primary,
        onPrimary = c.onPrimary,
        primaryContainer = c.primaryContainer,
        onPrimaryContainer = c.onPrimaryContainer,
        secondary = c.secondary,
        onSecondary = c.onSecondary,
        secondaryContainer = c.secondaryContainer,
        onSecondaryContainer = c.onSecondaryContainer,
        tertiary = c.tertiary,
        onTertiary = c.onTertiary,
        tertiaryContainer = c.tertiaryContainer,
        onTertiaryContainer = c.onTertiaryContainer,
        surface = c.surface,
        onSurface = c.onSurface,
        surfaceVariant = c.surfaceVariant,
        onSurfaceVariant = c.onSurfaceVariant,
        surfaceTint = c.surfaceTint,
        surfaceContainerLowest = c.surfaceContainerLowest,
        surfaceContainerLow = c.surfaceContainerLow,
        surfaceContainer = c.surfaceContainer,
        surfaceContainerHigh = c.surfaceContainerHigh,
        surfaceContainerHighest = c.surfaceContainerHighest,
        surfaceDim = c.surfaceDim,
        surfaceBright = c.surfaceBright,
        background = c.background,
        onBackground = c.onBackground,
        outline = c.outline,
        outlineVariant = c.outlineVariant,
        error = c.error,
        onError = c.onError,
        errorContainer = c.errorContainer,
        onErrorContainer = c.onErrorContainer,
        inverseSurface = c.inverseSurface,
        inverseOnSurface = c.inverseOnSurface,
        inversePrimary = c.inversePrimary
    )
}

private fun forestColorScheme(dark: Boolean): ColorScheme {
    if (dark) return midnightColorScheme()
    val c = ForestColors
    return lightColorScheme(
        primary = c.primary,
        onPrimary = c.onPrimary,
        primaryContainer = c.primaryContainer,
        onPrimaryContainer = c.onPrimaryContainer,
        secondary = c.secondary,
        onSecondary = c.onSecondary,
        secondaryContainer = c.secondaryContainer,
        onSecondaryContainer = c.onSecondaryContainer,
        tertiary = c.tertiary,
        onTertiary = c.onTertiary,
        tertiaryContainer = c.tertiaryContainer,
        onTertiaryContainer = c.onTertiaryContainer,
        surface = c.surface,
        onSurface = c.onSurface,
        surfaceVariant = c.surfaceVariant,
        onSurfaceVariant = c.onSurfaceVariant,
        surfaceTint = c.surfaceTint,
        surfaceContainerLowest = c.surfaceContainerLowest,
        surfaceContainerLow = c.surfaceContainerLow,
        surfaceContainer = c.surfaceContainer,
        surfaceContainerHigh = c.surfaceContainerHigh,
        surfaceContainerHighest = c.surfaceContainerHighest,
        surfaceDim = c.surfaceDim,
        surfaceBright = c.surfaceBright,
        background = c.background,
        onBackground = c.onBackground,
        outline = c.outline,
        outlineVariant = c.outlineVariant,
        error = c.error,
        onError = c.onError,
        errorContainer = c.errorContainer,
        onErrorContainer = c.onErrorContainer,
        inverseSurface = c.inverseSurface,
        inverseOnSurface = c.inverseOnSurface,
        inversePrimary = c.inversePrimary
    )
}

private fun oceanColorScheme(dark: Boolean): ColorScheme {
    if (dark) return midnightColorScheme()
    val c = OceanColors
    return lightColorScheme(
        primary = c.primary,
        onPrimary = c.onPrimary,
        primaryContainer = c.primaryContainer,
        onPrimaryContainer = c.onPrimaryContainer,
        secondary = c.secondary,
        onSecondary = c.onSecondary,
        secondaryContainer = c.secondaryContainer,
        onSecondaryContainer = c.onSecondaryContainer,
        tertiary = c.tertiary,
        onTertiary = c.onTertiary,
        tertiaryContainer = c.tertiaryContainer,
        onTertiaryContainer = c.onTertiaryContainer,
        surface = c.surface,
        onSurface = c.onSurface,
        surfaceVariant = c.surfaceVariant,
        onSurfaceVariant = c.onSurfaceVariant,
        surfaceTint = c.surfaceTint,
        surfaceContainerLowest = c.surfaceContainerLowest,
        surfaceContainerLow = c.surfaceContainerLow,
        surfaceContainer = c.surfaceContainer,
        surfaceContainerHigh = c.surfaceContainerHigh,
        surfaceContainerHighest = c.surfaceContainerHighest,
        surfaceDim = c.surfaceDim,
        surfaceBright = c.surfaceBright,
        background = c.background,
        onBackground = c.onBackground,
        outline = c.outline,
        outlineVariant = c.outlineVariant,
        error = c.error,
        onError = c.onError,
        errorContainer = c.errorContainer,
        onErrorContainer = c.onErrorContainer,
        inverseSurface = c.inverseSurface,
        inverseOnSurface = c.inverseOnSurface,
        inversePrimary = c.inversePrimary
    )
}

private fun midnightColorScheme(): ColorScheme {
    val c = MidnightColors
    return darkColorScheme(
        primary = c.primary,
        onPrimary = c.onPrimary,
        primaryContainer = c.primaryContainer,
        onPrimaryContainer = c.onPrimaryContainer,
        secondary = c.secondary,
        onSecondary = c.onSecondary,
        secondaryContainer = c.secondaryContainer,
        onSecondaryContainer = c.onSecondaryContainer,
        tertiary = c.tertiary,
        onTertiary = c.onTertiary,
        tertiaryContainer = c.tertiaryContainer,
        onTertiaryContainer = c.onTertiaryContainer,
        surface = c.surface,
        onSurface = c.onSurface,
        surfaceVariant = c.surfaceVariant,
        onSurfaceVariant = c.onSurfaceVariant,
        surfaceTint = c.surfaceTint,
        surfaceContainerLowest = c.surfaceContainerLowest,
        surfaceContainerLow = c.surfaceContainerLow,
        surfaceContainer = c.surfaceContainer,
        surfaceContainerHigh = c.surfaceContainerHigh,
        surfaceContainerHighest = c.surfaceContainerHighest,
        surfaceDim = c.surfaceDim,
        surfaceBright = c.surfaceBright,
        background = c.background,
        onBackground = c.onBackground,
        outline = c.outline,
        outlineVariant = c.outlineVariant,
        error = c.error,
        onError = c.onError,
        errorContainer = c.errorContainer,
        onErrorContainer = c.onErrorContainer,
        inverseSurface = c.inverseSurface,
        inverseOnSurface = c.inverseOnSurface,
        inversePrimary = c.inversePrimary
    )
}

private fun teamColorScheme(dark: Boolean): ColorScheme {
    if (dark) return midnightColorScheme()
    val c = TeamColors
    return lightColorScheme(
        primary = c.primary,
        onPrimary = c.onPrimary,
        primaryContainer = c.primaryContainer,
        onPrimaryContainer = c.onPrimaryContainer,
        secondary = c.secondary,
        onSecondary = c.onSecondary,
        secondaryContainer = c.secondaryContainer,
        onSecondaryContainer = c.onSecondaryContainer,
        tertiary = c.tertiary,
        onTertiary = c.onTertiary,
        tertiaryContainer = c.tertiaryContainer,
        onTertiaryContainer = c.onTertiaryContainer,
        surface = c.surface,
        onSurface = c.onSurface,
        surfaceVariant = c.surfaceVariant,
        onSurfaceVariant = c.onSurfaceVariant,
        surfaceTint = c.surfaceTint,
        surfaceContainerLowest = c.surfaceContainerLowest,
        surfaceContainerLow = c.surfaceContainerLow,
        surfaceContainer = c.surfaceContainer,
        surfaceContainerHigh = c.surfaceContainerHigh,
        surfaceContainerHighest = c.surfaceContainerHighest,
        surfaceDim = c.surfaceDim,
        surfaceBright = c.surfaceBright,
        background = c.background,
        onBackground = c.onBackground,
        outline = c.outline,
        outlineVariant = c.outlineVariant,
        error = c.error,
        onError = c.onError,
        errorContainer = c.errorContainer,
        onErrorContainer = c.onErrorContainer,
        inverseSurface = c.inverseSurface,
        inverseOnSurface = c.inverseOnSurface,
        inversePrimary = c.inversePrimary
    )
}

// ============================================================
// 主题入口 — 直接使用 ColorScheme（动画过渡仅在空间切换时由上层触发）
// ============================================================
@Composable
fun NotaskFlowTheme(
    config: ThemeConfig = ThemeConfig(),
    content: @Composable () -> Unit
) {
    val colorScheme = config.toColorScheme()
    val shapes = config.toShapes()
    val typography = config.toTypography()

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = shapes,
        typography = typography,
        content = content
    )
}
