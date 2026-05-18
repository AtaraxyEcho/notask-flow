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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.notaskflow.core.R
import com.notaskflow.core.common.formatDateTimeText
import com.notaskflow.core.model.SpaceType
import com.notaskflow.core.ui.components.BottomNavTab
import com.notaskflow.core.ui.components.PersonalSpaceTabs
import com.notaskflow.core.ui.components.SpaceAwareBottomNavBar
import com.notaskflow.core.ui.components.SpaceAwareTopAppBar
import com.notaskflow.core.ui.components.SpaceItem
import com.notaskflow.core.ui.components.TeamSpaceTabs
import com.notaskflow.domain.model.Note
import com.notaskflow.domain.model.PersonalNoteTrend
import com.notaskflow.domain.model.Todo
import com.notaskflow.feature.file.FileBrowserRoute
import com.notaskflow.feature.members.MembersRoute
import com.notaskflow.feature.note.NoteListRoute
import com.notaskflow.feature.project.ProjectRoute
import com.notaskflow.feature.stats.StatsRoute
import com.notaskflow.feature.task.TaskListRoute
import com.notaskflow.feature.todo.TodoListRoute
import kotlinx.coroutines.delay

@Composable
fun HomeRoute(
    currentSpace: SpaceItem? = null,
    spaces: List<SpaceItem> = emptyList(),
    initialTab: BottomNavTab? = null,
    unreadNotificationCount: Int = 0,
    userAvatarUrl: String? = null,
    onSpaceSelected: (SpaceItem) -> Unit = {},
    onNavigateToCreateTeam: () -> Unit = {},
    onNavigateToJoinTeam: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToNoteEdit: (Long?) -> Unit = {},
    onNavigateToProject: (Long) -> Unit = {},
    onNavigateToTaskDetail: (Long) -> Unit = {},
    onNavigateToFilePreview: (Long) -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToTaskCreate: () -> Unit = {},
    onNavigateToTodoCreate: () -> Unit = {}
) {
    var selectedTab by rememberSaveable(initialTab) { mutableStateOf(initialTab ?: BottomNavTab.HOME) }
    var isSpaceTransitioning by remember { mutableStateOf(false) }

    val effectiveSpaces = spaces
    val effectiveCurrentSpace = currentSpace ?: SpaceItem(
        id = EMPTY_SPACE_ID,
        name = "未选择空间",
        type = SpaceType.PERSONAL
    )
    val hasRealSpace = currentSpace != null && spaces.isNotEmpty()
    val maxContentWidth = if (effectiveCurrentSpace.type == SpaceType.TEAM) {
        TEAM_MAX_CONTENT_WIDTH
    } else {
        PERSONAL_MAX_CONTENT_WIDTH
    }

    LaunchedEffect(isSpaceTransitioning) {
        if (isSpaceTransitioning) {
            delay(450)
            isSpaceTransitioning = false
        }
    }
    LaunchedEffect(effectiveCurrentSpace.type) {
        val availableTabs = if (effectiveCurrentSpace.type == SpaceType.TEAM) {
            TeamSpaceTabs
        } else {
            PersonalSpaceTabs
        }
        if (selectedTab !in availableTabs) {
            selectedTab = availableTabs.first()
        }
    }

    Scaffold(
        topBar = {
            SpaceAwareTopAppBar(
                spaceType = effectiveCurrentSpace.type,
                spaceName = effectiveCurrentSpace.name,
                currentSpace = effectiveCurrentSpace,
                spaces = effectiveSpaces,
                onSpaceSelected = { space ->
                    if (space.id != effectiveCurrentSpace.id) {
                        isSpaceTransitioning = true
                        onSpaceSelected(space)
                        selectedTab = if (space.type == SpaceType.TEAM) BottomNavTab.PROJECT
                                      else BottomNavTab.HOME
                    }
                },
                onCreateTeamSpace = onNavigateToCreateTeam,
                onJoinTeam = onNavigateToJoinTeam,
                onSearchClick = onNavigateToSearch,
                onNotificationClick = onNavigateToNotifications,
                onProfileClick = onNavigateToSettings,
                unreadNotificationCount = unreadNotificationCount,
                userAvatarUrl = userAvatarUrl
            )
        },
        bottomBar = {
            SpaceAwareBottomNavBar(
                spaceType = effectiveCurrentSpace.type,
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = maxContentWidth)
            ) {
                if (hasRealSpace) {
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            (fadeIn(tween(250)) + slideInHorizontally(tween(250)) { it / 4 })
                                .togetherWith(fadeOut(tween(200)) + slideOutHorizontally(tween(200)) { -it / 4 })
                        },
                        label = "tabContent"
                    ) { tab ->
                        ContentForTab(
                            tab = tab,
                            spaceId = effectiveCurrentSpace.id,
                            onNavigateToNoteEdit = onNavigateToNoteEdit,
                            onNavigateToProject = onNavigateToProject,
                            onNavigateToTaskDetail = onNavigateToTaskDetail,
                            onNavigateToFilePreview = onNavigateToFilePreview,
                            onNavigateToTaskCreate = onNavigateToTaskCreate,
                            onNavigateToTodoCreate = onNavigateToTodoCreate,
                            isTeamSpace = effectiveCurrentSpace.type == SpaceType.TEAM,
                            onOpenNoteList = { selectedTab = BottomNavTab.NOTE },
                            onOpenTodoList = { selectedTab = BottomNavTab.TODO },
                        )
                    }
                } else {
                    NoSpaceContent(
                        modifier = Modifier.fillMaxSize(),
                        onCreateTeam = onNavigateToCreateTeam,
                        onJoinTeam = onNavigateToJoinTeam
                    )
                }

                AnimatedVisibility(
                    visible = isSpaceTransitioning,
                    enter = fadeIn(tween(300)),
                    exit = fadeOut(tween(300))
                ) {
                    SpaceTransitionOverlay(effectiveCurrentSpace.name)
                }
            }
        }
    }
}

