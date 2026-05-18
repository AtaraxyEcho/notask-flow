package com.notaskflow.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notaskflow.core.model.SpaceType
import com.notaskflow.core.ui.theme.TeamColors
import com.notaskflow.core.ui.theme.SunriseColors

// 空间项数据模型
data class SpaceItem(
    val id: Long,
    val name: String,
    val type: SpaceType,
    val memberCount: Int = 0,
    val hasUnread: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpaceSwitcher(
    currentSpace: SpaceItem,
    spaces: List<SpaceItem>,
    onSpaceSelected: (SpaceItem) -> Unit,
    onCreateTeamSpace: () -> Unit,
    onJoinTeam: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val borderColor = when (currentSpace.type) {
        SpaceType.PERSONAL -> MaterialTheme.colorScheme.outlineVariant
        SpaceType.TEAM -> TeamColors.outlineVariant
    }
    val containerColor = when (currentSpace.type) {
        SpaceType.PERSONAL -> MaterialTheme.colorScheme.surfaceContainer
        SpaceType.TEAM -> TeamColors.surfaceContainerLow
    }
    val contentColor = when (currentSpace.type) {
        SpaceType.PERSONAL -> MaterialTheme.colorScheme.primary
        SpaceType.TEAM -> TeamColors.primary
    }

    Surface(
        onClick = { showSheet = true },
        modifier = modifier
            .height(32.dp)
            .widthIn(min = 64.dp, max = 88.dp),
        shape = RoundedCornerShape(17.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.72f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (currentSpace.type == SpaceType.PERSONAL)
                    Icons.Filled.Person else Icons.Filled.Groups,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = contentColor
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = currentSpace.name,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.width(2.dp))
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = "展开空间列表",
                modifier = Modifier.size(13.dp),
                tint = contentColor
            )
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            containerColor = if (currentSpace.type == SpaceType.TEAM) {
                TeamColors.surface
            } else {
                MaterialTheme.colorScheme.surface
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "切换空间",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(16.dp))

                val personalSpaces = spaces.filter { it.type == SpaceType.PERSONAL }
                if (personalSpaces.isNotEmpty()) {
                    Text(
                        text = "个人空间",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    personalSpaces.forEach { space ->
                        SpaceListItem(
                            space = space,
                            isSelected = space.id == currentSpace.id,
                            onClick = {
                                onSpaceSelected(space)
                                showSheet = false
                            }
                        )
                    }
                }

                val teamSpaces = spaces.filter { it.type == SpaceType.TEAM }
                if (teamSpaces.isNotEmpty()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    Text(
                        text = "团队空间",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                        items(teamSpaces, key = { space -> space.id }) { space ->
                            SpaceListItem(
                                space = space,
                                isSelected = space.id == currentSpace.id,
                                onClick = {
                                    onSpaceSelected(space)
                                    showSheet = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onCreateTeamSpace()
                            showSheet = false
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, TeamColors.outlineVariant)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("新建团队")
                    }
                    OutlinedButton(
                        onClick = {
                        onJoinTeam()
                        showSheet = false
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, TeamColors.outlineVariant)
                ) {
                    Icon(Icons.Filled.Groups, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("加入团队")
                }
                }
            }
        }
    }
}

@Composable
private fun SpaceListItem(
    space: SpaceItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) {
        when (space.type) {
            SpaceType.TEAM -> TeamColors.primaryFixed
            else -> MaterialTheme.colorScheme.surfaceVariant
        }
    } else {
        when (space.type) {
            SpaceType.TEAM -> TeamColors.surfaceContainerLow
            else -> MaterialTheme.colorScheme.surface
        }
    }
    val textColor = if (isSelected) {
        when (space.type) {
            SpaceType.TEAM -> TeamColors.primary
            else -> MaterialTheme.colorScheme.primary
        }
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (space.type == SpaceType.PERSONAL) Icons.Filled.Person
                          else Icons.Filled.Groups,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = textColor
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = space.name,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (space.type == SpaceType.TEAM) {
            Text(
                text = "${space.memberCount}人",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (space.hasUnread) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error)
            )
        }
        if (isSelected) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "当前空间",
                modifier = Modifier.size(18.dp),
                tint = when (space.type) {
                    SpaceType.TEAM -> TeamColors.primary
                    SpaceType.PERSONAL -> MaterialTheme.colorScheme.primary
                }
            )
        }
    }
}
