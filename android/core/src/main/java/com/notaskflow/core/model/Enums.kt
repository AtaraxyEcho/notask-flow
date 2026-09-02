package com.notaskflow.core.model

enum class SpaceType {
    PERSONAL,
    TEAM
}

enum class TaskStatus {
    PENDING,
    OPEN,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}

enum class TaskPriority {
    LOW,
    MEDIUM,
    HIGH
}

enum class SyncStatus {
    SYNCED,
    PENDING,
    FAILED
}
