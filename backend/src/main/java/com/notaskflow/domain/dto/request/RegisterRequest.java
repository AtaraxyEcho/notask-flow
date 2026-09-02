package com.notaskflow.domain.dto.request;

import com.notaskflow.common.enums.RegisterTeamMode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户注册请求。
 *
 * @author LIN
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名长度不能超过50")
    private String username;

    @Size(max = 50, message = "昵称长度不能超过50")
    private String nickname;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100")
    private String email;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 64, message = "密码长度必须在8到64之间")
    private String password;

    @Size(max = 6, message = "邮箱验证码长度不能超过6")
    private String emailCode;

    private RegisterTeamMode teamMode = RegisterTeamMode.PERSONAL_ONLY;

    @Size(max = 100, message = "团队名称长度不能超过100")
    private String teamName;

    @Size(max = 100, message = "上级账号长度不能超过100")
    private String supervisorAccount;

    @Size(max = 500, message = "申请说明长度不能超过500")
    private String teamApplyRemark;

    @Size(max = 32, message = "邀请码长度不能超过32")
    private String inviteCode;
}
