package com.notaskflow.domain.model

data class NoteDraft(
    val spaceId: Long,
    val noteId: Long?,
    val notebookId: Long?,
    val title: String,
    val content: String,
    val contentHtml: String,
    val updatedAt: Long
)
