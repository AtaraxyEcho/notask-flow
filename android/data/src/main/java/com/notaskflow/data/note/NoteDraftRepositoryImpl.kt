package com.notaskflow.data.note

import com.notaskflow.core.database.NoteDraftDao
import com.notaskflow.core.database.NoteDraftEntity
import com.notaskflow.domain.model.NoteDraft
import com.notaskflow.domain.note.NoteDraftRepository
import javax.inject.Inject

class NoteDraftRepositoryImpl @Inject constructor(
    private val noteDraftDao: NoteDraftDao
) : NoteDraftRepository {
    override suspend fun get(spaceId: Long, noteId: Long?): NoteDraft? {
        return noteDraftDao.findDraft(draftKey(spaceId, noteId))?.toDomain()
    }

    override suspend fun save(draft: NoteDraft) {
        noteDraftDao.upsertDraft(draft.toEntity())
    }

    override suspend fun delete(spaceId: Long, noteId: Long?) {
        noteDraftDao.deleteDraft(draftKey(spaceId, noteId))
    }

    private fun NoteDraft.toEntity(): NoteDraftEntity {
        return NoteDraftEntity(
            draftKey = draftKey(spaceId, noteId),
            spaceId = spaceId,
            noteId = noteId,
            notebookId = notebookId,
            title = title,
            content = content,
            updatedAt = updatedAt
        )
    }

    private fun NoteDraftEntity.toDomain(): NoteDraft {
        return NoteDraft(
            spaceId = spaceId,
            noteId = noteId,
            notebookId = notebookId,
            title = title,
            content = content,
            updatedAt = updatedAt
        )
    }

    private fun draftKey(spaceId: Long, noteId: Long?): String {
        return "$spaceId:${noteId ?: NEW_NOTE_KEY}"
    }

    private companion object {
        const val NEW_NOTE_KEY = "new"
    }
}
