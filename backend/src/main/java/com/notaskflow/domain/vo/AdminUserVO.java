package com.notaskflow.domain.vo;

import com.notaskflow.common.enums.UserStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理端用户视图对象。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserVO {

    private Long id;

    private String username;

    private String nickname;

    private String email;

    private String avatarUrl;

    private UserStatus status;

    private Boolean online;

    private LocalDateTime gmtCreate;

    private LocalDateTime gmtModified;
}
