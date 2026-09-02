package com.notaskflow.controller;

import com.notaskflow.common.ApiResponse;
import com.notaskflow.common.PageQuery;
import com.notaskflow.common.PageResponse;
import com.notaskflow.domain.dto.request.AdminSystemNotificationRequest;
import com.notaskflow.domain.vo.NotificationVO;
import com.notaskflow.service.AdminSystemNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端系统通知控制器。
 *
 * @author LIN
 */
@Tag(name = "管理端系统通知")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/system-notifications")
public class AdminSystemNotificationController {

    private final AdminSystemNotificationService adminSystemNotificationService;

    /**
     * 向全部普通用户发送系统通知。
     *
     * @param request 系统通知请求
     * @return 空响应
     */
    @Operation(summary = "发送全员系统通知")
    @PostMapping
    public ApiResponse<Void> send(@Valid @RequestBody AdminSystemNotificationRequest request) {
        adminSystemNotificationService.sendToAllUsers(request);
        return ApiResponse.success();
    }

    /**
     * 查询系统通知历史。
     *
     * @param query 分页查询
     * @return 系统通知历史
     */
    @Operation(summary = "查询系统通知历史")
    @GetMapping("/history")
    public ApiResponse<PageResponse<NotificationVO>> history(@Valid @ModelAttribute PageQuery query) {
        return ApiResponse.success(adminSystemNotificationService.history(query));
    }
}
