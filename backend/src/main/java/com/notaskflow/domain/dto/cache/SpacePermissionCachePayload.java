package com.notaskflow.domain.dto.cache;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Redis 中保存的空间权限缓存负载。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpacePermissionCachePayload {

    private Long spaceId;

    private Long userId;

    private Long memberId;

    private Long roleId;

    private String roleCode;

    private List<String> permissionCodes;
}
