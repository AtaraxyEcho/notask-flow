package com.notaskflow.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: Long,
    @ColumnInfo(name = "space_id") val spaceId: Long,
    @ColumnInfo(name = "notebook_id") val notebookId: Long?,
    @ColumnInfo(name = "project_id") val projectId: Long?,
    @ColumnInfo(name = "project_name") val projectName: String?,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "content") val content: String?,
    @ColumnInfo(name = "content_html") val contentHtml: String?,
    @ColumnInfo(name = "can_edit") val canEdit: Boolean,
    @ColumnInfo(name = "is_public") val isPublic: Boolean,
    @ColumnInfo(name = "gmt_modified") val gmtModified: String?,
    @ColumnInfo(name = "sync_status") val syncStatus: String,
    @ColumnInfo(name = "cached_at") val cachedAt: Long
)

@Entity(tableName = "note_drafts")
data class NoteDraftEntity(
    @PrimaryKey @ColumnInfo(name = "draft_key") val draftKey: String,
    @ColumnInfo(name = "space_id") val spaceId: Long,
    @ColumnInfo(name = "note_id") val noteId: Long?,
    @ColumnInfo(name = "notebook_id") val notebookId: Long?,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "content") val content: String,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)

@Entity(tableName = "notebooks")
data class NotebookEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: Long,
    @ColumnInfo(name = "space_id") val spaceId: Long,
    @ColumnInfo(name = "parent_id") val parentId: Long?,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    @ColumnInfo(name = "cached_at") val cachedAt: Long
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: Long,
    @ColumnInfo(name = "space_id") val spaceId: Long,
    @ColumnInfo(name = "project_id") val projectId: Long?,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "status") val status: String,
    @ColumnInfo(name = "priority") val priority: String,
    @ColumnInfo(name = "deadline") val deadline: String?
)

@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: Long,
    @ColumnInfo(name = "space_id") val spaceId: Long,
    @ColumnInfo(name = "task_id") val taskId: Long?,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "is_completed") val isCompleted: Boolean,
    @ColumnInfo(name = "deadline") val deadline: String?,
    @ColumnInfo(name = "completed_at") val completedAt: String?,
    @ColumnInfo(name = "gmt_create") val gmtCreate: String?,
    @ColumnInfo(name = "cached_at") val cachedAt: Long
)

@Entity(tableName = "offline_queue")
data class OfflineQueueEntry(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "operation_type") val operationType: String,
    @ColumnInfo(name = "endpoint_path") val endpointPath: String,
    @ColumnInfo(name = "request_body_json") val requestBodyJson: String,
    @ColumnInfo(name = "http_method") val httpMethod: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "retry_count") val retryCount: Int = 0,
    @ColumnInfo(name = "status") val status: String = "PENDING"
)
