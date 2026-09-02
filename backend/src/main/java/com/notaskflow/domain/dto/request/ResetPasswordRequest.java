package com.notaskflow.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 重置密码请求。
 *
 * @author LIN
 */
@Data
public class ResetPasswordRequest {

    @NotBlank(message = "重置凭证不能为空")
    @Size(max = 128, message = "重置凭证长度不能超过128")
    private String resetToken;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 64, message = "新密码长度必须在8到64之间")
    private String newPassword;

    @NotBlank(message = "确认密码不能为空")
    @Size(min = 8, max = 64, message = "确认密码长度必须在8到64之间")
    private String confirmPassword;
}