@Composable
private fun ContentForTab(
    tab: BottomNavTab,
    spaceId: Long,
    onNavigateToNoteEdit: (Long?) -> Unit,
    onNavigateToProject: (Long) -> Unit,
    onNavigateToTaskDetail: (Long) -> Unit,
    onNavigateToFilePreview: (Long) -> Unit,
    onNavigateToTaskCreate: () -> Unit,
    onNavigateToTodoCreate: () -> Unit,
    isTeamSpace: Boolean,
    onOpenNoteList: () -> Unit,
    onOpenTodoList: () -> Unit
) {
    when (tab) {
        BottomNavTab.HOME -> PersonalHomeContent(
            spaceId = spaceId,
            onCreateNote = { onNavigateToNoteEdit(null) },
            onOpenNote = { onNavigateToNoteEdit(it) },
            onOpenNoteList = onOpenNoteList,
            onOpenTodoList = onOpenTodoList
        )
        BottomNavTab.PROJECT -> ProjectRoute(
            modifier = Modifier.fillMaxSize(),
            spaceId = spaceId,
            onProjectClick = onNavigateToProject
        )
        BottomNavTab.NOTE -> NoteListRoute(
            modifier = Modifier.fillMaxSize(),
            spaceId = spaceId,
            onNoteClick = { onNavigateToNoteEdit(it) },
            onCreateNote = { onNavigateToNoteEdit(null) }
        )
        BottomNavTab.DOCUMENT -> NoteListRoute(
            modifier = Modifier.fillMaxSize(),
            spaceId = spaceId,
            title = "文档",
            subtitle = "团队知识与协作资料",
            onNoteClick = { onNavigateToNoteEdit(it) },
            onCreateNote = { onNavigateToNoteEdit(null) }
        )
        BottomNavTab.TASK -> TaskListRoute(
            modifier = Modifier.fillMaxSize(),
            spaceId = spaceId,
            onTaskClick = onNavigateToTaskDetail,
            onCreateTask = onNavigateToTaskCreate
        )
        BottomNavTab.TODO -> TodoListRoute(Modifier.fillMaxSize(), spaceId = spaceId, onCreateTodo = onNavigateToTodoCreate)
        BottomNavTab.FILE -> FileBrowserRoute(
            spaceId = spaceId,
            modifier = Modifier.fillMaxSize(),
            onFileClick = onNavigateToFilePreview
        )
        BottomNavTab.STATS -> StatsRoute(
            modifier = Modifier.fillMaxSize(),
            spaceId = spaceId,
            isTeamSpace = isTeamSpace
        )
        BottomNavTab.MEMBERS -> MembersRoute(Modifier.fillMaxSize(), spaceId = spaceId)
    }
}

