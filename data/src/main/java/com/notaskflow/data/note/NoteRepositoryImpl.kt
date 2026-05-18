package com.notaskflow.data.note

import com.notaskflow.core.database.NoteDao
import com.notaskflow.data.common.toDomain
import com.notaskflow.data.attachment.dto.noteAttachmentUnbindRequestDto
import com.notaskflow.data.attachment.dto.toBindRequestDto
import com.notaskflow.data.attachment.dto.toNoteDomain
import com.notaskflow.data.note.api.NoteApi
import com.notaskflow.data.note.dto.NoteShareRequestDto
import com.notaskflow.data.note.dto.toDomain
import com.notaskflow.data.note.dto.toDto
import com.notaskflow.domain.model.CollabContentSave
import com.notaskflow.domain.model.CollabTicket
import com.notaskflow.domain.model.Note
import com.notaskflow.domain.model.NoteAttachment
import com.notaskflow.domain.model.NoteAttachmentUpload
import com.notaskflow.domain.model.NoteExportFile
import com.notaskflow.domain.model.NoteExportFormat
import com.notaskflow.domain.model.NoteHistory
import com.notaskflow.domain.model.NoteQuery
import com.notaskflow.domain.model.NoteSave
import com.notaskflow.domain.model.Notebook
import com.notaskflow.domain.model.NotebookSave
import com.notaskflow.domain.model.Page
import com.notaskflow.domain.model.Tag
import com.notaskflow.domain.note.NoteRepository
import javax.inject.Inject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import retrofit2.Response

