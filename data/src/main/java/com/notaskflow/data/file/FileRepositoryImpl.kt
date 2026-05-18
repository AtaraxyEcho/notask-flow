package com.notaskflow.data.file

import com.notaskflow.data.common.toDomain
import com.notaskflow.data.file.api.FileApi
import com.notaskflow.data.file.dto.toDomain
import com.notaskflow.data.file.dto.toDto
import com.notaskflow.domain.file.FileRepository
import com.notaskflow.domain.model.FileFolder
import com.notaskflow.domain.model.FileFolderSave
import com.notaskflow.domain.model.FilePreviewHtml
import com.notaskflow.domain.model.FilePreviewText
import com.notaskflow.domain.model.FileReference
import com.notaskflow.domain.model.ManagedFile
import com.notaskflow.domain.model.ManagedFileQuery
import com.notaskflow.domain.model.ManagedFileUpdate
import com.notaskflow.domain.model.ManagedFileUpload
import com.notaskflow.domain.model.Page
import javax.inject.Inject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class FileRepositoryImpl @Inject constructor(
    private val fileApi: FileApi
) : FileRepository {
    override suspend fun page(spaceId: Long, query: ManagedFileQuery): Result<Page<ManagedFile>> {
        return runCatching {
            fileApi.page(
                spaceId = spaceId,
                pageNum = query.pageNum,
                pageSize = query.pageSize,
                folderId = query.folderId,
                keyword = query.keyword,
                mimeType = query.mimeType,
                uploaderId = query.uploaderId,
                trashed = query.trashed
            ).getOrThrow().toDomain { it.toDomain() }
        }
    }

    override suspend fun folders(spaceId: Long): Result<List<FileFolder>> {
        return runCatching {
            fileApi.folders(spaceId).getOrThrow().map { it.toDomain() }
        }
    }

    override suspend fun createFolder(spaceId: Long, folder: FileFolderSave): Result<FileFolder> {
        return runCatching {
            fileApi.createFolder(spaceId, folder.toDto()).getOrThrow().toDomain()
        }
    }

    override suspend fun updateFolder(
        spaceId: Long,
        folderId: Long,
        folder: FileFolderSave
    ): Result<FileFolder> {
        return runCatching {
            fileApi.updateFolder(spaceId, folderId, folder.toDto()).getOrThrow().toDomain()
        }
    }

    override suspend fun deleteFolder(spaceId: Long, folderId: Long): Result<Unit> {
        return runCatching {
            fileApi.deleteFolder(spaceId, folderId).requireSuccess()
        }
    }

    override suspend fun get(spaceId: Long, fileId: Long): Result<ManagedFile> {
        return runCatching {
            fileApi.get(spaceId, fileId).getOrThrow().toDomain()
        }
    }

    override suspend fun updateFile(spaceId: Long, fileId: Long, file: ManagedFileUpdate): Result<ManagedFile> {
        return runCatching {
            fileApi.updateFile(spaceId, fileId, file.toDto()).getOrThrow().toDomain()
        }
    }

    override suspend fun delete(spaceId: Long, fileId: Long): Result<Unit> {
        return runCatching {
            fileApi.delete(spaceId, fileId).requireSuccess()
        }
    }

    override suspend fun restore(spaceId: Long, fileId: Long): Result<ManagedFile> {
        return runCatching {
            fileApi.restore(spaceId, fileId).getOrThrow().toDomain()
        }
    }

    override suspend fun physicalDelete(spaceId: Long, fileId: Long, force: Boolean): Result<Unit> {
        return runCatching {
            fileApi.physicalDelete(spaceId, fileId, force).requireSuccess()
        }
    }

    override suspend fun previewUrl(spaceId: Long, fileId: Long): Result<ManagedFile> {
        return runCatching {
            fileApi.previewUrl(spaceId, fileId).getOrThrow().toDomain()
        }
    }

    override suspend fun downloadUrl(spaceId: Long, fileId: Long): Result<ManagedFile> {
        return runCatching {
            fileApi.downloadUrl(spaceId, fileId).getOrThrow().toDomain()
        }
    }

    override suspend fun references(spaceId: Long, fileId: Long): Result<List<FileReference>> {
        return runCatching {
            fileApi.references(spaceId, fileId).getOrThrow().map { it.toDomain() }
        }
    }

    override suspend fun previewText(spaceId: Long, fileId: Long): Result<FilePreviewText> {
        return runCatching {
            fileApi.previewText(spaceId, fileId).getOrThrow().toDomain()
        }
    }

    override suspend fun previewHtml(spaceId: Long, fileId: Long): Result<FilePreviewHtml> {
        return runCatching {
            fileApi.previewHtml(spaceId, fileId).getOrThrow().toDomain()
        }
    }

    override suspend fun upload(spaceId: Long, upload: ManagedFileUpload): Result<ManagedFile> {
        return runCatching {
            val part = upload.toMultipartPart()
            fileApi.upload(
                spaceId = spaceId,
                folderId = upload.folderId,
                file = part
            ).getOrThrow().toDomain()
        }
    }

    override suspend fun editorUpload(spaceId: Long, upload: ManagedFileUpload): Result<ManagedFile> {
        return runCatching {
            fileApi.editorUpload(
                spaceId = spaceId,
                file = upload.toMultipartPart()
            ).getOrThrow().toDomain()
        }
    }

    private fun ManagedFileUpload.toMultipartPart(): MultipartBody.Part {
        val body = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
        return MultipartBody.Part.createFormData(
            name = "file",
            filename = fileName,
            body = body
        )
    }
}