@Composable
private fun NoSpaceContent(
    modifier: Modifier = Modifier,
    onCreateTeam: () -> Unit,
    onJoinTeam: () -> Unit
) {
    Box(
        modifier = modifier.padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "当前还没有可用空间",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "先创建团队空间，或通过邀请码加入现有团队。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onCreateTeam,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("新建团队")
                    }
                    TextButton(
                        onClick = onJoinTeam,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("加入团队")
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonalHomeContent(
    spaceId: Long,
    onCreateNote: () -> Unit,
    onOpenNote: (Long) -> Unit,
    onOpenNoteList: () -> Unit,
    onOpenTodoList: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val contentState = uiState as? PersonalHomeUiState.Content

    LaunchedEffect(spaceId) {
        viewModel.load(spaceId)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(184.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Image(
                    painter = painterResource(R.drawable.personal_home),
                    contentDescription = "首页封面",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = "今天也从最重要的一件事开始。",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "最近笔记和未完成待办会在这里汇总。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickActionCard(
                    title = "写笔记",
                    subtitle = "记录灵感",
                    icon = Icons.Filled.Edit,
                    iconTint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    onClick = onCreateNote
                )
                QuickActionCard(
                    title = "待办事项",
                    subtitle = "${contentState?.pendingTodoTotal ?: 0} 项未完成",
                    icon = Icons.Filled.CheckCircle,
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenTodoList
                )
            }
        }

        item {
            PersonalNoteTrendCard(
                trends = contentState?.personalNoteTrends.orEmpty(),
                isLoading = uiState is PersonalHomeUiState.Loading
            )
        }

        if (uiState is PersonalHomeUiState.Loading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        contentState?.errorMessage?.let { message ->
            item {
                EmptyStateCard(
                    title = "同步遇到问题",
                    subtitle = message
                )
            }
        }

        item {
            SectionHeader(title = "最近笔记", actionText = "查看全部", onActionClick = onOpenNoteList)
        }

        if (contentState?.recentNotes.isNullOrEmpty() && uiState !is PersonalHomeUiState.Loading) {
            item {
                EmptyStateCard(
                    title = "暂无最近笔记",
                    subtitle = "写下第一条内容后会出现在这里"
                )
            }
        } else {
            items(contentState?.recentNotes.orEmpty().take(3), key = { note -> "home-note-${note.id}" }) { note ->
                PersonalNoteCard(note = note, onClick = { onOpenNote(note.id) })
            }
        }

        item {
            SectionHeader(title = "未完成待办", actionText = "查看全部", onActionClick = onOpenTodoList)
        }

        if (contentState?.pendingTodos.isNullOrEmpty() && uiState !is PersonalHomeUiState.Loading) {
            item {
                EmptyStateCard(
                    title = "待办已清空",
                    subtitle = "当前空间没有未完成事项"
                )
            }
        } else {
            items(contentState?.pendingTodos.orEmpty().take(3), key = { todo -> "home-todo-${todo.id}" }) { todo ->
                PersonalTodoCard(todo = todo)
            }
        }

        item {
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun PersonalNoteTrendCard(
    trends: List<PersonalNoteTrend>,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "最近 7 日笔记趋势",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (trends.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TrendLegend(
                            color = MaterialTheme.colorScheme.primary,
                            label = "新建"
                        )
                        TrendLegend(
                            color = MaterialTheme.colorScheme.tertiary,
                            label = "更新"
                        )
                    }
                }
            }
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(122.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                trends.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(122.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暂无统计数据",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    val visibleTrends = trends.takeLast(7)
                    val maxCount = visibleTrends.maxOf { trend ->
                        maxOf(trend.createdCount, trend.updatedCount)
                    }.coerceAtLeast(1L)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(156.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        visibleTrends.forEach { trend ->
                            HomeTrendBarGroup(
                                trend = trend,
                                maxCount = maxCount,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeTrendBarGroup(
    trend: PersonalNoteTrend,
    maxCount: Long,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.height(112.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            HomeTrendBar(
                count = trend.createdCount,
                maxCount = maxCount,
                color = MaterialTheme.colorScheme.primary
            )
            HomeTrendBar(
                count = trend.updatedCount,
                maxCount = maxCount,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = trend.date.shortTrendDate(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Text(
            text = "${trend.createdCount + trend.updatedCount}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun HomeTrendBar(
    count: Long,
    maxCount: Long,
    color: Color
) {
    val ratio = (count.toFloat() / maxCount.toFloat()).coerceIn(0f, 1f)
    val height = if (count == 0L) 4.dp else (104.dp * ratio).coerceAtLeast(8.dp)
    Box(
        modifier = Modifier
            .width(8.dp)
            .height(height)
            .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
            .background(if (count == 0L) MaterialTheme.colorScheme.outlineVariant else color)
    )
}

@Composable
private fun TrendLegend(
    color: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun String.shortTrendDate(): String {
    return if (length >= 5) {
        takeLast(5)
    } else {
        this
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(108.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconTint.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = iconTint)
            }
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, actionText: String, onActionClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        TextButton(onClick = onActionClick) {
            Text(actionText, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun PersonalNoteCard(note: Note, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = formatDateTimeText(note.gmtModified).ifBlank { "未记录更新时间" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                note.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            note.content?.takeIf { it.isNotBlank() }?.let { content ->
                Spacer(Modifier.height(6.dp))
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (note.tags.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    note.tags.take(3).forEach { tag -> TagChip("#${tag.name}") }
                }
            }
        }
    }
}

@Composable
private fun PersonalTodoCard(todo: Todo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    todo.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                todo.deadline?.let { deadline ->
                    Text(
                        "截止 ${formatDateTimeText(deadline)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyStateCard(title: String, subtitle: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
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

private val PERSONAL_MAX_CONTENT_WIDTH = 840.dp
private val TEAM_MAX_CONTENT_WIDTH = 1180.dp
private const val EMPTY_SPACE_ID = -1L
