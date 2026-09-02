package com.notaskflow.domain.dto.request;

import com.notaskflow.common.enums.ClientType;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户登录请求。
 *
 * @author LIN
 */
@Data
public class LoginRequest {

    @NotBlank(message = "账号不能为空")
    private String account;

    @NotBlank(message = "密码不能为空")
    private String password;

    private ClientType clientType;

    private String deviceId;

    private String deviceName;

    private String appVersion;
}
