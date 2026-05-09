package com.notaskflow.feature.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notaskflow.core.model.SpaceType
import com.notaskflow.core.ui.components.BottomNavTab
import com.notaskflow.core.ui.components.SpaceAwareBottomNavBar
import com.notaskflow.core.ui.components.SpaceAwareTopAppBar
import com.notaskflow.core.ui.components.SpaceItem
import com.notaskflow.core.ui.theme.SunriseColors
import com.notaskflow.feature.file.FileBrowserRoute
import com.notaskflow.feature.note.NoteListRoute
import com.notaskflow.feature.stats.StatsRoute
import com.notaskflow.feature.task.TaskListRoute
import com.notaskflow.feature.todo.TodoListRoute
import kotlinx.coroutines.delay

@Composable
fun HomeRoute(
    onNavigateToNoteEdit: () -> Unit = {},
    onNavigateToTaskCreate: () -> Unit = {},
    onNavigateToTodoCreate: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(BottomNavTab.HOME) }
    var currentSpace by remember { mutableStateOf(
        SpaceItem(id = 0, name = "我的笔记空间", type = SpaceType.PERSONAL)
    )}
    var isSpaceTransitioning by remember { mutableStateOf(false) }

    val spaces = remember {
        listOf(
            SpaceItem(id = 0, name = "我的笔记空间", type = SpaceType.PERSONAL),
            SpaceItem(id = 1, name = "产品开发团队", type = SpaceType.TEAM, memberCount = 12),
            SpaceItem(id = 2, name = "设计团队", type = SpaceType.TEAM, memberCount = 8, hasUnread = true)
        )
    }

    LaunchedEffect(isSpaceTransitioning) {
        if (isSpaceTransitioning) {
            delay(450)
            isSpaceTransitioning = false
        }
    }

    Scaffold(
        topBar = {
            SpaceAwareTopAppBar(
                spaceType = currentSpace.type,
                spaceName = currentSpace.name,
                currentSpace = currentSpace,
                spaces = spaces,
                onSpaceSelected = { space ->
                    if (space.id != currentSpace.id) {
                        isSpaceTransitioning = true
                        currentSpace = space
                        selectedTab = if (space.type == SpaceType.TEAM) BottomNavTab.PROJECT
                                      else BottomNavTab.HOME
                    }
                },
                onCreateTeamSpace = { },
                onJoinTeam = { },
                onSearchClick = { },
                onNotificationClick = { },
                onProfileClick = { },
                unreadNotificationCount = 3
            )
        },
        bottomBar = {
            SpaceAwareBottomNavBar(
                spaceType = currentSpace.type,
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        },
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    (fadeIn(tween(250)) + slideInHorizontally(tween(250)) { it / 4 })
                        .togetherWith(fadeOut(tween(200)) + slideOutHorizontally(tween(200)) { -it / 4 })
                },
                label = "tabContent"
            ) { tab ->
                ContentForTab(tab, onNavigateToNoteEdit, onNavigateToTaskCreate, onNavigateToTodoCreate)
            }

            AnimatedVisibility(
                visible = isSpaceTransitioning,
                enter = fadeIn(tween(300)),
                exit = fadeOut(tween(300))
            ) {
                SpaceTransitionOverlay(currentSpace.name)
            }
        }
    }
}

@Composable
private fun ContentForTab(
    tab: BottomNavTab,
    onNavigateToNoteEdit: () -> Unit,
    onNavigateToTaskCreate: () -> Unit,
    onNavigateToTodoCreate: () -> Unit
) {
    when (tab) {
        BottomNavTab.HOME -> PersonalHomeContent()
        BottomNavTab.PROJECT -> TeamProjectWorkbench()
        BottomNavTab.NOTE -> NoteListRoute(Modifier.fillMaxSize(), onCreateNote = onNavigateToNoteEdit)
        BottomNavTab.DOCUMENT -> NoteListRoute(Modifier.fillMaxSize(), onCreateNote = onNavigateToNoteEdit)
        BottomNavTab.TASK -> TaskListRoute(Modifier.fillMaxSize(), onCreateTask = onNavigateToTaskCreate)
        BottomNavTab.TODO -> TodoListRoute(Modifier.fillMaxSize(), onCreateTodo = onNavigateToTodoCreate)
        BottomNavTab.FILE -> FileBrowserRoute(Modifier.fillMaxSize())
        BottomNavTab.STATS -> StatsRoute(Modifier.fillMaxSize())
        BottomNavTab.MEMBERS -> TeamMembersContent()
    }
}

