package com.notaskflow.data.project

import com.notaskflow.data.common.toDomain
import com.notaskflow.data.project.api.ProjectApi
import com.notaskflow.data.project.dto.ProjectArchiveRequestDto
import com.notaskflow.data.project.dto.ProjectMemberRoleUpdateRequestDto
import com.notaskflow.data.project.dto.toDomain
import com.notaskflow.data.project.dto.toDto
import com.notaskflow.data.task.dto.toDomain
import com.notaskflow.domain.model.Page
import com.notaskflow.domain.model.Project
import com.notaskflow.domain.model.ProjectMember
import com.notaskflow.domain.model.ProjectMemberRole
import com.notaskflow.domain.model.ProjectMemberSave
import com.notaskflow.domain.model.ProjectQuery
import com.notaskflow.domain.model.ProjectSave
import com.notaskflow.domain.model.Task
import com.notaskflow.domain.model.TaskQuery
import com.notaskflow.domain.project.ProjectRepository
import javax.inject.Inject

class ProjectRepositoryImpl @Inject constructor(
    private val projectApi: ProjectApi
) : ProjectRepository {
    override suspend fun page(spaceId: Long, query: ProjectQuery): Result<Page<Project>> {
        return runCatching {
            projectApi.page(
                spaceId = spaceId,
                pageNum = query.pageNum,
                pageSize = query.pageSize,
                keyword = query.keyword,
                archived = query.archived
            ).getOrThrow().toDomain { it.toDomain() }
        }
    }

    override suspend fun create(spaceId: Long, project: ProjectSave): Result<Project> {
        return runCatching {
            projectApi.create(spaceId, project.toDto()).getOrThrow().toDomain()
        }
    }

    override suspend fun get(spaceId: Long, projectId: Long): Result<Project> {
        return runCatching {
            projectApi.get(spaceId, projectId).getOrThrow().toDomain()
        }
    }

    override suspend fun update(spaceId: Long, projectId: Long, project: ProjectSave): Result<Project> {
        return runCatching {
            projectApi.update(spaceId, projectId, project.toDto()).getOrThrow().toDomain()
        }
    }

    override suspend fun delete(spaceId: Long, projectId: Long): Result<Unit> {
        return runCatching {
            projectApi.delete(spaceId, projectId).requireSuccess()
        }
    }

    override suspend fun archive(spaceId: Long, projectId: Long, archived: Boolean): Result<Project> {
        return runCatching {
            projectApi.archive(
                spaceId = spaceId,
                projectId = projectId,
                body = ProjectArchiveRequestDto(archived = archived)
            ).getOrThrow().toDomain()
        }
    }

    override suspend fun members(spaceId: Long, projectId: Long): Result<List<ProjectMember>> {
        return runCatching {
            projectApi.members(spaceId, projectId).getOrThrow().map { it.toDomain(fallbackProjectId = projectId) }
        }
    }

    override suspend fun addMember(
        spaceId: Long,
        projectId: Long,
        member: ProjectMemberSave
    ): Result<ProjectMember> {
        return runCatching {
            projectApi.addMember(spaceId, projectId, member.toDto())
                .getOrThrow()
                .toDomain(fallbackProjectId = projectId)
        }
    }

    override suspend fun updateMemberRole(
        spaceId: Long,
        projectId: Long,
        userId: Long,
        role: ProjectMemberRole
    ): Result<ProjectMember> {
        return runCatching {
            projectApi.updateMemberRole(
                spaceId = spaceId,
                projectId = projectId,
                userId = userId,
                body = ProjectMemberRoleUpdateRequestDto(role = role.name)
            ).getOrThrow().toDomain(fallbackProjectId = projectId)
        }
    }

    override suspend fun removeMember(spaceId: Long, projectId: Long, userId: Long): Result<Unit> {
        return runCatching {
            projectApi.removeMember(spaceId, projectId, userId).requireSuccess()
        }
    }

    override suspend fun tasks(spaceId: Long, projectId: Long, query: TaskQuery): Result<Page<Task>> {
        return runCatching {
            projectApi.tasks(
                spaceId = spaceId,
                projectId = projectId,
                pageNum = query.pageNum,
                pageSize = query.pageSize,
                keyword = query.keyword,
                status = query.status?.name,
                mode = query.mode?.name,
                assigneeId = query.assigneeId
            ).getOrThrow().toDomain { it.toDomain() }
        }
    }
}
