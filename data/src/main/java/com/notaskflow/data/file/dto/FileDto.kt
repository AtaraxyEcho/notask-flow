package com.notaskflow.data.file.dto

import com.notaskflow.domain.model.FileFolder
import com.notaskflow.domain.model.FileFolderSave
import com.notaskflow.domain.model.FilePreviewHtml
import com.notaskflow.domain.model.FilePreviewText
import com.notaskflow.domain.model.FileReference
import com.notaskflow.domain.model.ManagedFile
import com.notaskflow.domain.model.ManagedFileUpdate
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ManagedFileDto(
    @param:Json(name = "id") val id: Long,
    @param:Json(name = "attachmentId") val attachmentId: Long?,
    @param:Json(name = "spaceId") val spaceId: Long,
    @param:Json(name = "folderId") val folderId: Long?,
    @param:Json(name = "displayName") val displayName: String?,
    @param:Json(name = "fileName") val fileName: String?,
    @param:Json(name = "fileSize") val fileSize: Long?,
    @param:Json(name = "mimeType") val mimeType: String?,
    @param:Json(name = "uploaderId") val uploaderId: Long?,
    @param:Json(name = "createdBy") val createdBy: Long?,
    @param:Json(name = "trashed") val trashed: Boolean?,
    @param:Json(name = "deletedAt") val deletedAt: String?,
    @param:Json(name = "downloadUrl") val downloadUrl: String?,
    @param:Json(name = "gmtCreate") val gmtCreate: String?
)

@JsonClass(generateAdapter = true)
data class FileFolderDto(
    @param:Json(name = "id") val id: Long,
    @param:Json(name = "spaceId") val spaceId: Long,
    @param:Json(name = "parentId") val parentId: Long?,
    @param:Json(name = "name") val name: String?,
    @param:Json(name = "sortOrder") val sortOrder: Int?,
    @param:Json(name = "createdBy") val createdBy: Long?,
    @param:Json(name = "gmtCreate") val gmtCreate: String?,
    @param:Json(name = "children") val children: List<FileFolderDto>?
)

@JsonClass(generateAdapter = true)
data class FilePreviewTextDto(
    @param:Json(name = "fileName") val fileName: String?,
    @param:Json(name = "mimeType") val mimeType: String?,
    @param:Json(name = "textContent") val textContent: String?
)

@JsonClass(generateAdapter = true)
data class FilePreviewHtmlDto(
    @param:Json(name = "fileName") val fileName: String?,
    @param:Json(name = "mimeType") val mimeType: String?,
    @param:Json(name = "htmlContent") val htmlContent: String?
)

@JsonClass(generateAdapter = true)
data class FileReferenceDto(
    @param:Json(name = "id") val id: Long,
    @param:Json(name = "attachmentId") val attachmentId: Long?,
    @param:Json(name = "businessType") val businessType: String?,
    @param:Json(name = "businessId") val businessId: Long?,
    @param:Json(name = "referenceKey") val referenceKey: String?,
    @param:Json(name = "gmtCreate") val gmtCreate: String?
)

@JsonClass(generateAdapter = true)
data class FileFolderSaveRequestDto(
    @param:Json(name = "name") val name: String,
    @param:Json(name = "parentId") val parentId: Long?
)

@JsonClass(generateAdapter = true)
data class ManagedFileUpdateRequestDto(
    @param:Json(name = "displayName") val displayName: String?,
    @param:Json(name = "folderId") val folderId: Long?
)

fun ManagedFileDto.toDomain(): ManagedFile {
    return ManagedFile(
        id = id,
        attachmentId = attachmentId,
        spaceId = spaceId,
        folderId = folderId,
        displayName = displayName ?: fileName.orEmpty(),
        fileName = fileName ?: displayName.orEmpty(),
        fileSize = fileSize ?: 0,
        mimeType = mimeType,
        uploaderId = uploaderId,
        createdBy = createdBy,
        trashed = trashed == true,
        deletedAt = deletedAt,
        downloadUrl = downloadUrl,
        gmtCreate = gmtCreate
    )
}

fun FileFolderDto.toDomain(): FileFolder {
    return FileFolder(
        id = id,
        spaceId = spaceId,
        parentId = parentId,
        name = name.orEmpty(),
        sortOrder = sortOrder ?: 0,
        createdBy = createdBy,
        gmtCreate = gmtCreate,
        children = children.orEmpty().map { it.toDomain() }
    )
}

fun FileFolderSave.toDto(): FileFolderSaveRequestDto {
    return FileFolderSaveRequestDto(
        name = name,
        parentId = parentId
    )
}

fun ManagedFileUpdate.toDto(): ManagedFileUpdateRequestDto {
    return ManagedFileUpdateRequestDto(
        displayName = displayName,
        folderId = folderId
    )
}

fun FilePreviewTextDto.toDomain(): FilePreviewText {
    return FilePreviewText(
        fileName = fileName.orEmpty(),
        mimeType = mimeType,
        textContent = textContent.orEmpty()
    )
}

fun FilePreviewHtmlDto.toDomain(): FilePreviewHtml {
    return FilePreviewHtml(
        fileName = fileName.orEmpty(),
        mimeType = mimeType,
        htmlContent = htmlContent.orEmpty()
    )
}

fun FileReferenceDto.toDomain(): FileReference {
    return FileReference(
        id = id,
        attachmentId = attachmentId ?: 0L,
        businessType = businessType.orEmpty(),
        businessId = businessId ?: 0L,
        referenceKey = referenceKey,
        gmtCreate = gmtCreate
    )
}
