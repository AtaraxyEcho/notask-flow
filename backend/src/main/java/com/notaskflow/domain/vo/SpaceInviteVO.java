package com.notaskflow.domain.vo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 团队邀请码视图对象。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpaceInviteVO {

    private String code;

    private Long spaceId;

    private String roleCode;

    private LocalDateTime expiresAt;
}
