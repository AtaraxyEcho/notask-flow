package com.notaskflow.controller;

import com.notaskflow.common.ApiResponse;
import com.notaskflow.domain.dto.request.LoginRequest;
import com.notaskflow.domain.dto.response.LoginResponse;
import com.notaskflow.domain.vo.AdminMeVO;
import com.notaskflow.service.AdminAuthService;
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
 * 管理端认证控制器。
 *
 * @author LIN
 */
@Tag(name = "管理端认证")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/auth")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    /**
     * Administrator 登录管理平台。
     *
     * @param request 登录请求
     * @return 登录响应
     */
    @Operation(summary = "管理端登录")
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(adminAuthService.login(request));
    }

    /**
     * 管理端登出。
     *
     * @return 空响应
     */
    @Operation(summary = "管理端登出")
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        adminAuthService.logout();
        return ApiResponse.success();
    }

    /**
     * 获取当前管理端登录信息。
     *
     * @return 管理端登录信息
     */
    @Operation(summary = "获取当前管理端登录信息")
    @GetMapping("/me")
    public ApiResponse<AdminMeVO> me() {
        return ApiResponse.success(adminAuthService.me());
    }
}
