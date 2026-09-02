package com.notaskflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.notaskflow.common.PageResponse;
import com.notaskflow.common.enums.ProjectMemberRole;
import com.notaskflow.common.enums.SpaceRealtimeEventType;
import com.notaskflow.common.enums.SpaceType;
import com.notaskflow.common.enums.TaskStatus;
import com.notaskflow.domain.dto.request.ProjectArchiveRequest;
import com.notaskflow.domain.dto.request.ProjectMemberRoleUpdateRequest;
import com.notaskflow.domain.dto.request.ProjectMemberSaveRequest;
import com.notaskflow.domain.dto.request.ProjectSaveRequest;
import com.notaskflow.domain.entity.Note;
import com.notaskflow.domain.entity.Project;
import com.notaskflow.domain.entity.ProjectMember;
import com.notaskflow.domain.entity.Space;
import com.notaskflow.domain.entity.SpaceMember;
import com.notaskflow.domain.entity.Task;
import com.notaskflow.domain.entity.User;
import com.notaskflow.domain.query.ProjectQuery;
import com.notaskflow.domain.vo.ProjectMemberVO;
import com.notaskflow.domain.vo.ProjectVO;
import com.notaskflow.exception.AccessDeniedException;
import com.notaskflow.exception.BusinessException;
import com.notaskflow.exception.ErrorCode;
import com.notaskflow.exception.ResourceNotFoundException;
import com.notaskflow.mapper.NoteMapper;
import com.notaskflow.mapper.ProjectMapper;
import com.notaskflow.mapper.ProjectMemberMapper;
import com.notaskflow.mapper.RoleMapper;
import com.notaskflow.mapper.SpaceMapper;
import com.notaskflow.mapper.SpaceMemberMapper;
import com.notaskflow.mapper.TaskMapper;
import com.notaskflow.mapper.UserMapper;
import com.notaskflow.security.PermissionValidator;
import com.notaskflow.service.ProjectService;
import com.notaskflow.service.SpaceRealtimeEventService;
import com.notaskflow.storage.MinioStorageService;
import com.notaskflow.utils.AvatarUrlUtil;
import com.notaskflow.utils.LoginUserUtil;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 项目服务实现。
 *
 * @author LIN
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private static final String DEFAULT_COVER_COLOR = "#6366f1";

    private final ProjectMapper projectMapper;

    private final ProjectMemberMapper projectMemberMapper;

    private final TaskMapper taskMapper;

    private final NoteMapper noteMapper;

    private final UserMapper userMapper;

    private final SpaceMapper spaceMapper;

    private final SpaceMemberMapper spaceMemberMapper;

    private final RoleMapper roleMapper;

    private final MinioStorageService minioStorageService;

    private final PermissionValidator permissionValidator;

    private final SpaceRealtimeEventService spaceRealtimeEventService;

    /**
     * 分页查询空间内项目。
     *
     * @param spaceId 空间标识
     * @param query 查询条件
     * @return 项目分页结果
     */
    @Override
    public PageResponse<ProjectVO> page(Long spaceId, ProjectQuery query) {
        permissionValidator.ensureSpaceMember(spaceId, LoginUserUtil.currentUserId());
        ensureTeamSpace(spaceId);
        Page<Project> page = new Page<>(query.safePageNum(), query.safePageSize());
        LambdaQueryWrapper<Project> wrapper = Wrappers.<Project>lambdaQuery()
                .eq(Project::getSpaceId, spaceId)
                .eq(query.getArchived() != null, Project::getArchived, query.getArchived())
                .and(StringUtils.hasText(query.getKeyword()), condition -> condition.like(Project::getName,
                        query.getKeyword().trim())
                        .or()
                        .like(Project::getDescription, query.getKeyword().trim()))
                .orderByDesc(Project::getGmtModified);
        IPage<Project> result = projectMapper.selectPage(page, wrapper);
        List<ProjectVO> list = result.getRecords().stream().map(this::toProjectVO).toList();
        return PageResponse.of(page, list);
    }

    /**
     * 查询项目选项列表。
     *
     * @param spaceId 空间标识
     * @return 项目列表
     */
    @Override
    public List<ProjectVO> listOptions(Long spaceId) {
        permissionValidator.ensureSpaceMember(spaceId, LoginUserUtil.currentUserId());
        ensureTeamSpace(spaceId);
        return projectMapper.selectList(Wrappers.<Project>lambdaQuery()
                        .eq(Project::getSpaceId, spaceId)
                        .eq(Project::getArchived, false)
                        .orderByAsc(Project::getName))
                .stream()
                .map(this::toProjectVO)
                .toList();
    }

    /**
     * 查询项目详情。
     *
     * @param spaceId 空间标识
     * @param projectId 项目标识
     * @return 项目详情
     */
    @Override
    public ProjectVO get(Long spaceId, Long projectId) {
        permissionValidator.ensureSpaceMember(spaceId, LoginUserUtil.currentUserId());
        ensureTeamSpace(spaceId);
        return toProjectVO(findProject(spaceId, projectId));
    }

    /**
     * 创建项目并初始化负责人成员关系。
     *
     * @param spaceId 空间标识
     * @param request 项目保存请求
     * @return 项目详情
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectVO create(Long spaceId, ProjectSaveRequest request) {
        Long currentUserId = LoginUserUtil.currentUserId();
        permissionValidator.ensureSpaceCollaborator(spaceId, currentUserId);
        ensureTeamSpace(spaceId);
        Long ownerUserId = resolveOwnerUserId(spaceId, currentUserId, request.getOwnerUserId());
        Project project = new Project();
        project.setSpaceId(spaceId);
        project.setName(request.getName().trim());
        project.setDescription(request.getDescription());
        project.setCoverColor(normalizeCoverColor(request.getCoverColor()));
        project.setCoverImageUrl(request.getCoverImageUrl());
        project.setArchived(false);
        project.setOwnerUserId(ownerUserId);
        projectMapper.insert(project);
        saveProjectMember(project.getId(), ownerUserId, ProjectMemberRole.OWNER);
        log.info("项目创建完成，spaceId={}，projectId={}，ownerUserId={}，operatorId={}",
                spaceId, project.getId(), ownerUserId, currentUserId);
        publishProjectRealtimeEvent(spaceId, SpaceRealtimeEventType.PROJECT_CREATED, project);
        return toProjectVO(project);
    }

    /**
     * 更新项目基础信息。
     *
     * @param spaceId 空间标识
     * @param projectId 项目标识
     * @param request 项目保存请求
     * @return 项目详情
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectVO update(Long spaceId, Long projectId, ProjectSaveRequest request) {
        Long currentUserId = LoginUserUtil.currentUserId();
        Project project = findProject(spaceId, projectId);
        ensureProjectOwnerOrSpaceAdmin(project, currentUserId);
        project.setName(request.getName().trim());
        project.setDescription(request.getDescription());
        project.setCoverColor(normalizeCoverColor(request.getCoverColor()));
        project.setCoverImageUrl(request.getCoverImageUrl());
        if (request.getOwnerUserId() != null && !request.getOwnerUserId().equals(project.getOwnerUserId())) {
            Long ownerUserId = resolveOwnerUserId(spaceId, currentUserId, request.getOwnerUserId());
            project.setOwnerUserId(ownerUserId);
            upsertOwnerMember(projectId, ownerUserId);
        }
        projectMapper.updateById(project);
        log.info("项目更新完成，spaceId={}，projectId={}，operatorId={}", spaceId, projectId, currentUserId);
        publishProjectRealtimeEvent(spaceId, SpaceRealtimeEventType.PROJECT_UPDATED, project);
        return toProjectVO(project);
    }

    /**
     * 更新项目归档状态。
     *
     * @param spaceId 空间标识
     * @param projectId 项目标识
     * @param request 项目归档请求
     * @return 项目详情
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectVO archive(Long spaceId, Long projectId, ProjectArchiveRequest request) {
        Long currentUserId = LoginUserUtil.currentUserId();
        Project project = findProject(spaceId, projectId);
        ensureProjectOwnerOrSpaceAdmin(project, currentUserId);
        project.setArchived(request.getArchived());
        projectMapper.updateById(project);
        log.info("项目归档状态更新完成，spaceId={}，projectId={}，archived={}，operatorId={}",
                spaceId, projectId, request.getArchived(), currentUserId);
        publishProjectRealtimeEvent(spaceId, SpaceRealtimeEventType.PROJECT_UPDATED, project);
        return toProjectVO(project);
    }

    /**
     * 删除项目并解除关联任务与文档。
     *
     * @param spaceId 空间标识
     * @param projectId 项目标识
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long spaceId, Long projectId) {
        Long currentUserId = LoginUserUtil.currentUserId();
        Project project = findProject(spaceId, projectId);
        ensureProjectOwnerOrSpaceAdmin(project, currentUserId);
        taskMapper.update(null, Wrappers.<Task>lambdaUpdate()
                .eq(Task::getProjectId, projectId)
                .set(Task::getProjectId, null));
        noteMapper.update(null, Wrappers.<Note>lambdaUpdate()
                .eq(Note::getProjectId, projectId)
                .set(Note::getProjectId, null));
        projectMemberMapper.delete(Wrappers.<ProjectMember>lambdaQuery().eq(ProjectMember::getProjectId, projectId));
        projectMapper.deleteById(projectId);
        log.info("项目删除完成，spaceId={}，projectId={}，operatorId={}", spaceId, projectId, currentUserId);
        publishProjectRealtimeEvent(spaceId, SpaceRealtimeEventType.PROJECT_DELETED, project);
    }

    /**
     * 查询项目成员。
     *
     * @param spaceId 空间标识
     * @param projectId 项目标识
     * @return 项目成员列表
     */
    @Override
    public List<ProjectMemberVO> listMembers(Long spaceId, Long projectId) {
        permissionValidator.ensureSpaceMember(spaceId, LoginUserUtil.currentUserId());
        findProject(spaceId, projectId);
        return listMembersInternal(projectId);
    }

    /**
     * 添加项目成员。
     *
     * @param spaceId 空间标识
     * @param projectId 项目标识
     * @param request 项目成员保存请求
     * @return 项目成员详情
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectMemberVO addMember(Long spaceId, Long projectId, ProjectMemberSaveRequest request) {
        Long currentUserId = LoginUserUtil.currentUserId();
        Project project = findProject(spaceId, projectId);
        ensureProjectOwnerOrSpaceAdmin(project, currentUserId);
        ensureSpaceMemberExists(spaceId, request.getUserId());
        ProjectMember exists = findProjectMember(projectId, request.getUserId(), false);
        if (exists != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "用户已是项目成员");
        }
        ProjectMember member = saveProjectMember(projectId, request.getUserId(), request.getRole());
        if (ProjectMemberRole.OWNER.equals(request.getRole())) {
            project.setOwnerUserId(request.getUserId());
            projectMapper.updateById(project);
        }
        log.info("项目成员添加完成，spaceId={}，projectId={}，targetUserId={}，role={}，operatorId={}",
                spaceId, projectId, request.getUserId(), request.getRole(), currentUserId);
        publishProjectMemberRealtimeEvent(spaceId, projectId, request.getUserId());
        return toProjectMemberVO(member);
    }

    /**
     * 更新项目成员角色。
     *
     * @param spaceId 空间标识
     * @param projectId 项目标识
     * @param userId 用户标识
     * @param request 项目成员角色更新请求
     * @return 项目成员详情
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectMemberVO updateMemberRole(Long spaceId,
                                            Long projectId,
                                            Long userId,
                                            ProjectMemberRoleUpdateRequest request) {
        Long currentUserId = LoginUserUtil.currentUserId();
        Project project = findProject(spaceId, projectId);
        ensureProjectOwnerOrSpaceAdmin(project, currentUserId);
        ProjectMember member = requireProjectMember(projectId, userId);
        if (ProjectMemberRole.OWNER.equals(member.getRole())
                && !ProjectMemberRole.OWNER.equals(request.getRole())
                && ownerCount(projectId) <= 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "项目至少需要一名负责人");
        }
        member.setRole(request.getRole());
        projectMemberMapper.updateById(member);
        if (ProjectMemberRole.OWNER.equals(request.getRole())) {
            project.setOwnerUserId(userId);
            projectMapper.updateById(project);
        } else if (project.getOwnerUserId().equals(userId)) {
            ProjectMember owner = findLatestOwner(projectId);
            if (owner != null) {
                project.setOwnerUserId(owner.getUserId());
                projectMapper.updateById(project);
            }
        }
        log.info("项目成员角色更新完成，spaceId={}，projectId={}，targetUserId={}，role={}，operatorId={}",
                spaceId, projectId, userId, request.getRole(), currentUserId);
        publishProjectMemberRealtimeEvent(spaceId, projectId, userId);
        return toProjectMemberVO(member);
    }

    /**
     * 移除项目成员。
     *
     * @param spaceId 空间标识
     * @param projectId 项目标识
     * @param userId 用户标识
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeMember(Long spaceId, Long projectId, Long userId) {
        Long currentUserId = LoginUserUtil.currentUserId();
        Project project = findProject(spaceId, projectId);
        ensureProjectOwnerOrSpaceAdmin(project, currentUserId);
        ProjectMember member = requireProjectMember(projectId, userId);
        if (ProjectMemberRole.OWNER.equals(member.getRole()) && ownerCount(projectId) <= 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "项目至少需要一名负责人");
        }
        projectMemberMapper.deleteById(member.getId());
        if (project.getOwnerUserId().equals(userId)) {
            ProjectMember owner = findLatestOwner(projectId);
            if (owner != null) {
                project.setOwnerUserId(owner.getUserId());
                projectMapper.updateById(project);
            }
        }
        log.info("项目成员移除完成，spaceId={}，projectId={}，targetUserId={}，operatorId={}",
                spaceId, projectId, userId, currentUserId);
        publishProjectMemberRealtimeEvent(spaceId, projectId, userId);
    }

    /**
     * 发布项目实时事件。
     *
     * @param spaceId 空间标识
     * @param type 事件类型
     * @param project 项目实体
     */
    private void publishProjectRealtimeEvent(Long spaceId, SpaceRealtimeEventType type, Project project) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("projectId", project.getId());
        payload.put("name", project.getName());
        payload.put("archived", project.getArchived());
        payload.put("ownerUserId", project.getOwnerUserId());
        spaceRealtimeEventService.publish(spaceId, type, payload);
    }

    /**
     * 发布项目成员实时事件。
     *
     * @param spaceId 空间标识
     * @param projectId 项目标识
     * @param userId 用户标识
     */
    private void publishProjectMemberRealtimeEvent(Long spaceId, Long projectId, Long userId) {
        spaceRealtimeEventService.publish(spaceId, SpaceRealtimeEventType.PROJECT_MEMBER_CHANGED,
                Map.of("projectId", projectId, "userId", userId));
    }

    /**
     * 解析项目负责人。
     *
     * @param spaceId 空间标识
     * @param currentUserId 当前用户标识
     * @param ownerUserId 指定负责人标识
     * @return 负责人用户标识
     */
    private Long resolveOwnerUserId(Long spaceId, Long currentUserId, Long ownerUserId) {
        if (ownerUserId == null || ownerUserId.equals(currentUserId)) {
            return currentUserId;
        }
        permissionValidator.ensureSpaceAdminOrOwner(spaceId, currentUserId);
        ensureSpaceMemberExists(spaceId, ownerUserId);
        return ownerUserId;
    }

    /**
     * 根据空间与标识查询项目。
     *
     * @param spaceId 空间标识
     * @param projectId 项目标识
     * @return 项目实体
     */
    private Project findProject(Long spaceId, Long projectId) {
        Project project = projectMapper.selectOne(Wrappers.<Project>lambdaQuery()
                .eq(Project::getSpaceId, spaceId)
                .eq(Project::getId, projectId));
        if (project == null) {
            throw new ResourceNotFoundException("项目不存在");
        }
        return project;
    }

    /**
     * 校验空间是否为团队空间。
     *
     * @param spaceId 空间标识
     */
    private void ensureTeamSpace(Long spaceId) {
        Space space = spaceMapper.selectById(spaceId);
        if (space == null) {
            throw new ResourceNotFoundException("空间不存在");
        }
        if (!SpaceType.TEAM.equals(space.getType())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "项目仅支持团队空间");
        }
    }

    /**
     * 校验当前用户是否为项目负责人或空间管理员。
     *
     * @param project 项目实体
     * @param userId 用户标识
     */
    private void ensureProjectOwnerOrSpaceAdmin(Project project, Long userId) {
        if (project.getOwnerUserId().equals(userId)) {
            return;
        }
        ProjectMember ownerMember = findProjectMember(project.getId(), userId, true);
        if (ownerMember != null && ProjectMemberRole.OWNER.equals(ownerMember.getRole())) {
            return;
        }
        try {
            permissionValidator.ensureSpaceAdminOrOwner(project.getSpaceId(), userId);
        } catch (AccessDeniedException exception) {
            throw new AccessDeniedException("仅项目负责人或空间管理员可执行该操作");
        }
    }

    /**
     * 校验用户是否为空间成员。
     *
     * @param spaceId 空间标识
     * @param userId 用户标识
     */
    private void ensureSpaceMemberExists(Long spaceId, Long userId) {
        SpaceMember member = spaceMemberMapper.selectOne(Wrappers.<SpaceMember>lambdaQuery()
                .eq(SpaceMember::getSpaceId, spaceId)
                .eq(SpaceMember::getUserId, userId));
        if (member == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "目标用户不是当前空间成员");
        }
    }

    /**
     * 保存项目成员。
     *
     * @param projectId 项目标识
     * @param userId 用户标识
     * @param role 项目成员角色
     * @return 项目成员实体
     */
    private ProjectMember saveProjectMember(Long projectId, Long userId, ProjectMemberRole role) {
        ProjectMember member = new ProjectMember();
        member.setProjectId(projectId);
        member.setUserId(userId);
        member.setRole(role);
        projectMemberMapper.insert(member);
        return member;
    }

    /**
     * 确保指定用户为项目负责人。
     *
     * @param projectId 项目标识
     * @param userId 用户标识
     */
    private void upsertOwnerMember(Long projectId, Long userId) {
        ProjectMember member = findProjectMember(projectId, userId, false);
        if (member == null) {
            saveProjectMember(projectId, userId, ProjectMemberRole.OWNER);
            return;
        }
        member.setRole(ProjectMemberRole.OWNER);
        projectMemberMapper.updateById(member);
    }

    /**
     * 查询项目成员。
     *
     * @param projectId 项目标识
     * @param userId 用户标识
     * @param onlyOwner 是否只查询负责人
     * @return 项目成员实体
     */
    private ProjectMember findProjectMember(Long projectId, Long userId, boolean onlyOwner) {
        LambdaQueryWrapper<ProjectMember> wrapper = Wrappers.<ProjectMember>lambdaQuery()
                .eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getUserId, userId);
        if (onlyOwner) {
            wrapper.eq(ProjectMember::getRole, ProjectMemberRole.OWNER);
        }
        return projectMemberMapper.selectOne(wrapper);
    }

    /**
     * 查询必需存在的项目成员。
     *
     * @param projectId 项目标识
     * @param userId 用户标识
     * @return 项目成员实体
     */
    private ProjectMember requireProjectMember(Long projectId, Long userId) {
        ProjectMember member = findProjectMember(projectId, userId, false);
        if (member == null) {
            throw new ResourceNotFoundException("项目成员不存在");
        }
        return member;
    }

    /**
     * 统计项目负责人数量。
     *
     * @param projectId 项目标识
     * @return 负责人数量
     */
    private Long ownerCount(Long projectId) {
        return projectMemberMapper.selectCount(Wrappers.<ProjectMember>lambdaQuery()
                .eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getRole, ProjectMemberRole.OWNER));
    }

    /**
     * 查询最近加入的项目负责人。
     *
     * @param projectId 项目标识
     * @return 项目负责人
     */
    private ProjectMember findLatestOwner(Long projectId) {
        return projectMemberMapper.selectOne(Wrappers.<ProjectMember>lambdaQuery()
                .eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getRole, ProjectMemberRole.OWNER)
                .orderByDesc(ProjectMember::getGmtCreate)
                .last("limit 1"));
    }

    /**
     * 规范化封面颜色。
     *
     * @param coverColor 原始封面颜色
     * @return 规范化后的颜色
     */
    private String normalizeCoverColor(String coverColor) {
        if (!StringUtils.hasText(coverColor)) {
            return DEFAULT_COVER_COLOR;
        }
        return coverColor.trim();
    }

    /**
     * 统计项目任务总数。
     *
     * @param projectId 项目标识
     * @return 任务总数
     */
    private Long countTasks(Long projectId) {
        return taskMapper.selectCount(Wrappers.<Task>lambdaQuery().eq(Task::getProjectId, projectId));
    }

    /**
     * 统计已完成任务数。
     *
     * @param projectId 项目标识
     * @return 已完成任务数
     */
    private Long countCompletedTasks(Long projectId) {
        return taskMapper.selectCount(Wrappers.<Task>lambdaQuery()
                .eq(Task::getProjectId, projectId)
                .eq(Task::getStatus, TaskStatus.COMPLETED));
    }

    /**
     * 统计逾期任务数。
     *
     * @param projectId 项目标识
     * @return 逾期任务数
     */
    private Long countOverdueTasks(Long projectId) {
        return taskMapper.selectCount(Wrappers.<Task>lambdaQuery()
                .eq(Task::getProjectId, projectId)
                .isNotNull(Task::getDeadline)
                .lt(Task::getDeadline, LocalDateTime.now())
                .notIn(Task::getStatus, TaskStatus.COMPLETED, TaskStatus.CANCELLED));
    }

    /**
     * 统计文档数量。
     *
     * @param projectId 项目标识
     * @return 文档数量
     */
    private Long countDocuments(Long projectId) {
        return noteMapper.selectCount(Wrappers.<Note>lambdaQuery().eq(Note::getProjectId, projectId));
    }

    /**
     * 计算项目完成率。
     *
     * @param taskCount 任务总数
     * @param completedTaskCount 已完成任务数
     * @return 完成率百分比
     */
    private Integer completionRate(Long taskCount, Long completedTaskCount) {
        if (taskCount == null || taskCount == 0) {
            return 0;
        }
        return (int) Math.round(completedTaskCount * 100.0 / taskCount);
    }

    /**
     * 查询项目成员列表。
     *
     * @param projectId 项目标识
     * @return 项目成员列表
     */
    private List<ProjectMemberVO> listMembersInternal(Long projectId) {
        return projectMemberMapper.selectList(Wrappers.<ProjectMember>lambdaQuery()
                        .eq(ProjectMember::getProjectId, projectId)
                        .orderByAsc(ProjectMember::getRole)
                        .orderByAsc(ProjectMember::getGmtCreate))
                .stream()
                .map(this::toProjectMemberVO)
                .toList();
    }

    /**
     * 转换项目视图对象。
     *
     * @param project 项目实体
     * @return 项目视图对象
     */
    private ProjectVO toProjectVO(Project project) {
        Long taskCount = countTasks(project.getId());
        Long completedTaskCount = countCompletedTasks(project.getId());
        Long overdueTaskCount = countOverdueTasks(project.getId());
        Long documentCount = countDocuments(project.getId());
        return new ProjectVO(project.getId(), project.getSpaceId(), project.getName(), project.getDescription(),
                project.getCoverColor(), project.getCoverImageUrl(), project.getArchived(), project.getOwnerUserId(),
                taskCount, completedTaskCount, overdueTaskCount, documentCount,
                completionRate(taskCount, completedTaskCount), project.getGmtCreate(), project.getGmtModified(),
                listMembersInternal(project.getId()));
    }

    /**
     * 转换项目成员视图对象。
     *
     * @param member 项目成员实体
     * @return 项目成员视图对象
     */
    private ProjectMemberVO toProjectMemberVO(ProjectMember member) {
        User user = userMapper.selectById(member.getUserId());
        String username = user == null ? "" : user.getUsername();
        String nickname = user == null ? "" : resolveNickname(user);
        String email = user == null ? "" : user.getEmail();
        String avatarUrl = AvatarUrlUtil.proxyUrl(user);
        return new ProjectMemberVO(member.getProjectId(), member.getUserId(), username, nickname, email, avatarUrl,
                member.getRole(), member.getGmtCreate());
    }

    /**
     * 解析展示昵称。
     *
     * @param user 用户实体
     * @return 昵称
     */
    private String resolveNickname(User user) {
        if (user == null) {
            return "";
        }
        return StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
    }

}
