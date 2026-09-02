package com.notaskflow.domain.vo;

import com.notaskflow.common.enums.ClientType;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录会话视图对象，记录 Redis 会话中心的客户端元信息。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginSessionVO {

    private String sessionId;

    private Long userId;

    private String username;

    private ClientType clientType;

    private String deviceId;

    private String deviceName;

    private String appVersion;

    private String ip;

    private String userAgent;

    private LocalDateTime loginTime;

    private LocalDateTime lastActiveTime;

    private Long expireSeconds;
}
