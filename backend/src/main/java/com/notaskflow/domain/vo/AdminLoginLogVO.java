package com.notaskflow.domain.vo;

import com.notaskflow.common.enums.ClientType;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理端登录日志视图对象。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminLoginLogVO {

    private Long id;

    private Long userId;

    private String account;

    private ClientType clientType;

    private String deviceId;

    private String ipAddress;

    private String userAgent;

    private Boolean success;

    private String failReason;

    private LocalDateTime gmtCreate;
}
