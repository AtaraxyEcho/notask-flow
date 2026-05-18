package com.notaskflow.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.notaskflow.core.ui.theme.TeamColors

// 团队空间背景光斑
@Composable
fun TeamSpaceBackground(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        // 左上角蓝色光斑
        Box(
            modifier = Modifier
                .size(400.dp)
                .align(Alignment.TopStart)
                .offset(x = (-120).dp, y = (-80).dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            TeamColors.tertiaryFixed.copy(alpha = 0.4f),
                            TeamColors.tertiaryFixed.copy(alpha = 0.0f)
                        )
                    )
                )
                .blur(120.dp)
        )
        // 右下角品牌蓝光斑
        Box(
            modifier = Modifier
                .size(400.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 80.dp, y = 80.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            TeamColors.primary.copy(alpha = 0.2f),
                            TeamColors.primary.copy(alpha = 0.0f)
                        )
                    )
                )
                .blur(140.dp)
        )
    }
}
