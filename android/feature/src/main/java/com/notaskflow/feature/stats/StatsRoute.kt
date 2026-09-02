package com.notaskflow.feature.stats

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.notaskflow.core.common.formatDateTimeText
import com.notaskflow.domain.model.MemberTaskLoad
import com.notaskflow.domain.model.PersonalNoteTrend
import com.notaskflow.domain.model.StatsActivity

data class StatCard(
    val label: String,
    val value: String,
    val icon: ImageVector
)

@Composable
fun StatsRoute(
    modifier: Modifier = Modifier,
    spaceId: Long? = null,
    isTeamSpace: Boolean = false,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(spaceId, isTeamSpace) {
        if (isTeamSpace && spaceId != null) {
            viewModel.load(spaceId)
        } else if (!isTeamSpace) {
            viewModel.loadPersonal()
        }
    }

    val stats = if (isTeamSpace) {
        val createdTotal = uiState.trends.sumOf { it.createdCount }
        val completedTotal = uiState.trends.sumOf { it.completedCount }
        val activeLoad = uiState.loads.sumOf { it.loadCount }
        val roleCompleted = uiState.roleCompletions.sumOf { it.completedCount }
        listOf(
            StatCard("近 7 日创建", createdTotal.toString(), Icons.AutoMirrored.Filled.TrendingUp),
            StatCard("近 7 日完成", completedTotal.toString(), Icons.Filled.CheckCircle),
            StatCard("当前负载", activeLoad.toString(), Icons.Filled.Work),
            StatCard("角色完成", roleCompleted.toString(), Icons.Filled.Group)
        )
    } else {
        val personalStats = uiState.personalStats
        val createdTotal = uiState.personalNoteTrends.sumOf { it.createdCount }
        val updatedTotal = uiState.personalNoteTrends.sumOf { it.updatedCount }
        listOf(
            StatCard("笔记总数", (personalStats?.noteCount ?: 0L).toString(), Icons.AutoMirrored.Filled.TrendingUp),
            StatCard("未完职责", (personalStats?.unfinishedTaskMemberCount ?: 0L).toString(), Icons.Filled.Work),
            StatCard("本月完成", (personalStats?.completedTaskCountThisMonth ?: 0L).toString(), Icons.Filled.CheckCircle),
            StatCard("近 7 日更新", (createdTotal + updatedTotal).toString(), Icons.Filled.Update)
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "统计",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = if (isTeamSpace) "任务趋势、成员负载与近期动态" else "个人笔记、职责与本月完成情况",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (isTeamSpace && spaceId == null) {
            item { StateText("请先选择空间") }
        } else if (uiState.isLoading && uiState.trends.isEmpty() && uiState.loads.isEmpty() && uiState.personalStats == null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else {
            uiState.errorMessage?.let { message ->
                item {
                    Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
            item {
                StatsGrid(stats)
            }
            if (isTeamSpace) {
                item {
                    Text("成员负载", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                if (uiState.loads.isEmpty()) {
                    item { StateText("暂无负载数据") }
                } else {
                    items(uiState.loads, key = { load -> "member-load-${load.userId}" }) { load ->
                        LoadRow(load)
                    }
                }
                item {
                    Text("近期动态", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                if (uiState.activities.isEmpty()) {
                    item { StateText("暂无动态") }
                } else {
                    items(uiState.activities, key = { activity -> "activity-${activity.time}-${activity.memberUserId}" }) { activity ->
                        ActivityListItem(activity = activity)
                    }
                }
            } else {
                item {
                    Text("笔记趋势", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                if (uiState.personalNoteTrends.isEmpty()) {
                    item { StateText("暂无趋势数据") }
                } else {
                    item {
                        PersonalNoteTrendChart(uiState.personalNoteTrends)
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun PersonalNoteTrendChart(trends: List<PersonalNoteTrend>) {
    val maxCount = trends.maxOf { trend ->
        maxOf(trend.createdCount, trend.updatedCount)
    }.coerceAtLeast(1L)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("近 7 日笔记趋势", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ChartLegend(color = MaterialTheme.colorScheme.primary, label = "新建")
                    ChartLegend(color = MaterialTheme.colorScheme.tertiary, label = "更新")
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(168.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                trends.forEach { trend ->
                    TrendBarGroup(
                        trend = trend,
                        maxCount = maxCount,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TrendBarGroup(
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
            TrendBar(
                count = trend.createdCount,
                maxCount = maxCount,
                color = MaterialTheme.colorScheme.primary
            )
            TrendBar(
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
private fun TrendBar(count: Long, maxCount: Long, color: Color) {
    val ratio = (count.toFloat() / maxCount.toFloat()).coerceIn(0f, 1f)
    val height = if (count == 0L) 4.dp else (104.dp * ratio).coerceAtLeast(8.dp)
    Box(
        modifier = Modifier
            .width(9.dp)
            .height(height)
            .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
            .background(if (count == 0L) MaterialTheme.colorScheme.outlineVariant else color)
    )
}

@Composable
private fun ChartLegend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(50))
                .background(color)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatsGrid(stats: List<StatCard>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        stats.chunked(2).forEach { rowStats ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowStats.forEach { stat ->
                    StatCardView(stat = stat, modifier = Modifier.weight(1f))
                }
                if (rowStats.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StatCardView(
    stat: StatCard,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(
                imageVector = stat.icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stat.value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stat.label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LoadRow(load: MemberTaskLoad) {
    val denominator = load.loadCount.coerceAtLeast(1L)
    val progress = (load.completedCount.toFloat() / denominator).coerceIn(0f, 1f)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(load.username.ifBlank { "成员" }, fontWeight = FontWeight.SemiBold)
                Text("${load.completedCount}/${load.loadCount}", color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun ActivityListItem(activity: StatsActivity) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = activity.content.ifBlank { activity.type },
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = listOf(activity.member, formatDateTimeText(activity.time))
                .filterNot { it.isNullOrBlank() }
                .joinToString(" · "),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StateText(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun String.shortTrendDate(): String {
    return if (length >= 5) {
        takeLast(5)
    } else {
        this
    }
}
