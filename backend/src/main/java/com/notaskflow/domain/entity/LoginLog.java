package com.notaskflow.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.notaskflow.common.enums.ClientType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 登录日志实体。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("nt_login_log")
public class LoginLog extends BaseEntity {

    private Long userId;

    private String account;

    private ClientType clientType;

    private String deviceId;

    private String ipAddress;

    private String userAgent;

    private Boolean success;

    private String failReason;
}
