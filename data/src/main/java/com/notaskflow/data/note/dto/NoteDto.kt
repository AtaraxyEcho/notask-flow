package com.notaskflow.data.note.dto

import com.notaskflow.domain.model.CollabContentSave
import com.notaskflow.domain.model.CollabTicket
import com.notaskflow.domain.model.Note
import com.notaskflow.domain.model.NoteAttachment
import com.notaskflow.domain.model.NoteHistory
import com.notaskflow.domain.model.NoteSave
import com.notaskflow.domain.model.Notebook
import com.notaskflow.domain.model.NotebookSave
import com.notaskflow.domain.model.Tag
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TagDto(
    @param:Json(name = "id") val id: Long,
    @param:Json(name = "name") val name: String,
    @param:Json(name = "spaceId") val spaceId: Long
)

@JsonClass(generateAdapter = true)
data class NotebookDto(
    @param:Json(name = "id") val id: Long,
    @param:Json(name = "spaceId") val spaceId: Long,
    @param:Json(name = "parentId") val parentId: Long?,
    @param:Json(name = "name") val name: String,
    @param:Json(name = "sortOrder") val sortOrder: Int?,
    @param:Json(name = "children") val children: List<NotebookDto>?
)

@JsonClass(generateAdapter = true)
data class NotebookSaveRequestDto(
    @param:Json(name = "parentId") val parentId: Long?,
    @param:Json(name = "name") val name: String,
    @param:Json(name = "sortOrder") val sortOrder: Int?
)

@JsonClass(generateAdapter = true)
data class NoteDto(
    @param:Json(name = "id") val id: Long,
    @param:Json(name = "spaceId") val spaceId: Long,
    @param:Json(name = "notebookId") val notebookId: Long?,
    @param:Json(name = "projectId") val projectId: Long?,
    @param:Json(name = "projectName") val projectName: String?,
    @param:Json(name = "userId") val userId: Long?,
    @param:Json(name = "title") val title: String,
    @param:Json(name = "content") val content: String?,
    @param:Json(name = "contentHtml") val contentHtml: String?,
    @param:Json(name = "canEdit") val canEdit: Boolean?,
    @param:Json(name = "collabEnabled") val collabEnabled: Boolean?,
    @param:Json(name = "isPublic") val isPublic: Boolean?,
    @param:Json(name = "shareCode") val shareCode: String?,
    @param:Json(name = "shareExpire") val shareExpire: String?,
    @param:Json(name = "viewCount") val viewCount: Int?,
    @param:Json(name = "gmtCreate") val gmtCreate: String?,
    @param:Json(name = "gmtModified") val gmtModified: String?,
    @param:Json(name = "tags") val tags: List<TagDto>?
)

@JsonClass(generateAdapter = true)
data class NoteSaveRequestDto(
    @param:Json(name = "notebookId") val notebookId: Long,
    @param:Json(name = "title") val title: String,
    @param:Json(name = "projectId") val projectId: Long?,
    @param:Json(name = "content") val content: String?,
    @param:Json(name = "contentHtml") val contentHtml: String?,
    @param:Json(name = "isPublic") val isPublic: Boolean,
    @param:Json(name = "tagIds") val tagIds: List<Long>,
    @param:Json(name = "saveType") val saveType: String
)

@JsonClass(generateAdapter = true)
data class NoteShareRequestDto(
    @param:Json(name = "expireAt") val expireAt: String?
)

@JsonClass(generateAdapter = true)
data class CollabTicketDto(
    @param:Json(name = "ticket") val ticket: String?,
    @param:Json(name = "expiresIn") val expiresIn: Int?
)

@JsonClass(generateAdapter = true)
data class CollabContentSaveRequestDto(
    @param:Json(name = "content") val content: String,
    @param:Json(name = "contentHtml") val contentHtml: String?
)

@JsonClass(generateAdapter = true)
data class NoteHistoryDto(
    @param:Json(name = "id") val id: Long,
    @param:Json(name = "noteId") val noteId: Long,
    @param:Json(name = "title") val title: String?,
    @param:Json(name = "content") val content: String?,
    @param:Json(name = "version") val version: Int?,
    @param:Json(name = "changeSummary") val changeSummary: String?,
    @param:Json(name = "saveType") val saveType: String?,
    @param:Json(name = "gmtCreate") val gmtCreate: String?
)

@JsonClass(generateAdapter = true)
data class NoteAttachmentDto(
    @param:Json(name = "id") val id: Long,
    @param:Json(name = "fileName") val fileName: String?,
    @param:Json(name = "fileSize") val fileSize: Long?,
    @param:Json(name = "mimeType") val mimeType: String?,
    @param:Json(name = "downloadUrl") val downloadUrl: String?,
    @param:Json(name = "gmtCreate") val gmtCreate: String?
)

fun TagDto.toDomain(): Tag {
    return Tag(
        id = id,
        name = name,
        spaceId = spaceId
    )
}

fun NotebookDto.toDomain(): Notebook {
    return Notebook(
        id = id,
        spaceId = spaceId,
        parentId = parentId,
        name = name,
        sortOrder = sortOrder ?: 0,
        children = children.orEmpty().map { it.toDomain() }
    )
}

fun NotebookSave.toDto(): NotebookSaveRequestDto {
    return NotebookSaveRequestDto(
        parentId = parentId,
        name = name,
        sortOrder = sortOrder
    )
}

fun NoteDto.toDomain(): Note {
    return Note(
        id = id,
        spaceId = spaceId,
        notebookId = notebookId,
        projectId = projectId,
        projectName = projectName,
        userId = userId,
        title = title,
        content = content,
        contentHtml = contentHtml,
        canEdit = canEdit == true,
        collabEnabled = collabEnabled == true,
        isPublic = isPublic == true,
        shareCode = shareCode,
        shareExpire = shareExpire,
        viewCount = viewCount ?: 0,
        gmtCreate = gmtCreate,
        gmtModified = gmtModified,
        tags = tags.orEmpty().map { it.toDomain() }
    )
}

fun NoteHistoryDto.toDomain(): NoteHistory {
    return NoteHistory(
        id = id,
        noteId = noteId,
        title = title.orEmpty(),
        content = content,
        version = version ?: 0,
        changeSummary = changeSummary,
        saveType = saveType,
        gmtCreate = gmtCreate
    )
}

fun CollabTicketDto.toDomain(): CollabTicket {
    return CollabTicket(
        ticket = ticket.orEmpty(),
        expiresIn = expiresIn ?: 0
    )
}

fun CollabContentSave.toDto(): CollabContentSaveRequestDto {
    return CollabContentSaveRequestDto(
        content = content,
        contentHtml = contentHtml
    )
}

fun NoteAttachmentDto.toDomain(): NoteAttachment {
    return NoteAttachment(
        id = id,
        fileName = fileName.orEmpty(),
        fileSize = fileSize ?: 0L,
        mimeType = mimeType,
        downloadUrl = downloadUrl,
        gmtCreate = gmtCreate
    )
}

fun NoteSave.toDto(): NoteSaveRequestDto {
    return NoteSaveRequestDto(
        notebookId = notebookId,
        title = title,
        projectId = projectId,
        content = content,
        contentHtml = contentHtml,
        isPublic = isPublic,
        tagIds = tagIds,
        saveType = saveType
    )
}
