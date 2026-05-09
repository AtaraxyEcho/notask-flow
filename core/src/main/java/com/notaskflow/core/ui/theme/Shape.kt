package com.notaskflow.core.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ============================================================
// 个人空间形状 — 大圆角，温暖柔和
// ============================================================
val PersonalShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

// ============================================================
// 团队空间形状 — 小圆角，边框为主，高效专业
// ============================================================
val TeamShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp)
)

// 常用形状引用
object AppShapes {
    // 个人空间
    val PersonalButtonShape = CircleShape
    val PersonalCardShape = RoundedCornerShape(24.dp)
    val PersonalShellShape = RoundedCornerShape(32.dp)
    val PersonalInputShape = RoundedCornerShape(16.dp)
    val PersonalNavItemShape = RoundedCornerShape(16.dp)
    val PersonalDialogShape = RoundedCornerShape(28.dp)
    val PersonalFabShape = CircleShape

    // 团队空间
    val TeamButtonShape = RoundedCornerShape(50)
    val TeamCardShape = RoundedCornerShape(12.dp)
    val TeamShellShape = RoundedCornerShape(16.dp)
    val TeamInputShape = RoundedCornerShape(12.dp)
    val TeamNavItemShape = RoundedCornerShape(12.dp)
    val TeamDialogShape = RoundedCornerShape(16.dp)
    val TeamFabShape = RoundedCornerShape(16.dp)

    // 通用
    val SpaceSwitcherShape = RoundedCornerShape(20.dp)
    val SearchBarShape = RoundedCornerShape(20.dp)
}
