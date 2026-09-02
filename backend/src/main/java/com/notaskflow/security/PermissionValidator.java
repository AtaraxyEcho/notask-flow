package com.notaskflow.security;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.notaskflow.common.enums.RoleCode;
import com.notaskflow.domain.dto.cache.SpacePermissionCachePayload;
import com.notaskflow.domain.entity.Note;
import com.notaskflow.domain.entity.Space;
import com.notaskflow.domain.entity.SpaceMember;
import com.notaskflow.domain.entity.Task;
import com.notaskflow.domain.entity.TaskMember;
import com.notaskflow.exception.AccessDeniedException;
import com.notaskflow.exception.ResourceNotFoundException;
import com.notaskflow.mapper.SpaceMapper;
import com.notaskflow.mapper.SpaceMemberMapper;
import com.notaskflow.service.SpacePermissionCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 资源级权限校验组件。
 *
 * @author LIN
 */
@Component
@RequiredArgsConstructor
public class PermissionValidator {

    private final SpaceMemberMapper spaceMemberMapper;

    private final SpaceMapper spaceMapper;

    private final SpacePermissionCacheService spacePermissionCacheService;

    /**
     * 校验用户是否为空间成员。
     *
     * @param spaceId 空间标识
     * @param userId 用户标识
     */
    public void ensureSpaceMember(Long spaceId, Long userId) {
        SpacePermissionCachePayload payload = spacePermissionCacheService.getPermission(spaceId, userId);
        if (payload == null) {
            throw new AccessDeniedException("用户不是该空间成员");
        }
    }

    /**
     * 校验用户是否为空间所有者。
     *
     * @param spaceId 空间标识
     * @param userId 用户标识
     */
    public void ensureSpaceOwner(Long spaceId, Long userId) {
        Space space = spaceMapper.selectById(spaceId);
        if (space == null || !userId.equals(space.getOwnerUserId())) {
            throw new AccessDeniedException("仅空间所有者可执行该操作");
        }
    }

    /**
     * 校验用户是否为空间管理员或所有者。
     *
     * @param spaceId 空间标识
     * @param userId 用户标识
     */
    public void ensureSpaceAdminOrOwner(Long spaceId, Long userId) {
        SpacePermissionCachePayload payload = spacePermissionCacheService.getPermission(spaceId, userId);
        if (payload == null) {
            throw new AccessDeniedException("用户不是该空间成员");
        }
        if (payload.getRoleCode() == null) {
            throw new AccessDeniedException("空间角色不存在");
        }
        boolean allowed = RoleCode.SPACE_OWNER.getCode().equals(payload.getRoleCode())
                || RoleCode.SPACE_ADMIN.getCode().equals(payload.getRoleCode());
        if (!allowed) {
            throw new AccessDeniedException("仅空间管理员或所有者可执行该操作");
        }
    }

    /**
     * 校验用户是否为可编辑空间协作者。
     *
     * @param spaceId 空间标识
     * @param userId 用户标识
     */
    public void ensureSpaceCollaborator(Long spaceId, Long userId) {
        SpacePermissionCachePayload payload = spacePermissionCacheService.getPermission(spaceId, userId);
        if (payload == null) {
            throw new AccessDeniedException("用户不是该空间成员");
        }
        if (payload.getRoleCode() == null) {
            throw new AccessDeniedException("空间角色不存在");
        }
        if (RoleCode.SPACE_GUEST.getCode().equals(payload.getRoleCode())) {
            throw new AccessDeniedException("只读访客不允许执行写操作");
        }
    }

    /**
     * 校验用户是否为任务创建人或空间管理员。
     *
     * @param task 任务实体
     * @param userId 用户标识
     */
    public void ensureTaskCreatorOrAdmin(Task task, Long userId) {
        if (task == null) {
            throw new ResourceNotFoundException("任务不存在");
        }
        if (userId.equals(task.getCreatorId())) {
            return;
        }
        ensureSpaceAdminOrOwner(task.getSpaceId(), userId);
    }

    /**
     * 校验用户是否为笔记创建人或空间管理员。
     *
     * @param note 笔记实体
     * @param userId 用户标识
     */
    public void ensureNoteOwnerOrAdmin(Note note, Long userId) {
        if (note == null) {
            throw new ResourceNotFoundException("笔记不存在");
        }
        if (userId.equals(note.getUserId())) {
            return;
        }
        ensureSpaceAdminOrOwner(note.getSpaceId(), userId);
    }

    /**
     * 校验用户是否为任务成员本人。
     *
     * @param member 任务成员实体
     * @param userId 用户标识
     */
    public void ensureTaskMemberOwner(TaskMember member, Long userId) {
        if (member == null) {
            throw new ResourceNotFoundException("任务成员不存在");
        }
        if (!userId.equals(member.getUserId())) {
            throw new AccessDeniedException("只能操作自己的任务职责");
        }
    }

    /**
     * 查询空间成员关系。
     *
     * @param spaceId 空间标识
     * @param userId 用户标识
     * @return 空间成员关系
     */
    public SpaceMember findSpaceMember(Long spaceId, Long userId) {
        return spaceMemberMapper.selectOne(Wrappers.<SpaceMember>lambdaQuery()
                .eq(SpaceMember::getSpaceId, spaceId)
                .eq(SpaceMember::getUserId, userId));
    }
}
