package com.notaskflow.domain.policy

import com.notaskflow.domain.model.Task
import com.notaskflow.domain.model.TaskMode
import com.notaskflow.domain.model.TaskPriority
import com.notaskflow.domain.model.TaskStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TaskActionPolicyTest {
    @Test
    fun should_return_start_and_cancel_when_task_is_pending() {
        val task = taskWithStatus(TaskStatus.PENDING)

        val targets = TaskActionPolicy.availableStatusTargets(task)

        assertEquals(listOf(TaskStatus.IN_PROGRESS, TaskStatus.CANCELLED), targets)
    }

    @Test
    fun should_return_start_and_cancel_when_legacy_open_task_is_received() {
        val task = taskWithStatus(TaskStatus.OPEN)

        val targets = TaskActionPolicy.availableStatusTargets(task)

        assertEquals(listOf(TaskStatus.IN_PROGRESS, TaskStatus.CANCELLED), targets)
    }

    @Test
    fun should_return_complete_and_cancel_when_task_is_in_progress() {
        val task = taskWithStatus(TaskStatus.IN_PROGRESS)

        val targets = TaskActionPolicy.availableStatusTargets(task)

        assertEquals(listOf(TaskStatus.COMPLETED, TaskStatus.CANCELLED), targets)
    }

    @Test
    fun should_return_no_action_when_task_is_terminal() {
        val completedTask = taskWithStatus(TaskStatus.COMPLETED)
        val cancelledTask = taskWithStatus(TaskStatus.CANCELLED)

        assertEquals(emptyList<TaskStatus>(), TaskActionPolicy.availableStatusTargets(completedTask))
        assertEquals(emptyList<TaskStatus>(), TaskActionPolicy.availableStatusTargets(cancelledTask))
    }

    private fun taskWithStatus(status: TaskStatus): Task {
        return Task(
            id = 1,
            spaceId = 1,
            projectId = null,
            projectName = null,
            title = "测试任务",
            description = null,
            creatorId = 1,
            mode = TaskMode.ASSIGNED,
            status = status,
            priority = TaskPriority.MEDIUM,
            deadline = null,
            completedAt = null,
            gmtCreate = null,
            gmtModified = null,
            members = emptyList()
        )
    }
}
