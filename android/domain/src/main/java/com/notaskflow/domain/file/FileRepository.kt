package com.notaskflow.domain.file

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

interface FileRepository {
    suspend fun page(spaceId: Long, query: ManagedFileQuery): Result<Page<ManagedFile>>

    suspend fun folders(spaceId: Long): Result<List<FileFolder>>

    suspend fun createFolder(spaceId: Long, folder: FileFolderSave): Result<FileFolder>

    suspend fun updateFolder(spaceId: Long, folderId: Long, folder: FileFolderSave): Result<FileFolder>

    suspend fun deleteFolder(spaceId: Long, folderId: Long): Result<Unit>

    suspend fun get(spaceId: Long, fileId: Long): Result<ManagedFile>

    suspend fun updateFile(spaceId: Long, fileId: Long, file: ManagedFileUpdate): Result<ManagedFile>

    suspend fun delete(spaceId: Long, fileId: Long): Result<Unit>

    suspend fun restore(spaceId: Long, fileId: Long): Result<ManagedFile>

    suspend fun physicalDelete(spaceId: Long, fileId: Long, force: Boolean = false): Result<Unit>

    suspend fun previewUrl(spaceId: Long, fileId: Long): Result<ManagedFile>

    suspend fun downloadUrl(spaceId: Long, fileId: Long): Result<ManagedFile>

    suspend fun references(spaceId: Long, fileId: Long): Result<List<FileReference>>

    suspend fun previewText(spaceId: Long, fileId: Long): Result<FilePreviewText>

    suspend fun previewHtml(spaceId: Long, fileId: Long): Result<FilePreviewHtml>

    suspend fun upload(spaceId: Long, upload: ManagedFileUpload): Result<ManagedFile>

    suspend fun editorUpload(spaceId: Long, upload: ManagedFileUpload): Result<ManagedFile>
}
