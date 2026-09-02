package com.notaskflow.domain.policy

import com.notaskflow.domain.model.Task
import com.notaskflow.domain.model.TaskStatus

object TaskActionPolicy {
    fun availableStatusTargets(task: Task): List<TaskStatus> {
        return when (task.status) {
            TaskStatus.PENDING -> listOf(TaskStatus.IN_PROGRESS, TaskStatus.CANCELLED)
            TaskStatus.OPEN -> listOf(TaskStatus.IN_PROGRESS, TaskStatus.CANCELLED)
            TaskStatus.IN_PROGRESS -> listOf(TaskStatus.COMPLETED, TaskStatus.CANCELLED)
            TaskStatus.COMPLETED -> emptyList()
            TaskStatus.CANCELLED -> emptyList()
        }
    }
}
