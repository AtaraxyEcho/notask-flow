package com.notaskflow.controller;

import com.notaskflow.common.ApiResponse;
import com.notaskflow.domain.vo.LoginSessionVO;
import com.notaskflow.service.LoginSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端会话控制器。
 *
 * @author LIN
 */
@Tag(name = "管理端会话")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
public class AdminSessionController {

    private final LoginSessionService loginSessionService;

    /**
     * 查询活跃登录会话。
     *
     * @return 活跃登录会话
     */
    @Operation(summary = "查询活跃登录会话")
    @GetMapping("/sessions")
    public ApiResponse<List<LoginSessionVO>> listSessions() {
        return ApiResponse.success(loginSessionService.listActiveSessions());
    }

    /**
     * 踢出指定会话。
     *
     * @param sessionId 会话标识
     * @return 空响应
     */
    @Operation(summary = "踢出指定会话")
    @DeleteMapping("/sessions/{sessionId}")
    public ApiResponse<Void> revokeSession(@PathVariable String sessionId) {
        loginSessionService.revokeSession(sessionId);
        return ApiResponse.success();
    }

    /**
     * 踢出指定用户的全部会话。
     *
     * @param userId 用户标识
     * @return 空响应
     */
    @Operation(summary = "踢出指定用户全部会话")
    @DeleteMapping("/users/{userId}/sessions")
    public ApiResponse<Void> revokeUserSessions(@PathVariable Long userId) {
        loginSessionService.revokeUserSessions(userId);
        return ApiResponse.success();
    }
}
