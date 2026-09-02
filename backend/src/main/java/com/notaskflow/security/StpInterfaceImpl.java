package com.notaskflow.security;

import cn.dev33.satoken.stp.StpInterface;
import com.notaskflow.common.RequestContext;
import com.notaskflow.domain.dto.cache.SpacePermissionCachePayload;
import com.notaskflow.service.SpacePermissionCacheService;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Sa-Token 权限接口实现，按当前空间实时加载权限。
 *
 * @author LIN
 */
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final SpacePermissionCacheService spacePermissionCacheService;

    /**
     * 获取当前用户在当前空间下的权限列表。
     *
     * @param loginId 登录用户标识
     * @param loginType 登录类型
     * @return 权限码列表
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        Long spaceId = RequestContext.getCurrentSpaceId();
        if (spaceId == null || loginId == null) {
            return Collections.emptyList();
        }
        SpacePermissionCachePayload payload =
                spacePermissionCacheService.getPermission(spaceId, Long.valueOf(String.valueOf(loginId)));
        if (payload == null || payload.getPermissionCodes() == null) {
            return Collections.emptyList();
        }
        return payload.getPermissionCodes();
    }

    /**
     * 获取当前用户在当前空间下的角色列表。
     *
     * @param loginId 登录用户标识
     * @param loginType 登录类型
     * @return 角色编码列表
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Long spaceId = RequestContext.getCurrentSpaceId();
        if (spaceId == null || loginId == null) {
            return Collections.emptyList();
        }
        SpacePermissionCachePayload payload =
                spacePermissionCacheService.getPermission(spaceId, Long.valueOf(String.valueOf(loginId)));
        if (payload == null || payload.getRoleCode() == null) {
            return Collections.emptyList();
        }
        return List.of(payload.getRoleCode());
    }
}
