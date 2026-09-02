package com.notaskflow.domain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 验证找回密码验证码响应。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetVerifyResponse {

    private String resetToken;

    private Long expireSeconds;
}
