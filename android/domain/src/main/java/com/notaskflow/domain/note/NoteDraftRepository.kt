package com.notaskflow.domain.note

import com.notaskflow.domain.model.NoteDraft

interface NoteDraftRepository {
    suspend fun get(spaceId: Long, noteId: Long?): NoteDraft?

    suspend fun save(draft: NoteDraft)

    suspend fun delete(spaceId: Long, noteId: Long?)
}
