package com.notaskflow.controller;

import com.notaskflow.common.ApiResponse;
import com.notaskflow.common.PageResponse;
import com.notaskflow.domain.query.AdminLoginLogQuery;
import com.notaskflow.domain.query.AdminOperationLogQuery;
import com.notaskflow.domain.query.EventFailLogQuery;
import com.notaskflow.domain.vo.AdminLoginLogVO;
import com.notaskflow.domain.vo.AdminOperationLogVO;
import com.notaskflow.domain.vo.EventFailLogVO;
import com.notaskflow.service.AdminLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端日志控制器。
 *
 * @author LIN
 */
@Tag(name = "管理端日志")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/logs")
public class AdminLogController {

    private final AdminLogService adminLogService;

    /**
     * 分页查询登录日志。
     *
     * @param query 查询条件
     * @return 登录日志分页
     */
    @Operation(summary = "分页查询登录日志")
    @GetMapping("/login")
    public ApiResponse<PageResponse<AdminLoginLogVO>> loginLogs(@Valid @ModelAttribute AdminLoginLogQuery query) {
        return ApiResponse.success(adminLogService.loginLogs(query));
    }

    /**
     * 分页查询管理操作日志。
     *
     * @param query 查询条件
     * @return 管理操作日志分页
     */
    @Operation(summary = "分页查询管理操作日志")
    @GetMapping("/operations")
    public ApiResponse<PageResponse<AdminOperationLogVO>> operationLogs(
            @Valid @ModelAttribute AdminOperationLogQuery query) {
        return ApiResponse.success(adminLogService.operationLogs(query));
    }

    /**
     * 分页查询系统事件失败日志。
     *
     * @param query 查询条件
     * @return 系统事件失败日志分页
     */
    @Operation(summary = "分页查询系统事件失败日志")
    @GetMapping("/system")
    public ApiResponse<PageResponse<EventFailLogVO>> systemLogs(@Valid @ModelAttribute EventFailLogQuery query) {
        return ApiResponse.success(adminLogService.systemLogs(query));
    }
}
