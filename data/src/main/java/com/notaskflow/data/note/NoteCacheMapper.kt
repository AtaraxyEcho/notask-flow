package com.notaskflow.data.note

import com.notaskflow.core.database.NoteEntity
import com.notaskflow.core.database.NotebookEntity
import com.notaskflow.domain.model.Note
import com.notaskflow.domain.model.Notebook

fun Note.toNoteEntity(cachedAt: Long): NoteEntity {
    return NoteEntity(
        id = id,
        spaceId = spaceId,
        notebookId = notebookId,
        projectId = projectId,
        projectName = projectName,
        title = title,
        content = content,
        contentHtml = contentHtml,
        canEdit = canEdit,
        isPublic = isPublic,
        gmtModified = gmtModified,
        syncStatus = SYNC_STATUS_SYNCED,
        cachedAt = cachedAt
    )
}

fun NoteEntity.toCachedNote(): Note {
    return Note(
        id = id,
        spaceId = spaceId,
        notebookId = notebookId,
        projectId = projectId,
        projectName = projectName,
        userId = null,
        title = title,
        content = content,
        contentHtml = contentHtml,
        canEdit = canEdit,
        collabEnabled = false,
        isPublic = isPublic,
        shareCode = null,
        shareExpire = null,
        viewCount = 0,
        gmtCreate = null,
        gmtModified = gmtModified,
        tags = emptyList()
    )
}

fun List<Notebook>.toNotebookEntities(cachedAt: Long): List<NotebookEntity> {
    return flatMap { notebook -> notebook.toNotebookEntityList(cachedAt) }
}

fun List<NotebookEntity>.toNotebookTree(): List<Notebook> {
    val groupedByParent = groupBy { it.parentId }

    fun build(parentId: Long?): List<Notebook> {
        return groupedByParent[parentId]
            .orEmpty()
            .sortedWith(compareBy<NotebookEntity> { it.sortOrder }.thenBy { it.id })
            .map { entity ->
                Notebook(
                    id = entity.id,
                    spaceId = entity.spaceId,
                    parentId = entity.parentId,
                    name = entity.name,
                    sortOrder = entity.sortOrder,
                    children = build(entity.id)
                )
            }
    }

    return build(null)
}

private fun Notebook.toNotebookEntityList(cachedAt: Long): List<NotebookEntity> {
    val current = NotebookEntity(
        id = id,
        spaceId = spaceId,
        parentId = parentId,
        name = name,
        sortOrder = sortOrder,
        cachedAt = cachedAt
    )
    return listOf(current) + children.toNotebookEntities(cachedAt)
}

private const val SYNC_STATUS_SYNCED = "SYNCED"