class NoteRepositoryImpl @Inject constructor(
    private val noteApi: NoteApi,
    private val noteDao: NoteDao
) : NoteRepository {
    override suspend fun page(spaceId: Long, query: NoteQuery): Result<Page<Note>> {
        val networkResult = runCatching {
            noteApi.page(
                spaceId = spaceId,
                pageNum = query.pageNum,
                pageSize = query.pageSize,
                notebookId = query.notebookId,
                tagId = query.tagId,
                keyword = query.keyword,
                projectId = query.projectId
            ).getOrThrow().toDomain { it.toDomain() }
                .also { page ->
                    noteDao.upsertNotes(page.list.map { it.toNoteEntity(System.currentTimeMillis()) })
                }
        }
        if (networkResult.isSuccess) {
            return networkResult
        }
        return cachedPage(spaceId, query, networkResult.exceptionOrNull())
    }

    private suspend fun cachedPage(
        spaceId: Long,
        query: NoteQuery,
        failure: Throwable?
    ): Result<Page<Note>> {
        return runCatching {
            if (query.tagId != null) {
                throw failure ?: IllegalStateException("标签筛选暂无本地缓存")
            }
            val offset = ((query.pageNum - 1).coerceAtLeast(0)) * query.pageSize
            val cachedNotes = noteDao.queryNotes(
                spaceId = spaceId,
                notebookId = query.notebookId,
                projectId = query.projectId,
                keyword = query.keyword?.takeIf { it.isNotBlank() },
                limit = query.pageSize,
                offset = offset
            )
            val total = noteDao.countNotes(
                spaceId = spaceId,
                notebookId = query.notebookId,
                projectId = query.projectId,
                keyword = query.keyword?.takeIf { it.isNotBlank() }
            )
            if (total == 0L && cachedNotes.isEmpty()) {
                throw failure ?: IllegalStateException("本地暂无笔记缓存")
            }
            Page(
                total = total,
                pageNum = query.pageNum,
                pageSize = query.pageSize,
                list = cachedNotes.map { it.toCachedNote() }
            )
        }
    }

    override suspend fun get(spaceId: Long, id: Long): Result<Note> {
        val networkResult = runCatching {
            noteApi.get(spaceId, id).getOrThrow().toDomain()
                .also { note -> noteDao.upsertNote(note.toNoteEntity(System.currentTimeMillis())) }
        }
        if (networkResult.isSuccess) {
            return networkResult
        }
        return runCatching {
            noteDao.findNote(spaceId, id)?.toCachedNote()
                ?: throw networkResult.exceptionOrNull() ?: IllegalStateException("本地暂无笔记缓存")
        }
    }

    override suspend fun create(spaceId: Long, note: NoteSave): Result<Note> {
        return runCatching {
            noteApi.create(spaceId, note.toDto()).getOrThrow().toDomain()
                .also { created -> noteDao.upsertNote(created.toNoteEntity(System.currentTimeMillis())) }
        }
    }

    override suspend fun update(spaceId: Long, id: Long, note: NoteSave): Result<Note> {
        return runCatching {
            noteApi.update(spaceId, id, note.toDto()).getOrThrow().toDomain()
                .also { updated -> noteDao.upsertNote(updated.toNoteEntity(System.currentTimeMillis())) }
        }
    }

    override suspend fun delete(spaceId: Long, id: Long): Result<Unit> {
        return runCatching {
            noteApi.delete(spaceId, id).requireSuccess()
            noteDao.deleteNote(spaceId, id)
        }
    }

    override suspend fun share(spaceId: Long, id: Long, expireAt: String?): Result<Note> {
        return runCatching {
            noteApi.share(spaceId, id, NoteShareRequestDto(expireAt = expireAt)).getOrThrow().toDomain()
        }
    }

    override suspend fun createCollabTicket(spaceId: Long, id: Long): Result<CollabTicket> {
        return runCatching {
            noteApi.createCollabTicket(spaceId, id).getOrThrow().toDomain()
        }
    }

    override suspend fun saveCollabContent(spaceId: Long, id: Long, content: CollabContentSave): Result<Note> {
        return runCatching {
            noteApi.saveCollabContent(spaceId, id, content.toDto()).getOrThrow().toDomain()
                .also { updated -> noteDao.upsertNote(updated.toNoteEntity(System.currentTimeMillis())) }
        }
    }

    override suspend fun createCheckpoint(spaceId: Long, id: Long, content: CollabContentSave): Result<Note> {
        return runCatching {
            noteApi.createCheckpoint(spaceId, id, content.toDto()).getOrThrow().toDomain()
                .also { updated -> noteDao.upsertNote(updated.toNoteEntity(System.currentTimeMillis())) }
        }
    }

    override suspend fun export(spaceId: Long, id: Long, format: NoteExportFormat): Result<NoteExportFile> {
        return runCatching {
            val response = noteApi.export(spaceId, id, format.value)
            if (!response.isSuccessful) {
                throw IllegalStateException("导出失败")
            }
            val fallbackName = "notask-note.${format.defaultExtension()}"
            response.body()?.use { body ->
                NoteExportFile(
                    fileName = response.exportFileName(fallbackName),
                    contentType = body.contentType()?.toString()
                        ?: response.headers()[CONTENT_TYPE_HEADER]
                        ?: BINARY_CONTENT_TYPE,
                    bytes = body.bytes()
                )
            } ?: throw IllegalStateException("导出文件为空")
        }
    }

    override suspend fun histories(spaceId: Long, id: Long): Result<List<NoteHistory>> {
        return runCatching {
            noteApi.histories(spaceId, id).getOrThrow().map { it.toDomain() }
        }
    }

    override suspend fun history(spaceId: Long, id: Long, version: Int): Result<NoteHistory> {
        return runCatching {
            noteApi.history(spaceId, id, version).getOrThrow().toDomain()
        }
    }

    override suspend fun restore(spaceId: Long, id: Long, version: Int): Result<Note> {
        return runCatching {
            noteApi.restore(spaceId, id, version).getOrThrow().toDomain()
                .also { restored -> noteDao.upsertNote(restored.toNoteEntity(System.currentTimeMillis())) }
        }
    }

    override suspend fun attachments(spaceId: Long, id: Long): Result<List<NoteAttachment>> {
        return runCatching {
            noteApi.attachments(spaceId, id).getOrThrow().map { it.toDomain() }
        }
    }

    override suspend fun uploadAttachment(
        spaceId: Long,
        id: Long,
        upload: NoteAttachmentUpload
    ): Result<NoteAttachment> {
        return runCatching {
            val body = upload.bytes.toRequestBody(upload.mimeType.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData(
                name = "file",
                filename = upload.fileName,
                body = body
            )
            val attachment = noteApi.uploadAttachment(spaceId, part).getOrThrow()
            try {
                noteApi.bindAttachment(spaceId, upload.toBindRequestDto(attachment.id, id)).requireSuccess()
                attachment.toNoteDomain()
            } catch (throwable: Throwable) {
                runCatching {
                    noteApi.deleteAttachment(spaceId, attachment.id).requireSuccess()
                }
                throw throwable
            }
        }
    }

    override suspend fun unbindAttachment(
        spaceId: Long,
        id: Long,
        attachmentId: Long,
        referenceKey: String?
    ): Result<Unit> {
        return runCatching {
            noteApi.unbindAttachment(
                spaceId = spaceId,
                attachmentId = attachmentId,
                request = noteAttachmentUnbindRequestDto(id, referenceKey)
            ).requireSuccess()
        }
    }

    override suspend fun notebooks(spaceId: Long): Result<List<Notebook>> {
        val networkResult = runCatching {
            noteApi.notebooks(spaceId).getOrThrow().map { it.toDomain() }
                .also { notebooks ->
                    noteDao.deleteNotebooks(spaceId)
                    noteDao.upsertNotebooks(notebooks.toNotebookEntities(System.currentTimeMillis()))
                }
        }
        if (networkResult.isSuccess) {
            return networkResult
        }
        return runCatching {
            val cachedNotebooks = noteDao.queryNotebooks(spaceId).toNotebookTree()
            if (cachedNotebooks.isEmpty()) {
                throw networkResult.exceptionOrNull() ?: IllegalStateException("本地暂无笔记本缓存")
            }
            cachedNotebooks
        }
    }

    override suspend fun createNotebook(spaceId: Long, notebook: NotebookSave): Result<Notebook> {
        return runCatching {
            noteApi.createNotebook(spaceId, notebook.toDto()).getOrThrow().toDomain()
        }
    }

    override suspend fun updateNotebook(spaceId: Long, id: Long, notebook: NotebookSave): Result<Notebook> {
        return runCatching {
            noteApi.updateNotebook(spaceId, id, notebook.toDto()).getOrThrow().toDomain()
        }
    }

    override suspend fun deleteNotebook(spaceId: Long, id: Long): Result<Unit> {
        return runCatching {
            noteApi.deleteNotebook(spaceId, id).requireSuccess()
        }
    }

    override suspend fun tags(spaceId: Long): Result<List<Tag>> {
        return runCatching {
            noteApi.tags(spaceId).getOrThrow().map { it.toDomain() }
        }
    }

    private fun Response<*>.exportFileName(fallbackName: String): String {
        val disposition = headers()[CONTENT_DISPOSITION_HEADER].orEmpty()
        DISPOSITION_FILENAME_UTF8_REGEX.find(disposition)
            ?.groupValues
            ?.getOrNull(1)
            ?.takeIf { it.isNotBlank() }
            ?.let { encodedName ->
                return URLDecoder.decode(encodedName, StandardCharsets.UTF_8.name())
            }
        DISPOSITION_FILENAME_REGEX.find(disposition)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?.trim('"')
            ?.takeIf { it.isNotBlank() }
            ?.let { fileName -> return fileName }
        return fallbackName
    }

    private fun NoteExportFormat.defaultExtension(): String {
        return when (this) {
            NoteExportFormat.Pdf -> "pdf"
            NoteExportFormat.Word -> "docx"
            NoteExportFormat.Image -> "png"
        }
    }
}

private const val CONTENT_DISPOSITION_HEADER = "Content-Disposition"
private const val CONTENT_TYPE_HEADER = "Content-Type"
private const val BINARY_CONTENT_TYPE = "application/octet-stream"
private val DISPOSITION_FILENAME_UTF8_REGEX = Regex("filename\\*=UTF-8''([^;]+)", RegexOption.IGNORE_CASE)
private val DISPOSITION_FILENAME_REGEX = Regex("filename=([^;]+)", RegexOption.IGNORE_CASE)