// ============================================================
// 个人空间首页 — 匹配 home_screen_personal_space_english_nav
// ============================================================
@Composable
private fun PersonalHomeContent() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // 问候语（图片在上方，文字在下）
        item {
            Spacer(Modifier.height(8.dp))
            // 顶部大图
            Box(
                modifier = Modifier.fillMaxWidth().height(200.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Image(
                    painter = painterResource(com.notaskflow.core.R.drawable.personal_home),
                    contentDescription = "首页封面",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(Modifier.height(20.dp))
            Text("Good morning, Alex.",
                style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary)
            Text("准备好开始新的一天了吗？",
                style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontStyle = FontStyle.Italic, modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.height(28.dp))
        }

        // 快捷操作卡片
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(
                    Modifier.weight(1f).height(120.dp).clip(RoundedCornerShape(24.dp)).clickable { },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                        Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Edit, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                        Column {
                            Text("写笔记", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text("记录灵感", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Card(
                    Modifier.weight(1f).height(120.dp).clip(RoundedCornerShape(24.dp)).clickable { },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                        Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.CheckCircle, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.tertiary)
                        }
                        Column {
                            Text("待办事项", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text("今日 3 项", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
        }

        // 温柔提醒
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("温柔提醒", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
        }

        item {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🌸", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("给自己一个深呼吸", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text("忙碌中别忘了停下来，喝杯热茶，看看窗外。你已经做得很好了。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        // 最近笔记标题
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("最近笔记", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = { }) { Text("查看全部", color = MaterialTheme.colorScheme.primary) }
            }
            Spacer(Modifier.height(8.dp))
        }

        // 最近笔记卡片（Card 样式）
        item {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                        Spacer(Modifier.width(8.dp))
                        Text("Journal · Oct 24", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("秋天午后的宁静力量", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text("午后的阳光洒在地板上，温柔而安详。今天突然意识到...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TagChip("#觉知"); TagChip("#秋天")
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        item {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.CheckCircle, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("完成 《设计师之路》", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text("晨间笔记: 第12/30天", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(progress = { 0.4f }, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        // 本周观察
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("本周观察", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
        }

        item {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📊", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("笔记 128 篇 · 本周新增 5 篇", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text("任务完成率 72%，比上周提升 8%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun TagChip(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

// ============================================================
// 团队空间首页（项目工作台）
// ============================================================
@Composable
private fun TeamProjectWorkbench() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "项目工作台",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "产品开发团队 · 12 人",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 快速操作
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickActionCard("新建项目", Icons.Filled.Add, Modifier.weight(1f))
                QuickActionCard("新建文档", Icons.Filled.Edit, Modifier.weight(1f))
                QuickActionCard("新建任务", Icons.Filled.CheckCircle, Modifier.weight(1f))
            }
        }

        // 我的项目
        item {
            Text(
                text = "我的项目",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        items(3) { i ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SunriseColors.primary)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "项目 ${i + 1}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${(i + 1) * 8} 任务 · ${(i + 1) * 3} 已完成",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun QuickActionCard(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        onClick = { }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

// ============================================================
// 团队成员
// ============================================================
@Composable
private fun TeamMembersContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "团队成员",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "成员管理功能将在后续版本中实现",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ============================================================
// 空间切换遮罩
// ============================================================
@Composable
private fun SpaceTransitionOverlay(spaceName: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Text(
                text = "正在切换到 $spaceName",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}
