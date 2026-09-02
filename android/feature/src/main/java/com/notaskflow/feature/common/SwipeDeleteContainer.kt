package com.notaskflow.feature.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeDeleteContainer(
    onDeleteRequest: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    cornerRadius: Dp = 18.dp,
    deleteFromStartToEnd: Boolean = false,
    content: @Composable () -> Unit
) {
    if (!enabled) {
        Box(modifier = modifier) {
            content()
        }
        return
    }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            val deleteValue = if (deleteFromStartToEnd) {
                SwipeToDismissBoxValue.StartToEnd
            } else {
                SwipeToDismissBoxValue.EndToStart
            }
            if (value == deleteValue) {
                onDeleteRequest()
            }
            false
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier.clip(RoundedCornerShape(cornerRadius)),
        enableDismissFromStartToEnd = deleteFromStartToEnd,
        enableDismissFromEndToStart = !deleteFromStartToEnd,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(
                        start = if (deleteFromStartToEnd) 22.dp else 0.dp,
                        end = if (deleteFromStartToEnd) 0.dp else 22.dp
                    ),
                contentAlignment = if (deleteFromStartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "删除",
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        },
        content = { content() }
    )
}
