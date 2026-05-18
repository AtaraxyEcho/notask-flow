package com.notaskflow.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.notaskflow.core.R
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
    val actionTint = if (isTeam) TeamColors.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val avatarBackground = if (isTeam) TeamColors.primaryFixed else MaterialTheme.colorScheme.surfaceVariant

    TopAppBar(
        modifier = modifier,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier
                        .size(30.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = if (isTeam) BorderStroke(1.dp, TeamColors.glassBorder) else null
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(R.drawable.notask_logo),
                            contentDescription = null,
                            modifier = Modifier.size(26.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.widthIn(max = if (isTeam) 108.dp else 102.dp)) {
                    Text(
                        text = "Notask Flow",
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
                        color = if (isTeam) TeamColors.primary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isTeam) spaceName else "个人工作台",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = if (isTeam) TeamColors.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        actions = {
            SpaceSwitcher(
                currentSpace = currentSpace,
                spaces = spaces,
                onSpaceSelected = onSpaceSelected,
                onCreateTeamSpace = onCreateTeamSpace,
                onJoinTeam = onJoinTeam
            )
            Spacer(modifier = Modifier.width(2.dp))

            // 搜索按钮
            IconButton(
                onClick = onSearchClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "搜索",
                    tint = actionTint,
                    modifier = Modifier.size(19.dp)
                )
            }

            IconButton(
                onClick = onNotificationClick,
                modifier = Modifier.size(36.dp)
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
                        tint = actionTint,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            IconButton(
                onClick = onProfileClick,
                modifier = Modifier.size(36.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(avatarBackground),
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
                            tint = actionTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        },
        colors = if (isTeam) TopAppBarDefaults.topAppBarColors(
            containerColor = TeamColors.glassBackground,
            titleContentColor = TeamColors.onSurface
        ) else TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}
