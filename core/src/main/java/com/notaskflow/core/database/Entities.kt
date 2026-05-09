package com.notaskflow.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: Long,
    val spaceId: Long,
    val notebookId: Long?,
    val title: String,
    val content: String,
    val contentHtml: String?,
    val syncStatus: String
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: Long,
    val spaceId: Long,
    val projectId: Long?,
    val title: String,
    val status: String,
    val priority: String,
    val deadline: String?
)

@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey val id: Long,
    val spaceId: Long,
    val taskId: Long?,
    val title: String,
    val isCompleted: Boolean,
    val deadline: String?
)

@Entity(tableName = "offline_queue")
data class OfflineQueueEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val operationType: String,
    val endpointPath: String,
    val requestBodyJson: String,
    val httpMethod: String,
    val createdAt: Long,
    val retryCount: Int = 0,
    val status: String = "PENDING"
)
