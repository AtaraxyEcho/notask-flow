package com.notaskflow.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.notaskflow.common.constant.RedisKeyConstants;
import com.notaskflow.domain.dto.cache.SpacePermissionCachePayload;
import com.notaskflow.domain.entity.Permission;
import com.notaskflow.domain.entity.Role;
import com.notaskflow.domain.entity.RolePermission;
import com.notaskflow.domain.entity.SpaceMember;
import com.notaskflow.mapper.PermissionMapper;
import com.notaskflow.mapper.RoleMapper;
import com.notaskflow.mapper.RolePermissionMapper;
import com.notaskflow.mapper.SpaceMemberMapper;
import com.notaskflow.service.SpacePermissionCacheService;
import com.notaskflow.utils.RedisUtil;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 空间权限缓存服务实现。
 *
 * @author LIN
 */
@Service
@RequiredArgsConstructor
public class SpacePermissionCacheServiceImpl implements SpacePermissionCacheService {

    private static final Duration PERMISSION_CACHE_TTL = Duration.ofMinutes(5);

    private final SpaceMemberMapper spaceMemberMapper;

    private final RoleMapper roleMapper;

    private final RolePermissionMapper rolePermissionMapper;

    private final PermissionMapper permissionMapper;

    private final RedisUtil redisUtil;

    /**
     * 查询用户在空间中的权限缓存。
     *
     * @param spaceId 空间标识
     * @param userId 用户标识
     * @return 权限缓存负载，不是空间成员时返回 null
     */
    @Override
    public SpacePermissionCachePayload getPermission(Long spaceId, Long userId) {
        if (spaceId == null || userId == null) {
            return null;
        }
        String cacheKey = RedisKeyConstants.spacePermission(spaceId, userId);
        SpacePermissionCachePayload cachedPayload =
                redisUtil.getObject(cacheKey, SpacePermissionCachePayload.class);
        if (cachedPayload != null) {
            return cachedPayload;
        }

        SpaceMember member = spaceMemberMapper.selectOne(Wrappers.<SpaceMember>lambdaQuery()
                .eq(SpaceMember::getSpaceId, spaceId)
                .eq(SpaceMember::getUserId, userId));
        if (member == null) {
            return null;
        }

        Role role = roleMapper.selectById(member.getRoleId());
        List<String> permissionCodes = findPermissionCodes(member.getRoleId());
        SpacePermissionCachePayload payload = new SpacePermissionCachePayload(
                spaceId,
                userId,
                member.getId(),
                member.getRoleId(),
                role == null ? null : role.getCode(),
                permissionCodes
        );
        redisUtil.set(cacheKey, payload, PERMISSION_CACHE_TTL);
        return payload;
    }

    /**
     * 清理用户在空间中的权限缓存。
     *
     * @param spaceId 空间标识
     * @param userId 用户标识
     */
    @Override
    public void evict(Long spaceId, Long userId) {
        if (spaceId == null || userId == null) {
            return;
        }
        redisUtil.delete(RedisKeyConstants.spacePermission(spaceId, userId));
    }

    private List<String> findPermissionCodes(Long roleId) {
        if (roleId == null) {
            return Collections.emptyList();
        }
        List<Long> permissionIds = rolePermissionMapper.selectList(Wrappers.<RolePermission>lambdaQuery()
                        .eq(RolePermission::getRoleId, roleId))
                .stream()
                .map(RolePermission::getPermissionId)
                .toList();
        if (permissionIds.isEmpty()) {
            return Collections.emptyList();
        }
        return permissionMapper.selectBatchIds(permissionIds)
                .stream()
                .map(Permission::getCode)
                .toList();
    }
}
