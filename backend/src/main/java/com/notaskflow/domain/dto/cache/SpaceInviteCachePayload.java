package com.notaskflow.domain.dto.cache;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Redis 中保存的团队邀请码负载。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpaceInviteCachePayload {

    private Long spaceId;

    private String roleCode;

    private Long inviterUserId;
}
