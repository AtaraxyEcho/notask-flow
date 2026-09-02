package com.notaskflow.domain.vo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 团队邀请码预览视图对象。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpaceInvitePreviewVO {

    private String code;

    private Long spaceId;

    private String spaceName;

    private String ownerUsername;

    private String roleCode;

    private Long memberCount;

    private LocalDateTime expiresAt;
}
