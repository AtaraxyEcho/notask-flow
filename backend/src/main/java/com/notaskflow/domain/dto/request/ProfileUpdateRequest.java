package com.notaskflow.domain.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户资料更新请求。
 *
 * <p>头像不允许通过资料接口直接指定地址，必须通过 {@code POST /api/v1/user/avatar} 上传，
 * 避免客户端传入任意对象存储路径造成越权读取。</p>
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
}
