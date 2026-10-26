package com.notaskflow.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.notaskflow.common.constant.RedisKeyConstants;
import com.notaskflow.common.enums.BusinessType;
import com.notaskflow.common.enums.RoleCode;
import com.notaskflow.common.enums.SearchIndexOperation;
import com.notaskflow.common.enums.SpaceRealtimeEventType;
import com.notaskflow.common.enums.SpaceType;
import com.notaskflow.domain.dto.request.SpaceCreateRequest;
import com.notaskflow.domain.dto.request.SpaceMemberAddRequest;
import com.notaskflow.domain.dto.request.SpaceMemberRoleUpdateRequest;
import com.notaskflow.domain.dto.request.SpaceUpdateRequest;
import com.notaskflow.domain.entity.FileFolder;
import com.notaskflow.domain.entity.ManagedFile;
import com.notaskflow.domain.entity.Note;
import com.notaskflow.domain.entity.NoteHistory;
import com.notaskflow.domain.entity.Notification;
import com.notaskflow.domain.entity.Notebook;
import com.notaskflow.domain.entity.Project;
import com.notaskflow.domain.entity.ProjectMember;
import com.notaskflow.domain.entity.Role;
import com.notaskflow.domain.entity.Space;
import com.notaskflow.domain.entity.SpaceMember;
import com.notaskflow.domain.entity.Task;
import com.notaskflow.domain.entity.TaskMember;
import com.notaskflow.domain.entity.Todo;
import com.notaskflow.domain.entity.User;
import com.notaskflow.domain.vo.SpaceEventTicketConsumeVO;
import com.notaskflow.domain.vo.SpaceEventTicketVO;
import com.notaskflow.domain.vo.SpaceMemberVO;
import com.notaskflow.domain.vo.SpaceVO;
import com.notaskflow.event.SearchIndexRequestedEvent;
import com.notaskflow.exception.BusinessException;
import com.notaskflow.exception.ErrorCode;
import com.notaskflow.exception.ResourceNotFoundException;
import com.notaskflow.mapper.BusinessAttachmentMapper;
import com.notaskflow.mapper.FileFolderMapper;
import com.notaskflow.mapper.ManagedFileMapper;
import com.notaskflow.mapper.NoteHistoryMapper;
import com.notaskflow.mapper.NoteMapper;
import com.notaskflow.mapper.NoteTagMapper;
import com.notaskflow.mapper.NotebookMapper;
import com.notaskflow.mapper.NotificationMapper;
import com.notaskflow.mapper.ProjectMapper;
import com.notaskflow.mapper.ProjectMemberMapper;
import com.notaskflow.mapper.RoleMapper;
import com.notaskflow.mapper.SpaceMapper;
import com.notaskflow.mapper.SpaceMemberMapper;
import com.notaskflow.mapper.TaskMapper;
import com.notaskflow.mapper.TaskMemberMapper;
import com.notaskflow.mapper.TodoMapper;
import com.notaskflow.mapper.UserMapper;
import com.notaskflow.security.PermissionValidator;
import com.notaskflow.service.SpaceMemberPresenceService;
import com.notaskflow.service.SpacePermissionCacheService;
import com.notaskflow.service.SpaceRealtimeEventService;
import com.notaskflow.service.SpaceService;
import com.notaskflow.service.SystemSettingService;
import com.notaskflow.storage.MinioStorageService;
import com.notaskflow.utils.AvatarUrlUtil;
import com.notaskflow.utils.LoginUserUtil;
import com.notaskflow.utils.RedisUtil;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 空间服务实现，处理团队空间和空间成员管理。
 *
 * @author LIN
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpaceServiceImpl implements SpaceService {

    private static final String SPACE_EVENT_TICKET_DELIMITER = ":";

    private final SpaceMapper spaceMapper;

    private final SpaceMemberMapper spaceMemberMapper;

    private final RoleMapper roleMapper;

    private final UserMapper userMapper;

    private final NotificationMapper notificationMapper;

    private final ProjectMapper projectMapper;

    private final ProjectMemberMapper projectMemberMapper;

    private final TaskMapper taskMapper;

    private final TaskMemberMapper taskMemberMapper;

    private final TodoMapper todoMapper;

    private final NoteMapper noteMapper;

    private final NoteHistoryMapper noteHistoryMapper;

    private final NoteTagMapper noteTagMapper;

    private final NotebookMapper notebookMapper;

    private final ManagedFileMapper managedFileMapper;

    private final FileFolderMapper fileFolderMapper;

    private final BusinessAttachmentMapper businessAttachmentMapper;

    private final ApplicationEventPublisher applicationEventPublisher;

    private final MinioStorageService minioStorageService;

    private final PermissionValidator permissionValidator;

    private final SpaceMemberPresenceService spaceMemberPresenceService;

    private final SpacePermissionCacheService spacePermissionCacheService;

    private final RedisUtil redisUtil;

    private final SystemSettingService systemSettingService;

    private final SpaceRealtimeEventService spaceRealtimeEventService;

    /**
     * 查询当前用户加入的空间。
     *
     * @return 空间列表
     */
    @Override
    public List<SpaceVO> listMySpaces() {
        Long currentUserId = LoginUserUtil.currentUserId();
        List<Long> spaceIds = spaceMemberMapper.selectList(Wrappers.<SpaceMember>lambdaQuery()
                        .eq(SpaceMember::getUserId, currentUserId))
                .stream()
                .map(SpaceMember::getSpaceId)
                .toList();
        if (spaceIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Space> spaces = spaceMapper.selectBatchIds(spaceIds).stream()
                .sorted(spaceComparator())
                .toList();
        Map<Long, Long> memberCounts = countMembersBySpace(spaceIds);
        Map<Long, Long> unreadCounts = countUnreadBySpace(spaceIds, currentUserId);
        List<SpaceVO> list = new ArrayList<>(spaces.size());
        for (Space space : spaces) {
            list.add(toSpaceVO(space,
                    memberCounts.getOrDefault(space.getId(), 0L),
                    unreadCounts.getOrDefault(space.getId(), 0L)));
        }
        return list;
    }

    /**
     * 批量统计各空间成员数量。
     *
     * @param spaceIds 空间标识列表
     * @return 空间标识到成员数量的映射
     */
    private Map<Long, Long> countMembersBySpace(List<Long> spaceIds) {
        return spaceMemberMapper.selectList(Wrappers.<SpaceMember>lambdaQuery()
                        .select(SpaceMember::getSpaceId)
                        .in(SpaceMember::getSpaceId, spaceIds))
                .stream()
                .collect(Collectors.groupingBy(
                        SpaceMember::getSpaceId, Collectors.counting()));
    }

    /**
     * 批量统计各空间未读通知数量。
     *
     * @param spaceIds 空间标识列表
     * @param userId 用户标识
     * @return 空间标识到未读数量的映射
     */
    private Map<Long, Long> countUnreadBySpace(List<Long> spaceIds, Long userId) {
        return notificationMapper.selectList(Wrappers.<Notification>lambdaQuery()
                        .select(Notification::getSpaceId)
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getIsRead, false)
                        .in(Notification::getSpaceId, spaceIds))
                .stream()
                .collect(Collectors.groupingBy(
                        Notification::getSpaceId, Collectors.counting()));
    }

    /**
     * 创建团队空间并将当前用户设为空间所有者。
     *
     * @param request 空间创建请求
     * @return 空间信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SpaceVO createTeamSpace(SpaceCreateRequest request) {
        Long currentUserId = LoginUserUtil.currentUserId();
        Role ownerRole = findRoleByCode(RoleCode.SPACE_OWNER.getCode());
        Space space = new Space();
        space.setName(request.getName());
        space.setType(SpaceType.TEAM);
        space.setOwnerUserId(currentUserId);
        space.setJoinApprovalRequired(systemSettingService.isNewTeamJoinApprovalRequired());
        space.setMemberInviteEnabled(true);
        spaceMapper.insert(space);

        SpaceMember member = new SpaceMember();
        member.setSpaceId(space.getId());
        member.setUserId(currentUserId);
        member.setRoleId(ownerRole.getId());
        spaceMemberMapper.insert(member);
        log.info("团队空间创建完成，spaceId={}，ownerUserId={}", space.getId(), currentUserId);
        return toSpaceVO(space, currentUserId);
    }

    /**
     * 查询指定空间信息。
     *
     * @param spaceId 空间标识
     * @return 空间信息
     */
    @Override
    public SpaceVO getSpace(Long spaceId) {
        Long currentUserId = LoginUserUtil.currentUserId();
        permissionValidator.ensureSpaceMember(spaceId, currentUserId);
        return toSpaceVO(findSpace(spaceId), currentUserId);
    }

    /**
     * 更新指定空间的基础信息。
     *
     * @param spaceId 空间标识
     * @param request 空间更新请求
     * @return 空间信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SpaceVO updateSpace(Long spaceId, SpaceUpdateRequest request) {
        Long currentUserId = LoginUserUtil.currentUserId();
        permissionValidator.ensureSpaceAdminOrOwner(spaceId, currentUserId);
        Space space = findSpace(spaceId);
        space.setName(request.getName());
        if (request.getJoinApprovalRequired() != null) {
            space.setJoinApprovalRequired(request.getJoinApprovalRequired());
        }
        if (request.getMemberInviteEnabled() != null) {
            space.setMemberInviteEnabled(request.getMemberInviteEnabled());
        }
        spaceMapper.updateById(space);
        log.info("空间信息更新完成，spaceId={}，joinApprovalRequired={}，memberInviteEnabled={}，operatorId={}",
                spaceId, space.getJoinApprovalRequired(), space.getMemberInviteEnabled(), currentUserId);
        return toSpaceVO(space, currentUserId);
    }

    /**
     * 删除团队空间，并级联清理空间下的全部业务数据。
     *
     * <p>空间解散时必须同步软删除项目、任务、待办、笔记、网盘与笔记本数据，
     * 清理 Elasticsearch 索引与全部成员的权限缓存，否则统计接口仍会把已解散空间的数据计入个人负载。</p>
     *
     * @param spaceId 空间标识
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSpace(Long spaceId) {
        Long currentUserId = LoginUserUtil.currentUserId();
        permissionValidator.ensureSpaceOwner(spaceId, currentUserId);
        Space space = findSpace(spaceId);
        if (SpaceType.PERSONAL.equals(space.getType())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "个人空间不可删除");
        }
        List<SpaceMember> members = spaceMemberMapper.selectList(Wrappers.<SpaceMember>lambdaQuery()
                .eq(SpaceMember::getSpaceId, spaceId));
        List<Long> noteIds = noteMapper.selectList(Wrappers.<Note>lambdaQuery()
                        .select(Note::getId)
                        .eq(Note::getSpaceId, spaceId))
                .stream()
                .map(Note::getId)
                .toList();
        List<Long> taskIds = taskMapper.selectList(Wrappers.<Task>lambdaQuery()
                        .select(Task::getId)
                        .eq(Task::getSpaceId, spaceId))
                .stream()
                .map(Task::getId)
                .toList();
        List<Long> todoIds = todoMapper.selectList(Wrappers.<Todo>lambdaQuery()
                        .select(Todo::getId)
                        .eq(Todo::getSpaceId, spaceId))
                .stream()
                .map(Todo::getId)
                .toList();
        cleanSpaceNotes(noteIds);
        cleanSpaceBusinessAttachments(taskIds, todoIds);
        if (!taskIds.isEmpty()) {
            taskMemberMapper.delete(Wrappers.<TaskMember>lambdaQuery().in(TaskMember::getTaskId, taskIds));
        }
        taskMapper.delete(Wrappers.<Task>lambdaQuery().eq(Task::getSpaceId, spaceId));
        todoMapper.delete(Wrappers.<Todo>lambdaQuery().eq(Todo::getSpaceId, spaceId));
        List<Long> projectIds = projectMapper.selectList(Wrappers.<Project>lambdaQuery()
                        .select(Project::getId)
                        .eq(Project::getSpaceId, spaceId))
                .stream()
                .map(Project::getId)
                .toList();
        if (!projectIds.isEmpty()) {
            projectMemberMapper.delete(Wrappers.<ProjectMember>lambdaQuery()
                    .in(ProjectMember::getProjectId, projectIds));
        }
        projectMapper.delete(Wrappers.<Project>lambdaQuery().eq(Project::getSpaceId, spaceId));
        notebookMapper.delete(Wrappers.<Notebook>lambdaQuery().eq(Notebook::getSpaceId, spaceId));
        managedFileMapper.delete(Wrappers.<ManagedFile>lambdaQuery().eq(ManagedFile::getSpaceId, spaceId));
        fileFolderMapper.delete(Wrappers.<FileFolder>lambdaQuery().eq(FileFolder::getSpaceId, spaceId));
        spaceMapper.deleteById(spaceId);
        spaceMemberMapper.delete(Wrappers.<SpaceMember>lambdaQuery().eq(SpaceMember::getSpaceId, spaceId));
        for (SpaceMember member : members) {
            spacePermissionCacheService.evict(spaceId, member.getUserId());
        }
        spacePermissionCacheService.evict(spaceId, currentUserId);
        spaceRealtimeEventService.publishSystem(spaceId, SpaceRealtimeEventType.SPACE_MEMBER_CHANGED,
                Map.of("spaceId", spaceId, "deleted", true));
        log.info("团队空间删除完成，spaceId={}，operatorId={}，noteCount={}，taskCount={}",
                spaceId, currentUserId, noteIds.size(), taskIds.size());
    }

    /**
     * 清理空间下的笔记关联数据并发布搜索索引删除事件。
     *
     * @param noteIds 笔记标识列表
     */
    private void cleanSpaceNotes(List<Long> noteIds) {
        if (noteIds.isEmpty()) {
            return;
        }
        noteHistoryMapper.delete(Wrappers.<NoteHistory>lambdaQuery().in(NoteHistory::getNoteId, noteIds));
        noteTagMapper.physicalDeleteByNoteIds(noteIds);
        businessAttachmentMapper.physicalDeleteByBusinessIds(BusinessType.NOTE, noteIds);
        noteMapper.deleteBatchIds(noteIds);
        noteIds.forEach(noteId -> applicationEventPublisher.publishEvent(
                new SearchIndexRequestedEvent(noteId, SearchIndexOperation.DELETE)));
    }

    /**
     * 清理空间下任务与待办的附件绑定关系。
     *
     * @param taskIds 任务标识列表
     * @param todoIds 待办标识列表
     */
    private void cleanSpaceBusinessAttachments(List<Long> taskIds, List<Long> todoIds) {
        if (!taskIds.isEmpty()) {
            businessAttachmentMapper.physicalDeleteByBusinessIds(BusinessType.TASK, taskIds);
        }
        if (!todoIds.isEmpty()) {
            businessAttachmentMapper.physicalDeleteByBusinessIds(BusinessType.TODO, todoIds);
        }
    }

    /**
     * 查询指定空间的成员列表。
     *
     * @param spaceId 空间标识
     * @return 空间成员列表
     */
    @Override
    public List<SpaceMemberVO> listMembers(Long spaceId) {
        permissionValidator.ensureSpaceMember(spaceId, LoginUserUtil.currentUserId());
        return spaceMemberMapper.selectList(Wrappers.<SpaceMember>lambdaQuery()
                        .eq(SpaceMember::getSpaceId, spaceId))
                .stream()
                .map(this::toSpaceMemberVO)
                .toList();
    }

    /**
     * 刷新当前用户在指定团队空间的在线状态。
     *
     * @param spaceId 空间标识
     * @param clientId 客户端标识
     */
    @Override
    public void heartbeatMember(Long spaceId, String clientId) {
        Long currentUserId = LoginUserUtil.currentUserId();
        permissionValidator.ensureSpaceMember(spaceId, currentUserId);
        if (spaceMemberPresenceService.markOnline(spaceId, currentUserId, clientId)) {
            publishMemberPresenceEvent(spaceId, currentUserId, SpaceRealtimeEventType.MEMBER_ONLINE);
        }
    }

    /**
     * 清理当前用户在指定团队空间的在线状态。
     *
     * @param spaceId 空间标识
     * @param clientId 客户端标识
     */
    @Override
    public void offlineMember(Long spaceId, String clientId) {
        Long currentUserId = LoginUserUtil.currentUserId();
        permissionValidator.ensureSpaceMember(spaceId, currentUserId);
        if (spaceMemberPresenceService.markOffline(spaceId, currentUserId, clientId)) {
            publishMemberPresenceEvent(spaceId, currentUserId, SpaceRealtimeEventType.MEMBER_OFFLINE);
        }
    }

    /**
     * 添加用户为空间成员。
     *
     * @param spaceId 空间标识
     * @param request 成员添加请求
     * @return 空间成员信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SpaceMemberVO addMember(Long spaceId, SpaceMemberAddRequest request) {
        Long currentUserId = LoginUserUtil.currentUserId();
        permissionValidator.ensureSpaceAdminOrOwner(spaceId, currentUserId);
        Space space = findSpace(spaceId);
        if (SpaceType.PERSONAL.equals(space.getType())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "个人空间不允许添加成员");
        }
        User user = userMapper.selectById(request.getUserId());
        if (user == null) {
            throw new ResourceNotFoundException("用户不存在");
        }
        Role role = findRoleByCode(request.getRoleCode());
        SpaceMember exists = permissionValidator.findSpaceMember(spaceId, request.getUserId());
        if (exists != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "用户已是空间成员");
        }
        SpaceMember member = new SpaceMember();
        member.setSpaceId(spaceId);
        member.setUserId(request.getUserId());
        member.setRoleId(role.getId());
        spaceMemberMapper.insert(member);
        log.info("空间成员添加完成，spaceId={}，targetUserId={}，roleCode={}，operatorId={}",
                spaceId, request.getUserId(), request.getRoleCode(), currentUserId);
        publishSpaceMemberChangedEvent(spaceId, request.getUserId());
        return toSpaceMemberVO(member);
    }

    /**
     * 更新空间成员角色。
     *
     * @param spaceId 空间标识
     * @param userId 用户标识
     * @param request 角色更新请求
     * @return 空间成员信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SpaceMemberVO updateMemberRole(Long spaceId, Long userId, SpaceMemberRoleUpdateRequest request) {
        Long currentUserId = LoginUserUtil.currentUserId();
        permissionValidator.ensureSpaceAdminOrOwner(spaceId, currentUserId);
        Space space = findSpace(spaceId);
        if (space.getOwnerUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "空间所有者角色不可通过成员接口修改");
        }
        Role role = findRoleByCode(request.getRoleCode());
        SpaceMember member = permissionValidator.findSpaceMember(spaceId, userId);
        if (member == null) {
            throw new ResourceNotFoundException("空间成员不存在");
        }
        member.setRoleId(role.getId());
        spaceMemberMapper.update(member, Wrappers.<SpaceMember>lambdaUpdate()
                .eq(SpaceMember::getSpaceId, spaceId)
                .eq(SpaceMember::getUserId, userId));
        log.info("空间成员角色更新完成，spaceId={}，targetUserId={}，roleCode={}，operatorId={}",
                spaceId, userId, request.getRoleCode(), currentUserId);
        publishSpaceMemberChangedEvent(spaceId, userId);
        return toSpaceMemberVO(member);
    }

    /**
     * 移除指定空间成员。
     *
     * @param spaceId 空间标识
     * @param userId 用户标识
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeMember(Long spaceId, Long userId) {
        Long currentUserId = LoginUserUtil.currentUserId();
        permissionValidator.ensureSpaceAdminOrOwner(spaceId, currentUserId);
        Space space = findSpace(spaceId);
        if (space.getOwnerUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "空间所有者不可移除");
        }
        SpaceMember member = permissionValidator.findSpaceMember(spaceId, userId);
        if (member == null) {
            throw new ResourceNotFoundException("空间成员不存在");
        }
        spaceMemberPresenceService.clearMemberOnline(spaceId, userId);
        spaceMemberMapper.delete(Wrappers.<SpaceMember>lambdaQuery()
                .eq(SpaceMember::getSpaceId, spaceId)
                .eq(SpaceMember::getUserId, userId));
        log.info("空间成员移除完成，spaceId={}，targetUserId={}，operatorId={}", spaceId, userId, currentUserId);
        publishSpaceMemberChangedEvent(spaceId, userId);
    }

    /**
     * 创建空间实时事件 Ticket。
     *
     * @param spaceId 空间标识
     * @return 空间实时事件 Ticket
     */
    @Override
    public SpaceEventTicketVO createSpaceEventTicket(Long spaceId) {
        Long currentUserId = LoginUserUtil.currentUserId();
        permissionValidator.ensureSpaceMember(spaceId, currentUserId);
        findSpace(spaceId);
        String ticket = UUID.randomUUID().toString().replace("-", "");
        Integer expiresIn = systemSettingService.getCollabTicketExpireSeconds();
        String ticketValue = spaceId + SPACE_EVENT_TICKET_DELIMITER + currentUserId;
        redisUtil.set(RedisKeyConstants.spaceEventTicket(ticket), ticketValue, Duration.ofSeconds(expiresIn));
        return new SpaceEventTicketVO(ticket, expiresIn);
    }

    /**
     * 消费空间实时事件 Ticket。
     *
     * @param ticket Ticket 值
     * @return Ticket 消费结果
     */
    @Override
    public SpaceEventTicketConsumeVO consumeSpaceEventTicket(String ticket) {
        String ticketValue = redisUtil.getAndDeleteString(RedisKeyConstants.spaceEventTicket(ticket));
        if (!StringUtils.hasText(ticketValue)) {
            return new SpaceEventTicketConsumeVO(false, null, null, null, null);
        }
        String[] parts = ticketValue.split(SPACE_EVENT_TICKET_DELIMITER, 2);
        if (parts.length != 2) {
            return new SpaceEventTicketConsumeVO(false, null, null, null, null);
        }

        Long spaceId = Long.valueOf(parts[0]);
        Long userId = Long.valueOf(parts[1]);
        if (permissionValidator.findSpaceMember(spaceId, userId) == null) {
            return new SpaceEventTicketConsumeVO(false, null, null, null, null);
        }

        User user = userMapper.selectById(userId);
        return new SpaceEventTicketConsumeVO(
                true,
                spaceId,
                userId,
                resolveNickname(user),
                AvatarUrlUtil.proxyUrl(user)
        );
    }

    /**
     * 当前用户退出团队空间。
     *
     * @param spaceId 空间标识
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void leaveSpace(Long spaceId) {
        Long currentUserId = LoginUserUtil.currentUserId();
        Space space = findSpace(spaceId);
        permissionValidator.ensureSpaceMember(spaceId, currentUserId);
        if (SpaceType.PERSONAL.equals(space.getType())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "个人空间不支持退出");
        }
        if (space.getOwnerUserId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "空间所有者不可直接退出团队");
        }
        spaceMemberPresenceService.clearMemberOnline(spaceId, currentUserId);
        spaceMemberMapper.delete(Wrappers.<SpaceMember>lambdaQuery()
                .eq(SpaceMember::getSpaceId, spaceId)
                .eq(SpaceMember::getUserId, currentUserId));
        log.info("用户退出团队空间完成，spaceId={}，userId={}", spaceId, currentUserId);
        publishSpaceMemberChangedEvent(spaceId, currentUserId);
    }

    /**
     * 发布空间成员变更事件。
     *
     * @param spaceId 空间标识
     * @param userId 用户标识
     */
    private void publishSpaceMemberChangedEvent(Long spaceId, Long userId) {
        spacePermissionCacheService.evict(spaceId, userId);
        spaceRealtimeEventService.publish(spaceId, SpaceRealtimeEventType.SPACE_MEMBER_CHANGED,
                Map.of("userId", userId));
    }

    /**
     * 发布成员在线状态变更事件。
     *
     * @param spaceId 空间标识
     * @param userId 用户标识
     * @param type 事件类型
     */
    private void publishMemberPresenceEvent(Long spaceId, Long userId, SpaceRealtimeEventType type) {
        spaceRealtimeEventService.publish(spaceId, type, Map.of("userId", userId));
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
     * 构造空间排序规则，个人空间固定展示在团队空间之前。
     *
     * @return 空间排序器
     */
    private Comparator<Space> spaceComparator() {
        return Comparator.comparing((Space space) -> !SpaceType.PERSONAL.equals(space.getType()))
                .thenComparing(Space::getGmtCreate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Space::getId);
    }

    /**
     * 根据角色编码查询角色。
     *
     * @param roleCode 角色编码
     * @return 角色实体
     */
    private Role findRoleByCode(String roleCode) {
        Role role = roleMapper.selectOne(Wrappers.<Role>lambdaQuery().eq(Role::getCode, roleCode));
        if (role == null) {
            throw new ResourceNotFoundException("系统角色不存在: " + roleCode);
        }
        return role;
    }

    /**
     * 统计空间成员数量。
     *
     * @param spaceId 空间标识
     * @return 成员数量
     */
    private Long memberCount(Long spaceId) {
        return spaceMemberMapper.selectCount(Wrappers.<SpaceMember>lambdaQuery()
                .eq(SpaceMember::getSpaceId, spaceId));
    }

    /**
     * 统计当前用户在指定空间的未读通知数量。
     *
     * @param spaceId 空间标识
     * @param userId 用户标识
     * @return 未读通知数量
     */
    private Long unreadCount(Long spaceId, Long userId) {
        return notificationMapper.selectCount(Wrappers.<Notification>lambdaQuery()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getSpaceId, spaceId)
                .eq(Notification::getIsRead, false));
    }

    /**
     * 转换空间视图对象。
     *
     * @param space 空间实体
     * @param currentUserId 当前用户标识
     * @return 空间视图对象
     */
    private SpaceVO toSpaceVO(Space space, Long currentUserId) {
        return toSpaceVO(space, memberCount(space.getId()), unreadCount(space.getId(), currentUserId));
    }

    /**
     * 使用已预加载的统计值转换空间视图对象。
     *
     * @param space 空间实体
     * @param memberCountValue 成员数量
     * @param unreadCountValue 未读通知数量
     * @return 空间视图对象
     */
    private SpaceVO toSpaceVO(Space space, Long memberCountValue, Long unreadCountValue) {
        return new SpaceVO(
                space.getId(),
                space.getName(),
                space.getType(),
                space.getOwnerUserId(),
                memberCountValue,
                unreadCountValue,
                Boolean.TRUE.equals(space.getJoinApprovalRequired()),
                !Boolean.FALSE.equals(space.getMemberInviteEnabled()),
                space.getGmtCreate()
        );
    }

    /**
     * 转换空间成员视图对象。
     *
     * @param member 空间成员实体
     * @return 空间成员视图对象
     */
    private SpaceMemberVO toSpaceMemberVO(SpaceMember member) {
        User user = userMapper.selectById(member.getUserId());
        Role role = roleMapper.selectById(member.getRoleId());
        String username = user == null ? "" : user.getUsername();
        String nickname = user == null ? "" : resolveNickname(user);
        String email = user == null ? "" : user.getEmail();
        String avatarUrl = AvatarUrlUtil.proxyUrl(user);
        String roleCode = role == null ? "" : role.getCode();
        String roleName = role == null ? "" : role.getName();
        return new SpaceMemberVO(
                member.getSpaceId(),
                member.getUserId(),
                username,
                nickname,
                email,
                avatarUrl,
                member.getRoleId(),
                roleCode,
                roleName,
                member.getGmtJoined(),
                spaceMemberPresenceService.isOnline(member.getSpaceId(), member.getUserId())
        );
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
