package com.notaskflow.controller;

import com.notaskflow.common.ApiResponse;
import com.notaskflow.domain.dto.request.ForgotPasswordRequest;
import com.notaskflow.domain.dto.request.LoginRequest;
import com.notaskflow.domain.dto.request.RegisterRequest;
import com.notaskflow.domain.dto.request.ResetPasswordRequest;
import com.notaskflow.domain.dto.request.SendRegisterEmailCodeRequest;
import com.notaskflow.domain.dto.request.VerifyResetCodeRequest;
import com.notaskflow.domain.dto.response.LoginResponse;
import com.notaskflow.domain.dto.response.PasswordResetVerifyResponse;
import com.notaskflow.domain.vo.AuthSystemSettingVO;
import com.notaskflow.domain.vo.UserProfileVO;
import com.notaskflow.service.AuthService;
import com.notaskflow.service.SystemSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器。
 *
 * @author LIN
 */
@Tag(name = "用户与认证")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    private final SystemSettingService systemSettingService;

    /**
     * 获取认证相关系统设置。
     *
     * @return 认证系统设置
     */
    @Operation(summary = "获取认证相关系统设置")
    @GetMapping("/settings")
    public ApiResponse<AuthSystemSettingVO> authSettings() {
        return ApiResponse.success(systemSettingService.getAuthSettings());
    }

    /**
     * 发送注册邮箱验证码。
     *
     * @param request 发送注册邮箱验证码请求
     * @return 空响应
     */
    @Operation(summary = "发送注册邮箱验证码")
    @PostMapping("/register/send-email-code")
    public ApiResponse<Void> sendRegisterEmailCode(@Valid @RequestBody SendRegisterEmailCodeRequest request) {
        authService.sendRegisterEmailCode(request);
        return ApiResponse.success();
    }

    /**
     * 用户注册。
     *
     * @param request 注册请求
     * @return 用户资料
     */
    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public ApiResponse<UserProfileVO> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request));
    }

    /**
     * 发送重置密码验证码。
     *
     * @param request 忘记密码请求
     * @return 空响应
     */
    @Operation(summary = "发送重置密码验证码")
    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ApiResponse.success();
    }

    /**
     * 校验找回密码验证码。
     *
     * @param request 校验验证码请求
     * @return 重置凭证
     */
    @Operation(summary = "校验找回密码验证码")
    @PostMapping("/verify-reset-code")
    public ApiResponse<PasswordResetVerifyResponse> verifyResetCode(@Valid @RequestBody VerifyResetCodeRequest request) {
        return ApiResponse.success(authService.verifyResetCode(request));
    }

    /**
     * 重置密码。
     *
     * @param request 重置密码请求
     * @return 空响应
     */
    @Operation(summary = "重置密码")
    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.success();
    }

    /**
     * 用户登录。
     *
     * @param request 登录请求
     * @return 登录响应
     */
    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    /**
     * 用户登出。
     *
     * @return 空响应
     */
    @Operation(summary = "用户登出")
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        authService.logout();
        return ApiResponse.success();
    }

    /**
     * 刷新令牌信息。
     *
     * @return 登录响应
     */
    @Operation(summary = "刷新令牌")
    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refresh() {
        return ApiResponse.success(authService.refresh());
    }
}
