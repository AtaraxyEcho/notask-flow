package com.notaskflow.controller;

import com.notaskflow.common.ApiResponse;
import com.notaskflow.common.PageResponse;
import com.notaskflow.domain.dto.request.AdminUserPasswordResetRequest;
import com.notaskflow.domain.dto.request.AdminUserStatusUpdateRequest;
import com.notaskflow.domain.query.AdminUserQuery;
import com.notaskflow.domain.vo.AdminUserStatsVO;
import com.notaskflow.domain.vo.AdminUserVO;
import com.notaskflow.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端用户管理控制器。
 *
 * @author LIN
 */
@Tag(name = "管理端用户管理")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    /**
     * 分页查询用户。
     *
     * @param query 查询条件
     * @return 用户分页
     */
    @Operation(summary = "分页查询用户")
    @GetMapping
    public ApiResponse<PageResponse<AdminUserVO>> page(@Valid AdminUserQuery query) {
        return ApiResponse.success(adminUserService.page(query));
    }

    /**
     * 查询用户统计。
     *
     * @return 用户统计
     */
    @Operation(summary = "查询用户统计")
    @GetMapping("/stats")
    public ApiResponse<AdminUserStatsVO> stats() {
        return ApiResponse.success(adminUserService.stats());
    }

    /**
     * 更新用户状态。
     *
     * @param userId 用户标识
     * @param request 状态更新请求
     * @return 用户视图对象
     */
    @Operation(summary = "更新用户状态")
    @PutMapping("/{userId}/status")
    public ApiResponse<AdminUserVO> updateStatus(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUserStatusUpdateRequest request) {
        return ApiResponse.success(adminUserService.updateStatus(userId, request.getStatus()));
    }

    /**
     * 重置用户密码。
     *
     * @param userId 用户标识
     * @param request 密码重置请求
     * @return 空响应
     */
    @Operation(summary = "重置用户密码")
    @PutMapping("/{userId}/password")
    public ApiResponse<Void> resetPassword(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUserPasswordResetRequest request) {
        adminUserService.resetPassword(userId, request);
        return ApiResponse.success();
    }

    /**
     * 删除用户。
     *
     * @param userId 用户标识
     * @return 空响应
     */
    @Operation(summary = "删除用户")
    @DeleteMapping("/{userId}")
    public ApiResponse<Void> deleteUser(@PathVariable Long userId) {
        adminUserService.deleteUser(userId);
        return ApiResponse.success();
    }
}
