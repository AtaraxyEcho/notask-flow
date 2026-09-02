package com.notaskflow.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Task
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Task
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.notaskflow.core.model.SpaceType
import com.notaskflow.core.ui.theme.TeamColors

enum class BottomNavTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val personalOnly: Boolean = false,
    val teamOnly: Boolean = false
) {
    HOME("首页", Icons.Filled.Home, Icons.Outlined.Home),
    NOTE("笔记", Icons.Filled.Description, Icons.Outlined.Description, personalOnly = true),
    TASK("任务", Icons.Filled.Task, Icons.Outlined.Task),
    TODO("待办", Icons.Filled.CheckCircle, Icons.Outlined.CheckCircle, personalOnly = true),
    FILE("文件", Icons.Filled.Folder, Icons.Outlined.Folder),
    STATS("统计", Icons.Filled.BarChart, Icons.Outlined.BarChart),
    DOCUMENT("文档", Icons.Filled.Description, Icons.Outlined.Description, teamOnly = true),
    PROJECT("项目", Icons.Filled.Home, Icons.Outlined.Home, teamOnly = true),
    MEMBERS("成员", Icons.Filled.Groups, Icons.Outlined.Groups, teamOnly = true)
}

val PersonalSpaceTabs = listOf(
    BottomNavTab.HOME, BottomNavTab.NOTE, BottomNavTab.TASK, BottomNavTab.TODO, BottomNavTab.FILE
)

val TeamSpaceTabs = listOf(
    BottomNavTab.PROJECT,
    BottomNavTab.DOCUMENT,
    BottomNavTab.TASK,
    BottomNavTab.FILE,
    BottomNavTab.MEMBERS
)

@Composable
fun SpaceAwareBottomNavBar(
    spaceType: SpaceType,
    selectedTab: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = when (spaceType) {
        SpaceType.PERSONAL -> PersonalSpaceTabs
        SpaceType.TEAM -> TeamSpaceTabs
    }
    val isTeam = spaceType == SpaceType.TEAM

    NavigationBar(
        modifier = modifier.fillMaxWidth(),
        containerColor = if (isTeam) TeamColors.glassBackground
                         else MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        tabs.forEach { tab ->
            val isSelected = selectedTab == tab
            val interactionSource = MutableInteractionSource()
            val pressed by interactionSource.collectIsPressedAsState()
            val scale by animateFloatAsState(
                targetValue = when {
                    pressed -> 0.92f
                    isSelected -> 1.08f
                    else -> 1f
                },
                animationSpec = tween(160),
                label = "navScale"
            )
            val targetColor = when {
                isSelected && isTeam -> TeamColors.primary
                isSelected && !isTeam -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            val targetIndicator = when {
                isSelected && !isTeam -> MaterialTheme.colorScheme.primaryContainer
                isSelected && isTeam -> TeamColors.primaryContainer.copy(alpha = 0.2f)
                else -> Color.Transparent
            }
            val iconColor by animateColorAsState(targetColor, spring(), label = "navIcon")
            val indicatorColor by animateColorAsState(targetIndicator, spring(), label = "navIndicator")

            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                interactionSource = interactionSource,
                icon = {
                    Icon(
                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.label,
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            },
                        tint = iconColor
                    )
                },
                label = {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = iconColor,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = indicatorColor
                )
            )
        }
    }
}
