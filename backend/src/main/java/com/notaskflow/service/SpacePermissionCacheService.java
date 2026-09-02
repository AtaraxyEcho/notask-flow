package com.notaskflow.service;

import com.notaskflow.domain.dto.cache.SpacePermissionCachePayload;

/**
 * 空间权限缓存服务。
 *
 * @author LIN
 */
public interface SpacePermissionCacheService {

    /**
     * 查询用户在空间中的权限缓存。
     *
     * @param spaceId 空间标识
     * @param userId 用户标识
     * @return 权限缓存负载，不是空间成员时返回 null
     */
    SpacePermissionCachePayload getPermission(Long spaceId, Long userId);

    /**
     * 清理用户在空间中的权限缓存。
     *
     * @param spaceId 空间标识
     * @param userId 用户标识
     */
    void evict(Long spaceId, Long userId);
}
