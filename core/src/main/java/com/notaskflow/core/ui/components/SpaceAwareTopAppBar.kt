package com.notaskflow.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.notaskflow.core.model.SpaceType
import com.notaskflow.core.ui.theme.TeamColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpaceAwareTopAppBar(
    spaceType: SpaceType,
    spaceName: String,
    spaces: List<SpaceItem>,
    currentSpace: SpaceItem,
    onSpaceSelected: (SpaceItem) -> Unit,
    onCreateTeamSpace: () -> Unit,
    onJoinTeam: () -> Unit,
    onSearchClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onProfileClick: () -> Unit,
    unreadNotificationCount: Int = 0,
    userAvatarUrl: String? = null,
    modifier: Modifier = Modifier
) {
    val isTeam = spaceType == SpaceType.TEAM
    val topBarContent = if (isTeam) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    TopAppBar(
        modifier = modifier,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Logo 区域
                Text(
                    text = "Notask Flow",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isTeam) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        actions = {
            // 空间切换器
            SpaceSwitcher(
                currentSpace = currentSpace,
                spaces = spaces,
                onSpaceSelected = onSpaceSelected,
                onCreateTeamSpace = onCreateTeamSpace,
                onJoinTeam = onJoinTeam
            )
            Spacer(modifier = Modifier.width(4.dp))

            // 搜索按钮
            IconButton(
                onClick = onSearchClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "搜索",
                    tint = if (isTeam) MaterialTheme.colorScheme.onPrimary
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            // 通知铃铛（含未读角标）
            IconButton(
                onClick = onNotificationClick,
                modifier = Modifier.size(40.dp)
            ) {
                BadgedBox(
                    badge = {
                        if (unreadNotificationCount > 0) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.error
                            ) {
                                Text(
                                    text = if (unreadNotificationCount > 99) "99+"
                                           else unreadNotificationCount.toString(),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = "通知",
                        tint = if (isTeam) MaterialTheme.colorScheme.onPrimary
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 用户头像
            IconButton(
                onClick = onProfileClick,
                modifier = Modifier.size(40.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (userAvatarUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(userAvatarUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "个人资料",
                            modifier = Modifier.size(32.dp),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "个人资料",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        },
        colors = if (isTeam) TopAppBarDefaults.topAppBarColors(
            containerColor = TeamColors.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimary
        ) else TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}
