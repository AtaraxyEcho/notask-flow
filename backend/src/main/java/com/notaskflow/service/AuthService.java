package com.notaskflow.service;

import com.notaskflow.domain.dto.request.ForgotPasswordRequest;
import com.notaskflow.domain.dto.request.LoginRequest;
import com.notaskflow.domain.dto.request.RegisterRequest;
import com.notaskflow.domain.dto.request.ResetPasswordRequest;
import com.notaskflow.domain.dto.request.SendRegisterEmailCodeRequest;
import com.notaskflow.domain.dto.request.VerifyResetCodeRequest;
import com.notaskflow.domain.dto.response.LoginResponse;
import com.notaskflow.domain.dto.response.PasswordResetVerifyResponse;
import com.notaskflow.domain.vo.UserProfileVO;

/**
 * 认证服务接口。
 *
 * @author LIN
 */
public interface AuthService {

    /**
     * 发送注册邮箱验证码。
     *
     * @param request 发送注册邮箱验证码请求
     */
    void sendRegisterEmailCode(SendRegisterEmailCodeRequest request);

    /**
     * 注册用户并创建个人空间。
     *
     * @param request 注册请求
     * @return 用户资料
     */
    UserProfileVO register(RegisterRequest request);

    /**
     * 发送找回密码验证码。
     *
     * @param request 忘记密码请求
     */
    void forgotPassword(ForgotPasswordRequest request);

    /**
     * 校验找回密码验证码。
     *
     * @param request 校验验证码请求
     * @return 重置凭证
     */
    PasswordResetVerifyResponse verifyResetCode(VerifyResetCodeRequest request);

    /**
     * 使用重置凭证设置新密码。
     *
     * @param request 重置密码请求
     */
    void resetPassword(ResetPasswordRequest request);

    /**
     * 登录并返回令牌信息。
     *
     * @param request 登录请求
     * @return 登录响应
     */
    LoginResponse login(LoginRequest request);

    /**
     * 登出当前用户。
     */
    void logout();

    /**
     * 刷新当前令牌信息。
     *
     * @return 登录响应
     */
    LoginResponse refresh();
}
