package com.notaskflow.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.notaskflow.common.PageResponse;
import com.notaskflow.common.enums.AssignmentType;
import com.notaskflow.common.enums.BusinessType;
import com.notaskflow.common.enums.NotificationType;
import com.notaskflow.common.enums.SpaceRealtimeEventType;
import com.notaskflow.common.enums.SpaceType;
import com.notaskflow.common.enums.TaskMemberStatus;
import com.notaskflow.common.enums.TaskMode;
import com.notaskflow.common.enums.TaskStatus;
import com.notaskflow.domain.dto.request.TaskAssignmentRequest;
import com.notaskflow.domain.dto.request.TaskClaimRequest;
import com.notaskflow.domain.dto.request.TaskCommentCreateRequest;
import com.notaskflow.domain.dto.request.TaskCreateRequest;
import com.notaskflow.domain.dto.request.TaskMemberCompleteRequest;
import com.notaskflow.domain.dto.request.TaskStatusUpdateRequest;
import com.notaskflow.domain.dto.request.TaskUpdateRequest;
import com.notaskflow.domain.entity.CommentMention;
import com.notaskflow.domain.entity.Project;
import com.notaskflow.domain.entity.Space;
import com.notaskflow.domain.entity.SpaceMember;
import com.notaskflow.domain.entity.Task;
import com.notaskflow.domain.entity.TaskComment;
import com.notaskflow.domain.entity.TaskMember;
import com.notaskflow.domain.entity.Todo;
import com.notaskflow.domain.entity.User;
import com.notaskflow.domain.query.TaskQuery;
import com.notaskflow.domain.vo.TaskCommentVO;
import com.notaskflow.domain.vo.TaskMemberVO;
import com.notaskflow.domain.vo.TaskVO;
import com.notaskflow.event.TaskMemberStatusChangedEvent;
import com.notaskflow.exception.BusinessException;
import com.notaskflow.exception.DuplicateClaimException;
import com.notaskflow.exception.ErrorCode;
import com.notaskflow.exception.ResourceNotFoundException;
import com.notaskflow.exception.TaskMemberNotFoundException;
import com.notaskflow.mapper.CommentMentionMapper;
import com.notaskflow.mapper.ProjectMapper;
import com.notaskflow.mapper.SpaceMapper;
import com.notaskflow.mapper.TaskCommentMapper;
import com.notaskflow.mapper.TaskMapper;
import com.notaskflow.mapper.TaskMemberMapper;
import com.notaskflow.mapper.TodoMapper;
import com.notaskflow.mapper.UserMapper;
import com.notaskflow.security.PermissionValidator;
import com.notaskflow.service.NotificationService;
import com.notaskflow.service.SpaceRealtimeEventService;
import com.notaskflow.service.TaskService;
import com.notaskflow.service.TaskStatusRecalculationService;
import com.notaskflow.utils.LoginUserUtil;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 任务服务实现。
 *
 * @author LIN
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskMapper taskMapper;

    private final TaskMemberMapper taskMemberMapper;

    private final TodoMapper todoMapper;

    private final TaskCommentMapper taskCommentMapper;

    private final CommentMentionMapper commentMentionMapper;

    private final ProjectMapper projectMapper;

    private final UserMapper userMapper;

    private final SpaceMapper spaceMapper;

    private final PermissionValidator permissionValidator;

    private final TaskStatusRecalculationService taskStatusRecalculationService;

    private final NotificationService notificationService;

    private final ApplicationEventPublisher eventPublisher;

    private final SpaceRealtimeEventService spaceRealtimeEventService;

    /**
     * 分页查询空间内任务。
     *
     * @param spaceId 空间标识
     * @param query 查询条件
     * @return 任务分页结果
     */
    @Override
    public PageResponse<TaskVO> page(Long spaceId, TaskQuery query) {
        permissionValidator.ensureSpaceMember(spaceId, LoginUserUtil.currentUserId());
        Page<Task> page = new Page<>(query.safePageNum(), query.safePageSize());
        List<Long> taskIds = findTaskIdsByAssignee(query.getAssigneeId());
        if (query.getAssigneeId() != null && taskIds.isEmpty()) {
            return PageResponse.of(page, Collections.emptyList());
        }
        LambdaQueryWrapper<Task> wrapper = Wrappers.<Task>lambdaQuery()
                .eq(Task::getSpaceId, spaceId)
                .eq(query.getProjectId() != null, Task::getProjectId, query.getProjectId())
                .and(StringUtils.hasText(query.getKeyword()), wrapperItem -> wrapperItem
                        .like(Task::getTitle, query.getKeyword().trim())
                        .or()
                        .like(Task::getDescription, query.getKeyword().trim()))
                .eq(query.getMode() != null, Task::getMode, query.getMode())
                .in(!taskIds.isEmpty(), Task::getId, taskIds)
                .orderByDesc(Task::getGmtModified);
        applyTaskStatusFilter(wrapper, query.getStatus());
        IPage<Task> result = taskMapper.selectPage(page, wrapper);
        List<TaskVO> list = result.getRecords().stream().map(this::toTaskVO).toList();
        return PageResponse.of(page, list);
    }

    /**
     * 创建空间内任务并按任务模式初始化成员职责。
     *
     * @param spaceId 空间标识
     * @param request 创建请求
     * @return 任务详情
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskVO create(Long spaceId, TaskCreateRequest request) {
        Long currentUserId = LoginUserUtil.currentUserId();
        permissionValidator.ensureSpaceMember(spaceId, currentUserId);
        Space space = findSpace(spaceId);
        prepareCreateRequest(space, request, currentUserId);
        Task task = new Task();
        task.setSpaceId(spaceId);
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setCreatorId(currentUserId);
        task.setMode(request.getMode());
        task.setPriority(request.getPriority());
        task.setDeadline(request.getDeadline());
        task.setProjectId(validateProjectInSpace(spaceId, request.getProjectId()));
        task.setStatus(TaskStatus.PENDING);
        taskMapper.insert(task);
        if (TaskMode.ASSIGNED.equals(request.getMode())) {
            for (TaskAssignmentRequest assignment : request.getAssignments()) {
                TaskMember member = createAssignedMember(task, assignment);
                createTodoForMember(task, member);
                notifyAssignee(task, member);
            }
        }
        publishTaskRealtimeEvent(spaceId, SpaceRealtimeEventType.TASK_CREATED, task);
        log.info("任务创建完成，spaceId={}，taskId={}，creatorId={}，mode={}，status={}",
                spaceId, task.getId(), currentUserId, task.getMode(), task.getStatus());
        return toTaskVO(task);
    }

    /**
     * 查询空间内任务详情。
     *
     * @param spaceId 空间标识
     * @param id 任务标识
     * @return 任务详情
     */
    @Override
    public TaskVO get(Long spaceId, Long id) {
        permissionValidator.ensureSpaceMember(spaceId, LoginUserUtil.currentUserId());
        return toTaskVO(findTask(spaceId, id));
    }

    /**
     * 更新任务基础信息。
     *
     * @param spaceId 空间标识
     * @param id 任务标识
     * @param request 更新请求
     * @return 任务详情
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskVO update(Long spaceId, Long id, TaskUpdateRequest request) {
        Task task = findTask(spaceId, id);
        permissionValidator.ensureTaskCreatorOrAdmin(task, LoginUserUtil.currentUserId());
        ensureTaskMutable(task);
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setPriority(request.getPriority());
        task.setDeadline(request.getDeadline());
        task.setProjectId(validateProjectInSpace(spaceId, request.getProjectId()));
        taskMapper.updateById(task);
        publishTaskRealtimeEvent(spaceId, SpaceRealtimeEventType.TASK_UPDATED, task);
        log.info("任务基础信息更新完成，spaceId={}，taskId={}，operatorId={}",
                spaceId, id, LoginUserUtil.currentUserId());
        return toTaskVO(task);
    }

    /**
     * 按任务状态机修改任务整体状态。
     *
     * @param spaceId 空间标识
     * @param id 任务标识
     * @param request 状态修改请求
     * @return 任务详情
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskVO changeStatus(Long spaceId, Long id, TaskStatusUpdateRequest request) {
        Task task = findTask(spaceId, id);
        permissionValidator.ensureTaskCreatorOrAdmin(task, LoginUserUtil.currentUserId());
        TaskStatus targetStatus = request.getStatus();
        ensureManualTaskStatusAllowed(targetStatus);
        TaskStatus storedSourceStatus = task.getStatus();
        TaskStatus sourceStatus = normalizeTaskStatus(storedSourceStatus);
        sourceStatus.checkTransition(targetStatus);
        if (TaskStatus.COMPLETED.equals(targetStatus)) {
            ensureAllRequiredMembersCompleted(task.getId());
        }
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<Task> wrapper = Wrappers.<Task>lambdaUpdate()
                .eq(Task::getId, task.getId())
                .eq(Task::getStatus, storedSourceStatus)
                .set(Task::getStatus, targetStatus)
                .set(TaskStatus.COMPLETED.equals(targetStatus), Task::getCompletedAt, now);
        int updated = taskMapper.update(null, wrapper);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "任务状态已被其他操作更新");
        }
        task.setStatus(targetStatus);
        if (TaskStatus.COMPLETED.equals(targetStatus)) {
            task.setCompletedAt(now);
        }
        publishTaskRealtimeEvent(spaceId, SpaceRealtimeEventType.TASK_UPDATED, task);
        log.info("任务状态变更完成，spaceId={}，taskId={}，sourceStatus={}，targetStatus={}，operatorId={}",
                spaceId, id, sourceStatus, targetStatus, LoginUserUtil.currentUserId());
        return toTaskVO(task);
    }

    /**
     * 删除任务及关联的成员职责和待办。
     *
     * @param spaceId 空间标识
     * @param id 任务标识
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long spaceId, Long id) {
        Task task = findTask(spaceId, id);
        permissionValidator.ensureTaskCreatorOrAdmin(task, LoginUserUtil.currentUserId());
        List<Long> memberIds = taskMemberMapper.selectList(Wrappers.<TaskMember>lambdaQuery()
                        .eq(TaskMember::getTaskId, id))
                .stream()
                .map(TaskMember::getId)
                .toList();
        if (!memberIds.isEmpty()) {
            todoMapper.delete(Wrappers.<Todo>lambdaQuery().in(Todo::getTaskMemberId, memberIds));
            taskMemberMapper.delete(Wrappers.<TaskMember>lambdaQuery().in(TaskMember::getId, memberIds));
        }
        taskMapper.deleteById(id);
        publishTaskRealtimeEvent(spaceId, SpaceRealtimeEventType.TASK_DELETED, task);
        log.info("任务删除完成，spaceId={}，taskId={}，operatorId={}",
                spaceId, id, LoginUserUtil.currentUserId());
    }

    /**
     * 将任务成员职责标记为进行中。
     *
     * @param spaceId 空间标识
     * @param taskId 任务标识
     * @param memberId 成员职责标识
     * @return 成员职责信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskMemberVO startMember(Long spaceId, Long taskId, Long memberId) {
        Task task = findTask(spaceId, taskId);
        ensureTaskMutable(task);
        TaskMember member = findMember(taskId, memberId);
        permissionValidator.ensureTaskMemberOwner(member, LoginUserUtil.currentUserId());
        updateMemberStatus(task, member, TaskMemberStatus.IN_PROGRESS, null);
        publishTaskRealtimeEvent(spaceId, SpaceRealtimeEventType.TASK_UPDATED, task);
        log.info("任务成员开始处理，spaceId={}，taskId={}，memberId={}，operatorId={}",
                spaceId, taskId, memberId, LoginUserUtil.currentUserId());
        return toTaskMemberVO(member);
    }

    /**
     * 将任务成员职责标记为已完成并同步关联待办。
     *
     * @param spaceId 空间标识
     * @param taskId 任务标识
     * @param memberId 成员职责标识
     * @return 成员职责信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskMemberVO completeMember(Long spaceId, Long taskId, Long memberId, TaskMemberCompleteRequest request) {
        Task task = findTask(spaceId, taskId);
        ensureTaskMutable(task);
        TaskMember member = findMember(taskId, memberId);
        permissionValidator.ensureTaskMemberOwner(member, LoginUserUtil.currentUserId());
        updateMemberStatus(task, member, TaskMemberStatus.COMPLETED, completionRemark(request));
        todoMapper.update(null, Wrappers.<Todo>lambdaUpdate()
                .eq(Todo::getTaskMemberId, member.getId())
                .set(Todo::getIsCompleted, true));
        publishTaskRealtimeEvent(spaceId, SpaceRealtimeEventType.TASK_UPDATED, task);
        notifyTaskCreator(task, member.getUserId(), NotificationType.TASK_MEMBER_COMPLETED, "任务工作项已完成",
                "任务「" + task.getTitle() + "」中的职责「" + member.getResponsibility() + "」已提交完成。");
        log.info("任务成员完成处理，spaceId={}，taskId={}，memberId={}，operatorId={}",
                spaceId, taskId, memberId, LoginUserUtil.currentUserId());
        return toTaskMemberVO(member);
    }

    /**
     * 认领开放任务中的职责。
     *
     * @param spaceId 空间标识
     * @param taskId 任务标识
     * @param request 认领请求
     * @return 成员职责信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskMemberVO claim(Long spaceId, Long taskId, TaskClaimRequest request) {
        Long currentUserId = LoginUserUtil.currentUserId();
        permissionValidator.ensureSpaceMember(spaceId, currentUserId);
        Task task = findTask(spaceId, taskId);
        ensureTaskMutable(task);
        if (!TaskMode.OPEN.equals(task.getMode())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "仅开放模式任务允许认领");
        }
        ensureTeamTask(task.getSpaceId(), "开放认领任务仅支持团队空间");
        Long duplicateCount = taskMemberMapper.selectCount(Wrappers.<TaskMember>lambdaQuery()
                .eq(TaskMember::getTaskId, taskId)
                .eq(TaskMember::getUserId, currentUserId)
                .eq(TaskMember::getResponsibility, request.getResponsibility()));
        if (duplicateCount > 0) {
            throw new DuplicateClaimException("同一用户不可重复认领相同职责");
        }
        TaskMember member = new TaskMember();
        member.setTaskId(taskId);
        member.setUserId(currentUserId);
        member.setResponsibility(request.getResponsibility());
        member.setAssignmentType(AssignmentType.CLAIMED);
        member.setStatus(TaskMemberStatus.IN_PROGRESS);
        member.setIsRequired(Boolean.TRUE.equals(request.getIsRequired()));
        member.setStartedAt(LocalDateTime.now());
        member.setVersion(0);
        taskMemberMapper.insert(member);
        createTodoForMember(task, member);
        publishStatusChanged(task, member, TaskMemberStatus.IN_PROGRESS);
        publishTaskRealtimeEvent(spaceId, SpaceRealtimeEventType.TASK_UPDATED, task);
        notifyTaskCreator(task, currentUserId, NotificationType.TASK_CLAIMED, "开放任务已被认领",
                "任务「" + task.getTitle() + "」已被认领，职责：" + member.getResponsibility() + "。");
        log.info("开放任务认领完成，spaceId={}，taskId={}，memberId={}，operatorId={}",
                spaceId, taskId, member.getId(), currentUserId);
        return toTaskMemberVO(member);
    }

    /**
     * 为指派任务追加成员职责。
     *
     * @param spaceId 空间标识
     * @param taskId 任务标识
     * @param request 指派请求
     * @return 成员职责信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskMemberVO assign(Long spaceId, Long taskId, TaskAssignmentRequest request) {
        Task task = findTask(spaceId, taskId);
        permissionValidator.ensureTaskCreatorOrAdmin(task, LoginUserUtil.currentUserId());
        ensureTaskMutable(task);
        if (!TaskMode.ASSIGNED.equals(task.getMode())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "仅指派模式任务允许追加职责");
        }
        ensureTeamTask(task.getSpaceId(), "追加指派职责仅支持团队空间");
        validateAssigneeInSpace(spaceId, request.getUserId());
        Long duplicateCount = taskMemberMapper.selectCount(Wrappers.<TaskMember>lambdaQuery()
                .eq(TaskMember::getTaskId, taskId)
                .eq(TaskMember::getUserId, request.getUserId())
                .eq(TaskMember::getResponsibility, request.getResponsibility()));
        if (duplicateCount > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "相同职责已存在");
        }
        TaskMember member = createAssignedMember(task, request);
        createTodoForMember(task, member);
        notifyAssignee(task, member);
        log.info("任务职责指派完成，spaceId={}，taskId={}，memberId={}，assigneeId={}，operatorId={}",
                spaceId, taskId, member.getId(), request.getUserId(), LoginUserUtil.currentUserId());
        return toTaskMemberVO(member);
    }

    /**
     * 移除尚未开始的指派任务成员职责。
     *
     * @param spaceId 空间标识
     * @param taskId 任务标识
     * @param memberId 成员职责标识
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeMember(Long spaceId, Long taskId, Long memberId) {
        Task task = findTask(spaceId, taskId);
        permissionValidator.ensureTaskCreatorOrAdmin(task, LoginUserUtil.currentUserId());
        if (!TaskMode.ASSIGNED.equals(task.getMode())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "仅指派模式任务允许移除成员");
        }
        ensureTeamTask(task.getSpaceId(), "移除指派职责仅支持团队空间");
        TaskMember member = findMember(taskId, memberId);
        if (!TaskMemberStatus.PENDING.equals(member.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "已开始的职责不可移除");
        }
        todoMapper.delete(Wrappers.<Todo>lambdaQuery().eq(Todo::getTaskMemberId, memberId));
        taskMemberMapper.deleteById(memberId);
        taskStatusRecalculationService.recalculateAfterMemberRemoved(task.getId(), task.getSpaceId());
        log.info("任务职责移除完成，spaceId={}，taskId={}，memberId={}，operatorId={}",
                spaceId, taskId, memberId, LoginUserUtil.currentUserId());
    }

    /**
     * 查询任务评论列表。
     *
     * @param spaceId 空间标识
     * @param taskId 任务标识
     * @return 评论列表
     */
    @Override
    public List<TaskCommentVO> listComments(Long spaceId, Long taskId) {
        permissionValidator.ensureSpaceMember(spaceId, LoginUserUtil.currentUserId());
        findTask(spaceId, taskId);
        return taskCommentMapper.selectList(Wrappers.<TaskComment>lambdaQuery()
                        .eq(TaskComment::getTaskId, taskId)
                        .orderByAsc(TaskComment::getGmtCreate))
                .stream()
                .map(this::toTaskCommentVO)
                .toList();
    }

    /**
     * 为任务新增评论和提及关系。
     *
     * @param spaceId 空间标识
     * @param taskId 任务标识
     * @param request 评论创建请求
     * @return 评论详情
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskCommentVO addComment(Long spaceId, Long taskId, TaskCommentCreateRequest request) {
        Long currentUserId = LoginUserUtil.currentUserId();
        permissionValidator.ensureSpaceMember(spaceId, currentUserId);
        Task task = findTask(spaceId, taskId);
        List<Long> mentionUserIds = normalizedMentionUserIds(request.getMentionUserIds(), currentUserId);
        validateMentions(spaceId, mentionUserIds);
        TaskComment comment = new TaskComment();
        comment.setTaskId(taskId);
        comment.setUserId(currentUserId);
        comment.setParentCommentId(request.getParentCommentId());
        comment.setContent(request.getContent());
        taskCommentMapper.insert(comment);
        for (Long userId : mentionUserIds) {
            CommentMention mention = new CommentMention();
            mention.setCommentId(comment.getId());
            mention.setUserId(userId);
            commentMentionMapper.insert(mention);
            notifyMentionedUser(task, comment.getUserId(), userId);
        }
        log.info("任务评论新增完成，spaceId={}，taskId={}，commentId={}，operatorId={}",
                spaceId, taskId, comment.getId(), currentUserId);
        return toTaskCommentVO(comment);
    }

    /**
     * 删除当前用户有权限删除的任务评论。
     *
     * @param commentId 评论标识
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long commentId) {
        TaskComment comment = taskCommentMapper.selectById(commentId);
        if (comment == null) {
            throw new ResourceNotFoundException("评论不存在");
        }
        Task task = taskMapper.selectById(comment.getTaskId());
        if (task == null) {
            throw new ResourceNotFoundException("任务不存在");
        }
        Long currentUserId = LoginUserUtil.currentUserId();
        if (!currentUserId.equals(comment.getUserId())) {
            permissionValidator.ensureSpaceAdminOrOwner(task.getSpaceId(), currentUserId);
        }
        commentMentionMapper.delete(Wrappers.<CommentMention>lambdaQuery()
                .eq(CommentMention::getCommentId, commentId));
        taskCommentMapper.deleteById(commentId);
        log.info("任务评论删除完成，taskId={}，commentId={}，operatorId={}",
                task.getId(), commentId, currentUserId);
    }

    /**
     * 校验创建任务请求。
     *
     * @param space 空间实体
     * @param request 创建请求
     * @param currentUserId 当前用户标识
     */
    private void prepareCreateRequest(Space space, TaskCreateRequest request, Long currentUserId) {
        ensureAssignmentList(request);
        checkCreatePermissionByMode(request);
        if (SpaceType.PERSONAL.equals(space.getType())) {
            preparePersonalTaskRequest(request, currentUserId);
            return;
        }
        if (TaskMode.ASSIGNED.equals(request.getMode()) && request.getAssignments().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "指派模式至少需要一个职责");
        }
        if (TaskMode.OPEN.equals(request.getMode()) && !request.getAssignments().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "开放模式创建时不应指定责任人");
        }
        for (TaskAssignmentRequest assignment : request.getAssignments()) {
            validateAssigneeInSpace(space.getId(), assignment.getUserId());
        }
    }

    /**
     * 按任务模式校验创建权限。
     *
     * @param request 创建请求
     */
    private void checkCreatePermissionByMode(TaskCreateRequest request) {
        if (TaskMode.OPEN.equals(request.getMode())) {
            StpUtil.checkPermission("space:task:claim");
            return;
        }
        StpUtil.checkPermission("space:task:assign");
    }

    /**
     * 确保任务职责列表可安全使用。
     *
     * @param request 创建请求
     */
    private void ensureAssignmentList(TaskCreateRequest request) {
        if (request.getAssignments() == null) {
            request.setAssignments(new ArrayList<>());
        }
    }

    /**
     * 准备个人空间任务请求。
     *
     * @param request 创建请求
     * @param currentUserId 当前用户标识
     */
    private void preparePersonalTaskRequest(TaskCreateRequest request, Long currentUserId) {
        if (TaskMode.OPEN.equals(request.getMode())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "个人空间不支持开放认领任务");
        }
        if (request.getAssignments().isEmpty()) {
            TaskAssignmentRequest assignment = new TaskAssignmentRequest();
            assignment.setUserId(currentUserId);
            assignment.setResponsibility(request.getTitle());
            assignment.setIsRequired(true);
            request.getAssignments().add(assignment);
            return;
        }
        for (TaskAssignmentRequest assignment : request.getAssignments()) {
            if (!currentUserId.equals(assignment.getUserId())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "个人空间任务只能分配给当前用户");
            }
        }
    }

    /**
     * 校验任务所属空间是否为团队空间。
     *
     * @param spaceId 空间标识
     * @param message 错误提示
     */
    private void ensureTeamTask(Long spaceId, String message) {
        Space space = findSpace(spaceId);
        if (!SpaceType.TEAM.equals(space.getType())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, message);
        }
    }

    /**
     * 查询空间实体。
     *
     * @param spaceId 空间标识
     * @return 空间实体
     */
    private Space findSpace(Long spaceId) {
        Space space = spaceMapper.selectById(spaceId);
        if (space == null) {
            throw new ResourceNotFoundException("空间不存在");
        }
        return space;
    }

    /**
     * 校验责任人是否为空间成员。
     *
     * @param spaceId 空间标识
     * @param userId 用户标识
     */
    private void validateAssigneeInSpace(Long spaceId, Long userId) {
        SpaceMember member = permissionValidator.findSpaceMember(spaceId, userId);
        if (member == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "责任人不是当前空间成员");
        }
    }

    /**
     * 创建指派任务成员。
     *
     * @param task 任务实体
     * @param assignment 分配请求
     * @return 任务成员
     */
    private TaskMember createAssignedMember(Task task, TaskAssignmentRequest assignment) {
        TaskMember member = new TaskMember();
        member.setTaskId(task.getId());
        member.setUserId(assignment.getUserId());
        member.setResponsibility(assignment.getResponsibility());
        member.setAssignmentType(AssignmentType.ASSIGNED);
        member.setStatus(TaskMemberStatus.PENDING);
        member.setIsRequired(Boolean.TRUE.equals(assignment.getIsRequired()));
        member.setVersion(0);
        taskMemberMapper.insert(member);
        return member;
    }

    /**
     * 为任务成员创建关联待办。
     *
     * @param task 任务实体
     * @param member 任务成员
     */
    private void createTodoForMember(Task task, TaskMember member) {
        Todo todo = new Todo();
        todo.setSpaceId(task.getSpaceId());
        todo.setUserId(member.getUserId());
        todo.setTaskMemberId(member.getId());
        todo.setTitle("任务：" + task.getTitle() + " - " + member.getResponsibility());
        todo.setIsCompleted(false);
        todo.setDeadline(task.getDeadline());
        todoMapper.insert(todo);
        notificationService.create(member.getUserId(), NotificationType.TODO_CREATED, BusinessType.TODO, todo.getId(),
                "待办已生成", "任务《" + task.getTitle() + "》已同步生成待办。");
    }

    /**
     * 向被指派的成员发送任务通知。
     *
     * @param task 任务实体
     * @param member 任务成员
     */
    private void notifyAssignee(Task task, TaskMember member) {
        if (member.getUserId() == null || member.getUserId().equals(task.getCreatorId())) {
            return;
        }
        notificationService.create(member.getUserId(), NotificationType.TASK_CREATED, BusinessType.TASK, task.getId(),
                "你被指派了任务", "任务「" + task.getTitle() + "」已指派给你，职责：" + member.getResponsibility() + "。");
    }

    /**
     * 向任务创建者发送任务动态通知。
     *
     * @param task 任务实体
     * @param actorUserId 动作发起用户标识
     * @param type 通知类型
     * @param title 通知标题
     * @param content 通知内容
     */
    private void notifyTaskCreator(Task task, Long actorUserId, NotificationType type, String title, String content) {
        if (task.getCreatorId() == null || task.getCreatorId().equals(actorUserId)) {
            return;
        }
        notificationService.create(task.getCreatorId(), type, BusinessType.TASK, task.getId(), title, content);
    }

    /**
     * 向评论中被提及的用户发送通知。
     *
     * @param task 任务实体
     * @param actorUserId 评论用户标识
     * @param mentionedUserId 被提及用户标识
     */
    private void notifyMentionedUser(Task task, Long actorUserId, Long mentionedUserId) {
        if (mentionedUserId == null || mentionedUserId.equals(actorUserId)) {
            return;
        }
        notificationService.create(mentionedUserId, NotificationType.COMMENT_MENTIONED, BusinessType.TASK, task.getId(),
                "你在任务评论中被提及", "任务「" + task.getTitle() + "」中的评论提及了你。");
    }

    /**
     * 更新任务成员状态并发布领域事件。
     *
     * @param task 任务实体
     * @param member 成员工作项
     * @param targetStatus 目标状态
     */
    private void updateMemberStatus(Task task, TaskMember member, TaskMemberStatus targetStatus,
                                    String completionRemark) {
        TaskMemberStatus sourceStatus = member.getStatus();
        sourceStatus.checkTransition(targetStatus);
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<TaskMember> wrapper = Wrappers.<TaskMember>lambdaUpdate()
                .eq(TaskMember::getId, member.getId())
                .eq(TaskMember::getStatus, sourceStatus)
                .eq(TaskMember::getVersion, member.getVersion())
                .set(TaskMember::getStatus, targetStatus)
                .set(TaskMember::getVersion, member.getVersion() + 1)
                .set(TaskMemberStatus.IN_PROGRESS.equals(targetStatus), TaskMember::getStartedAt, now)
                .set(TaskMemberStatus.COMPLETED.equals(targetStatus), TaskMember::getCompletedAt, now)
                .set(TaskMemberStatus.COMPLETED.equals(targetStatus), TaskMember::getCompletionRemark,
                        completionRemark);
        int updated = taskMemberMapper.update(null, wrapper);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "成员状态已被其他操作更新");
        }
        member.setStatus(targetStatus);
        member.setVersion(member.getVersion() + 1);
        if (TaskMemberStatus.IN_PROGRESS.equals(targetStatus)) {
            member.setStartedAt(now);
        }
        if (TaskMemberStatus.COMPLETED.equals(targetStatus)) {
            member.setCompletedAt(now);
            member.setCompletionRemark(completionRemark);
        }
        publishStatusChanged(task, member, targetStatus);
    }

    /**
     * 规范化任务完成说明。
     *
     * @param request 任务成员完成请求
     * @return 完成说明
     */
    private String completionRemark(TaskMemberCompleteRequest request) {
        if (request == null || !StringUtils.hasText(request.getCompletionRemark())) {
            return null;
        }
        return request.getCompletionRemark().trim();
    }

    /**
     * 发布任务成员状态变更事件。
     *
     * @param task 任务实体
     * @param member 成员工作项
     * @param targetStatus 目标状态
     */
    private void publishStatusChanged(Task task, TaskMember member, TaskMemberStatus targetStatus) {
        eventPublisher.publishEvent(new TaskMemberStatusChangedEvent(task.getId(), member.getId(), task.getSpaceId(),
                targetStatus));
    }

    /**
     * 发布任务实时事件。
     *
     * @param spaceId 空间标识
     * @param type 事件类型
     * @param task 任务实体
     */
    private void publishTaskRealtimeEvent(Long spaceId, SpaceRealtimeEventType type, Task task) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("taskId", task.getId());
        payload.put("status", task.getStatus());
        payload.put("title", task.getTitle());
        if (task.getProjectId() != null) {
            payload.put("projectId", task.getProjectId());
        }
        spaceRealtimeEventService.publish(spaceId, type, payload);
    }

    /**
     * 查询任务实体。
     *
     * @param spaceId 空间标识
     * @param taskId 任务标识
     * @return 任务实体
     */
    private Task findTask(Long spaceId, Long taskId) {
        Task task = taskMapper.selectOne(Wrappers.<Task>lambdaQuery()
                .eq(Task::getSpaceId, spaceId)
                .eq(Task::getId, taskId));
        if (task == null) {
            throw new ResourceNotFoundException("任务不存在");
        }
        return task;
    }

    /**
     * 查询任务成员。
     *
     * @param taskId 任务标识
     * @param memberId 成员工作项标识
     * @return 任务成员
     */
    private TaskMember findMember(Long taskId, Long memberId) {
        TaskMember member = taskMemberMapper.selectOne(Wrappers.<TaskMember>lambdaQuery()
                .eq(TaskMember::getTaskId, taskId)
                .eq(TaskMember::getId, memberId));
        if (member == null) {
            throw new TaskMemberNotFoundException("任务成员不存在");
        }
        return member;
    }

    /**
     * 校验任务是否可变更。
     *
     * @param task 任务实体
     */
    private void ensureTaskMutable(Task task) {
        if (TaskStatus.COMPLETED.equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.TASK_ALREADY_COMPLETED);
        }
        if (TaskStatus.CANCELLED.equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.TASK_STATUS_ILLEGAL, "任务已取消，不可操作");
        }
    }

    /**
     * 校验任务整体状态是否允许人工修改。
     *
     * @param targetStatus 目标状态
     */
    private void ensureManualTaskStatusAllowed(TaskStatus targetStatus) {
        if (TaskStatus.OPEN.equals(targetStatus)) {
            throw new BusinessException(ErrorCode.TASK_STATUS_ILLEGAL, "任务整体状态只支持待开始、进行中、已完成、已取消");
        }
        if (TaskStatus.IN_PROGRESS.equals(targetStatus)) {
            throw new BusinessException(ErrorCode.TASK_STATUS_ILLEGAL, "任务进行中状态由认领或责任成员开始后自动推进");
        }
    }

    /**
     * 应用任务状态查询条件，并将历史开放状态归入待开始列。
     *
     * @param wrapper 查询条件
     * @param status 查询状态
     */
    private void applyTaskStatusFilter(LambdaQueryWrapper<Task> wrapper, TaskStatus status) {
        if (status == null) {
            return;
        }
        TaskStatus normalizedStatus = normalizeTaskStatus(status);
        if (TaskStatus.PENDING.equals(normalizedStatus)) {
            wrapper.in(Task::getStatus, List.of(TaskStatus.PENDING, TaskStatus.OPEN));
            return;
        }
        wrapper.eq(Task::getStatus, normalizedStatus);
    }

    /**
     * 将历史开放状态规范化为待开始状态。
     *
     * @param status 原始状态
     * @return 对外展示状态
     */
    private TaskStatus normalizeTaskStatus(TaskStatus status) {
        if (TaskStatus.OPEN.equals(status)) {
            return TaskStatus.PENDING;
        }
        return status;
    }

    /**
     * 校验所有必需职责是否已完成。
     *
     * @param taskId 任务标识
     */
    private void ensureAllRequiredMembersCompleted(Long taskId) {
        Long unfinishedRequiredCount = taskMemberMapper.selectCount(Wrappers.<TaskMember>lambdaQuery()
                .eq(TaskMember::getTaskId, taskId)
                .eq(TaskMember::getIsRequired, true)
                .ne(TaskMember::getStatus, TaskMemberStatus.COMPLETED));
        if (unfinishedRequiredCount > 0) {
            throw new BusinessException(ErrorCode.TASK_STATUS_ILLEGAL, "存在未完成的必需职责");
        }
    }

    /**
     * 根据责任人查询任务标识。
     *
     * @param assigneeId 责任人标识
     * @return 任务标识列表
     */
    private List<Long> findTaskIdsByAssignee(Long assigneeId) {
        if (assigneeId == null) {
            return Collections.emptyList();
        }
        return taskMemberMapper.selectList(Wrappers.<TaskMember>lambdaQuery().eq(TaskMember::getUserId, assigneeId))
                .stream()
                .map(TaskMember::getTaskId)
                .distinct()
                .toList();
    }

    /**
     * 规范化评论提及用户列表。
     *
     * @param mentionUserIds 原始提及用户标识列表
     * @param currentUserId 当前用户标识
     * @return 去重后的提及用户标识列表
     */
    private List<Long> normalizedMentionUserIds(List<Long> mentionUserIds, Long currentUserId) {
        if (mentionUserIds == null) {
            return Collections.emptyList();
        }
        return mentionUserIds.stream()
                .filter(Objects::nonNull)
                .filter(userId -> !userId.equals(currentUserId))
                .distinct()
                .toList();
    }

    /**
     * 校验提及用户是否为空间成员。
     *
     * @param spaceId 空间标识
     * @param mentionUserIds 提及用户标识列表
     */
    private void validateMentions(Long spaceId, List<Long> mentionUserIds) {
        for (Long userId : mentionUserIds) {
            validateAssigneeInSpace(spaceId, userId);
        }
    }

    /**
     * 转换任务视图对象。
     *
     * @param task 任务实体
     * @return 任务视图对象
     */
    private TaskVO toTaskVO(Task task) {
        List<TaskMemberVO> members = taskMemberMapper.selectList(Wrappers.<TaskMember>lambdaQuery()
                        .eq(TaskMember::getTaskId, task.getId())
                        .orderByAsc(TaskMember::getId))
                .stream()
                .map(this::toTaskMemberVO)
                .toList();
        return new TaskVO(task.getId(), task.getSpaceId(), task.getProjectId(), projectNameOf(task.getProjectId()),
                task.getTitle(), task.getDescription(), task.getCreatorId(), task.getMode(), normalizeTaskStatus(task.getStatus()),
                task.getPriority(), task.getDeadline(), task.getCompletedAt(), task.getGmtCreate(),
                task.getGmtModified(), members);
    }

    /**
     * 校验项目是否属于当前空间且可关联。
     *
     * @param spaceId 空间标识
     * @param projectId 项目标识
     * @return 合法的项目标识
     */
    private Long validateProjectInSpace(Long spaceId, Long projectId) {
        if (projectId == null) {
            return null;
        }
        Project project = projectMapper.selectOne(Wrappers.<Project>lambdaQuery()
                .eq(Project::getSpaceId, spaceId)
                .eq(Project::getId, projectId)
                .eq(Project::getArchived, false));
        if (project == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "项目不存在或已归档");
        }
        return projectId;
    }

    /**
     * 查询项目名称。
     *
     * @param projectId 项目标识
     * @return 项目名称
     */
    private String projectNameOf(Long projectId) {
        if (projectId == null) {
            return null;
        }
        Project project = projectMapper.selectById(projectId);
        return project == null ? null : project.getName();
    }

    /**
     * 转换任务成员视图对象。
     *
     * @param member 任务成员实体
     * @return 任务成员视图对象
     */
    private TaskMemberVO toTaskMemberVO(TaskMember member) {
        User user = userMapper.selectById(member.getUserId());
        String username = user == null ? "" : user.getUsername();
        return new TaskMemberVO(member.getId(), member.getTaskId(), member.getUserId(), username,
                member.getResponsibility(), member.getAssignmentType(), member.getStatus(), member.getIsRequired(),
                member.getStartedAt(), member.getCompletedAt(), member.getCompletionRemark(), member.getVersion());
    }

    /**
     * 转换任务评论视图对象。
     *
     * @param comment 评论实体
     * @return 评论视图对象
     */
    private TaskCommentVO toTaskCommentVO(TaskComment comment) {
        User user = userMapper.selectById(comment.getUserId());
        String username = user == null ? "" : user.getUsername();
        List<Long> mentionUserIds = commentMentionMapper.selectList(Wrappers.<CommentMention>lambdaQuery()
                        .eq(CommentMention::getCommentId, comment.getId()))
                .stream()
                .map(CommentMention::getUserId)
                .toList();
        return new TaskCommentVO(comment.getId(), comment.getTaskId(), comment.getUserId(), username,
                comment.getParentCommentId(), comment.getContent(), comment.getGmtCreate(), mentionUserIds);
    }
}
