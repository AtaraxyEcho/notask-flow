package com.notaskflow.domain.vo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 空间成员视图对象。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpaceMemberVO {

    private Long spaceId;

    private Long userId;

    private String username;

    private String nickname;

    private String email;

    private String avatarUrl;

    private Long roleId;

    private String roleCode;

    private String roleName;

    private LocalDateTime gmtJoined;

    private Boolean online;
}
