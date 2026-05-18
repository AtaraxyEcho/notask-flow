package com.notaskflow.domain.project

import com.notaskflow.domain.model.Page
import com.notaskflow.domain.model.Project
import com.notaskflow.domain.model.ProjectMember
import com.notaskflow.domain.model.ProjectMemberRole
import com.notaskflow.domain.model.ProjectMemberSave
import com.notaskflow.domain.model.ProjectQuery
import com.notaskflow.domain.model.ProjectSave
import com.notaskflow.domain.model.Task
import com.notaskflow.domain.model.TaskQuery

interface ProjectRepository {
    suspend fun page(spaceId: Long, query: ProjectQuery): Result<Page<Project>>

    suspend fun create(spaceId: Long, project: ProjectSave): Result<Project>

    suspend fun get(spaceId: Long, projectId: Long): Result<Project>

    suspend fun update(spaceId: Long, projectId: Long, project: ProjectSave): Result<Project>

    suspend fun delete(spaceId: Long, projectId: Long): Result<Unit>

    suspend fun archive(spaceId: Long, projectId: Long, archived: Boolean): Result<Project>

    suspend fun members(spaceId: Long, projectId: Long): Result<List<ProjectMember>>

    suspend fun addMember(spaceId: Long, projectId: Long, member: ProjectMemberSave): Result<ProjectMember>

    suspend fun updateMemberRole(
        spaceId: Long,
        projectId: Long,
        userId: Long,
        role: ProjectMemberRole
    ): Result<ProjectMember>

    suspend fun removeMember(spaceId: Long, projectId: Long, userId: Long): Result<Unit>

    suspend fun tasks(spaceId: Long, projectId: Long, query: TaskQuery): Result<Page<Task>>
}
