package com.notaskflow.domain.model

data class Tag(
    val id: Long,
    val name: String,
    val spaceId: Long
)

data class Notebook(
    val id: Long,
    val spaceId: Long,
    val parentId: Long?,
    val name: String,
    val sortOrder: Int,
    val children: List<Notebook>
)

data class NotebookSave(
    val parentId: Long? = null,
    val name: String,
    val sortOrder: Int? = null
)

data class Note(
    val id: Long,
    val spaceId: Long,
    val notebookId: Long?,
    val projectId: Long?,
    val projectName: String?,
    val userId: Long?,
    val title: String,
    val content: String?,
    val contentHtml: String?,
    val canEdit: Boolean,
    val collabEnabled: Boolean,
    val isPublic: Boolean,
    val shareCode: String?,
    val shareExpire: String?,
    val viewCount: Int,
    val gmtCreate: String?,
    val gmtModified: String?,
    val tags: List<Tag>
)

data class NoteQuery(
    val pageNum: Long = 1,
    val pageSize: Long = 20,
    val notebookId: Long? = null,
    val tagId: Long? = null,
    val keyword: String? = null,
    val projectId: Long? = null
)

data class NoteSave(
    val notebookId: Long,
    val title: String,
    val projectId: Long? = null,
    val content: String? = null,
    val contentHtml: String? = null,
    val isPublic: Boolean = false,
    val tagIds: List<Long> = emptyList(),
    val saveType: String = "MANUAL"
)

data class NoteHistory(
    val id: Long,
    val noteId: Long,
    val title: String,
    val content: String?,
    val version: Int,
    val changeSummary: String?,
    val saveType: String?,
    val gmtCreate: String?
)

data class NoteAttachment(
    val id: Long,
    val fileName: String,
    val fileSize: Long,
    val mimeType: String?,
    val downloadUrl: String?,
    val gmtCreate: String?
)

data class NoteAttachmentUpload(
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray,
    val referenceKey: String? = null
)

data class NoteExportFile(
    val fileName: String,
    val contentType: String,
    val bytes: ByteArray
)

data class CollabTicket(
    val ticket: String,
    val expiresIn: Int
)

data class CollabContentSave(
    val content: String,
    val contentHtml: String?
)

sealed class NoteExportFormat(val value: String) {
    data object Pdf : NoteExportFormat("pdf")
    data object Word : NoteExportFormat("word")
    data object Image : NoteExportFormat("image")
}
