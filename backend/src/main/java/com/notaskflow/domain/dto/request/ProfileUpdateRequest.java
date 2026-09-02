package com.notaskflow.domain.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户资料更新请求。
 *
 * @author LIN
 */
@Data
public class ProfileUpdateRequest {

    @Size(max = 50, message = "昵称长度不能超过50")
    private String nickname;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100")
    private String email;

    @Size(max = 255, message = "头像地址长度不能超过255")
    private String avatarUrl;
}
