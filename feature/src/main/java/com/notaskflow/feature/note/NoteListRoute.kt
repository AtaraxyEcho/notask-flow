package com.notaskflow.feature.note

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

data class NotebookNode(
    val id: Long, val name: String, val noteCount: Int = 0,
    val children: List<NotebookNode> = emptyList()
)

data class NoteSummary(
    val id: Long, val title: String, val excerpt: String, val updatedAt: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteListRoute(
    modifier: Modifier = Modifier,
    onNoteClick: (Long) -> Unit = {},
    onCreateNote: () -> Unit = {}
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val notebooks = remember {
        listOf(
            NotebookNode(1, "Personal", 42), NotebookNode(2, "Work", 18),
            NotebookNode(3, "Ideas", 105), NotebookNode(4, "Journal", 365)
        )
    }
    val notes = remember {
        listOf(
            NoteSummary(1, "Project Alpha Brainstorm", "Key takeaways from the meeting regarding the new timeline.", "2h ago"),
            NoteSummary(2, "Weekly Reflections", "It felt like a very productive week, managed to clear the backlog.", "Yesterday"),
            NoteSummary(3, "Reading Notes: Deep Work", "Cal Newport argues that focus is the new IQ.", "2 days ago"),
            NoteSummary(4, "Design System Tokens", "Spacing, color, and typography tokens for mobile.", "3 days ago"),
            NoteSummary(5, "Sprint Planning Q2", "Priorities: mobile app, API v2, performance.", "1 week ago")
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet { NotebookDrawerContent(notebooks, onNotebookClick = { scope.launch { drawerState.close() } }) }
        }
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // 笔记本网格
                item {
                    val rows = notebooks.chunked(2)
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        rows.forEach { rowItems ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                rowItems.forEach { nb -> NotebookCard(nb, Modifier.weight(1f)) }
                                if (rowItems.size < 2) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }

                // 最近笔记标题
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Recent Notes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // 笔记卡片列表 — 立体阴影设计
                items(notes) { note ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 7.dp, horizontal = 1.dp)
                            .shadow(
                                elevation = 4.dp,
                                shape = RoundedCornerShape(16.dp),
                                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            )
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onNoteClick(note.id) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = note.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = note.updatedAt,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = note.excerpt,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(80.dp)) }
            }

            // FAB
            FloatingActionButton(
                onClick = onCreateNote,
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.Add, "新建笔记", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Composable
private fun NotebookCard(notebook: NotebookNode, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Folder, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(16.dp))
            Text(notebook.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text("${notebook.noteCount} notes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun NotebookDrawerContent(notebooks: List<NotebookNode>, onNotebookClick: (Long) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Text("笔记本", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp)); HorizontalDivider(); Spacer(Modifier.height(8.dp))
        notebooks.forEach { nb -> NotebookTreeItem(nb, onNotebookClick) }
        Spacer(Modifier.weight(1f)); HorizontalDivider(); Spacer(Modifier.height(8.dp))
        TextButton(onClick = { }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("新建笔记本")
        }
    }
}

@Composable
private fun NotebookTreeItem(node: NotebookNode, onClick: (Long) -> Unit, depth: Int = 0) {
    var expanded by remember { mutableStateOf(true) }
    val hasChildren = node.children.isNotEmpty()
    Column {
        Row(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small)
            .clickable { if (hasChildren) expanded = !expanded else onClick(node.id) }
            .padding(vertical = 10.dp, horizontal = 12.dp).padding(start = (depth * 20).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (hasChildren) {
                Icon(if (expanded) Icons.Filled.ExpandMore else Icons.Filled.ChevronRight,
                    if (expanded) "折叠" else "展开", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            } else { Spacer(Modifier.size(18.dp)) }
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Filled.Folder, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(node.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            if (node.noteCount > 0) Text("${node.noteCount}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (hasChildren && expanded) node.children.forEach { c -> NotebookTreeItem(c, onClick, depth + 1) }
    }
}
