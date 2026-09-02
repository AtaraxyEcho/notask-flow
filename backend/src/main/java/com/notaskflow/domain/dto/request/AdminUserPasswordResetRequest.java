package com.notaskflow.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理端重置用户密码请求。
 *
 * @author LIN
 */
@Data
public class AdminUserPasswordResetRequest {

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 64, message = "新密码长度必须在6到64位之间")
    private String newPassword;
}
