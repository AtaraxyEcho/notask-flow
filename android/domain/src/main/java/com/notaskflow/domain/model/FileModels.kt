package com.notaskflow.domain.model

data class ManagedFile(
    val id: Long,
    val attachmentId: Long?,
    val spaceId: Long,
    val folderId: Long?,
    val displayName: String,
    val fileName: String,
    val fileSize: Long,
    val mimeType: String?,
    val uploaderId: Long?,
    val createdBy: Long?,
    val trashed: Boolean,
    val deletedAt: String?,
    val downloadUrl: String?,
    val gmtCreate: String?
)

data class FileFolder(
    val id: Long,
    val spaceId: Long,
    val parentId: Long?,
    val name: String,
    val sortOrder: Int,
    val createdBy: Long?,
    val gmtCreate: String?,
    val children: List<FileFolder>
)

data class FileFolderSave(
    val name: String,
    val parentId: Long? = null
)

data class ManagedFileQuery(
    val pageNum: Long = 1,
    val pageSize: Long = 20,
    val folderId: Long? = null,
    val keyword: String? = null,
    val mimeType: String? = null,
    val uploaderId: Long? = null,
    val trashed: Boolean? = false
)

data class ManagedFileUpdate(
    val displayName: String? = null,
    val folderId: Long? = null
)

data class FilePreviewText(
    val fileName: String,
    val mimeType: String?,
    val textContent: String
)

data class FilePreviewHtml(
    val fileName: String,
    val mimeType: String?,
    val htmlContent: String
)

data class FileReference(
    val id: Long,
    val attachmentId: Long,
    val businessType: String,
    val businessId: Long,
    val referenceKey: String?,
    val gmtCreate: String?
)

class ManagedFileUpload(
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray,
    val folderId: Long? = null
)
