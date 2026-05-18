package com.notaskflow.domain.note

import com.notaskflow.domain.model.Note
import com.notaskflow.domain.model.NoteAttachment
import com.notaskflow.domain.model.NoteAttachmentUpload
import com.notaskflow.domain.model.CollabContentSave
import com.notaskflow.domain.model.CollabTicket
import com.notaskflow.domain.model.NoteExportFile
import com.notaskflow.domain.model.NoteExportFormat
import com.notaskflow.domain.model.NoteHistory
import com.notaskflow.domain.model.NoteQuery
import com.notaskflow.domain.model.NoteSave
import com.notaskflow.domain.model.Notebook
import com.notaskflow.domain.model.NotebookSave
import com.notaskflow.domain.model.Page
import com.notaskflow.domain.model.Tag

interface NoteRepository {
    suspend fun page(spaceId: Long, query: NoteQuery): Result<Page<Note>>
    suspend fun get(spaceId: Long, id: Long): Result<Note>
    suspend fun create(spaceId: Long, note: NoteSave): Result<Note>
    suspend fun update(spaceId: Long, id: Long, note: NoteSave): Result<Note>
    suspend fun delete(spaceId: Long, id: Long): Result<Unit>
    suspend fun share(spaceId: Long, id: Long, expireAt: String? = null): Result<Note>
    suspend fun createCollabTicket(spaceId: Long, id: Long): Result<CollabTicket>
    suspend fun saveCollabContent(spaceId: Long, id: Long, content: CollabContentSave): Result<Note>
    suspend fun createCheckpoint(spaceId: Long, id: Long, content: CollabContentSave): Result<Note>
    suspend fun export(spaceId: Long, id: Long, format: NoteExportFormat): Result<NoteExportFile>
    suspend fun histories(spaceId: Long, id: Long): Result<List<NoteHistory>>
    suspend fun history(spaceId: Long, id: Long, version: Int): Result<NoteHistory>
    suspend fun restore(spaceId: Long, id: Long, version: Int): Result<Note>
    suspend fun attachments(spaceId: Long, id: Long): Result<List<NoteAttachment>>
    suspend fun uploadAttachment(spaceId: Long, id: Long, upload: NoteAttachmentUpload): Result<NoteAttachment>
    suspend fun unbindAttachment(spaceId: Long, id: Long, attachmentId: Long, referenceKey: String? = null): Result<Unit>
    suspend fun notebooks(spaceId: Long): Result<List<Notebook>>
    suspend fun createNotebook(spaceId: Long, notebook: NotebookSave): Result<Notebook>
    suspend fun updateNotebook(spaceId: Long, id: Long, notebook: NotebookSave): Result<Notebook>
    suspend fun deleteNotebook(spaceId: Long, id: Long): Result<Unit>
    suspend fun tags(spaceId: Long): Result<List<Tag>>
}
